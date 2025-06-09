from contextlib import closing
import sqlite3
from langchain_ollama import OllamaEmbeddings
from langchain_chroma import Chroma
import os

class ChromaDatabase:
    def __init__(self, db_name="db_photos", db_collection_name="photo_collection", embedding_model="mxbai-embed-large"):
        self.db_name = db_name
        self.db_collection_name = db_collection_name

        self.db = Chroma(collection_name=self.db_collection_name,
                        embedding_function=OllamaEmbeddings(model=embedding_model),
                        persist_directory=f"./{self.db_name}")

    def get_processed_files(self):
        with closing(sqlite3.connect(f"./{self.db_name}/chroma.sqlite3")) as connection:
            sql = "select string_value from embedding_metadata where key='file_name'"
            rows = connection.execute(sql).fetchall()
            processed_files = [file_name for file_name, in rows]
        return processed_files
    
    def get_similar_pictures(self, prompt, nb_results):
        results = self.db.similarity_search(prompt, k=nb_results)
        for doc in results:
            print(doc.metadata['image_name'])
