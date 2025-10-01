from PIL import Image
import os

class ImageDetails:
    def __init__(self, image_path: str, detected_objects=None, description=None, generated_with=None):
        self.image_path = image_path
        self.image_name = self._get_image_name()
        self.image = Image.open(self.image_path)
        self.date_time, self.latitude, self.longitude = self._extract_exif_data()
        self.detected_objects = detected_objects
        self.description = description
        self.generated_with = generated_with

    def __str__(self):
        retval = ''
        retval += f'EXIF DATA:\n'
        retval += f'IMAGE   : {self.image_name}\n'
        retval += f"DATE    : {self.date_time}\n"
        retval += f"LAT     : {self.latitude}\n"
        retval += f"LON     : {self.longitude}\n"
        retval += '\n'
        retval += f'DETECTED OBJECTS:\n'
        retval += self.get_detected_objects_text()
        retval += '\n\n'
        retval += f'IMAGE DESCRIPTION:\n'
        retval += self.description
        return retval
    
    def to_dict(self):
        dict =  {
            'image_name': self.image_name,
            'image_path': self.image_path,
            'date_time': self.date_time,
            'latitude': self.latitude,
            'longitude': self.longitude,
            'detected_objects': self.get_detected_objects_text(),
            'description': self.description,
            'generated_with': self.generated_with
        }

        filtered_dict = self.filter_complex_metadata(dict)

        return filtered_dict
    
    def filter_complex_metadata(self, metadata: dict):
        allowed_types = (str, int, float, bool)
        return {k: v for k, v in metadata.items() if isinstance(v, allowed_types) and v is not None}
    
    def get_page_content(self):
        retval = f"{self.get_detected_objects_text()}\n\n{self.description}"
        return retval

    def get_detected_objects_text(self) -> str:
         retval = '\n'.join([f"{item['name']} - {item['description']}" for item in self.detected_objects])
         return retval

    def _get_image_name(self):
        return os.path.basename(self.image_path)
    
    def _get_from_dict(property_name, dict):
        return str(dict[property_name]) if property_name in dict.keys() else ''

    def _extract_exif_data(self):
            exifdata = self.image._getexif()
            date_time, latitude, longitude = None, None, None
            if exifdata:
                for tag_id, value in exifdata.items():
                    tag = Image.ExifTags.TAGS.get(tag_id, tag_id)
                    if tag == "DateTime":
                        date_time = value
                    elif tag == "GPSInfo":
                        gps_filtered = {k: value[k] for k in [1, 2, 3, 4] if k in value}
                        if gps_filtered:
                            lat, lon = self.__extract_coordinates(gps_filtered)
                            latitude = float(lat)
                            longitude = float(lon)
            else:
                print("Aucune donnée EXIF trouvée.")

            return date_time, latitude, longitude
    
    def __dms_to_decimal(self, dms, ref):
        degrees, minutes, seconds = dms
        decimal = degrees + minutes / 60 + seconds / 3600
        if ref in ['S', 'W']:
            decimal = -decimal
        return decimal

    def __extract_coordinates(self, gps_dict):
        lat = self.__dms_to_decimal(gps_dict[2], gps_dict[1])
        lon = self.__dms_to_decimal(gps_dict[4], gps_dict[3])
        return lat, lon