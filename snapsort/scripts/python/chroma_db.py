from contextlib import closing
import sqlite3
from langchain_ollama import OllamaEmbeddings
from langchain_chroma import Chroma
import shutil
import os

class ChromaDatabase:
    def __init__(self, db_name="db_photos", db_collection_name="photo_collection", embedding_model="mxbai-embed-large", path="/scripts/database", new=False):
        if new : 
            self._clean_db(db_name=db_name)
        self.db_name = db_name
        self.db_collection_name = db_collection_name
        os.makedirs(path, exist_ok=True)
        current_path = os.getcwd()
        print(f"{current_path}{path}/{self.db_name}")
        path_to_db = f"{current_path}{path}/{self.db_name}"
        self.path = path_to_db

        self.db = Chroma(collection_name=self.db_collection_name,
                        embedding_function=OllamaEmbeddings(model=embedding_model),
                        persist_directory=f"{self.path}")

    def get_processed_files(self):
        db_file = f"{self.path}/chroma.sqlite3"
        with closing(sqlite3.connect(db_file)) as connection:
            sql = "select string_value from embedding_metadata where key='image_name'"
            rows = connection.execute(sql).fetchall()
            processed_files = [file_name for file_name, in rows]
        return processed_files
    
    def get_similar_pictures(self, prompt, threshold=2, k=100, printing=True):
        results = self.db.similarity_search_with_score(prompt, k=k)
        filtered = [(doc, score) for doc, score in results if score <= threshold] # Métrique L2 donc on cherche le plus petit score 
        if printing :
            for doc, score in filtered:
                print(f"{doc.metadata['image_name']} (score: {score:.3f})")
        return filtered

    def _clean_db(self, db_name):
        shutil.rmtree(f"./{db_name}")
