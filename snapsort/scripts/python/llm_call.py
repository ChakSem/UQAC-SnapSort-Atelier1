import os
import time
from contextlib import closing
import sqlite3
import base64
from io import BytesIO
from PIL import Image
import uuid
from colorama import Fore, Style
from langchain.schema import AIMessage
from langchain_core.documents import Document
from langchain_ollama import ChatOllama, OllamaEmbeddings
from langchain_core.messages import SystemMessage, HumanMessage
from langchain_core.output_parsers import JsonOutputParser, StrOutputParser
from langchain_chroma import Chroma
from image_details import ImageDetails
import sys

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
    You're an expert image and photo analyzer.
    You are very perceptive in analyzing images and photos. 
    You possess excelent vision. 
    Do not read any text unless it is the most prominent in the image. 
    Your description should be neutral in tone.
    '''
        return system_message_text

    def get_object_system_message(self):
        system_message_text = '''
    You're an expert image and photo analyzer.
    You are very perceptive in analyzing images and photos. 
    You possess excelent vision. 
    Do not read any text unless it is the most prominent in the image. 
    You should always output your results in json format, for example:

    [
    {'name': 'a detected object', 'description': 'the detected object's description'},
    {'name': 'another detected object', 'description': 'the other detected object's description'}
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
    
    
    def analyze_image(self, image_file):
        image_b64 = self.encode_image(image_file)

        detected_objects = self.object_chain.invoke ({"text":"""Identify objects in the image. Return a json list of json items of the detected objects. 
                Include only the names of each object and a short description of the object. 
                The field names should be 'name' and 'description' respectively.""", 
                "image": image_b64, 
                "system_message_text": self.get_object_system_message()})
        
        image_description = self.vision_chain.invoke ({"text": "Describe the image in as much detail as possible.", 
                "image": image_b64, 
                "system_message_text": self.get_vision_system_message()})
        
        image_details = ImageDetails(image_file, detected_objects, image_description, self.model)
        doc = Document(id=str(uuid.uuid4()), page_content=image_details.get_page_content(), metadata=image_details.to_dict())
        #db.add_documents([doc])
        return image_details
        

if __name__== "__main__":
    model = "gemma3"
    llm_call = LLMCall(model=model)

    # Example usage
    image_file = r"C:\Users\elise\Documents\GitHub\UQAC-SnapSort-Atelier2\photos_final\20240902_150137.jpg" 
    image_details = llm_call.analyze_image(image_file)
    
    print(image_details)  # Print the details of the analyzed image"""