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

DIRECTORY = "test_data"
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

    def checking_all_categories(self):
        keywords_empty_categories = {}
        none_possibilities = [None, "", [], "None", np.nan]
        for row in self.df.itertuples():

            categories = getattr(row, "categories", None)
            image_name = getattr(row, "image_name", None)
            keywords = getattr(row, "keywords", None)

            if isinstance(categories, float) and pd.isna(categories):
                keywords_empty_categories.update({image_name: keywords})
            elif categories in none_possibilities:
                keywords_empty_categories.update({image_name: keywords})

        return keywords_empty_categories

    def get_cat_list(self, initial_cat_list):
        cat_list = initial_cat_list
        for row in self.df.itertuples():
            categories = getattr(row, "categories", None)
            if categories not in cat_list:
                cat_list.append(categories)

        return cat_list

    def categories_call(self, keywords, limit_size, cat_list, cat_chain):
        for i in range(0, len(keywords), limit_size):
            interval = [i, min(i + limit_size, len(self.df))]

            subset_keys = list(keywords.keys())[interval[0]:interval[1]]

            subset_keywords = {key: keywords[key] for key in subset_keys}
            print(f"Keywords : {subset_keywords}")

            wrong_json = True
            max_iter = 100
            while wrong_json and max_iter > 0:
                prompt = {"type": "categories", "text": f"""
                    1. Regroupe les images en fonction de l'action, évènement ou de l'activité qu'elles représentent.
                    2. Chaque catégorie est définie par un seul mot-clé descriptif.
                    3. Une image ne peut appartenir qu'à une seule catégorie.
                    4. Priorise les catégories existantes : {cat_list}. Si une image correspond à l'une d'elles, classe-la dedans.
                    5. Si aucune catégorie existante ne convient, crée une nouvelle catégorie proche d’une activité de voyage ou une catégorie plus générique (ex: "Nature", "Repas", "Loisirs")
                    6. Réduis autant que possible la catégorie "Autres". N’y mets une image que si elle est vraiment impossible à classer ailleurs.

                    - Listes de mots-clés détectés pour chaque image dans le format dict = "image_name": [mots-cles] : {subset_keywords}

                    Retourne uniquement le résultat et au format JSON : {{ "categorie1": [ "image_name", "image_name" ], "categorie2": [ "image_name", "image_name" ],...}}"""
                          }

                categories_output = self.call_func(cat_chain, prompt)
                print(f"Categories : {categories_output}\n")

                if categories_output is None:
                    max_iter -= 1
                    print(f"On re-essaie avec au maximum : {max_iter}\n")
                else:
                    self.dataframe_manager.add_categories_to_df(categories_output)
                    self.df = self.dataframe_manager.get_dataframe()

                    cat_list = self.get_cat_list(cat_list)
                    print(f"Nouvelle liste des categories : {cat_list}")
                    wrong_json = False

        return self.df

    def pipeline_categories(self, limit_size=20):
        new_keywords = self.df.set_index("image_name")["keywords"].to_dict()
        # print(new_keywords)
        cat_list = ["Paysage", "Ville", "Plage", "Randonnée", "Sport", "Musée", "Restaurant", "Autres", "Loisirs", "Repas", "Nature", "Voyage", "Culture", "Animaux"]

        cat_chain = self.prompt_chain | self.llm

        all_categories = False
        only_once = False

        while not all_categories:
            self.df =  self.categories_call(new_keywords, limit_size, cat_list, cat_chain)

            if only_once:
                self.df.loc[1, "categories"] = None
                only_once = False

            new_keywords =  self.checking_all_categories()
            print(f"Keywords à repasser après checking : {new_keywords}")
            cat_list =  self.get_cat_list(cat_list)

            if not new_keywords:
                all_categories = True

        return self.df
    
    def pipeline_categories_embedding(self, threshold=0.2, batch_size=10, predefined_categories=None):
        if predefined_categories is None:
            predefined_categories = ["Ville", "Plage", "Randonnée", "Sport", "Musée", "Restaurant", "Voyages", "Nature", "Autres"]

        clip_model = CLIPModel.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        clip_processor = CLIPProcessor.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        
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
        
        # Convertit la liste de descriptions en tenseurs PyTorch
        # Padding pour même longueur
        text_inputs = clip_processor(text=en_categories, return_tensors="pt", padding=True)

        # Economie de mémoire
        with torch.no_grad():
            category_embeddings = clip_model.get_text_features(**text_inputs)
        
        # Normalisation des embeddings
        category_embeddings = category_embeddings / category_embeddings.norm(p=2, dim=-1, keepdim=True)
        category_embeddings = category_embeddings.cpu().numpy()
        
        # Préchargement des images
        images = []
        valid_indices = []
        
        for idx, row in self.df.iterrows():
            image_path = row["path"]
            try:
                image = Image.open(image_path).convert("RGB")
                images.append(image)
                valid_indices.append(idx)
            except Exception as e:
                print(f"Erreur lors du chargement de l'image {image_path}: {e}")
                self.df.loc[idx, "assigned_category"] = "Autres"
                continue
        
        # Traitement par lots
        for i in range(0, len(images), batch_size):
            batch_images = images[i:i+batch_size]
            batch_indices = valid_indices[i:i+batch_size]
            
            # Prétraitement des images en batch
            image_inputs = clip_processor(images=batch_images, return_tensors="pt", padding=True)
            
            with torch.no_grad():
                image_embeddings = clip_model.get_image_features(**image_inputs)
            
            # Normalisation
            image_embeddings = image_embeddings / image_embeddings.norm(p=2, dim=-1, keepdim=True)
            image_embeddings = image_embeddings.cpu().numpy()
            
            # Calcul des similarités pour tout le batch
            similarities = cosine_similarity(image_embeddings, category_embeddings)
            
            for j, (idx, sims) in enumerate(zip(batch_indices, similarities)):
                # Normalisation des similarités
                if np.max(sims) - np.min(sims) > 1e-8:  # Éviter la division par zéro
                    normalized_sims = (sims - np.min(sims)) / (np.max(sims) - np.min(sims))
                else:
                    normalized_sims = sims
                
                best_sim = np.max(normalized_sims)
                best_cat_idx = np.argmax(normalized_sims)
                best_cat = predefined_categories[best_cat_idx]
                
                # Si la différence est trop faible retourner "Autres"
                if best_sim < threshold:
                    self.df.loc[idx, "categories"] = "Autres"
                else:
                    self.df.loc[idx, "categories"] = best_cat
        return self.df


    def pipeline(self, strating_time):
        print("RECHERCHE DE MOTS CLES...")
        self.df = self.pipeline_keywords()
        keywords_time = time.time() - strating_time
        print(tabulate(self.df, headers="keys", tablefmt="psql"))
        print(f"Temps de recherche des mots clés : {keywords_time:.2f} secondes")

        print("RECHERCHE DES CATEGORIES...")
        #self.df = self.pipeline_categories()
        self.df = self.pipeline_categories_embedding()
        categories_time = time.time() - strating_time
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

