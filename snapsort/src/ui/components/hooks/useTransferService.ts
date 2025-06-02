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
  vendor?: string;
  status: 'active' | 'inactive';
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
      
      // Démarrer immédiatement un scan des appareils
      await fetchConnectedDevices();
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
      setConnectedDevices([]); // Vider la liste des appareils
      console.log("Service de transfert arrêté:", result);
    } catch (error) {
      console.error("Erreur lors de l'arrêt du service de transfert:", error);
    }
  };

  // Récupère la liste des appareils connectés
  const fetchConnectedDevices = async () => {
    try {
      // Vérifier si la fonction existe avant de l'appeler
      if (typeof (window as any).electron?.getConnectedDevices === 'function') {
        console.log('📱 Scan des appareils connectés...');
        const devices = await (window as any).electron.getConnectedDevices();
        
        if (Array.isArray(devices)) {
          setConnectedDevices(devices);
          console.log(`📱 ${devices.length} appareil(s) connecté(s) trouvé(s)`);
          
          // Afficher les détails des appareils trouvés
          if (devices.length > 0) {
            devices.forEach((device, index) => {
              console.log(`📱 Appareil ${index + 1}: ${device.name} (${device.ip}) - ${device.vendor || 'Fabricant inconnu'}`);
            });
          }
        } else {
          console.log("📱 Format de données incorrect reçu du scanner");
          setConnectedDevices([]);
        }
      } else {
        console.log("⚠️ Fonction getConnectedDevices non disponible");
        setConnectedDevices([]);
      }
    } catch (error) {
      console.error("❌ Erreur lors de la récupération des appareils connectés:", error);
      setConnectedDevices([]);
    }
  };

  // Récupère les statistiques réseau
  const fetchNetworkStats = async () => {
    try {
      if (typeof (window as any).electron?.getNetworkStats === 'function') {
        const stats = await (window as any).electron.getNetworkStats();
        console.log('📊 Statistiques réseau:', stats);
        return stats;
      }
    } catch (error) {
      console.error("Erreur lors de la récupération des stats réseau:", error);
    }
    return null;
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
            // Essayer d'abord de récupérer l'IP réelle
            const ipResult = await (window as any).electron.getIpAdress?.();
            if (ipResult) {
              setServerIp(ipResult);
            } else {
              // Utiliser l'IP par défaut du hotspot Windows
              setServerIp("192.168.137.1");
            }
            
            // Scanner les appareils connectés si le service est déjà actif
            await fetchConnectedDevices();
          } catch (error) {
            console.log("Impossible de récupérer l'IP du serveur, utilisation de l'IP par défaut");
            setServerIp("192.168.137.1");
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
        console.log("🚀 Transfert démarré:", info.fileName);
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
        setCurrentTransfer(prevTransfer => {
          if (!prevTransfer) return null;
          
          const updatedTransfer = {
            ...prevTransfer,
            ...info
          };
          
          // Log de progression uniquement pour les étapes importantes
          if (info.progress && (info.progress === 1.0 || info.progress % 0.25 === 0)) {
            console.log(`📈 Progression ${info.fileName}: ${(info.progress * 100).toFixed(1)}%`);
          }
          
          return updatedTransfer;
        });
      });
      
      const unsubscribeComplete = electron.on('transfer:complete', (info: { fileName: string }) => {
        console.log("✅ Transfert terminé:", info.fileName);
        setCompletedTransfers(prev => [...prev, info.fileName]);
        setCurrentTransfer(null);
        
        // Optionnel: rescanner les appareils après un transfert
        // setTimeout(fetchConnectedDevices, 2000);
      });
      
      const unsubscribeError = electron.on('transfer:error', (info: { error: string }) => {
        console.error("❌ Erreur de transfert:", info.error);
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
      // Scan initial
      fetchConnectedDevices();
      
      // Scan périodique toutes les 15 secondes pour éviter la surcharge
      const interval = setInterval(() => {
        fetchConnectedDevices();
      }, 15000);
      
      return () => {
        clearInterval(interval);
      };
    } else {
      // Vider la liste si le service n'est pas actif
      setConnectedDevices([]);
    }
  }, [isServiceActive]);

  // Fonction pour forcer un refresh manuel des appareils
  const refreshConnectedDevices = async () => {
    if (isServiceActive) {
      await fetchConnectedDevices();
    }
  };

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
    fetchConnectedDevices: refreshConnectedDevices,
    fetchNetworkStats
  };
};