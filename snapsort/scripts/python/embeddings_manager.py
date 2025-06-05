from PIL import Image
import torch
from transformers import CLIPProcessor, CLIPModel


class EmbeddingsManager:
    def __init__(self, clip_model=None, clip_processor=None):
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        if clip_model is None:
            self.clip_model = CLIPModel.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K").to(self.device)
        else:
            self.clip_model = clip_model.to(self.device)
        if clip_processor is None:
            self.clip_processor = CLIPProcessor.from_pretrained("laion/CLIP-ViT-L-14-laion2B-s32B-b82K")
        else:
            self.clip_processor = clip_processor

    def image_embedding(self, paths=None, images=None):
        if images is None:
            images = []
            for path in paths:
                try:
                    image = Image.open(path).convert("RGB")
                    images.append(image)
                except Exception as e:
                    print(f"Erreur lors du chargement de l'image {path}: {e}")
                    continue

            if not images:
                return None

        # Prétraitement des images en batch
        image_inputs = self.clip_processor(images=images, return_tensors="pt", padding=True).to(self.device)

        with torch.no_grad():
            image_embeddings = self.clip_model.get_image_features(**image_inputs)

        # Normalisation
        image_embeddings = image_embeddings / image_embeddings.norm(p=2, dim=-1, keepdim=True)
        image_embeddings = image_embeddings.cpu().numpy()

        return image_embeddings

