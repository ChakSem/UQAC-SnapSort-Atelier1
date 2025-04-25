import shutil
import time
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

DIRECTORY = "photos_victor"
DESTINATION_DIRECORY = "results"

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

    def neighbors_similarity_clustering(self, embeddings_dict, threshold=0.6, n_neighbors=3):
        clusters_by_day = {}
        global_cluster_id = 0  # Compteur global pour l'ID de cluster
        
        for day, image_list in embeddings_dict.items():
            paths = [image['path'] for image in image_list]
            embeddings = np.array([image['embedding'] for image in image_list])
            N = len(paths)
            
            clusters = {}
            current_cluster = []
            already_clustered = set()
            
            print(f"Clustering du jour {day} avec {N} images...")
            
            for i in range(N):
                current_img = paths[i]
                if current_img in already_clustered:
                    continue
                    
                # Calculer les similarités avec les n voisins suivants
                end_idx = min(i + n_neighbors + 1, N)
                similar_images = []
                
                # Ajouter l'image courante au cluster si pas encore clustérisée
                if current_img not in already_clustered:
                    current_cluster.append(current_img)
                    already_clustered.add(current_img)
                    
                # Chercher des images similaires parmi les voisins
                for j in range(i + 1, end_idx):
                    sim = np.dot(embeddings[i], embeddings[j])
                    if sim > threshold:
                        neighbor_img = paths[j]
                        if neighbor_img not in already_clustered:
                            current_cluster.append(neighbor_img)
                            already_clustered.add(neighbor_img)
                            similar_images.append(neighbor_img)
                
                # Si aucune image similaire trouvée et qu'on a un cluster en cours, finaliser le cluster
                if not similar_images and len(current_cluster) > 0:
                    cluster_name = f"cluster_{global_cluster_id}"  # Utiliser l'ID global
                    clusters[cluster_name] = current_cluster.copy()
                    current_cluster = []
                    global_cluster_id += 1  # Incrémenter l'ID global
            
            # Traitement du dernier cluster s'il n'est pas vide
            if current_cluster:
                cluster_name = f"cluster_{global_cluster_id}"  # Utiliser l'ID global
                clusters[cluster_name] = current_cluster
                global_cluster_id += 1  # Incrémenter l'ID global
            
            # Collecter les images non clustérisées dans "others"
            other_cluster = []
            for path in paths:
                found = False
                for cluster_name, cluster_images in clusters.items():
                    if path in cluster_images:
                        found = True
                        break
                if not found:
                    other_cluster.append(path)
            
            if other_cluster:
                clusters["others"] = other_cluster
                
            clusters_by_day[day] = clusters
        
        return clusters_by_day

    def perform_neighbors_clustering(self, threshold=0.6, n_neighbors=3):
        print("Début du clustering par voisins proches...")
        days_dict = self.day_sorting()
        embeddings_dict = self.days_embedding(days_dict)
        clusters = self.neighbors_similarity_clustering(embeddings_dict, threshold, n_neighbors)
        
        # Mise à jour du DataFrame avec les informations de cluster
        cluster_mapping = {}
        for day, day_clusters in clusters.items():
            for cluster_name, image_paths in day_clusters.items():
                for path in image_paths:
                    cluster_mapping[path] = cluster_name
        
        self.df['cluster'] = self.df['path'].map(cluster_mapping)
        
        return self.df, clusters

class LLMCall:
    def __init__(self, directory=DIRECTORY, allowed_extensions=None):
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif"}
        self.allowed_extensions = allowed_extensions
        self.directory = directory

        self.image_paths = self.get_image_paths(directory)

        self.dataframe_manager = DataframeCompletion(self.image_paths)
        self.df = DataframeCompletion(self.image_paths).get_dataframe()

    def get_image_paths(self, directory):
        image_paths = [os.path.join(directory, filename) for filename in os.listdir(directory) if os.path.splitext(filename)[1].lower() in self.allowed_extensions]
        return image_paths

    def pipeline_categories_embedding_with_clusters(self, threshold=0.1, batch_size=10, predefined_categories=None):
        """
        Attribue des catégories en utilisant les clusters comme unité de base.
        Toutes les images d'un même cluster reçoivent la même catégorie.
        """
        if predefined_categories is None:
            predefined_categories = ["Ville", "Plage", "Randonnée", "Sport", "Musée", "Nourriture", "Voyages", "Nature", "Neige", "Bâtiment", "Autres", "Famille et amis", "Animaux"]
        
        clustering_manager = ClusteringManager(self.df)
        
        # Choix de la méthode de clustering
        clustered_df, clusters_by_day = clustering_manager.perform_neighbors_clustering(threshold=0.6, n_neighbors=3)
        
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

                    for cat, sim in zip(predefined_categories, normalized_similarities):
                        print(f"{cat}: {sim:.3f}")
                    print("\n")

                    if len(image_paths) == 1:
                        best_cat = "Autres"
                    
                    # Assignation de la catégorie à toutes les images du cluster
                    # Vérifier si "Autres" est suffisamment proche du meilleur score
                    autres_index = predefined_categories.index("Autres")
                    autres_score = normalized_similarities[autres_index]
                    diff_with_best = best_cat_score - autres_score
                    
                    # Attribuer "Autres" si:
                    # - soit c'est déjà la meilleure catégorie (best_cat == "Autres")
                    # - soit son score est suffisamment proche du meilleur score (diff < 0.2) et qu'il n'est pas déjà le meilleur
                    formatted_date = day.replace(":", "_")
                    category = formatted_date + "_" + best_cat
                    if best_cat == "Autres" or (diff_with_best < threshold and best_cat != "Autres"):
                        category = "Autres"
                    
                    print(f"Cluster {cluster_name}: catégorie attribuée = {category} (score: {best_cat_score:.3f})")
                    
                    # Mise à jour du DataFrame
                    for path in valid_paths:
                        self.df.loc[self.df["path"] == path, "categories"] = category
        return self.df

    def pipeline(self, starting_time):
        print("RECHERCHE DES CATEGORIES AVEC CLUSTERING...")
        self.df = self.pipeline_categories_embedding_with_clusters()
        categories_time = time.time() - starting_time
        print(tabulate(self.df, headers="keys", tablefmt="psql"))
        print(f"Temps de recherche des catégories : {categories_time:.2f} secondes")
        
        self.dataframe_manager.df = self.df 
        self.dataframe_manager.save_to_csv(self.directory + ".csv")

if __name__ == "__main__":
    call = LLMCall(directory=DIRECTORY)
    starting_time = time.time()

    call.pipeline(starting_time)

    if os.path.exists(DESTINATION_DIRECORY):
        shutil.rmtree(DESTINATION_DIRECORY)

    os.mkdir(DESTINATION_DIRECORY)

    create_category_folders_from_csv(DIRECTORY + ".csv", DESTINATION_DIRECORY)

    total_time = time.time() - starting_time
    print(f"Temps total d'exécution : {total_time:.2f} secondes")

