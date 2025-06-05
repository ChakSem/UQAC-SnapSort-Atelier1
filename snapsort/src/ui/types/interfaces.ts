import { MediaFile } from './types';

// Définition du type pour les props
export interface ImagesViewerProps {
    mediaFiles: MediaFile[];
    height?: number;
}

export interface NavBarItemProps {
    label: string;
    imageUrl: string;
    redirectTo: string;
}
  
export interface BottomItemProps {
    imageUrl: string;
    redirectTo: string;
}

// Définition des props pour le composant
export interface RenderMediaItemProps {
    mediaFile: MediaFile;
    imageHeight: number;
    index: number;
    openEnlargedView: (index: number) => void;
  }

export interface EnlargedViewProps {
    enlargedImageIndex: number | null;
    mediaFiles: MediaFile[];
    closeEnlargedView: () => void;
    goToPrevious: (e: React.MouseEvent | KeyboardEvent) => void;
    goToNext: (e: React.MouseEvent | KeyboardEvent) => void;
}

export interface FolderTreeProps {
    handleGetMediaFiles: (path: String) => Promise<void>;
}