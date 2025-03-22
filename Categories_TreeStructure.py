import os
import shutil
import pandas as pd

def create_category_folders_from_csv(csv_file, source_directory, destination_directory):
    df = pd.read_csv(csv_file)

    if 'categories' not in df.columns:
        print("Le CSV ne contient pas de colonne 'categories'.")
        return

    os.makedirs(destination_directory, exist_ok=True)

    categories = df['categories'].dropna().unique().tolist()

    for category in categories:
        category_folder = os.path.join(destination_directory, category)
        os.makedirs(category_folder, exist_ok=True)

        images_in_category = df[df['categories'] == category]['image_name'].tolist()

        for image_name in images_in_category:
            source_path = os.path.join(source_directory, image_name)
            destination_path = os.path.join(category_folder, image_name)

            if os.path.exists(source_path):
                shutil.copy(source_path, destination_path)
                print(f"Copié : {source_path} -> {destination_path}")
            else:
                print(f"Fichier non trouvé : {source_path}")

if __name__ == "__main__":
    create_category_folders_from_csv(csv_file="test_data_copy.csv", source_directory="test_data_copy", 
                                    destination_directory="TreeStructure")
