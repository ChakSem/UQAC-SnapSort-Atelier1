import { useState, useEffect } from 'react';

interface TransferInfo {
  fileName: string;
  progress: number;
  receivedBytes: number;
  totalBytes: number;
  fileSize?: number;
  index?: number;
  total?: number;
}

interface ConnectedDevice {
  ip: string;
  mac: string;
  name: string;
}

export const useTransferService = () => {
  const [isServiceActive, setIsServiceActive] = useState<boolean>(false);
  const [transferQrCode, setTransferQrCode] = useState<string>("");
  const [completedTransfers, setCompletedTransfers] = useState<string[]>([]);
  const [currentTransfer, setCurrentTransfer] = useState<TransferInfo | null>(null);
  const [serverIp, setServerIp] = useState<string>("");
  const [connectedDevices, setConnectedDevices] = useState<ConnectedDevice[]>([]);
  const [isStarting, setIsStarting] = useState<boolean>(false);

  // Démarre le service de transfert d'images
  const startService = async () => {
    if (isStarting || isServiceActive) {
      console.log("Service déjà en cours de démarrage ou actif");
      return;
    }

    setIsStarting(true);
    try {
      // Arrêter d'abord tout service existant
      await stopService();
      
      // Attendre un court délai pour s'assurer que le port est libéré
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const result = await (window as any).electron.startImageTransferService();
      
      if (result.error) {
        console.error("Erreur lors du démarrage du service:", result.error);
        return;
      }
      
      setIsServiceActive(true);
      setServerIp(result.serverIp);
      console.log("Service de transfert démarré:", result.serverIp);
    } catch (error) {
      console.error("Erreur lors du démarrage du service de transfert:", error);
    } finally {
      setIsStarting(false);
    }
  };

  // Arrête le service de transfert
  const stopService = async () => {
    try {
      const result = await (window as any).electron.stopImageTransferService();
      setIsServiceActive(false);
      setTransferQrCode("");
      setCurrentTransfer(null);
      setServerIp("");
      console.log("Service de transfert arrêté:", result);
    } catch (error) {
      console.error("Erreur lors de l'arrêt du service de transfert:", error);
    }
  };

  // Récupère la liste des appareils connectés (fonction protégée)
  const fetchConnectedDevices = async () => {
    try {
      // Vérifier si la fonction existe avant de l'appeler
      if (typeof (window as any).electron?.getConnectedDevices === 'function') {
        const devices = await (window as any).electron.getConnectedDevices();
        setConnectedDevices(Array.isArray(devices) ? devices : []);
      } else {
        // Fonction non implémentée - on utilise un mock temporaire
        console.log("Fonction getConnectedDevices non implémentée");
        setConnectedDevices([]);
      }
    } catch (error) {
      console.error("Erreur lors de la récupération des appareils connectés:", error);
      setConnectedDevices([]);
    }
  };

  // Configuration des écouteurs d'événements
  useEffect(() => {
    // Vérification de l'état initial du service
    const checkServiceStatus = async () => {
      try {
        const status = await (window as any).electron.getTransferServiceStatus();
        setIsServiceActive(status.active);
        if (status.active) {
          // Si le service est actif, récupérer l'IP du serveur
          try {
            const ipResult = await (window as any).electron.getIpAdress();
            if (ipResult) {
              setServerIp("192.168.137.1"); // IP par défaut du hotspot Windows
            }
          } catch (error) {
            console.log("Impossible de récupérer l'IP du serveur");
          }
        }
      } catch (error) {
        console.error("Erreur lors de la vérification du statut:", error);
      }
    };
    
    checkServiceStatus();
    
    // Configuration des gestionnaires d'événements de transfert
    const setupEventListeners = () => {
      const electron = (window as any).electron;
      
      if (!electron || typeof electron.on !== 'function') {
        console.error("API Electron non disponible");
        return () => {}; // Fonction de nettoyage vide
      }

      const unsubscribeStart = electron.on('transfer:start', (info: TransferInfo) => {
        console.log("Transfert démarré:", info);
        setCurrentTransfer({
          fileName: info.fileName,
          progress: 0,
          receivedBytes: 0,
          totalBytes: info.fileSize || 0,
          index: info.index,
          total: info.total
        });
      });
      
      const unsubscribeProgress = electron.on('transfer:progress', (info: TransferInfo) => {
        setCurrentTransfer(prevTransfer => ({
          ...prevTransfer,
          ...info
        }));
      });
      
      const unsubscribeComplete = electron.on('transfer:complete', (info: { fileName: string }) => {
        console.log("Transfert terminé:", info);
        setCompletedTransfers(prev => [...prev, info.fileName]);
        setCurrentTransfer(null);
      });
      
      const unsubscribeError = electron.on('transfer:error', (info: { error: string }) => {
        console.error("Erreur de transfert:", info);
        setCurrentTransfer(null);
      });
      
      // Retourner la fonction de nettoyage
      return () => {
        try {
          unsubscribeStart();
          unsubscribeProgress();
          unsubscribeComplete();
          unsubscribeError();
        } catch (error) {
          console.error("Erreur lors du nettoyage des écouteurs:", error);
        }
      };
    };

    const cleanup = setupEventListeners();
    
    // Nettoyage à la destruction du composant
    return cleanup;
  }, []);

  // Récupération périodique des appareils connectés (uniquement si service actif)
  useEffect(() => {
    if (isServiceActive) {
      fetchConnectedDevices();
      
      // Réduire la fréquence pour éviter le spam d'erreurs
      const interval = setInterval(fetchConnectedDevices, 10000); // 10 secondes au lieu de 5
      
      return () => clearInterval(interval);
    }
  }, [isServiceActive]);

  return {
    isServiceActive,
    transferQrCode,
    completedTransfers,
    currentTransfer,
    serverIp,
    connectedDevices,
    isStarting,
    startService,
    stopService,
    fetchConnectedDevices
  };
};