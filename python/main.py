import shutil
import time
import os
import sys
sys.stdout.reconfigure(line_buffering=True)

from functions import create_category_folders_from_csv, set_parser
from categories_manager import CategoriesManager
from images_manager import ImageCleaner

if __name__ == "__main__":
    args = set_parser()
    directory = args.directory
    destination_directory = args.destination_directory
    images_manager = ImageCleaner(directory)
    output_folder = images_manager.process()
    call = CategoriesManager(directory=output_folder)
    starting_time = time.time()

    call.pipeline(starting_time)

    if os.path.exists(destination_directory):
        shutil.rmtree(destination_directory)

    os.mkdir(destination_directory)

    create_category_folders_from_csv(output_folder + ".csv", destination_directory)

    total_time = time.time() - starting_time
    print(f"Temps total d'exécution : {total_time:.2f} secondes")
