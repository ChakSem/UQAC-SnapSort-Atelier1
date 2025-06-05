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

            date_time, latitude, longitude = self.extract_exif_data(image)

            image_list.append((image_name, path, date_time, latitude, longitude))

        df = pd.DataFrame(image_list, columns=["image_name", "path", "date_time", "latitude", "longitude"])
        return df

    def extract_exif_data(self, image):
            exifdata = image._getexif()
            date_time, latitude, longitude = None, None, None
            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = Image.ExifTags.TAGS.get(tag_id, tag_id)
                    if tag == "DateTime":
                        date_time = value
                    elif tag == "GPSInfo":
                        gps_filtered = {k: value[k] for k in [1, 2, 3, 4] if k in value}
                        if gps_filtered:
                            lat, lon = self.extract_coordinates(gps_filtered)
                            latitude = float(lat)
                            longitude = float(lon)
            else:
                print("Aucune donnée EXIF trouvée.")

            return date_time, latitude, longitude

    def dms_to_decimal(self, dms, ref):
        degrees, minutes, seconds = dms
        decimal = degrees + minutes / 60 + seconds / 3600
        if ref in ['S', 'W']:
            decimal = -decimal
        return decimal

    def extract_coordinates(self, gps_dict):
        lat = self.dms_to_decimal(gps_dict[2], gps_dict[1])
        lon = self.dms_to_decimal(gps_dict[4], gps_dict[3])
        return lat, lon

    def get_dataframe(self):
        return self.df

    def save_to_csv(self, file_path="result.csv"):
        try:
            self.df.to_csv(file_path, index=False)
            print(f"DataFrame sauvegardé sous {file_path}")

        except Exception as e:
            print(f"Erreur lors de la sauvegarde : {e}")