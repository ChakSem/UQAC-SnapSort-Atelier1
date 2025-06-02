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

  // Démarre le service de transfert d'images
  const startService = async () => {
    try {
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
    }
  };

  // Arrête le service de transfert
  const stopService = async () => {
    try {
      const result = await (window as any).electron.stopImageTransferService();
      setIsServiceActive(false);
      setTransferQrCode("");
      setCurrentTransfer(null);
      console.log("Service de transfert arrêté:", result);
    } catch (error) {
      console.error("Erreur lors de l'arrêt du service de transfert:", error);
    }
  };

  // Récupère la liste des appareils connectés
  const fetchConnectedDevices = async () => {
    try {
      const devices = await (window as any).electron.getConnectedDevices();
      setConnectedDevices(Array.isArray(devices) ? devices : []);
    } catch (error) {
      console.error("Erreur lors de la récupération des appareils connectés:", error);
    }
  };

  // Configuration des écouteurs d'événements
  useEffect(() => {
    // Vérification de l'état initial du service
    const checkServiceStatus = async () => {
      const status = await (window as any).electron.getTransferServiceStatus();
      setIsServiceActive(status.active);
    };
    
    checkServiceStatus();
    
    // Configuration des gestionnaires d'événements de transfert
    const unsubscribeStart = (window as any).electron.on('transfer:start', (info: TransferInfo) => {
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
    
    const unsubscribeProgress = (window as any).electron.on('transfer:progress', (info: TransferInfo) => {
      setCurrentTransfer(info);
    });
    
    const unsubscribeComplete = (window as any).electron.on('transfer:complete', (info: { fileName: string }) => {
      console.log("Transfert terminé:", info);
      setCompletedTransfers(prev => [...prev, info.fileName]);
      setCurrentTransfer(null);
    });
    
    const unsubscribeError = (window as any).electron.on('transfer:error', (info: { error: string }) => {
      console.error("Erreur de transfert:", info);
    });
    
    // Nettoyage des écouteurs
    return () => {
      unsubscribeStart();
      unsubscribeProgress();
      unsubscribeComplete();
      unsubscribeError();
    };
  }, []);

  // Récupération périodique des appareils connectés
  useEffect(() => {
    if (isServiceActive) {
      fetchConnectedDevices();
      const interval = setInterval(fetchConnectedDevices, 5000);
      
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
    startService,
    stopService,
    fetchConnectedDevices
  };
};