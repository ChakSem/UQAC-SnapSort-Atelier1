import os
import time

from PIL import Image
from tabulate import tabulate
import numpy as np
import torch
from sklearn.metrics.pairwise import cosine_similarity

from dataframe_completion import DataframeCompletion
from clustering_manager import ClusteringManager
from embeddings_manager import EmbeddingsManager
from images_manager import ImageCleaner

class CategoriesManager(EmbeddingsManager):
    def __init__(self, directory, allowed_extensions=None):
        super().__init__()
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif"}
        self.allowed_extensions = allowed_extensions
        self.directory = directory

        self.image_paths = self.get_image_paths(directory)
        self.image_cleaner = ImageCleaner()

        self.dataframe_manager = DataframeCompletion(self.image_paths)
        self.df = DataframeCompletion(self.image_paths).get_dataframe()

    def get_image_paths(self, directory):
        image_paths = [os.path.join(directory, filename) for filename in os.listdir(directory) if os.path.splitext(filename)[1].lower() in self.allowed_extensions]
        return image_paths

    def get_predifined_categories(self):
        predefined_categories = ["Ville", "Plage", "Randonnée", "Sport", "Musée", "Nourriture", "Voyages", "Nature",
                                 "Neige", "Bâtiment", "Autres", "Famille et amis", "Animaux"]

        # Ajout de traduction anglaise pour chaque catégorie pour améliorer la correspondance car CLIP fonctionne mieux en anglais qu'en français
        fr_to_en = {
            "Ville": "City urban buildings",
            "Plage": "Beach sea ocean sand",
            "Randonnée": "Hiking trail forest path",
            "Sport": "Sports activity athletic",
            "Musée": "Museum exhibition art gallery",
            "Restaurant": "Restaurant dining food",
            "Voyages": "Travel vacation snow",
            "Nature": "Nature wildlife environment flora fauna",
            "Autres": "Miscellaneous other"
        }

        en_categories = [fr_to_en.get(cat, cat) for cat in predefined_categories]

        return en_categories, predefined_categories

    def get_cluster_images(self, image_paths):
        image_paths = self.image_cleaner.clean_cluster(image_paths)
        if not image_paths:
            print("Aucune image retenue après nettoyage!")
            return []
        
        # Encodage par lots des images du cluster
        cluster_images = []

        # Chargement des images du cluster
        for path in image_paths:
            try:
                image = Image.open(path).convert("RGB")
                cluster_images.append(image)
            except Exception as e:
                print(f"Erreur lors du chargement de l'image {path}: {e}")
                continue

        return cluster_images

    def best_cluster_category(self, all_embeddings, category_embeddings, predefined_categories):
        cluster_embeddings = np.vstack(all_embeddings)

        # Calcul de l'embedding moyen du cluster
        cluster_centroid = np.mean(cluster_embeddings, axis=0)
        cluster_centroid = cluster_centroid / np.linalg.norm(cluster_centroid)

        # Calcul des similarités avec chaque catégorie
        similarities = cosine_similarity([cluster_centroid], category_embeddings)[0]

        # Normalisation des scores
        if np.max(similarities) - np.min(similarities) > 1e-8:
            normalized_similarities = (similarities - np.min(similarities)) / (
                    np.max(similarities) - np.min(similarities))
        else:
            normalized_similarities = similarities

        # Sélection de la meilleure catégorie
        best_cat_idx = np.argmax(normalized_similarities)
        best_cat_score = normalized_similarities[best_cat_idx]
        best_cat = predefined_categories[best_cat_idx]

        '''for cat, sim in zip(predefined_categories, normalized_similarities):
            print(f"{cat}: {sim:.3f}")
        print("\n")'''

        # Assignation de la catégorie à toutes les images du cluster
        # Vérifier si "Autres" est suffisamment proche du meilleur score
        autres_index = predefined_categories.index("Autres")
        autres_score = normalized_similarities[autres_index]
        diff_with_best = best_cat_score - autres_score

        return best_cat, best_cat_score, diff_with_best

    def pipeline_categories_embedding_with_clusters(self, threshold=0.1, batch_size=10, predefined_categories=None):
        """
        Attribue des catégories en utilisant les clusters comme unité de base.
        Toutes les images d'un même cluster reçoivent la même catégorie.
        """
        if predefined_categories is None:
            en_categories, predefined_categories = self.get_predifined_categories()
        else:
            en_categories = predefined_categories

        clustering_manager = ClusteringManager(self.df)

        # Choix de la méthode de clustering
        clustered_df, clusters_by_day = clustering_manager.perform_neighbors_clustering(threshold=0.6, n_neighbors=3)

        self.df = clustered_df
        #print(f"Clustering terminé: {len(clusters_by_day)} jours traités")

        # Encodage des catégories
        text_inputs = self.clip_processor(text=en_categories, return_tensors="pt", padding=True).to(self.device)
        with torch.no_grad():
            category_embeddings = self.clip_model.get_text_features(**text_inputs)
        category_embeddings = category_embeddings / category_embeddings.norm(p=2, dim=-1, keepdim=True)
        category_embeddings = category_embeddings.cpu().numpy()

        #print(clusters_by_day)

        # Traitement de chaque cluster
        print(f"ETAPE 3 - Association des noms aux clusters :\n")
        total_clusters = sum(len(clusters) for clusters in clusters_by_day.values())
        cluster_counter = 0
        for day, day_clusters in clusters_by_day.items():
            for cluster_name, image_paths in day_clusters.items():
                cluster_counter += 1
                print(f"Cluster {cluster_counter}/{total_clusters}")

                if not image_paths:
                    continue

                #print(f"\nTraitement du cluster {cluster_name} avec {len(image_paths)} images")

                cluster_images = self.get_cluster_images(image_paths)
                if not cluster_images:
                    continue

                # Traitement des images par lots pour éviter les problèmes de mémoire
                all_embeddings = []
                for i in range(0, len(cluster_images), batch_size):
                    batch_images = cluster_images[i:i + batch_size]
                    image_embeddings = self.image_embedding(images=batch_images)
                    all_embeddings.append(image_embeddings)

                # Combinaison de tous les embeddings du cluster
                if all_embeddings:

                    best_cat, best_cat_score, diff_with_best = self.best_cluster_category(all_embeddings,
                                                                                          category_embeddings,
                                                                                          predefined_categories)

                    if len(image_paths) == 1:
                        best_cat = "Autres"

                    # Attribuer "Autres" si:
                    # - soit c'est déjà la meilleure catégorie (best_cat == "Autres")
                    # - soit son score est suffisamment proche du meilleur score (diff < 0.2) et qu'il n'est pas déjà le meilleur
                    formatted_date = day.replace(":", "_")
                    category = formatted_date + "_" + best_cat
                    if best_cat == "Autres" or (diff_with_best < threshold and best_cat != "Autres"):
                        category = "Autres"
                    print(f"Cluster {cluster_counter}: catégorie attribuée = {category} (score: {best_cat_score:.3f})")

                    # Mise à jour du DataFrame
                    for path in image_paths:
                        self.df.loc[self.df["path"] == path, "categories"] = category

        return self.df

    def pipeline(self, starting_time):
        #print("RECHERCHE DES CATEGORIES AVEC CLUSTERING...")
        self.df = self.pipeline_categories_embedding_with_clusters()
        categories_time = time.time() - starting_time
        #print(tabulate(self.df, headers="keys", tablefmt="psql"))
        print(f"Temps de recherche des catégories : {categories_time:.2f} secondes")

        self.dataframe_manager.df = self.df
        print(f"ETAPE 4 - Copie des images triées :\n")
        self.dataframe_manager.save_to_csv(self.directory + ".csv")