import shutil
import time
import os
import sys
sys.stdout.reconfigure(line_buffering=True)
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

from functions import create_category_folders_from_csv, create_arborescence_from_csv, set_parser_main, copy_all_images, empty_directory
from categories_manager import CategoriesManager

CLEANING = False

if __name__ == "__main__":
    args = set_parser_main()
    directory = args.directory
    destination_directory = args.destination_directory

    copy_directory = args.copy_directory
    copy_all_images(directory, copy_directory)

    call = CategoriesManager(directory=directory)
    starting_time = time.time()
    
    call.pipeline(starting_time)

    if CLEANING:
        if os.path.exists(destination_directory):
            shutil.rmtree(destination_directory)

    create_arborescence_from_csv(directory + ".csv")
    create_category_folders_from_csv(directory + ".csv", destination_directory, arborescence=True)

    total_time = time.time() - starting_time
    print(f"Temps total d'exécution : {total_time:.2f} secondes")

    empty_directory(directory)


