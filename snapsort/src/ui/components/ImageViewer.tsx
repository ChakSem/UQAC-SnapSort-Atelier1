import { useState } from 'react';
import { ImagesViewerProps } from '../types/interfaces';
import RenderMediaItem from './RenderMediaItem';
import EnlargedView from './EnlargedView';

function ImagesViewer({ mediaFiles, height }: ImagesViewerProps) {
  // If height is not provided, set it to 125.6px
  const defaultHeight = height || 125.6;

  // État pour suivre le niveau de zoom (par défaut 200px)
  const [imageHeight, setImageHeight] = useState<number>(200);
  
  // État pour suivre l'image actuellement agrandie (null si aucune)
  const [enlargedImageIndex, setEnlargedImageIndex] = useState<number | null>(null);
  
  // Fonction pour augmenter la taille des images
  const zoomIn = () => {
    setImageHeight((prev: number) => Math.min(prev + 25, 300));
  };
  
  // Fonction pour diminuer la taille des images
  const zoomOut = () => {
    setImageHeight((prev: number) => Math.max(prev - 25, 100));
  };
  
  // Fonction pour ouvrir l'image en mode agrandi
  const openEnlargedView = (index: number) => {
    setEnlargedImageIndex(index);
  };
  
  // Fonction pour fermer l'image en mode agrandi
  const closeEnlargedView = () => {
    setEnlargedImageIndex(null);
  };
  
  // Navigation vers l'image précédente
  const goToPrevious = (e: React.MouseEvent | KeyboardEvent) => {
    e.stopPropagation(); // Empêche la propagation qui fermerait la vue
    if (enlargedImageIndex !== null && enlargedImageIndex > 0) {
      setEnlargedImageIndex(enlargedImageIndex - 1);
    }
  };
  
  // Navigation vers l'image suivante
  const goToNext = (e: React.MouseEvent | KeyboardEvent) => {
    e.stopPropagation(); // Empêche la propagation qui fermerait la vue
    if (enlargedImageIndex !== null && enlargedImageIndex < mediaFiles.length - 1) {
      setEnlargedImageIndex(enlargedImageIndex + 1);
    }
  };

  return (
    <div className="image-viewer">
      <div className="image-viewer-header">
        <p>zoom :</p>
        <span className="zoom-level">{imageHeight}px</span>
        <i className="fi fi-br-add" onClick={zoomIn}></i>
        <i className="fi fi-br-minus-circle" onClick={zoomOut}></i>
      </div>

      <div
        className="image-viewer-container"
        style={{ height: `calc(100vh - ${defaultHeight}px)` }}
      >
        {mediaFiles.map((mediaFile, index) => (
          <RenderMediaItem
            key={mediaFile.path || index} // Utilisez une clé unique (de préférence `mediaFile.path`)
            mediaFile={mediaFile}
            imageHeight={imageHeight}
            index={index}
            openEnlargedView={openEnlargedView}
          />
        ))}
      </div>
      
      {/* Overlay pour l'image agrandie */}
      <EnlargedView
        enlargedImageIndex={enlargedImageIndex}
        mediaFiles={mediaFiles}
        closeEnlargedView={closeEnlargedView}
        goToPrevious={goToPrevious}
        goToNext={goToNext}
      />
    </div>
  );
}

export default ImagesViewer;