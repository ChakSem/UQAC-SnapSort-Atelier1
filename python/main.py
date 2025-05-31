import shutil
import time
import os
import sys
sys.stdout.reconfigure(line_buffering=True)
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

from functions import create_category_folders_from_csv, set_parser, create_arborescence_from_csv
from categories_manager import CategoriesManager

DIRECTORY = r"..\photos_final"
DESTINATION_DIRECORY = r"..\results"

if __name__ == "__main__":
    directory = DIRECTORY
    destination_directory = DESTINATION_DIRECORY
    call = CategoriesManager(directory=directory)
    starting_time = time.time()
    
    call.pipeline(starting_time)

    if os.path.exists(destination_directory):
        shutil.rmtree(destination_directory)

    os.mkdir(destination_directory)
    #call.create_autres_subfolders(destination_directory)

    create_arborescence_from_csv(directory + ".csv")
    create_category_folders_from_csv(directory + ".csv", destination_directory, test=False)

    total_time = time.time() - starting_time
    print(f"Temps total d'exécution : {total_time:.2f} secondes")
