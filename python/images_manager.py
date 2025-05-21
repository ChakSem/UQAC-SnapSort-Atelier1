import os
import time
import cv2
import shutil
import tempfile
from PIL import Image
import imagehash


class ImageCleaner:
    def __init__(self, directory, target_size=(600, 600), allowed_extensions=None):
        """
        :param directory: Répertoire contenant les images.
        :param target_size: Tuple auquel redimensionner toutes les images.
        :param allowed_extensions: Ensemble des extensions d'image autorisées.
        """
        if allowed_extensions is None:
            allowed_extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"}

        self.directory = directory
        self.target_size = target_size
        self.allowed_extensions = allowed_extensions

    def get_image_paths(self):
        paths = [] 

        for file_name in os.listdir(self.directory):
            extension = os.path.splitext(file_name)[1].lower()
            if extension in self.allowed_extensions:
                path = os.path.join(self.directory, file_name)
                paths.append(path)

        return paths

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

    def get_images_with_quality(self):
        """
        Parcourt toutes les images du répertoire, les redimensionne et calcule leur qualité
        (variance du Laplacien). Retourne une liste de tuples (chemin, qualité).
        """
        images_with_quality = []

        for path in self.get_image_paths():
            img = self.read_and_resize(path)
            if img is None:
                continue
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            quality = cv2.Laplacian(gray, cv2.CV_64F).var()
            images_with_quality.append((path, quality))

        return images_with_quality

    def remove_duplicates(self, images_with_quality, phash_threshold=20):
        """
        Compare les images (basée sur le pHash) pour éliminer les doublons parmi l'ensemble des images.
        Si deux images ont une distance pHash < phash_threshold, elles sont considérées comme
        quasi-identiques. On conserve alors l'image avec la meilleure qualité.
        
        :param images_with_quality: Liste de tuples (chemin, qualité) pour toutes les images.
        :param phash_threshold: Seuil de distance pHash pour considérer deux images comme identiques.
        :return: Tuple (unique, duplicates) : listes des chemins d'images uniques et des doublons.
        """
        unique = []      
        duplicates = []

        i = 0
        start_all = time.time()
        print(f"ETAPE 1 - Traitement des doublons :\n")
        for path, quality in images_with_quality:
            i += 1
            start_one = time.time()
            print(f"Image {i}/{len(images_with_quality)}")
            img1 = self.read_and_resize(path)
            if img1 is None:
                continue
            is_duplicate = False

            for idx, (unique_path, unique_quality) in enumerate(unique):
                img2 = self.read_and_resize(unique_path)
                if img2 is None:
                    continue
                distance = self.calculate_phash_distance(img1, img2)
                if distance < phash_threshold:
                    if quality > unique_quality:
                        duplicates.append(unique_path)
                        unique[idx] = (path, quality)
                    else:
                        duplicates.append(path)
                    is_duplicate = True
                    break
            
            if not is_duplicate:
                unique.append((path, quality))

            end_one = time.time()
            print(f"Temps écoulé : {end_one - start_one:.2f} s\n")

        end_all = time.time()
        print(f"Temps total pour le traitement des doublons : {end_all - start_all:.2f} secondes")

        return unique, duplicates

    def copy_images_to_folder(self, image_paths, output_folder):
        """
        Copie les images retenues vers le dossier de sortie.
        Vide le dossier s'il existe déjà.
        
        :param image_paths: Liste des chemins des images à copier.
        :param output_folder: Dossier de destination.
        """
        # Si le dossier existe, on le vide complètement
        if os.path.exists(output_folder):
            print(f"Le dossier {output_folder} existe déjà. Vidage en cours...")
            shutil.rmtree(output_folder)
        
        # Création du dossier (vide)
        os.makedirs(output_folder)
        print(f"Dossier créé/vidé : {output_folder}")
        
        copied_count = 0
        for image_path in image_paths:
            if os.path.exists(image_path):
                filename = os.path.basename(image_path)
                destination = os.path.join(output_folder, filename)
                counter = 1
                base_name, extension = os.path.splitext(filename)
                while os.path.exists(destination):
                    new_filename = f"{base_name}_{counter}{extension}"
                    destination = os.path.join(output_folder, new_filename)
                    counter += 1
                
                shutil.copy2(image_path, destination)
                copied_count += 1
        
        print(f"Nombre d'images copiées : {copied_count}")
        return copied_count

    def process(self, output_folder=None, blur_threshold=100.0, phash_threshold=20):
        """
        Récupère toutes les images et calcule leur qualité. Applique la détection des doublons sur l'ensemble des images.
        Sépare les images floues selon le seuil de qualité. Copie les images uniques non floues dans un dossier de sortie.
        
        :param output_folder: Dossier de sortie pour les images retenues. Si None, utilise un dossier temporaire.
        :param blur_threshold: Seuil de qualité (variance Laplacian) pour classer une image comme floue.
        :param phash_threshold: Seuil de distance pHash pour la détection de doublons.
        :param use_temp_folder: Si True, crée un dossier temporaire. Si False, utilise output_folder.
        :return: Chemin du dossier contenant les images retenues.
        """

        #print("TRAITEMENT DES DOUBLONS ET DES IMAGES FLOUES...")

        images_with_quality = self.get_images_with_quality()
        print(f"Nombre total d'images trouvées : {len(images_with_quality)}")
        
        unique, duplicates = self.remove_duplicates(images_with_quality, phash_threshold=phash_threshold)
        unique_paths = [p for (p, q) in unique]
        print(f"Nombre d'images uniques : {len(unique_paths)}")
        print(f"Nombre de doublons : {len(duplicates)}")
        
        blurry = [path for (path, quality) in images_with_quality if quality < blur_threshold]
        print(f"Nombre d'images floues (qualité < {blur_threshold}) : {len(blurry)}")

        retained_images = [path for (path, quality) in unique if quality > blur_threshold]
        print(f"Nombre d'images retenues : {len(retained_images)}")
        
        # Création d'un dossier de sortie
        output_folder = f"{self.directory}_cleaned"
        output_path = output_folder
        print(f"Dossier de sortie : {output_path}")

        # Copie des images retenues
        self.copy_images_to_folder(retained_images, output_path)
        
        # Affichage des statistiques
        print("\nRésultats finaux :")
        print(f"Doublons détectés : {len(duplicates)}")
        print(f"Images floues détectées : {len(blurry)}")
        print(f"Images uniques : {len(unique_paths)}")
        print(f"Images retenues et copiées : {len(retained_images)}")
        print(f"Dossier de sortie : {output_path}")
        
        return output_path