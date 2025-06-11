import time
import json
import os

from functions import set_parser_image_retrieval
from chroma_db import ChromaDatabase

DIRECTORY_PATH = "./scripts/temp_files"

def json_saving(similar_images):
    images_to_save = [
        {
            "image_name": doc.metadata["image_name"],
            "score": score
        }
        for doc, score in similar_images
    ]
    os.makedirs(DIRECTORY_PATH, exist_ok=True)

    json_path = DIRECTORY_PATH + "/similar_images.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(images_to_save, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    args = set_parser_image_retrieval()

    #prompt = args.prompt
    prompt = "une randonnée avec des arbres jaunes et rouges"
    database = ChromaDatabase()

    starting_time = time.time()
    
    similar_images = database.get_similar_pictures(prompt, printing=False)

    json_saving(similar_images)    
    
    ending_time = time.time()
    print(f"Temps total pour récupérer les images: {ending_time - starting_time:.2f} sec")