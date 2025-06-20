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
        predefined_categories = ["Ville", "Plage", "Randonnée", "Sport", "Musée", "Nourriture", "Restaurant", "Voyages", "Nature",
                                 "Neige", "Famille et amis", "Jeux", "Animaux", "Autres"]

        # Ajout de traduction anglaise pour chaque catégorie pour améliorer la correspondance car CLIP fonctionne mieux en anglais qu'en français
        fr_to_en = {
            "Ville": "City urban buildings",
            "Plage": "Beach sea ocean sand",
            "Randonnée": "Hiking trail forest path",
            "Sport": "Sports activity athletic",
            "Musée": "Museum exhibition art gallery",
            "Nourriture": "Food cuisine meal",
            "Restaurant": "Restaurant dining food",
            "Voyages": "Travel vacation trip",
            "Nature": "Nature wildlife water flora fauna",
            "Neige": "Snow winter cold",
            "Famille et amis": "Family friends gathering",
            "Jeux": "Games entertainment fun",
            "Animaux": "Animals pets wildlife",
            "Autres": "miscellaneous computer screenshots"
        }

        en_categories = [fr_to_en.get(cat, cat) for cat in predefined_categories]

        return en_categories, predefined_categories

    def get_cluster_images(self, image_paths, duplicates_to_remove):
        # Obtenir les images nettoyées (sans doublons ni floues)
        cleaned_paths = self.image_cleaner.clean_cluster(image_paths)
        print(f"Images après nettoyage : {cleaned_paths}")
        
        if not cleaned_paths:
            print("Aucune image retenue après nettoyage!")
            # Marquer toutes les images du cluster comme doublons à éliminer
            duplicates_to_remove.extend(image_paths)
                
        # Identifier les doublons pour ce cluster
        removed_images = set(image_paths) - set(cleaned_paths)
        duplicates_to_remove.extend(removed_images)

        # Charger les images nettoyées
        cluster_images = []
        for path in cleaned_paths:
            try:
                image = Image.open(path).convert("RGB")
                cluster_images.append(image)
            except Exception as e:
                print(f"Erreur lors du chargement de l'image {path}: {e}")
                continue

        return cluster_images, duplicates_to_remove

    def best_cluster_category(self, all_embeddings, category_embeddings, predefined_categories, threshold=0.10):
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


        if best_cat == "Autres":
            # Prendre la 2e meilleure catégorie si "Autres" est sélectionné
            second_best_idx = np.argsort(normalized_similarities)[-2]
            second_best_score = normalized_similarities[second_best_idx]
            difference_autre_score = best_cat_score - second_best_score
            # Si la différence entre "Autres" et la 2e meilleure catégorie est inférieure au seuil, on garde la 2e meilleure catégorie
            if difference_autre_score <= threshold :
                best_cat = predefined_categories[second_best_idx]
                best_cat_score = second_best_score


        '''for cat, sim in zip(predefined_categories, normalized_similarities):
            print(f"{cat}: {sim:.3f}")
        print("\n")'''

        # Assignation de la catégorie à toutes les images du cluster
        # Vérifier si "Autres" est suffisamment proche du meilleur score
        '''autres_index = predefined_categories.index("Autres")
        autres_score = normalized_similarities[autres_index]
        diff_with_best = best_cat_score - autres_score'''

        return best_cat, best_cat_score

    def pipeline_categories_embedding_with_clusters(self, threshold_category=0.05, threshold_clustering=0.55, batch_size=10, predefined_categories=None):
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
        clustered_df, clusters_by_day = clustering_manager.perform_neighbors_clustering(threshold=threshold_clustering, n_neighbors=3)

        self.df = clustered_df
        #print(f"Clustering terminé: {len(clusters_by_day)} jours traités")

        # Liste pour suivre les doublons à éliminer
        duplicates_to_remove = []

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
                print(f"\nEtape [3/4] : [{cluster_counter}/{total_clusters}]")
                print(f"Images du cluster : {image_paths}")

                if not image_paths:
                    continue

                #print(f"\nTraitement du cluster {cluster_name} avec {len(image_paths)} images")

                cluster_images, duplicates_to_remove = self.get_cluster_images(image_paths, duplicates_to_remove)
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
                    best_cat, best_cat_score = self.best_cluster_category(all_embeddings,category_embeddings,predefined_categories, threshold=threshold_category)

                    is_single_image = len(image_paths) == 1
                    #is_ambiguous = (diff_with_best < threshold and best_cat != "Autres")
                    formatted_date = day.replace(":", "_")
                    category = formatted_date + "_" + best_cat

                    # Attribuer "Autres" si:
                        # - soit son score est suffisamment proche du meilleur score (diff < threshold) et qu'il n'est pas déjà le meilleur
                        # - soit c'est déjà la meilleure catégorie (best_cat == "Autres")
                        # - soit le cluster ne contient qu'une seule image

                    '''if is_ambiguous:
                        category = "Autres/Autres"'''

                    if best_cat == "Autres" or is_single_image:
                        category = f"Autres/{best_cat}" # Sous dossier dans "Autres" avec la catégorie précédemment attribuée

                    print(f"Cluster {cluster_counter}: catégorie attribuée = {category} (score: {best_cat_score:.3f})\n")

                    # Mise à jour du DataFrame
                    for path in image_paths:
                        self.df.loc[self.df["path"] == path, "categories"] = category

        # Supprimer les doublons du DataFrame
        if duplicates_to_remove:
            print(f"Suppression de {len(duplicates_to_remove)} doublons du DataFrame final")
            self.df = self.df[~self.df["path"].isin(duplicates_to_remove)]

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