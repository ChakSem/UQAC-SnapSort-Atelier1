import { useEffect, useState } from "react";
import '../styles/components.css';
import ImagesViewer from "../components/ImageViewer";
import { MediaFile } from "../types/types";
import SearchBar from "../components/SearchBar";

const AllImages =() => {
    const [files, setFiles] = useState<MediaFile[]>([]);


    useEffect(() => {

        // Charger le chemin du dossier principal
        (window as any).electron.getSetting("directoryPath").then((path: string) => {
        
            if (path) {
                // Créer le chemin vers le sous-dossier "unsorted_images"
                const unsortedPath = `${path}${path.endsWith('/') || path.endsWith('\\') ? '' : '/'}all_images`;
                
                // Charger les fichiers du sous-dossier
                (window as any).electron.getMediaFiles(unsortedPath).then((response: any) => {
                if (response.files) {
                    setFiles(response.files);
                } else if (response.error) {
                    console.error("Error loading media files:", response.error);
                }
                });
            }
        });
    }, []);


    return (
        <div className="all-images">
            <div className="all-images-header">
                <SearchBar />
            </div>
            <div className="container">
                <ImagesViewer mediaFiles={files} />
            </div>
        </div>
    );
}

export default AllImages;