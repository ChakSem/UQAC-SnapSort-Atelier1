import os

from PIL import Image
import pandas as pd

from image_details import ImageDetails  

class DataframeCompletion:
    def __init__(self, image_paths):
        self.image_paths = image_paths
        self.df = self.create_df()

    def create_df(self):
        image_list = []
        for path in self.image_paths:
            image = ImageDetails(path)
            image_list.append((image.image_name, path, image.date_time, image.latitude, image.longitude))

        df = pd.DataFrame(image_list, columns=["image_name", "path", "date_time", "latitude", "longitude"])
        return df


    def get_dataframe(self):
        return self.df

    def save_to_csv(self, file_path="result.csv"):
        try:
            self.df.to_csv(file_path, index=False)
            print(f"DataFrame sauvegardé sous {file_path}")

        except Exception as e:
            print(f"Erreur lors de la sauvegarde : {e}")