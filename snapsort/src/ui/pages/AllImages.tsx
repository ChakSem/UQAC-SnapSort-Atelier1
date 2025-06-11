import { useEffect, useState } from "react";
import '../styles/components.css';
import ImagesViewer from "../components/ImageViewer";
import { MediaFile } from "../types/types";
import SearchBar from "../components/SearchBar";
import anim_spinner from "../assets/anim_spinner.svg";

const AllImages =() => {
    const [files, setFiles] = useState<MediaFile[]>([]);
    const [aiProcessing, setAIProcessing] = useState(false);

    // runPythonScript
    const runImageRetrival = async (prompt: string) => {
        // Change the UI state to indicate that AI processing is in progress
        setAIProcessing(true);

        // Call the Python script
        try {
        const listOrder = await (window as any).electron.runImageRetrival(prompt);
            console.log("List order from Python:", listOrder);
            const newOrderedFiles = reorderImages(files, listOrder);
            console.log("Reordered files:", newOrderedFiles);
            setFiles(newOrderedFiles);
        } catch (error) {
            console.log(`Error: ${error}`);
        }

        setAIProcessing(false);
    };

    // Handler pour les logs Python
    const handleLog = (msg: string) => {
        console.log(msg);
    };

    function reorderImages(images: MediaFile[], desiredOrder: string[]): MediaFile[] {
        // Créer une map des images par leur nom
        const imageMap: Map<string, MediaFile> = new Map(images.map(img => [img.name, img]));

        // Récupérer dans l'ordre les images présentes dans desiredOrder
        const ordered: MediaFile[] = desiredOrder
            .map(name => imageMap.get(name))
            .filter((img): img is MediaFile => img !== undefined); // Filtrer les undefined

        // Ajouter les images restantes qui ne sont pas dans desiredOrder
        const remaining: MediaFile[] = images.filter(img => !desiredOrder.includes(img.name));

        return [...ordered, ...remaining];
    }


    const loadMediaFiles = async () => {
        // Charger le chemin du dossier principal
        (window as any).electron.getSetting("directoryPath").then((path: string) => {
        
            if (path) {
                // Créer le chemin vers le sous-dossier "unsorted_images"
                const unsortedPath = `${path}${path.endsWith('/') || path.endsWith('\\') ? '' : '/'}all_images`;
                
                // Charger les fichiers du sous-dossier
                (window as any).electron.getMediaFiles(unsortedPath).then((response: any) => {
                if (response.files) {
                    setFiles(response.files);
                    console.log("Media files loaded:", response.files);
                } else if (response.error) {
                    console.error("Error loading media files:", response.error);
                }
                });
            }
        });
    }

    useEffect(() => {
        loadMediaFiles();
    }, []);

    useEffect(() => {
    
        // Écouter les événements du script Python
        (window as any).electron.onPythonLog(handleLog);

        // Nettoyage pour éviter les doublons
        return () => {
        (window as any).electron.removePythonLogListener?.(handleLog);
        };
    }, []);


    return (
        <div className="all-images">
            <div className="all-images-header">
                <SearchBar onSearch={runImageRetrival} />
                {aiProcessing === true && (
                    <div className="all-images-header-loading">
                        <img src={anim_spinner} alt="AI Processing" style={{ width: 32, height: 32 }} />
                        <span className="ai-processing-text">Recherche des images en cours</span>
                    </div>
                )}
            </div>
            <div className="container">
                <ImagesViewer mediaFiles={files} />
            </div>
        </div>
    );
}

export default AllImages;