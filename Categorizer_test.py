from openai import OpenAI
from secret import OPENAI_API_KEY
from PIL import Image
import pandas as pd

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
            return self.extract_json(result)

        except Exception as e:
            print(f"Erreur OpenAI : {e}")

    def gpt_get_key_words(self, base64_images, image_names):

        # Liste pour chaque image et chaque texte associé
        content_list = []
        for i, base64_image in enumerate(base64_images):
            content_list.append({
                "type": "text",
                "text": f"""Décris moi toutes les images avec 5 mots-clés.
                        Retourne le résultat au format JSON comme ceci :
                        {{
                            {image_names[i]} : [mot-clé1, mot-clé2, mot-clé3, mot-clé4, mot-clé5],
                            ...

                        }}
                        Garde bien le nom de l'image {image_names[i]} pour décrire l'image
                        """
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

            En te basant sur ces informations, regroupe les images similaires dans des catégories. Une catégorie est décrite par un seul mot-clé. Une image ne peut appartenir qu'à une seule catégorie.
            Retourne le résultat au format JSON comme tel :
            {{
                "categorie1": [ "name", "name" ],
                "categorie2": [ "name", "name" ],
                ...
            }}
            """
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
            df["Keywords"] = df["image_name"].apply(lambda img: keywords_output.get(img, None))
        return df

    def add_categories_to_df(self, df, categories_output):
        if categories_output:
            df["Category"] = df["image_name"].map(
                lambda image: next((cat for cat in categories_output if image in categories_output[cat]), None))
        else:
            print("Aucune catégorisation trouvée !")

        return df


    def process(self):
        image_paths = self.get_image_paths()
        image_names = [os.path.basename(path) for path in image_paths]
        print(image_names)
        base64_images = [self.encode_image(path) for path in image_paths]

        df = self.create_df(image_paths)
        print(df)

        keywords_output = self.gpt_get_key_words(base64_images, image_names)
        print("Mots-clés par image :")
        print(keywords_output)

        categories = self.gpt_get_categories(keywords_output)
        print("Catégorisation des images :")
        print(categories)

        df = self.add_keywords_to_df(df, keywords_output)
        df = self.add_categories_to_df(df, categories)

        print("Affichage du DataFrame :")
        print(df)

if __name__ == "__main__":
    client = OpenAI(api_key=OPENAI_API_KEY)
    
    categorizer = CategorizerImages(client, directory="test_data")
    categorizer.process()
