import numpy as np

from embeddings_manager import EmbeddingsManager

class ClusteringManager(EmbeddingsManager):
    def __init__(self, df):
        super().__init__()
        self.df = df

    def day_sorting(self):
        days = {}
        no_date_images = []
        for index, row in self.df.iterrows():
            date = row["date_time"]
            if date:
                day = date.split(" ")[0]
                if day not in days:
                    days[day] = []
                days[day].append(row["path"])
            else:
                no_date_images.append(row["path"])
                
        if no_date_images:
            days["no_date"] = no_date_images
            print(f"Images sans date trouvées: {len(no_date_images)}")
        
        return days

    def days_embedding(self, days_dict):
        embeddings_dict = {}
        total_images = sum(len(images) for images in days_dict.values())
        image_counter = 0
        for day, images in days_dict.items():
            # Génération des embeddings pour chaque image
            embeddings = self.image_embedding(images)
            
            if embeddings is None:
                continue
                
            embeddings_dict[day] = []
            for i, image in enumerate(images):
                image_counter += 1
                print(f"Etape [1/4] : [{image_counter}/{total_images}]")
                if i < len(embeddings):  # Protection contre les indices hors limites
                    embeddings_dict[day].append({
                        'path': image,
                        'embedding': embeddings[i]
                    })
        return embeddings_dict

    def neighbors_similarity_clustering(self, embeddings_dict, threshold, n_neighbors=3):
        clusters_by_day = {}
        self.global_cluster_id = 0

        total_images = sum(len(images) for images in embeddings_dict.values())
        last_number = 1
        for day, image_list in embeddings_dict.items():
            clusters = self._cluster_day_embeddings(image_list, threshold, n_neighbors, total_images, last_number)
            last_number += len(image_list)
            clusters_by_day[day] = clusters

        return clusters_by_day

    def _cluster_day_embeddings(self, image_list, threshold, n_neighbors, total_images, last_number):
        paths = [image['path'] for image in image_list]
        embeddings = np.array([image['embedding'] for image in image_list])
        N = len(paths)

        clusters = {}
        current_cluster = []        # Liste pour stocker les images du cluster en cours
        already_clustered = set()   # Ensemble pour suivre les images déjà clustérisées
        all_outliers = []
        last_index_added = -1

        for i in range(N):
            print(f"Etape [2/4] : [{i + last_number}/{total_images}]\n")
            current_img = paths[i]
            #print(f"Traitement de l'image {current_img}")

            end_idx = min(i + n_neighbors + 1, N)
            checking_paths = paths[i:end_idx]
            checking_embeddings = embeddings[i:end_idx]

            photos, outliers = self._photos_to_add(checking_paths, checking_embeddings, threshold)
            if outliers:
                all_outliers.append(outliers[0])

            if photos and current_img not in already_clustered:
                current_cluster.append(current_img)
                already_clustered.add(current_img)
                last_index_added = max(last_index_added, i)
                print(f"Ajout de {current_img} au cluster {self.global_cluster_id}")

            for elem in photos:
                path = elem[0]
                idx = paths.index(path)  # Trouver l'index de l'image dans la liste des chemins
                if path not in already_clustered:
                    current_cluster.append(path)
                    already_clustered.add(path)
                    last_index_added = max(last_index_added, idx) # Mettre à jour l'index du dernier ajout
                    print(f"Ajout de {path} au cluster {self.global_cluster_id}")

            # Si aucune image similaire trouvée et qu'on a un cluster en cours, finaliser le cluster
            if not photos and current_cluster and i >= last_index_added:
                self._finalize_cluster(clusters, current_cluster)

        # Traitement du dernier cluster s'il n'est pas vide
        if current_cluster:
            self._finalize_cluster(clusters, current_cluster)

        # Collecter les images non clustérisées dans "others"
        other_cluster = self._find_unclustered_images(paths, clusters)
        if other_cluster:
            clusters["others"] = other_cluster

        return clusters

    def _photos_to_add(self, paths, embeddings, threshold):
        photos = []
        outliers = []
        all_photos = False

        # Vérification de la similarité entre la première et la dernière image pour accepter un outlier si nécessaire
        sim_furthest = np.dot(embeddings[0], embeddings[-1])
        if sim_furthest >= threshold:
            all_photos = True

        for i in range(1, len(paths)):
            sim = np.dot(embeddings[0], embeddings[i])
            #print(f"Les photos {paths[0]} et {paths[i]} ont une similarité de {sim:.2f}")
            if sim >= threshold:
                photos.append((paths[i], sim))
            elif all_photos:
                photos.append((paths[i], sim))
                outliers.append(paths[i])

        return photos, outliers

    def _finalize_cluster(self, clusters, cluster_images):
        cluster_name = f"cluster_{self.global_cluster_id}"
        clusters[cluster_name] = cluster_images.copy()
        cluster_images.clear()
        self.global_cluster_id += 1

    def _find_unclustered_images(self, paths, clusters):
        other_cluster = []
        for path in paths:
            found = False
            for cluster_name, cluster_images in clusters.items():
                if path in cluster_images:
                    found = True
                    break
            if not found:
                other_cluster.append(path)

        return other_cluster

    def perform_neighbors_clustering(self, threshold, n_neighbors=3):
        #print("CLUSTERING DES IMAGES PAR VOISINS PROCHES...")
        days_dict = self.day_sorting()
        print(f"ETAPE 1 - Génération des embeddings : \n")
        embeddings_dict = self.days_embedding(days_dict)
        print(f"ETAPE 2 - Clustering des images :\n")
        clusters = self.neighbors_similarity_clustering(embeddings_dict, threshold, n_neighbors)
        
        # Mise à jour du DataFrame avec les informations de cluster
        cluster_mapping = {}
        for day, day_clusters in clusters.items():
            for cluster_name, image_paths in day_clusters.items():
                for path in image_paths:
                    cluster_mapping[path] = cluster_name
        
        self.df['cluster'] = self.df['path'].map(cluster_mapping)
        
        return self.df, clusters