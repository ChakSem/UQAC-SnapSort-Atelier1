import { useState, useEffect, useRef } from "react";
import QRCode from 'qrcode';

// Style pour la section de transfert
const transferSectionStyle = {
  marginTop: '20px',
  padding: '15px',
  border: '1px solid #ddd',
  borderRadius: '8px',
  backgroundColor: '#f9f9f9'
};

// Type pour les informations de transfert
interface TransferInfo {
  fileName: string;
  progress: number;
  receivedBytes: number;
  totalBytes: number;
  index?: number;
  total?: number;
}

interface TransferComplete {
  fileName: string;
  filePath: string;
  size: number;
}

function Connexion() {
  const [wifiString, setWifiString] = useState<string>("");
  const [qrCode, setQrCode] = useState<string>("");
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [phoneIp, setPhoneIp] = useState<string>("");
  const [serverStatus, setServerStatus] = useState<string>("Arrêté");
  const [serverIp, setServerIp] = useState<string>("");
  
  // États pour le transfert de fichiers
  const [isTransferActive, setIsTransferActive] = useState<boolean>(false);
  const [currentTransfer, setCurrentTransfer] = useState<TransferInfo | null>(null);
  const [completedTransfers, setCompletedTransfers] = useState<TransferComplete[]>([]);
  const [transferError, setTransferError] = useState<string | null>(null);
  const [totalImages, setTotalImages] = useState<number>(0);
  const [currentImageIndex, setCurrentImageIndex] = useState<number>(0);

  const handleStartService = async () => {
    try {
      const result = await (window as any).electron.startImageTransferService();
      
      if (result?.wifiString && result?.serverIp) {
        setWifiString(result.wifiString);
        setServerIp(result.serverIp);
        setServerStatus("Actif");
        
        // Générer le QR Code avec les informations du WiFi ET du serveur
        const transferQRCode = await (window as any).electron.generateTransferQRCode(
          result.wifiString, 
          result.serverIp
        );
        
        generateQRCode(transferQRCode);
        console.log("Service démarré:", result);
      } else {
        setWifiString(result?.error || "Erreur lors de l'activation du service");
        setServerStatus("Erreur");
      }
    } catch (error) {
      console.error("Erreur:", error);
      setServerStatus("Erreur");
      setWifiString(String(error));
    }
  };

  const generateQRCode = async (wifiString: string) => {
    try {
      // Nettoyage du wifiString avant génération du QR Code
      const cleanedWifiString = wifiString.trim()
        .replace(/\r/g, "")  
        .replace(/\n/g, "") 
        .replace(/\s*;\s*/g, ";")  
        .replace(/\s+/g, " ");  

      // Génération du QR Code avec la string nettoyée
      const qrCodeDataUrl = await QRCode.toDataURL(cleanedWifiString, {
        errorCorrectionLevel: 'H',  
        width: 300,  
        margin: 1,  
        color: {
          dark: '#000000', 
          light: '#FFFFFF'  
        }
      });
      setQrCode(qrCodeDataUrl);
    } catch (error) {
      console.error("Erreur lors de la génération du QR Code:", error);
    }
  }
  
  // Fonction pour récupérer l'adresse IP du téléphone 
  const fetchIpAddress = async () => {
    const result = await (window as any).electron.getConnectedDevices();
    if (result.error) {
      setPhoneIp(result.error);
      return;
    }
    if (result.length === 0) {
      setPhoneIp("Aucune adresse IP trouvée");
      return;
    }
    // Prendre la première adresse IP trouvée
    setPhoneIp(result[0]);
    
    console.log("Adresses IP connectées:", result);
  }

  // S'abonner aux événements de transfert
  useEffect(() => {
    // Configurer les listeners d'événements
    const transferStartListener = (event: any, data: any) => {
      setIsTransferActive(true);
      setTransferError(null);
      
      // Si c'est le premier fichier, mettre à jour le nombre total
      if (data.index === 1) {
        setTotalImages(data.total || 1);
        setCurrentImageIndex(1);
      } else {
        setCurrentImageIndex(data.index || 1);
      }
      
      setCurrentTransfer({
        fileName: data.fileName,
        progress: 0,
        receivedBytes: 0,
        totalBytes: data.fileSize,
        index: data.index,
        total: data.total
      });
    };
    
    const transferProgressListener = (event: any, data: any) => {
      setCurrentTransfer(prev => {
        if (!prev) return data;
        return {
          ...prev,
          progress: data.progress,
          receivedBytes: data.receivedBytes,
          totalBytes: data.totalBytes
        };
      });
    };
    
    const transferCompleteListener = (event: any, data: any) => {
      // Ajouter le transfert terminé à la liste
      setCompletedTransfers(prev => [
        {
          fileName: data.fileName,
          filePath: data.filePath,
          size: data.size
        },
        ...prev.slice(0, 9)  // Garder seulement les 10 derniers transferts
      ]);
      
      // Si c'était le dernier fichier, réinitialiser l'état
      if (currentImageIndex >= totalImages) {
        setIsTransferActive(false);
        setCurrentTransfer(null);
        setCurrentImageIndex(0);
        setTotalImages(0);
      }
      // Sinon, attendre le prochain fichier mais effacer le transfert actuel
      else {
        setCurrentTransfer(null);
      }
    };
    
    const transferErrorListener = (event: any, data: any) => {
      setTransferError(data.error);
      setIsTransferActive(false);
      setCurrentTransfer(null);
    };
    
    // Ajouter les listeners
    (window as any).electron.on('transfer:start', transferStartListener);
    (window as any).electron.on('transfer:progress', transferProgressListener);
    (window as any).electron.on('transfer:complete', transferCompleteListener);
    (window as any).electron.on('transfer:error', transferErrorListener);
    
    // Nettoyer les listeners lors du démontage
    return () => {
      (window as any).electron.off('transfer:start', transferStartListener);
      (window as any).electron.off('transfer:progress', transferProgressListener);
      (window as any).electron.off('transfer:complete', transferCompleteListener);
      (window as any).electron.off('transfer:error', transferErrorListener);
    };
  }, [currentImageIndex, totalImages]);

  // Render du canvas QRCode
  useEffect(() => {
    if (canvasRef.current && qrCode) {
      const context = canvasRef.current.getContext("2d");
      if (context) {
        const img = new Image();
        img.src = qrCode;
        img.onload = () => {
          context.clearRect(0, 0, canvasRef.current!.width, canvasRef.current!.height);
          context.drawImage(img, 0, 0);
        };
      }
    }
  }, [qrCode]);

  // Formatter la taille en KB ou MB
  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="connexion-page">
      <main className="main-content">
        <section className="access-point-section">
          <button onClick={handleStartService} className="btn-access-point">
            Démarrer le service de transfert
          </button>
          <div className="access-info">
            <p>Statut du serveur: {serverStatus}</p>
            {serverIp && <p>Adresse IP du serveur: {serverIp}</p>}
            <p>{wifiString}</p>
          </div>
        </section>

        <section className="qrcode-section">
          {qrCode && (
            <div>
              <h3>Scannez le QR Code pour vous connecter:</h3>
              <img src={qrCode} alt="QR Code WiFi" />
            </div>
          )}
        </section>

        {/* Section pour afficher le statut du transfert */}
        <section style={transferSectionStyle}>
          <h3>Transfert d'images</h3>
          
          {isTransferActive && (
            <div>
              {totalImages > 1 && (
                <p>Progression totale: {currentImageIndex} / {totalImages} images</p>
              )}
              
              {currentTransfer ? (
                <div>
                  <p>Réception en cours: {currentTransfer.fileName}</p>
                  <progress 
                    value={currentTransfer.progress * 100} 
                    max="100" 
                    style={{width: '100%', height: '20px'}}
                  />
                  <p>{formatSize(currentTransfer.receivedBytes)} / {formatSize(currentTransfer.totalBytes)} 
                    ({(currentTransfer.progress * 100).toFixed(1)}%)</p>
                </div>
              ) : (
                <p>Préparation du transfert suivant...</p>
              )}
            </div>
          )}
          
          {!isTransferActive && !transferError && (
            <p>En attente de transferts...</p>
          )}
          
          {transferError && (
            <div style={{color: 'red'}}>
              <p>Erreur: {transferError}</p>
            </div>
          )}
          
          {completedTransfers.length > 0 && (
            <div>
              <h4>Transferts récents:</h4>
              <ul>
                {completedTransfers.map((transfer, index) => (
                  <li key={index}>
                    {transfer.fileName} - {formatSize(transfer.size)}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>

        <div className="get-ip">
          <button onClick={fetchIpAddress} className="btn-access-point">
            Obtenir l'adresse IP du téléphone
          </button>
          <p>Adresse IP du téléphone: {phoneIp}</p>
        </div>
      </main>
    </div>
  );
}

export default Connexion;