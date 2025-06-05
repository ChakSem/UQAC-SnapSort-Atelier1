import { contextBridge, ipcRenderer } from 'electron';

// Interface pour les appareils connectés
interface ConnectedDevice {
  ip: string;
  mac: string;
  name: string;
  vendor?: string;
  status: 'active' | 'inactive';
}

// Interface pour les statistiques réseau
interface NetworkStats {
  hotspotActive: boolean;
  hotspotIP: string | null;
  connectedDevices: number;
  lastScanTime: Date;
}

// Canaux d'événements autorisés pour la sécurité
const VALID_CHANNELS = [
  'transfer:start',
  'transfer:progress',
  'transfer:complete',
  'transfer:error',
  'transfer:service-started',
  'transfer:service-stopped'
] as const;

let logHandler: ((event: any, msg: string) => void) | null = null;

// Exposition des APIs Electron au renderer process
contextBridge.exposeInMainWorld('electron', {
  
  // ========== GESTIONNAIRES PYTHON ==========
  runPython: () => ipcRenderer.invoke('run-python'),
  onPythonLog: (callback: (msg: string) => void) => {
      logHandler = (_, msg) => callback(msg);
      ipcRenderer.on("log", logHandler);
  },
  removePythonLogListener: () => {
      if (logHandler) {
          ipcRenderer.removeListener("log", logHandler);
          logHandler = null;
      }
  },


  // ========== GESTIONNAIRES PARAMÈTRES ==========
  getSetting: (key: string) => ipcRenderer.invoke('get-setting', key),
  setSetting: (key: string, value: any) => ipcRenderer.invoke('set-setting', key, value),
  selectDirectory: () => ipcRenderer.invoke('select-directory'),

  // ========== GESTIONNAIRES FICHIERS MÉDIA ==========
  getMediaFiles: (key: string) => ipcRenderer.invoke("get-media-files", key),
  getGlobalVariables: (key: string) => ipcRenderer.invoke("get-global-variables", key),
  setGlobalVariables: (key: string, value: any) => ipcRenderer.invoke("set-global-variables", key, value),

  // ========== GESTIONNAIRES CONNEXION HOTSPOT ==========
  startHotspot: () => ipcRenderer.invoke('start-hotspot'),
  getIpAdress: () => ipcRenderer.invoke('get-ip'),
  getWifiInfo: () => ipcRenderer.invoke('get-wifi-info'),

  // ========== GESTIONNAIRES RÉSEAU ET APPAREILS CONNECTÉS ==========
  getConnectedDevices: (): Promise<ConnectedDevice[]> => ipcRenderer.invoke('get-connected-devices'),
  getNetworkStats: (): Promise<NetworkStats> => ipcRenderer.invoke('get-network-stats'),

  // ========== GESTIONNAIRES SERVICE DE TRANSFERT ==========
  startImageTransferService: () => ipcRenderer.invoke('start-image-transfer-service'),
  stopImageTransferService: () => ipcRenderer.invoke('stop-image-transfer-service'),
  generateTransferQRCode: (wifiString: string, serverIp: string) => 
    ipcRenderer.invoke('generate-transfer-qrcode', wifiString, serverIp),
  getTransferServiceStatus: () => ipcRenderer.invoke('get-transfer-service-status'),

  // ========== GESTIONNAIRES DOSSIERS ==========
  getFolders: (rootPath: string) => ipcRenderer.invoke('get-folders', rootPath),

  // ========== GESTIONNAIRES D'ÉVÉNEMENTS ==========
  
  // Fonction pour s'abonner aux événements
  on: (channel: string, callback: (...args: any[]) => void) => {
    if (VALID_CHANNELS.includes(channel as any)) {
      const subscription = (_event: any, ...args: any[]) => callback(...args);
      ipcRenderer.on(channel, subscription);
      
      // Retourner une fonction de nettoyage
      return () => {
        ipcRenderer.removeListener(channel, subscription);
      };
    } else {
      console.warn(`Canal d'événement non autorisé: ${channel}`);
      return () => {}; // Fonction de nettoyage vide
    }
  },

  // Fonction pour se désabonner d'un événement spécifique
  off: (channel: string, callback: (...args: any[]) => void) => {
    if (VALID_CHANNELS.includes(channel as any)) {
      ipcRenderer.removeListener(channel, callback);
    } else {
      console.warn(`Canal d'événement non autorisé: ${channel}`);
    }
  },

  // Fonction pour supprimer tous les écouteurs d'un canal
  removeAllListeners: (channel: string) => {
    if (VALID_CHANNELS.includes(channel as any)) {
      ipcRenderer.removeAllListeners(channel);
    } else {
      console.warn(`Canal d'événement non autorisé: ${channel}`);
    }
  },

  // ========== UTILITAIRES DE DEBUG ==========
  
  // Fonction pour tester la connectivité (utile pour le développement)
  testConnectivity: async () => {
    try {
      const results = {
        hotspot: await ipcRenderer.invoke('get-wifi-info'),
        devices: await ipcRenderer.invoke('get-connected-devices'),
        networkStats: await ipcRenderer.invoke('get-network-stats'),
        transferStatus: await ipcRenderer.invoke('get-transfer-service-status'),
        timestamp: new Date().toISOString()
      };
      
      console.log('🔍 Test de connectivité:', results);
      return results;
    } catch (error) {
      console.error('❌ Erreur lors du test de connectivité:', error);
      return { error: String(error), timestamp: new Date().toISOString() };
    }
  },

  // Fonction pour obtenir la version de l'API
  getApiVersion: () => '1.0.0',

  // Fonction pour lister les canaux d'événements disponibles
  getValidChannels: () => [...VALID_CHANNELS]
});

// Déclaration TypeScript globale pour l'objet window
declare global {
  interface Window {
    electron: {
      // Python
      runPython: () => Promise<any>;
      
      // Paramètres  
      getSetting: (key: string) => Promise<any>;
      setSetting: (key: string, value: any) => Promise<void>;
      selectDirectory: () => Promise<string | null>;
      
      // Fichiers média
      getMediaFiles: (directoryPath: string) => Promise<any>;
      
      // Connexion hotspot
      startHotspot: () => Promise<any>;
      getIpAdress: () => Promise<string | null>;
      getWifiInfo: () => Promise<any>;
      
      // Réseau et appareils
      getConnectedDevices: () => Promise<ConnectedDevice[]>;
      getNetworkStats: () => Promise<NetworkStats>;
      
      // Service de transfert
      startImageTransferService: () => Promise<any>;
      stopImageTransferService: () => Promise<any>;
      generateTransferQRCode: (wifiString: string, serverIp: string) => Promise<string>;
      getTransferServiceStatus: () => Promise<any>;
      
      // Dossiers
      getFolders: (rootPath: string) => Promise<any>;
      
      // Événements
      on: (channel: string, callback: (...args: any[]) => void) => () => void;
      off: (channel: string, callback: (...args: any[]) => void) => void;
      removeAllListeners: (channel: string) => void;
      
      // Utilitaires
      testConnectivity: () => Promise<any>;
      getApiVersion: () => string;
      getValidChannels: () => readonly string[];
    };
  }
}

export {};