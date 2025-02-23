from openai import OpenAI
from secret import OPENAI_API_KEY

import os
import base64
import json
import re

class Algo2:
    def __init__(self, client, directory="test_data", allowed_extensions=None):
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

    def encode_image(self, image_path):
        try:
            with open(image_path, "rb") as image_file:
                return base64.b64encode(image_file.read()).decode("utf-8")
        except Exception as e:
            print(f"Erreur lors de l'encodage de l'image {image_path} : {e}")
            return None

    def get_image_paths(self):
        image_paths = [os.path.join(self.directory, filename) for filename in os.listdir(self.directory) if os.path.splitext(filename)[1].lower() in self.allowed_extensions]
        return image_paths

    def chat(self, base64_images):
        try:
            # Liste pour chaque image et chaque texte associé
            content_list = []
            for i, base64_image in enumerate(base64_images):
                content_list.append({
                    "type": "text",
                    "text": f"Décris moi l'image {i+1} avec 5 mots-clés. Le format est le suivant : [mot-clé1, mot-clé2, mot-clé3, mot-clé4, mot-clé5]"
                })
                # Ajoute l'image encodée
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

            chat_completion = self.client.chat.completions.create(
                model="gpt-4o-mini", messages=messages
            )
            return chat_completion.choices[0].message.content.strip()

        except Exception as e:
            print(f"Erreur OpenAI lors de l'extraction des mots-clés : {e}")
            return None

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

    def categorize_photos(self, keywords_output, image_paths):
        try:
            prompt = f"""Voici les listes de mots-clés obtenues pour chaque image (dans l'ordre) : {keywords_output}

            L'ordre des images est le suivant : {chr(10).join([f"Image {i+1} : {path}" for i, path in enumerate(image_paths)])}

            En te basant sur ces informations, regroupe les images similaires dans des catégories.
            Retourne uniquement le résultat au format JSON en indiquant pour chaque catégorie la liste des indices des images (par exemple : "Image 1", "Image 3", etc.) qui appartiennent à cette catégorie.
            Exemple de format :
            {{
                "categorie1": [ "Image 1", "Image 3" ],
                "categorie2": [ "Image 2", "Image 4" ]
            }}
            """
            messages = [
                {
                    "role": "user",
                    "content": prompt
                }
            ]
            chat_completion = self.client.chat.completions.create(
                model="gpt-4o-mini", messages=messages
            )
            response_text = chat_completion.choices[0].message.content.strip()
            return self.extract_json(response_text)
        
        except Exception as e:
            print(f"Erreur OpenAI lors de la catégorisation : {e}")
            return None

    def process(self):
        image_paths = self.get_image_paths()
        base64_images = [self.encode_image(path) for path in image_paths]
        keywords_output = self.chat(base64_images)
        print("Mots-clés par image :")
        print(keywords_output)
        categories = self.categorize_photos(keywords_output, image_paths)
        print("Catégorisation des images :")
        print(categories)

if __name__ == "__main__":
    client = OpenAI(api_key=OPENAI_API_KEY)
    
    categorizer = Algo2(client, directory="test_data")
    categorizer.process()
