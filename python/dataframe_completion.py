import os

from PIL import Image
import pandas as pd

class DataframeCompletion:
    def __init__(self, image_paths):
        self.image_paths = image_paths
        self.df = self.create_df()

    def create_df(self):
        image_list = []
        for path in self.image_paths:
            image = Image.open(path)
            image_name = os.path.basename(path)

            date_time, localisation = self.extract_exif_data(image)

            image_list.append((image_name, path, date_time, localisation))

        df = pd.DataFrame(image_list, columns=["image_name", "path", "date_time", "localisation"])

        return df

    def extract_exif_data(self, image):
            exifdata = image._getexif()
            date_time, localisation = None, None
            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = Image.ExifTags.TAGS.get(tag_id, tag_id)
                    if tag == "DateTime":
                        date_time = value
                    elif tag == "GPSInfo":
                        localisation = value
            else:
                print("Aucune donnée EXIF trouvée.")

            return date_time, localisation

    def get_dataframe(self):
        return self.df

    def save_to_csv(self, file_path="result.csv"):
        try:
            self.df.to_csv(file_path, index=False)
            print(f"DataFrame sauvegardé sous {file_path}")

        except Exception as e:
            print(f"Erreur lors de la sauvegarde : {e}")