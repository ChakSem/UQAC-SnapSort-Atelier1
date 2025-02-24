from openai import OpenAI
from secret import OPENAI_API_KEY

import os
import base64
import json
import re

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

    def gpt_send_request(self, message, extracting_json=False):
        try:
            response = self.client.chat.completions.create(
                model=self.model, messages=message
            )

            result = response.choices[0].message.content.strip()
            if extracting_json:
                result = self.extract_json(result)

            return result

        except Exception as e:
            print(f"Erreur OpenAI : {e}")

    def gpt_get_key_words(self, base64_images, image_names):

        # Liste pour chaque image et chaque texte associé
        content_list = []
        for i, base64_image in enumerate(base64_images):
            content_list.append({
                "type": "text",
                "text": f"Décris moi toutes les images avec 5 mots-clés. Le format est le suivant {image_names[i]} : [mot-clé1, mot-clé2, mot-clé3, mot-clé4, mot-clé5]"            })
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

        return self.gpt_send_request(messages, extracting_json=False)


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

    def gpt_get_categories(self, keywords_output, image_names):
        """
        Utilise les mots-clés extraits pour regrouper les images similaires en catégories.
        Les images sont identifiées par leur ordre dans la liste.
        """

        # Préparation d'un prompt détaillé incluant le résultat des mots-clés et l'ordre des images
        prompt = f"""Voici les listes de mots-clés obtenues pour chaque image (dans l'ordre) : {keywords_output}

            L'ordre et le nom des images est le suivant : {image_names}

            En te basant sur ces informations, regroupe les images similaires dans des catégories. Une catégorie est décrite par un seul mot-clé.
            Retourne le résultat au format JSON en indiquant pour chaque catégorie la liste des indices des images qui appartiennent à cette catégorie.
            Le format attendu est :
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

        return self.gpt_send_request(messages, extracting_json=True)

    def process(self):
        image_paths = self.get_image_paths()
        image_names = [os.path.basename(path) for path in image_paths]
        base64_images = [self.encode_image(path) for path in image_paths]
        keywords_output = self.gpt_get_key_words(base64_images, image_names)
        print("Mots-clés par image :")
        print(keywords_output)
        categories = self.gpt_get_categories(keywords_output, image_names)
        print("Catégorisation des images :")
        print(categories)

if __name__ == "__main__":
    client = OpenAI(api_key=OPENAI_API_KEY)
    
    categorizer = CategorizerImages(client, directory="test_data")
    categorizer.process()
