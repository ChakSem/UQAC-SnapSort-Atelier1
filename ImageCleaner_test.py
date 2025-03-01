import os
import cv2
from PIL import Image

# Utilisation de pHash plutôt que PSNR car meilleur résultat
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
        
        for path, quality in images_with_quality:
            img1 = self.read_and_resize(path)
            if img1 is None:
                continue
            is_duplicate = False

            for idx, (unique_path, unique_quality) in enumerate(unique):
                img2 = self.read_and_resize(unique_path)
                # print("img1 :", path)
                # print("img2 :", unique_path)
                if img2 is None:
                    continue
                distance = self.calculate_phash_distance(img1, img2)
                #print(f"Comparaison entre {path} et {unique_path} : distance = {distance}")
                if distance < phash_threshold:
                    if quality > unique_quality:
                        print(f"Doublon détecté (pHash={distance}) : remplacement de {unique_path} par {path}.")
                        duplicates.append(unique_path)
                        unique[idx] = (path, quality)
                    else:
                        print(f"Doublon détecté (pHash={distance}) : {path} est similaire à {unique_path}.")
                        duplicates.append(path)
                    is_duplicate = True
                    break
            
            if not is_duplicate:
                unique.append((path, quality))
        
        #unique_paths = [p for (p, q) in unique]
        return unique, duplicates

    def display_images(self, image_paths, window_title):
        """
        Affiche chaque image de la liste dans une fenêtre OpenCV.
        
        :param image_paths: Liste des chemins d'images à afficher.
        :param window_title: Titre de la fenêtre.
        """
        for path in image_paths:
            img = self.read_and_resize(path)
            if img is None:
                continue
            cv2.imshow(window_title, img)
            print(f"Affichage de l'image {path}. Appuyez sur une touche pour continuer...")
            cv2.waitKey(0)
        cv2.destroyAllWindows()

    def process(self, blur_threshold=100.0, phash_threshold=20):
        """
        Exécute le pipeline complet :
        1. Récupère toutes les images et calcule leur qualité.
        2. Applique la détection des doublons sur l'ensemble des images.
        3. Sépare les images floues selon le seuil de qualité.
        4. Garde les images uniques non floues pour envoie à l'API
        
        :param blur_threshold: Seuil de qualité (variance Laplacian) pour classer une image comme floue.
        :param phash_threshold: Seuil de distance pHash pour la détection de doublons.
        :return: Dictionnaire contenant les listes 'duplicates', 'blurry', 'unique' et 'retained', dans cet ordre.
        """
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
        
        return {
            "duplicates": duplicates,
            "blurry": blurry,
            "unique": unique_paths,
            "retained": retained_images
        }

if __name__ == "__main__":
    cleaner = ImageCleaner(directory="test_floues_doublons", target_size=(600, 600))
    results = cleaner.process(blur_threshold=100.0, phash_threshold=20)
    
    print("\nRésultats finaux :")
    print("Doublons :", results["duplicates"])
    print("Images floues :", results["blurry"])
    print("Images uniques :", results["unique"])
    print("Images retenues :", results["retained"])
    
    # Affichage des doublons
    if results["duplicates"]:
        print("\nAffichage des doublons :")
        cleaner.display_images(results["duplicates"], "Doublon")
    
    # Affichage des images floues
    if results["blurry"]:
        print("\nAffichage des images floues :")
        cleaner.display_images(results["blurry"], "Image Floue")
