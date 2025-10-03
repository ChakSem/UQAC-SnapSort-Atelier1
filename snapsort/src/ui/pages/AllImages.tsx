import { useEffect, useState, useRef } from "react";
import '../styles/components.css';
import ImagesViewer from "../components/ImageViewer";
import { MediaFile, Status } from "../types/types";
import SearchBar, { SearchBarRef } from "../components/SearchBar";
import anim_spinner from "../assets/anim_spinner.svg";

const AllImages =() => {
    const [files, setFiles] = useState<MediaFile[]>([]);
    const [aiSearching, setAISearching] = useState(false);
    const [status, setStatus] = useState<Status>('no-loading');
    const [progress, setProgress] = useState(0);
    const [logs, setLogs] = useState<string[]>([]);
    const searchBarRef = useRef<SearchBarRef>(null);
    

    // runPythonScript
    const runImageRetrival = async (prompt: string) => {
        // Change the UI state to indicate that AI processing is in progress
        setAISearching(true);

        // Call the Python script
        try {
        const listOrder = await (window as any).electron.runPythonRetrieveImages(prompt);
            const newOrderedFiles = reorderImages(files, listOrder);
            setFiles(newOrderedFiles);
        } catch (error) {
            console.log(`Error: ${error}`);
        }

        setAISearching(false);
    };

    const handleSearchButtonClick = () => {
        const searchValue = searchBarRef.current?.getValue();
        if (searchValue) {
            runImageRetrival(searchValue);
        }
    };

    const handleExtendLoading = () => {
        setStatus('extended-loading');
    }

    const handleReduceLoading = () => {
        setStatus('loading');
    }

    const handlePythonEnd = () => {
        setStatus('no-loading');
    };

    // Handler pour les logs Python
    const handleLog = (msg: string) => {
        console.log(msg);
        // Estimer le progrès
        estimateProgress(msg);
        // Store the progress in the state
        setLogs(prevLogs => {
            const newLogs = [...prevLogs, msg];
            return newLogs.length > 30 ? newLogs.slice(newLogs.length - 30) : newLogs;
        });
    };

    const estimateProgress = (msg: string) => {
        // New format: [PERCENTAGE]: 17 / 18
        let match = msg.match(/\[PERCENTAGE\]:\s*(\d+)\s*\/\s*(\d+)/);

        if (match) {
            const [, current, total] = match;
            const progress = Math.round((parseInt(current) / parseInt(total)) * 100);
            setProgress(progress);
            (window as any).electron.setGlobalVariables("AIFillingDatabaseProgress", progress);
            return;
        } else {
            console.log(`Does not match: ${msg}`);
        }
    }

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
        // Load the global variables for database filling
        (window as any).electron.getGlobalVariables("AIFillingDatabase").then((value: boolean) => {
            setStatus(value ? 'loading' : 'no-loading');
        });
    
        (window as any).electron.getGlobalVariables("AIFillingDatabaseProgress").then((value: number) => {
            setProgress(value);
        });

        loadMediaFiles();
    }, []);

    useEffect(() => {
    
        // Listen to the Python script log and end events
        (window as any).electron.onPythonLog('retrieval', handleLog);

        (window as any).electron.onPythonLog('database', handleLog);
        (window as any).electron.onPythonEnd('database', handlePythonEnd);
    
        // Clean up to avoid duplicates
        return () => {
          (window as any).electron.removePythonLogListener('retrieval');

          (window as any).electron.removePythonLogListener('database');
          (window as any).electron.removePythonEndListener('database', handlePythonEnd);
        };
      }, []);


    return (
        <div className="all-images">

            <div className="all-images-header">
                <div className="all-images-header-search">
                    <SearchBar ref={searchBarRef} onSearch={runImageRetrival} />
                    <button onClick={handleSearchButtonClick}>
                        Rechercher
                    </button>
                </div>
                {aiSearching === true && (
                    <div className="all-images-header-loading">
                        <img src={anim_spinner} alt="AI Processing" style={{ width: 32, height: 32 }} />
                        <span className="ai-processing-text">
                            Recherche des images en cours
                        </span>
                    </div>
                )}
            </div>

            <div className="container">
                {status === "no-loading" && (<ImagesViewer mediaFiles={files} />)}
                {status === "loading" && (<ImagesViewer mediaFiles={files} height={194}/>)}
                {status === "extended-loading" && (<ImagesViewer mediaFiles={files} height={525}/>)}
            </div>

            {status === "loading" && (
            <div className="unsorted-images-loading-bar">
                <i onClick={handleExtendLoading} className="fi fi-rr-angle-double-small-up"></i>
                <div className="unsorted-images-loading-bar-progress">
                <progress value={progress} max="100"></progress>
                <span>{progress} %</span>
                </div>
            </div>
            )}

            {status === "extended-loading" && (
            <div className="unsorted-images-loading-bar">
                <i onClick={handleReduceLoading} className="fi fi-rr-angle-double-small-down"></i>
                <p>Traitement des images en cours...</p>
                <div className="unsorted-images-loading-bar-progress">
                <progress value={progress} max="100"></progress>
                <span>{progress} %</span>
                </div>
                <div className="unsorted-images-log-container">
                <div className="unsorted-images-log-content">
                    {logs.map((log, index) => (
                    <div className="unsorted-images-log-item" key={index}>log : {log}</div>
                    ))}
                </div>
                </div>
            </div>
            )}
        </div>
    );
}

export default AllImages;