from openai import OpenAI
from secret import OPENAI_API_KEY
from PIL import Image
import pandas as pd
from tabulate import tabulate
import os
import base64
import json
import re
import io


class CategorizerImages:
    def __init__(self, client, directory="test_data", allowed_extensions=None, model="gpt-4o-mini"):
        """
        :param client: Instance du client OpenAI.
        :param directory: Répertoire où se trouvent les images.
        :param allowed_extensions: Ensemble des extensions de fichiers d'image autorisées.
        """
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif"}
        self.client = client
        self.directory = directory
        self.allowed_extensions = allowed_extensions
        self.model = model

    def encode_image(self, image_path, max_size=(512, 512), quality=80):
        image = Image.open(image_path)

        # Redimensionner l'image
        image.thumbnail(max_size)

        # Convertir en bytes avec compression
        buffer = io.BytesIO()
        image.save(buffer, format="JPEG", quality=quality)

        # Encoder en Base64
        encoded_string = base64.b64encode(buffer.getvalue()).decode("utf-8")

        return encoded_string

    def get_image_paths(self):
        image_paths = [os.path.join(self.directory, filename) for filename in os.listdir(self.directory) if os.path.splitext(filename)[1].lower() in self.allowed_extensions]
        return image_paths

    def gpt_send_request(self, message):
        try:
            response = self.client.chat.completions.create(
                model=self.model, messages=message
            )

            result = response.choices[0].message.content.strip()

            tokens = response.usage.total_tokens

            return self.extract_json(result), tokens

        except Exception as e:
            print(f"Erreur OpenAI : {e}")

    def gpt_get_key_words(self, image_paths):

        # Liste pour chaque image et chaque texte associé
        content_list = []
        for image_path in image_paths:
            base64_image = self.encode_image(image_path)
            image_name = os.path.basename(image_path)
            content_list.append({
                "type": "text",
                "text": f"""Décris moi l'image avec 5 mots-clés.Retourne le résultat au format JSON : {{ {image_name} : [mot-clé1, mot-clé2, mot-clé3, mot-clé4, mot-clé5] }} """
            })
            content_list.append({
                "type": "image_url",
                "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}
            })

        messages = [
            {
                "role": "user",
                "content": content_list
            }
        ]

        return self.gpt_send_request(messages)


    def extract_json(self, response_text):
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

    def gpt_get_categories(self, keywords_output):
        """
        Utilise les mots-clés extraits pour regrouper les images similaires en catégories.
        Les images sont identifiées par leur ordre dans la liste.
        """

        # Préparation d'un prompt détaillé incluant le résultat des mots-clés et l'ordre des images
        prompt = f"""Voici les listes de mots-clés obtenues pour chaque image (dans l'ordre) : {keywords_output}
                    Regroupe les images similaires dans des catégories. Une catégorie est décrite par un seul mot-clé. Une image ne peut appartenir qu'à une seule catégorie. Retourne le résultat au format JSON : {{ "categorie1": [ "name", "name" ], "categorie2": [ "name", "name" ],...}}"""

        messages = [
            {
                "role": "user",
                "content": prompt
            }
        ]

        return self.gpt_send_request(messages)

    def create_df(self,image_paths):
        image_list = []
        for path in image_paths:
            image = Image.open(path)
            image_name = os.path.basename(path)
            exifdata = image._getexif()
            date_time, localisation = None, None
            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = Image.ExifTags.TAGS.get(tag_id, tag_id)
                    if tag == "DateTime":
                        date_time = value
                    elif tag == "GPSInfo":
                        localisation = value

                image_list.append((image_name, path, date_time, localisation))

            else:
                print("Aucune donnée EXIF trouvée.")

        df = pd.DataFrame(image_list, columns=["image_name", "path", "date_time", "localisation"])

        return df

    def add_keywords_to_df(self, df, keywords_output):
        if keywords_output:
            df.loc[df["image_name"].isin(keywords_output.keys()), "keywords"] = df["image_name"].map(keywords_output)
        else:
            print("Aucun mot clé fourni ! ")
        return df

    def add_categories_to_df(self, df, categories_output):
        if categories_output:
            # Inversion du dict : on associe une categorie a chaque image
            image_to_categories = {img: cat for cat, images in categories_output.items() for img in images}

            df.loc[df["image_name"].isin(image_to_categories.keys()), "categories"] = df["image_name"].map(image_to_categories)
        else:
            print("Aucune catégorisation trouvée !")

        return df

    def pipeline_keywords(self, image_paths, limit_size=10):
        image_data = self.create_df(image_paths)
        total_keywords_tokens = 0
        for i in range(0, len(image_paths), limit_size):
            interval = [i, min(i + limit_size, len(image_paths))]
            subset_image_paths = image_paths[interval[0]:interval[1]]
            #print(f"Image paths : {subset_image_paths}")

            keywords_output, keywords_tokens = self.gpt_get_key_words(subset_image_paths)
            total_keywords_tokens += keywords_tokens

            image_data = self.add_keywords_to_df(image_data, keywords_output)

            #print(f"Total tokens : {total_keywords_tokens}")

        return image_data, total_keywords_tokens

    def pipeline_categories(self, df, limit_size=200):
        keywords = df.set_index("image_name")["keywords"].to_dict()
        total_categories_tokens = 0

        for i in range(0, len(keywords), limit_size):
            interval = [i, min(i + limit_size, len(df))]
            subset_keys = list(keywords.keys())[interval[0]:interval[1]]

            subset_keywords = {key: keywords[key] for key in subset_keys}

            categories_output, categories_tokens = self.gpt_get_categories(subset_keywords)
            total_categories_tokens += categories_tokens

            df = self.add_categories_to_df(df, categories_output)

        return df, total_categories_tokens


    def process(self):
        image_paths = self.get_image_paths()

        df = self.create_df(image_paths)
        #print(tabulate(df, headers="keys", tablefmt="psql"))

        df, keywords_tokens = self.pipeline_keywords(image_paths, limit_size=50)
        #print("Mots-clés par image :")
        #print(keywords_output)
        print(tabulate(df, headers="keys", tablefmt="psql"))

        df, categories_tokens = self.pipeline_categories(df, limit_size=200)
        #print("Catégorisation des images :")
        #print(categories_output)
        print(tabulate(df, headers="keys", tablefmt="psql"))

        total_tokens = keywords_tokens + categories_tokens
        print(f"Nombre de token utilisés : {total_tokens}\n")

        #print(tabulate(df, headers="keys", tablefmt="psql"))

if __name__ == "__main__":
    client = OpenAI(api_key=OPENAI_API_KEY)
    
    categorizer = CategorizerImages(client, directory="photos_victor")
    categorizer.process()
