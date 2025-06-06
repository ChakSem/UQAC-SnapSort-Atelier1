import os
import time
import cv2
from PIL import Image
import imagehash


class ImageCleaner:
    def __init__(self, target_size=(600, 600), allowed_extensions=None):
        """
        :param target_size: Tuple auquel redimensionner toutes les images.
        :param allowed_extensions: Ensemble des extensions d'image autorisées.
        """
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"}

        self.target_size = target_size
        self.allowed_extensions = allowed_extensions

    def read_and_resize(self, path):
        img = cv2.imread(path)
        if img is None:
            print(f"Impossible de lire l'image {path}.")
            return None
        
        return cv2.resize(img, self.target_size)

    def calculate_phash_distance(self, img1, img2):
        """
        Calcule la distance entre les pHash de deux images OpenCV.
        Plus la distance est faible, plus les images sont similaires.
        
        :param img1: Image redimensionnée.
        :param img2: Image redimensionnée.
        """
        pil1 = Image.fromarray(cv2.cvtColor(img1, cv2.COLOR_BGR2RGB))
        pil2 = Image.fromarray(cv2.cvtColor(img2, cv2.COLOR_BGR2RGB))
        hash1 = imagehash.phash(pil1)
        hash2 = imagehash.phash(pil2)
        return abs(hash1 - hash2)

    def get_images_with_quality(self, image_paths=None):
        """
        Calcule la qualité de chaque image.
        
        :param image_paths: Liste de chemins d'images à analyser
        :return: Liste de tuples (chemin, qualité)
        """
        images_with_quality = []
        
        for path in image_paths:
            img = self.read_and_resize(path)
            if img is None:
                continue
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            quality = cv2.Laplacian(gray, cv2.CV_64F).var()
            images_with_quality.append((path, quality))

        return images_with_quality

    def remove_duplicates(self, images_with_quality, phash_threshold=20, batch_size=10):
        """
        Compare les images (basée sur le pHash) pour éliminer les doublons parmi l'ensemble des images.
        Si deux images ont une distance pHash < phash_threshold, elles sont considérées comme
        quasi-identiques. On conserve alors l'image avec la meilleure qualité.
        
        :param images_with_quality: Liste de tuples (chemin, qualité) pour toutes les images.
        :param phash_threshold: Seuil de distance pHash pour considérer deux images comme identiques.
        :param batch_size: Taille du lot d'images à traiter à chaque itération
        :return: Tuple (unique, duplicates) : listes des chemins d'images uniques et des doublons.
        """
        unique = []      
        duplicates = []

        total_images = len(images_with_quality)

        start_all = time.time()
        
        for batch_start in range(0, total_images, batch_size):
            batch_end = min(batch_start + batch_size, total_images)
            current_batch = images_with_quality[batch_start:batch_end]
            
            print(f"Traitement du lot d'images {batch_start+1}-{batch_end} sur {total_images}")
            batch_start_time = time.time()
            
            for path, quality in current_batch:
                img1 = self.read_and_resize(path)
                if img1 is None:
                    continue
                    
                is_duplicate = False
                
                # Vérification des doublons parmi les images déjà conservées
                for idx, (unique_path, unique_quality) in enumerate(unique):
                    img2 = self.read_and_resize(unique_path)
                    if img2 is None:
                        continue
                        
                    distance = self.calculate_phash_distance(img1, img2)
                    if distance < phash_threshold:
                        # On garde l'image avec la meilleure qualité
                        if quality > unique_quality:
                            duplicates.append(unique_path)
                            unique[idx] = (path, quality)
                        else:
                            duplicates.append(path)

                        is_duplicate = True
                        break
                
                # Si ce n'est pas un doublon, on l'ajoute à la liste d'images uniques
                if not is_duplicate:
                    unique.append((path, quality))
            
            batch_end_time = time.time()
            print(f"Lot traité en {batch_end_time - batch_start_time:.2f} secondes")
            print(f"Images uniques trouvées jusqu'à présent: {len(unique)}")
            print(f"Doublons trouvés jusqu'à présent: {len(duplicates)}\n")
        
        end_all = time.time()
        print("Unique :", unique)
        print("Doublons :", duplicates)
        print(f"Temps total pour le traitement des doublons: {end_all - start_all:.2f} secondes")
        print(f"Images uniques: {len(unique)}, Doublons: {len(duplicates)}")
        
        return unique, duplicates
    
    def clean_cluster(self, image_paths, blur_threshold=50.0, phash_threshold=20):
        """
        Nettoie un cluster d'images en supprimant les doublons et les images floues.
        
        :param image_paths: Liste des chemins des images du cluster
        :param blur_threshold: Seuil de qualité (variance Laplacian)
        :param phash_threshold: Seuil de distance pHash pour la détection de doublons
        :return: Liste des chemins d'images conservées
        """
        images_with_quality = self.get_images_with_quality(image_paths)
        
        if not images_with_quality:
            return []
            
        print(f"Nombre d'images dans le cluster : {len(images_with_quality)}")
        
        # Suppression des doublons
        #print("ETAPE 4 - Suppression des doublons :\n")
        unique, duplicates = self.remove_duplicates(images_with_quality, phash_threshold=phash_threshold, batch_size=10)
        
        # Filtrage des images floues
        retained_images = [path for (path, quality) in unique if quality > blur_threshold]
        
        print(f"Images retenues après nettoyage : {len(retained_images)}/{len(image_paths)}")
        print(f"Doublons supprimés : {len(duplicates)}")
        print(f"Images floues supprimées : {len(unique) - len(retained_images)}")
        
        return retained_images