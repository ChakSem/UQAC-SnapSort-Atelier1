from contextlib import closing
import sqlite3
from langchain_ollama import OllamaEmbeddings
from langchain_chroma import Chroma
import shutil

class ChromaDatabase:
    def __init__(self, db_name="db_photos", db_collection_name="photo_collection", embedding_model="mxbai-embed-large", new=False):
        if new : 
            self._clean_db(db_name=db_name)
        self.db_name = db_name
        self.db_collection_name = db_collection_name

        self.db = Chroma(collection_name=self.db_collection_name,
                        embedding_function=OllamaEmbeddings(model=embedding_model),
                        persist_directory=f"./{self.db_name}")

    def get_processed_files(self):
        with closing(sqlite3.connect(f"./{self.db_name}/chroma.sqlite3")) as connection:
            sql = "select string_value from embedding_metadata where key='image_path'"
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
