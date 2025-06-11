import time
import base64
from io import BytesIO
from PIL import Image
import uuid
from langchain_core.documents import Document
from langchain_ollama import ChatOllama
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.output_parsers import JsonOutputParser, StrOutputParser

from image_details import ImageDetails
from functions import get_image_paths
from chroma_db import ChromaDatabase

class LLMCall:
    def __init__(self, model="gemma3"):
        self.model = model
        self.llm = ChatOllama(model=model, temperature=0.2)
        self.vision_chain = self.prompt_func | self.llm | StrOutputParser()
        self.object_chain = self.prompt_func | self.llm | JsonOutputParser()

    
    def encode_image(self, image_path, max_size=(512, 512), quality=80):
        image = Image.open(image_path)
        # Redimensionner l'image
        image.thumbnail(max_size)
        # Convertir en bytes avec compression
        buffer = BytesIO()
        image.save(buffer, format="JPEG", quality=quality)
        # Encoder en Base64
        encoded_string = base64.b64encode(buffer.getvalue()).decode("utf-8")

        return encoded_string
    
    def get_vision_system_message(self):
        system_message_text = '''
    Vous êtes un expert en analyse d'images et de photos.
    Vous êtes très perspicace dans l'analyse des images et des photos.
    Vous possédez une excellente vision.
    Votre description doit être neutre.
'''
        return system_message_text

    def get_object_system_message(self):
        system_message_text = '''
Vous êtes un expert en analyse d'images et de photos.
Vous êtes très perspicace dans l'analyse des images et des photos.
Vous possédez une excellente vision.
N'utilise pas d'apastrophe car cela poserait problème pour le json.
Vous devez toujours donner vos résultats au format json, par exemple :

[
{'name': 'un objet détecté', 'description': "la description de l objet détecté"},
{'name': 'un autre objet détecté', 'description': "la description de l autre objet détecté"}
]
'''
        return system_message_text
    
    def prompt_func(self, data):
        text = data["text"]
        image = data["image"]

        system_message = SystemMessage(content=data["system_message_text"])

        image_part = {
            "type": "image_url",
            "image_url": f"data:image/jpeg;base64,{image}",
        }

        content_parts = []

        text_part = {"type": "text", "text": text}

        content_parts.append(image_part)
        content_parts.append(text_part)
        human_message = HumanMessage(content=content_parts)

        return [system_message, human_message]
    

    def call_function(self, chain, prompt, image):
        if chain == "object":
            llm_chain = self.object_chain
            system_message = self.get_object_system_message()
        elif chain == "description":
            llm_chain = self.vision_chain
            system_message = self.get_vision_system_message()
        else : 
            print("Mauvaise commande, utiliser 'object' ou 'description' \n") 
            return -2
        
        try:
            llm_response = llm_chain.invoke ({"text":prompt, 
                    "image": image, 
                    "system_message_text": system_message})
        except Exception as e:
            print(f"Erreur : {e}. Nouvelle tentative...")
            return -1
        
        return llm_response

    def analyze_image(self, image_file):
        image_b64 = self.encode_image(image_file)

        object_prompt = """Identifie les objets présents dans l'image. Retourne une liste json d'éléments json correspondant aux objets détectés. 
Inclue uniquement le nom de chaque objet et une courte description de l'objet. 
Les champs doivent s'appeler 'name' et 'description' respectivement."""

        while True : 
            detected_objects = self.call_function("object", object_prompt, image_b64)
            if detected_objects == -1 :
                object_prompt = object_prompt + " Ta réponse doit être au format json, fais attention à ne pas utiliser d'apostrophes dans le texte des champs."
            else :
                break

        description_prompt = "Décris l'image aussi précisément que possible."
        while True : 
            image_description = self.call_function("description", description_prompt, image_b64)
            if image_description != -1 :
                break

        image_details = ImageDetails(image_file, detected_objects, image_description, self.model)
        
        return image_details
    
    def pipeline_calls(self, image_paths, database):

        processed_files = database.get_processed_files()

        counter = 1
        for image_path in image_paths:
            print('---------------------------------------------------------------')
            print(f'{counter} / {len(image_paths)}')
            print(image_path)
            print('\n')
            if image_path in processed_files:
                print(f'FILE ALREADY PROCESSED, SKIPPED')
            else:
                image_details = self.analyze_image(image_path)
                print(image_details)
                doc = Document(id=str(uuid.uuid4()), page_content=image_details.get_page_content(), metadata=image_details.to_dict())
                database.db.add_documents([doc])
            counter += 1



def process_images(directory):
    image_paths = get_image_paths(directory)

    image_model = "gemma3"
    llm_call = LLMCall(model=image_model)

    # Example usage
    # image_file = r".\photos_final\20240902_150137.jpg" 
    # image_details = llm_call.analyze_image(image_file)
    # print(image_details)

    llm_call.pipeline_calls(image_paths, database)

       

if __name__== "__main__":
    directory = r".\photos_final"
    embedding_model = "mxbai-embed-large"
    database = ChromaDatabase(embedding_model=embedding_model, new=False)

    starting_time = time.time()
    process_images(directory)
    ending_time = time.time()
    print(f"Temps total pour traiter les images: {ending_time - starting_time:.2f} sec")




