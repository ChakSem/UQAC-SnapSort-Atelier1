import { useEffect, useState } from "react";
import '../styles/components.css';
import ImagesViewer from "../components/ImageViewer";
import { MediaFile } from "../types/types";
import { Status } from "../types/types";
import { useNavigate } from "react-router-dom";

function UnsortedImages() {
  const [files, setFiles] = useState<MediaFile[]>([]);
  const [status, setStatus] = useState<Status>('no-loading');
  const [logs, setLogs] = useState<string[]>([]);
  const [progress, setProgress] = useState(0);
  const navigate = useNavigate();

  const runPythonScript = async () => {
    // Change the UI state to indicate that AI processing is in progress
    setStatus('extended-loading');

    // Call the Python script
    try {
      const output = await (window as any).electron.runPythonSortImages();
      console.log(output);
    } catch (error) {
      console.log(`Error: ${error}`);
    }
  };

  const handleExtendLoading = () => {
    setStatus('extended-loading');
  }

  const handleReduceLoading = () => {
    setStatus('loading');
  }

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

  const handlePythonEnd = () => {
    // Redirect to Albums page
    navigate("/albums");
  };

  const estimateProgress = (msg: string) => {
    const match = msg.match(/Etape \[(\d+)\/(\d+)\] : \[(\d+)\/(\d+)\]/);

    if (match) {
      // Extract the numbers from the message
      const [_, nbr1, nbr2, nbr3, nbr4] = match;

      // Calculate the progress percentage
      const gap = (1 / parseInt(nbr2)) * 100;
      const progress1 = ((parseInt(nbr1) - 1) / parseInt(nbr2)) * 100;
      const progress2 = (parseInt(nbr3) / parseInt(nbr4)) * gap;
      const progress = Math.round(progress1 + progress2);
      setProgress(progress);
      // Set the global variable AISortingProgress
      (window as any).electron.setGlobalVariables("AISortingProgress", progress);

    } else {
      console.log(`Does not match: ${msg}`);
    }
  }

  useEffect(() => {
    // Load the global variable AISorting and AISortingProgress
    (window as any).electron.getGlobalVariables("AISorting").then((value: boolean) => {
      setStatus(value ? 'loading' : 'no-loading');
    });

    (window as any).electron.getGlobalVariables("AISortingProgress").then((value: number) => {
      setProgress(value);
    });

    // Load the root directory path
    (window as any).electron.getSetting("directoryPath").then((path: string) => {
      
      if (path) {
        // Create the path to the "unsorted_images" subfolder
        const unsortedPath = `${path}${path.endsWith('/') || path.endsWith('\\') ? '' : '/'}unsorted_images`;
        
        // Load the files from the subfolder
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

  useEffect(() => {
    
    // Listen to the Python script log and end events
    (window as any).electron.onPythonLog('sorting', handleLog);
    (window as any).electron.onPythonEnd('sorting', handlePythonEnd);

    // Clean up to avoid duplicates
    return () => {
      (window as any).electron.removePythonLogListener('sorting');
      (window as any).electron.removePythonEndListener('sorting', handlePythonEnd);
    };
  }, []);

  return (
    <div className="unsorted-images">

        {status === "no-loading" && (<ImagesViewer mediaFiles={files} />)}
        {status === "loading" && (<ImagesViewer mediaFiles={files} height={175.6}/>)}
        {status === "extended-loading" && (<ImagesViewer mediaFiles={files} height={504.4}/>)}

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

        {status === "no-loading" && (<div className="unsorted-images-bottombar">
          <button
            onClick={runPythonScript}
            style={{
              backgroundColor: "var(--third-button-bg)",
              cursor: "pointer"
            }}
          >
            Tri automatique</button>
          <button
            style={{
              backgroundColor: "var(--third-button-bg)",
              cursor: "pointer"
            }}
          >
            Tri avancé (experimental)</button>
        </div>)}
        {status !== "no-loading" && (<div className="unsorted-images-bottombar">
          <button
            style={{
              backgroundColor: "var(--fourth-button-bg)",
              cursor: "pointer"
              // cursor: "not-allowed"
            }}
          >
            Tri automatique</button>
          <button
            style={{
              backgroundColor: "var(--fourth-button-bg)",
              cursor: "not-allowed"
            }}
          >
            Tri avancé (experimental)</button>
        </div>)}
    </div>
  );
}

export default UnsortedImages;
