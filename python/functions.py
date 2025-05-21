import os
import shutil
import argparse

import pandas as pd

def set_parser():
    parser = argparse.ArgumentParser()

    # Training arguments
    parser.add_argument('--directory', type=str, default="unsorted_images")
    parser.add_argument('--destination_directory', type=str, default="albums")

    args = parser.parse_args()

    print("\n----------- Arguments --------------")
    print(args)
    print("------------------------------------")

    return args

def create_category_folders_from_csv(csv_file, destination_directory):
    df = pd.read_csv(csv_file)

    if 'categories' not in df.columns:
        print("Le CSV ne contient pas de colonne 'categories'.")
        return

    os.makedirs(destination_directory, exist_ok=True)

    categories = df['categories'].dropna().unique().tolist()

    total_images = len(df['image_name'].unique())
    i = 0

    for category in categories:
        category_folder = os.path.join(destination_directory, category)
        os.makedirs(category_folder, exist_ok=True)

        images_in_category = df[df['categories'] == category]['path'].tolist()

        for source_path in images_in_category:
            i += 1
            if os.path.exists(source_path):
                destination_path = os.path.join(category_folder, os.path.basename(source_path))
                shutil.copy(source_path, destination_path)
                print(f"Images {i}/{total_images}")
                #print(f"Copié : {source_path} -> {destination_path}")
            else:
                print(f"Fichier non trouvé : {source_path}")