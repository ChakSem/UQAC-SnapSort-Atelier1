import os
import shutil
import argparse

import pandas as pd
import numpy as np
import reverse_geocoder as rg

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

def get_season(month):
    if month in [12, 1, 2]:
        return "Hiver"
    elif month in [3, 4, 5]:
        return "Printemps"
    elif month in [6, 7, 8]:
        return "Été"
    elif month in [9, 10, 11]:
        return "Automne"
    else:
        return None

def reverse_geocode_lib(lat, lon):
    try:
        result = rg.search((lat, lon), mode=1)[0]
        return f"{result['name']}_{result['cc']}"
    except Exception as e:
        print(f"Erreur sur coords ({lat}, {lon}) : {e}")
        return None

def get_localisation(latitude, longitude, cache):
    lat = round(latitude, 3)
    lon = round(longitude, 3)
    coords = (lat, lon)

    if coords in cache:
        localisation = cache[coords]
    else:
        localisation = reverse_geocode_lib(*coords)
        cache[coords] = localisation

    return localisation

def create_arborescence_from_csv(csv_file):
    data = pd.read_csv(csv_file)
    tree_paths = []
    cache = {}  # Pour éviter les appels redondants

    if 'date_time' not in data.columns or 'latitude' not in data.columns or 'longitude' not in data.columns or 'categories' not in data.columns:
        print("Le CSV doit contenir les colonnes : date_time, latitude, longitude, categories.")
        return None

    for row in data.itertuples():

        date_time = row.date_time.split(":")
        year = int(date_time[0])
        month = int(date_time[1])
        season = get_season(month)

        latitude = row.latitude
        longitude = row.longitude
        localisation = None
        if not np.isnan(latitude) and not np.isnan(longitude):
            localisation = get_localisation(latitude, longitude, cache)

        category = row.categories

        if localisation :
            tree_path = f"{year}/{season}/{localisation}/{category}"
        else:
            tree_path = f"{year}/{season}/{category}"

        tree_paths.append(tree_path)

    data['folder_path'] = tree_paths

    data.to_csv(csv_file, index=False)


def create_category_folders_from_csv(csv_file, destination_directory, test=False):

    if not test:
        tree_struct = 'folder_path'
    else :
        tree_struct = 'categories'

    df = pd.read_csv(csv_file)

    if tree_struct not in df.columns:
        print(f"Le CSV ne contient pas de colonne {tree_struct}.")
        return

    os.makedirs(destination_directory, exist_ok=True)

    categories = df[tree_struct].dropna().unique().tolist()

    total_images = len(df['image_name'].unique())
    i = 0

    for category in categories:
        category_folder = os.path.join(destination_directory, category)
        print(f"Création du dossier pour la catégorie : {category_folder}")
        os.makedirs(category_folder, exist_ok=True)

        images_in_category = df[df[tree_struct] == category]['path'].tolist()

        for source_path in images_in_category:
            i += 1
            if os.path.exists(source_path):
                destination_path = os.path.join(category_folder, os.path.basename(source_path))
                shutil.copy(source_path, destination_path)
                print(f"Etape [4/4] : [{i}/{total_images}]")
                #print(f"Copié : {source_path} -> {destination_path}")
            else:
                print(f"Fichier non trouvé : {source_path}")