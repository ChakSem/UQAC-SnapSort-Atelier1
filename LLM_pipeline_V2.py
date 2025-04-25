import shutil
import time
from langchain_ollama import ChatOllama
from langchain_core.messages import HumanMessage
from langchain.schema import AIMessage
import json
import re
from langchain_core.runnables import RunnableLambda
import base64
from PIL import Image
import io
import os
import pandas as pd
from tabulate import tabulate
import numpy as np
from Categories_TreeStructure import create_category_folders_from_csv
import torch
from transformers import CLIPProcessor, CLIPModel
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.cluster import DBSCAN

DIRECTORY = "test_data_copy"
MODEL = "gemma3"
DESTINATION_DIRECORY = "results"

def extract_json(response_text):
    """
    Extrait la portion JSON (délimitée par {}) de la réponse textuelle pour seulement avoir le dictionnaire
    et non le texte généré par l'ia.

    :param response_text: Texte contenant le JSON.
    :return: Dictionnaire Python obtenu à partir du JSON.
    """
    match = re.search(r'\{.*\}', response_text, re.DOTALL)
    if match:
        json_str = match.group()
        try:
            return json.loads(json_str)
        except Exception as e:
            print(f"Erreur lors du chargement du JSON : {e}")
            return None
    else:
        print("Aucun JSON trouvé dans la réponse.")
        return None

def encode_image(image_path, max_size=(512, 512), quality=80):
    image = Image.open(image_path)

    # Redimensionner l'image
    image.thumbnail(max_size)

    # Convertir en bytes avec compression
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=quality)

    # Encoder en Base64
    encoded_string = base64.b64encode(buffer.getvalue()).decode("utf-8")

    return encoded_string

class DataframeCompletion:
    def __init__(self, image_paths):
        self.image_paths = image_paths
        self.df = self.create_df()

    def create_df(self):
        image_list = []
        for path in self.image_paths:
            image = Image.open(path)
            image_name = os.path.basename(path)

            date_time, localisation = self.extract_exif_data(image)

            image_list.append((image_name, path, date_time, localisation))

        df = pd.DataFrame(image_list, columns=["image_name", "path", "date_time", "localisation"])

        return df

    def extract_exif_data(self, image):
            exifdata = image._getexif()
            date_time, localisation = None, None
            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = Image.ExifTags.TAGS.get(tag_id, tag_id)
                    if tag == "DateTime":
                        date_time = value
                    elif tag == "GPSInfo":
                        localisation = value
            else:
                print("Aucune donnée EXIF trouvée.")

            return date_time, localisation

    def get_dataframe(self):
        return self.df

    def save_to_csv(self, file_path="result.csv"):
        try:
            self.df.to_csv(file_path, index=False)
            print(f"DataFrame sauvegardé sous {file_path}")

        except Exception as e:
            print(f"Erreur lors de la sauvegarde : {e}")

    def add_keywords_to_df(self, keywords_output):
        if keywords_output:
            self.df.loc[self.df["image_name"].isin(keywords_output.keys()), "keywords"] = self.df["image_name"].map(keywords_output)
        else:
            print("Aucun mot clé fourni ! ")
        return self.df

    def add_categories_to_df(self, categories_output):
        if categories_output:
            # Inversion du dict : on associe une categorie a chaque image
            image_to_categories = {img: cat for cat, images in categories_output.items() for img in images}

            self.df.loc[self.df["image_name"].isin(image_to_categories.keys()), "categories"] = self.df["image_name"].map(image_to_categories)
        else:
            print("Aucune catégorisation trouvée !")

        return self.df

class ClusteringManager:
    def __init__(self, df, device="cpu"):
        self.df = df
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.clip_model = CLIPModel.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K").to(self.device)
        self.clip_processor = CLIPProcessor.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        
    def image_embedding(self, paths):
        images = []
        for path in paths:
            try:
                image = Image.open(path).convert("RGB")
                images.append(image)
            except Exception as e:
                print(f"Erreur lors du chargement de l'image {path}: {e}")
                continue

        if not images:
            return None

        # Prétraitement des images en batch
        image_inputs = self.clip_processor(images=images, return_tensors="pt", padding=True).to(self.device)

        with torch.no_grad():
            image_embeddings = self.clip_model.get_image_features(**image_inputs)

        # Normalisation
        image_embeddings = image_embeddings / image_embeddings.norm(p=2, dim=-1, keepdim=True)
        image_embeddings = image_embeddings.cpu().numpy()

        return image_embeddings

    def day_sorting(self):
        days = {}
        for index, row in self.df.iterrows():
            date = row["date_time"]
            if date:
                day = date.split(" ")[0]
                if day not in days:
                    days[day] = []
                days[day].append(row["path"])

        return days

    def days_embedding(self, days_dict):
        embeddings_dict = {}
        for day, images in days_dict.items():
            print(f"Génération des embeddings pour le jour: {day}")
            embeddings = self.image_embedding(images)
            
            if embeddings is None:
                continue
                
            embeddings_dict[day] = []
            for i, image in enumerate(images):
                if i < len(embeddings):  # Protection contre les indices hors limites
                    embeddings_dict[day].append({
                        'path': image,
                        'embedding': embeddings[i]
                    })
        return embeddings_dict

    def matrice_similarities(self, embeddings_dict):
        similarities_matrices = {}
        for day, image_list in embeddings_dict.items():
            paths = [image['path'] for image in image_list]
            embeddings = np.array([image['embedding'] for image in image_list])
            N = len(paths)
            similarities = np.zeros((N, N))

            for i in range(N):
                for j in range(N):
                    sim = np.dot(embeddings[i], embeddings[j])
                    similarities[i][j] = sim

            similarities_matrices[day] = ({'paths': paths, 'similarities': similarities})

        return similarities_matrices

    def cluster_from_similarity_matrices(self, similarities_matrices_dict, threshold=0.6):
        clusters_by_day = {}

        for day, data in similarities_matrices_dict.items():
            paths = data["paths"]
            sim_matrix = data["similarities"]

            # Ensure distance matrix has only non-negative values
            distance_matrix = 1 - sim_matrix
            distance_matrix = np.clip(distance_matrix, 0, None)  # Clip to ensure all values >= 0
            
            # DBSCAN with a métrique de distance pré-calculée
            model = DBSCAN(eps=1-threshold, min_samples=1, metric='precomputed')
            labels = model.fit_predict(distance_matrix)

            # Organization of clusters
            clusters = {}
            for path, label in zip(paths, labels):
                cluster_name = f"cluster_{label}"
                if cluster_name not in clusters:
                    clusters[cluster_name] = []
                clusters[cluster_name].append(path)

            clusters_by_day[day] = clusters

        return clusters_by_day
    
    def perform_day_based_clustering(self, threshold=0.6):
        print("Début du clustering par jour...")
        days_dict = self.day_sorting()
        embeddings_dict = self.days_embedding(days_dict)
        similarities_matrices = self.matrice_similarities(embeddings_dict)
        clusters = self.cluster_from_similarity_matrices(similarities_matrices, threshold)
        
        # Mise à jour du DataFrame avec les informations de cluster
        cluster_mapping = {}
        for day, day_clusters in clusters.items():
            for cluster_name, image_paths in day_clusters.items():
                for path in image_paths:
                    cluster_mapping[path] = cluster_name
        
        self.df['cluster'] = self.df['path'].map(cluster_mapping)
        
        return self.df, clusters

class LLMCall:
    def __init__(self, directory=DIRECTORY, allowed_extensions=None, model=MODEL):
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif"}
        self.allowed_extensions = allowed_extensions
        self.directory = directory
        self.model = model
        self.llm = ChatOllama(model=model)

        self.prompt_chain = RunnableLambda(self.prompt_func)

        self.image_paths = self.get_image_paths(directory)

        self.dataframe_manager = DataframeCompletion(self.image_paths)
        self.df = DataframeCompletion(self.image_paths).get_dataframe()

    def get_image_paths(self, directory):
        image_paths = [os.path.join(directory, filename) for filename in os.listdir(directory) if os.path.splitext(filename)[1].lower() in self.allowed_extensions]
        return image_paths

    def prompt_func(self, data):
        type_ = data["type"]
        text = data["text"]
        content_parts = []

        if type_ == "keywords":
            image = data["image"]
            image_part = {
                "type": "image_url",
                "image_url": f"data:image/jpeg;base64,{image}",
            }
            content_parts.append(image_part)

        text_part = {"type": "text", "text": text}
        content_parts.append(text_part)
        human_message = HumanMessage(content=content_parts)

        return [human_message]

    def call_func(self, chain, prompt):
        try:
            response = chain.invoke(prompt)

            if isinstance(response, AIMessage):
                response_text = response.content
            else:
                response_text = str(response)
            # print(f"Reponse du llm : {response_text}")

            return extract_json(response_text)

        except Exception as e:
            print(f"Erreur de parsing JSON : {e}. Nouvelle tentative...")
            return -1

    def checking_all_keywords(self):
        path_images_empty = []
        none_possibilities = [None, "", [], "None", np.nan]
        for row in self.df.itertuples():
            keywords = getattr(row, "keywords", None)
            path = getattr(row, "path", None)

            if isinstance(keywords, float) and pd.isna(keywords):
                path_images_empty.append(path)

            elif keywords in none_possibilities:
                path_images_empty.append(path)

        return path_images_empty

    def keywords_call(self, image_paths, keywords_chain):
        for i in range(0, len(image_paths)):
            print(f"Image {i} : {image_paths[i]}")
            image_b64 = encode_image(image_paths[i])
            image_name = os.path.basename(image_paths[i])
            # print("Image name donnée au model : ", image_name)

            wrong_json = True
            max_iter = 100
            while wrong_json and max_iter > 0:
                prompt = {
                    "type": "keywords",
                    "text": f"""Décris-moi l'image avec 5 mots-clés. Les mots-clés doivent en priorité inclure des actions, des objets et un lieu si identifiables. Les mots-clés doivent être en français et peuvent être des mots composés.
                    Retourne le résultat au format JSON suivant: {{ "{image_name}" : ["mot-clé1", "mot-clé2", "mot-clé3", "mot-clé4", "mot-clé5"] }}""",
                    "image": image_b64
                }

                keywords_output = self.call_func(keywords_chain, prompt)
                print(f"Keywords : {keywords_output}\n")

                if keywords_output is None:
                    max_iter -= 1
                    print(f"On re-essaie avec au maximum : {max_iter}\n")
                else:

                    self.dataframe_manager.add_keywords_to_df(keywords_output)
                    self.df = self.dataframe_manager.get_dataframe()
                    wrong_json = False

        return self.df

    def pipeline_keywords(self):
        new_image_paths = self.image_paths

        keyword_chain = self.prompt_chain | self.llm

        all_keywords = False
        only_once = False

        while not all_keywords:
            self.df = self.keywords_call(new_image_paths, keyword_chain)

            if only_once:
                new_row = pd.DataFrame(
                    [{"image_name": "IMG_20241228_132157.jpg", "path": "photos_victor/IMG_20241228_132157.jpg"}])
                self.df = pd.concat([self.df, new_row], ignore_index=True)
                only_once = False

            new_image_paths = self.checking_all_keywords()
            print(f"Images à traiter après le passage : {new_image_paths}")

            if not new_image_paths:
                all_keywords = True

        return self.df

    def pipeline_categories_embedding_with_clusters(self, threshold=0.2, batch_size=10, predefined_categories=None):
        """
        Attribue des catégories en utilisant les clusters comme unité de base.
        Toutes les images d'un même cluster reçoivent la même catégorie.
        """
        if predefined_categories is None:
            predefined_categories = ["Ville", "Plage", "Randonnée", "Sport", "Musée", "Restaurant", "Voyages", "Nature", "Autres"]
        
        clustering_manager = ClusteringManager(self.df)
        clustered_df, clusters_by_day = clustering_manager.perform_day_based_clustering(threshold=0.6)
        self.df = clustered_df
        print(f"Clustering terminé: {len(clusters_by_day)} jours traités")
        
        clip_model = CLIPModel.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        clip_processor = CLIPProcessor.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        device = "cuda" if torch.cuda.is_available() else "cpu"
        clip_model = clip_model.to(device)
        
        # Ajout de traduction anglaise pour chaque catégorie pour améliorer la correspondance car CLIP fonctionne mieux en anglais qu'en français
        fr_to_en = {
            "Ville": "City urban buildings",
            "Plage": "Beach sea ocean sand",
            "Randonnée": "Hiking trail mountain path",
            "Sport": "Sports activity athletic",
            "Musée": "Museum exhibition art gallery",
            "Restaurant": "Restaurant dining food", 
            "Voyages": "Travel vacation snow",
            "Nature": "Nature wildlife environment flora fauna",
            "Autres": "Miscellaneous other"
        }
        
        en_categories = [fr_to_en.get(cat, cat) for cat in predefined_categories]
        
        # Encodage des catégories
        text_inputs = clip_processor(text=en_categories, return_tensors="pt", padding=True).to(device)
        with torch.no_grad():
            category_embeddings = clip_model.get_text_features(**text_inputs)
        category_embeddings = category_embeddings / category_embeddings.norm(p=2, dim=-1, keepdim=True)
        category_embeddings = category_embeddings.cpu().numpy()

        print(clusters_by_day)
        
        # Traitement de chaque cluster
        for day, day_clusters in clusters_by_day.items():
            for cluster_name, image_paths in day_clusters.items():
                if not image_paths:
                    continue
                    
                print(f"Traitement du cluster {cluster_name} avec {len(image_paths)} images")
                
                # Encodage par lots des images du cluster
                cluster_images = []
                valid_paths = []
                
                # Chargement des images du cluster
                for path in image_paths:
                    try:
                        image = Image.open(path).convert("RGB")
                        cluster_images.append(image)
                        valid_paths.append(path)
                    except Exception as e:
                        print(f"Erreur lors du chargement de l'image {path}: {e}")
                        continue
                
                if not cluster_images:
                    continue
                
                # Traitement des images par lots pour éviter les problèmes de mémoire
                all_embeddings = []
                for i in range(0, len(cluster_images), batch_size):
                    batch_images = cluster_images[i:i+batch_size]
                    
                    # Prétraitement des images
                    image_inputs = clip_processor(images=batch_images, return_tensors="pt", padding=True).to(device)
                    
                    # Obtention des embeddings
                    with torch.no_grad():
                        image_embeddings = clip_model.get_image_features(**image_inputs)
                    
                    # Normalisation
                    image_embeddings = image_embeddings / image_embeddings.norm(p=2, dim=-1, keepdim=True)
                    image_embeddings = image_embeddings.cpu().numpy()
                    all_embeddings.append(image_embeddings)
                
                # Combinaison de tous les embeddings du cluster
                if all_embeddings:
                    cluster_embeddings = np.vstack(all_embeddings)
                    
                    # Calcul de l'embedding moyen du cluster
                    cluster_centroid = np.mean(cluster_embeddings, axis=0)
                    cluster_centroid = cluster_centroid / np.linalg.norm(cluster_centroid)
                    
                    # Calcul des similarités avec chaque catégorie
                    similarities = cosine_similarity([cluster_centroid], category_embeddings)[0]
                    
                    # Normalisation des scores
                    if np.max(similarities) - np.min(similarities) > 1e-8:
                        normalized_similarities = (similarities - np.min(similarities)) / (np.max(similarities) - np.min(similarities))
                    else:
                        normalized_similarities = similarities
                    
                    # Sélection de la meilleure catégorie
                    best_cat_idx = np.argmax(normalized_similarities)
                    best_cat_score = normalized_similarities[best_cat_idx]
                    best_cat = predefined_categories[best_cat_idx]
                    
                    # Assignation de la catégorie à toutes les images du cluster
                    category = "Autres" if best_cat_score < threshold else best_cat
                    print(f"Cluster {cluster_name}: catégorie attribuée = {category} (score: {best_cat_score:.3f})")
                    
                    # Mise à jour du DataFrame
                    for path in valid_paths:
                        self.df.loc[self.df["path"] == path, "categories"] = category
        return self.df

    def pipeline(self, starting_time):
        print("RECHERCHE DE MOTS CLES...")
        self.df = self.pipeline_keywords()
        keywords_time = time.time() - starting_time
        print(tabulate(self.df, headers="keys", tablefmt="psql"))
        print(f"Temps de recherche des mots clés : {keywords_time:.2f} secondes")

        print("RECHERCHE DES CATEGORIES AVEC CLUSTERING...")
        self.df = self.pipeline_categories_embedding_with_clusters()
        categories_time = time.time() - starting_time
        print(tabulate(self.df, headers="keys", tablefmt="psql"))
        print(f"Temps de recherche des catégories : {categories_time:.2f} secondes")
        
        self.dataframe_manager.save_to_csv(self.directory + ".csv")

if __name__ == "__main__":
    call = LLMCall(directory=DIRECTORY, model=MODEL)
    starting_time = time.time()

    call.pipeline(starting_time)

    if os.path.exists(DESTINATION_DIRECORY):
        shutil.rmtree(DESTINATION_DIRECORY)

    os.mkdir(DESTINATION_DIRECORY)

    create_category_folders_from_csv(DIRECTORY + ".csv", DESTINATION_DIRECORY)

    total_time = time.time() - starting_time
    print(f"Temps total d'exécution : {total_time:.2f} secondes")

