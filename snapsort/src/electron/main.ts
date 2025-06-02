import { app, BrowserWindow, dialog, ipcMain } from 'electron';
import http from 'http';
import { 
  startImageTransferService, 
  stopImageTransferService, 
  generateTransferQRCode, 
  transferEvents 
} from './server.js';
import path from 'path';
import fs from 'fs';
import { isDev, cleanTempFolder, generateThumbnail } from './util.js';
import { getPreloadPath, getScriptsPath } from './pathResolver.js';
import connectionService from './connectionService.js';
import store from "./store.js";
import { getFolders } from './folderManager.js';
import { runPipeline } from './python.js';

let mainWindow: BrowserWindow | null = null;

// Configuration de la fenêtre principale
app.on('ready', () => {
  mainWindow = new BrowserWindow({
    width: 1340,
    height: 900,
    webPreferences: {
      nodeIntegration: false,
      webSecurity: false,
      preload: getPreloadPath(),
    },
    frame: true
  });

  if (isDev()) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(app.getAppPath(), '/dist-react/index.html'));
  }
});

// ========== Gestionnaires Python ==========

// Exécution du script Python pour le tri d'images
ipcMain.handle('run-python', async () => {
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) {
    return { error: "No root directory path set" };
  }

  const unsortedImagesPath = path.join(rootPath, "unsorted_images");
  if (!fs.existsSync(unsortedImagesPath)) {
    return { error: "Dossier unsorted_images non trouvé" };
  }

  const albumsPath = path.join(rootPath, 'albums');
  if (!fs.existsSync(albumsPath)) {
    fs.mkdirSync(albumsPath, { recursive: true });
  }

  const pythonScriptPath = getScriptsPath('LLM_pipeline.py');
  if (!fs.existsSync(pythonScriptPath)) {
    return { error: "Le script Python n'existe pas" };
  }

  try {
    console.log("Exécution du script Python...");
    console.log("Chemin images non triées:", unsortedImagesPath);
    console.log("Chemin albums:", albumsPath);
    
    const output = await runPipeline({ 
      directory: unsortedImagesPath, 
      destination_directory: albumsPath 
    });
    return { output };
  } catch (error) {
    console.error("Erreur lors de l'exécution du script Python:", error);
    return { error: "Erreur lors de l'exécution du script Python" };
  }
});

// ========== Gestionnaires Paramètres ==========

// Récupération d'une valeur du store
ipcMain.handle("get-setting", (_, key) => {
  return store.get(key);
});

// Sauvegarde d'une valeur dans le store
ipcMain.handle("set-setting", (_, key, value) => {
  store.set(key, value);
});

// Sélection d'un dossier via l'explorateur
ipcMain.handle("select-directory", async () => {
  const result = await dialog.showOpenDialog(mainWindow!, {
    properties: ["openDirectory"],
  });

  if (!result.canceled && result.filePaths.length > 0) {
    store.set("directoryPath", result.filePaths[0]);
    return result.filePaths[0];
  }
  return null;
});

// ========== Gestionnaires Fichiers Média ==========

// Récupération des fichiers média d'un dossier
ipcMain.handle("get-media-files", async (_, directoryPath) => {
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) {
    return { error: "Aucun chemin de dossier racine défini" };
  }

  const tempDirectoryPath = path.join(rootPath, "temp");
  
  // Gestion du dossier temporaire
  if (fs.existsSync(tempDirectoryPath)) {
    cleanTempFolder(directoryPath, tempDirectoryPath);
  } else {
    fs.mkdirSync(tempDirectoryPath, { recursive: true });
  }

  try {
    // Lecture et filtrage des fichiers média
    const files = fs.readdirSync(directoryPath).filter(file => {
      const ext = path.extname(file).toLowerCase();
      return [".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov", ".avi"].includes(ext);
    });

    // Génération des miniatures pour les vidéos
    const mediaFiles = await Promise.all(
      files.map(async file => {
        const filePath = path.join(directoryPath, file);
        const fileExt = path.extname(file).toLowerCase();
        const isVideo = [".mp4", ".mov", ".avi"].includes(fileExt);

        let thumbnailPath = null;
        if (isVideo) {
          try {
            thumbnailPath = await generateThumbnail(filePath, tempDirectoryPath);
          } catch (err) {
            console.error(`Erreur génération miniature pour ${file}:`, err);
          }
        }

        return {
          path: filePath,
          name: file,
          isVideo: isVideo,
          thumbnailPath: thumbnailPath
        };
      })
    );

    return { directoryPath: directoryPath, files: mediaFiles };
  } catch (error) {
    return { error: `Échec de lecture du dossier: ${error}` };
  }
});

// ========== Gestionnaires Connexion Hotspot ==========

// Démarrage du hotspot WiFi
ipcMain.handle("start-hotspot", async () => {
  try {
    // Démarrage du hotspot
    const hotspotResult = await connectionService.startHotspot();
    
    if (hotspotResult.error) {
      return hotspotResult;
    }

    // Attente pour la stabilisation du hotspot
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Récupération des informations WiFi
    const wifiCredentials = await connectionService.getWifiCredentials();
    
    if (!wifiCredentials.ssid || !wifiCredentials.password) {
      return { error: "Impossible de récupérer les informations du hotspot" };
    }

    // Génération de la chaîne WiFi pour QR code
    const wifiString = connectionService.generateWifiQRString(
      wifiCredentials.ssid, 
      wifiCredentials.password, 
      "WPA"
    );
    
    return { wifiString };
  } catch (error) {
    console.error("Erreur lors du démarrage du hotspot:", error);
    return { error: "Erreur lors du démarrage du hotspot" };
  }
});

// Récupération de l'adresse IP du téléphone
ipcMain.handle("get-ip", async () => {
  try {
    const ipAddress = await connectionService.getConnectedPhoneIP();
    return ipAddress;
  } catch (error) {
    console.error("Erreur lors de la récupération de l'IP:", error);
    return null;
  }
});

// Récupération des informations WiFi
ipcMain.handle('get-wifi-info', async () => {
  try {
    const wifiCredentials = await connectionService.getWifiCredentials();
    return wifiCredentials;
  } catch (error) {
    console.error("Erreur lors de la récupération des informations WiFi:", error);
    return { error: `Erreur: ${error}` };
  }
});

// ========== Gestionnaires Service de Transfert ==========

let imageTransferServer: http.Server | null = null;
let transferServiceStatus = false;

// Démarrage du service de transfert d'images
ipcMain.handle('start-image-transfer-service', async () => {
  try {
    if (transferServiceStatus) {
      return { 
        message: "Le service est déjà actif", 
        status: true,
        serverIp: "déjà en cours"
      };
    }
    
    // Démarrage du service de transfert
    const serviceInfo = await startImageTransferService();
    transferServiceStatus = true;
    
    // Notification du démarrage du service
    mainWindow?.webContents.send('transfer:service-started', serviceInfo);
    
    // Configuration des écouteurs d'événements de transfert
    setupTransferEventListeners();
    
    console.log("Service de transfert démarré:", serviceInfo);
    return serviceInfo;
  } catch (error) {
    console.error("Erreur lors du démarrage du service de transfert:", error);
    return { error: `Erreur lors du démarrage: ${error}` };
  }
});

// Arrêt du service de transfert d'images
ipcMain.handle('stop-image-transfer-service', async () => {
  try {
    if (!transferServiceStatus) {
      return { 
        message: "Le service est déjà arrêté", 
        status: false 
      };
    }
    
    // Arrêt du serveur
    if (imageTransferServer) {
      await stopImageTransferService(imageTransferServer);
      imageTransferServer = null;
    }
    
    transferServiceStatus = false;
    
    // Notification d'arrêt du service
    mainWindow?.webContents.send('transfer:service-stopped');
    
    console.log("Service de transfert arrêté avec succès");
    return { 
      message: "Service arrêté avec succès", 
      status: false 
    };
  } catch (error) {
    console.error("Erreur lors de l'arrêt du service:", error);
    return { error: `Erreur lors de l'arrêt: ${error}` };
  }
});

// Configuration des écouteurs d'événements de transfert
function setupTransferEventListeners() {
  transferEvents.removeAllListeners(); // Nettoyage des anciens écouteurs
  
  transferEvents.on('transfer:start', (info) => {
    console.log("Transfert démarré:", info.fileName);
    mainWindow?.webContents.send('transfer:start', info);
  });
  
  transferEvents.on('transfer:progress', (info) => {
    mainWindow?.webContents.send('transfer:progress', info);
  });
  
  transferEvents.on('transfer:complete', (info) => {
    console.log("Transfert terminé:", info.fileName);
    mainWindow?.webContents.send('transfer:complete', info);
  });
  
  transferEvents.on('transfer:error', (info) => {
    console.error("Erreur de transfert:", info.error);
    mainWindow?.webContents.send('transfer:error', info);
  });
}

// Génération d'un QR code pour le transfert
ipcMain.handle('generate-transfer-qrcode', (_, wifiString, serverIp) => {
  try {
    return generateTransferQRCode(wifiString, serverIp);
  } catch (error) {
    console.error("Erreur lors de la génération du QR code:", error);
    return { error: "Erreur lors de la génération du QR code" };
  }
});

// Récupération du statut du service de transfert
ipcMain.handle('get-transfer-service-status', () => {
  return { 
    active: transferServiceStatus,
    timestamp: new Date().toISOString()
  };
});

// Récupération des appareils connectés
ipcMain.handle('get-connected-devices', async () => {
  try {
    // Cette fonction devrait être implémentée dans connectionService
    // Pour l'instant, retourne un tableau vide
    return [];
  } catch (error) {
    console.error("Erreur lors de la récupération des appareils:", error);
    return [];
  }
});

// ========== Gestionnaires Dossiers ==========

// Récupération de l'arborescence des dossiers
ipcMain.handle("get-folders", async (_, rootPath) => {
  try {
    return getFolders(rootPath);
  } catch (error) {
    console.error("Erreur lors de la récupération des dossiers:", error);
    return { error: "Erreur lors de la récupération des dossiers" };
  }
});

// ========== Gestion de la fermeture de l'application ==========

app.on('window-all-closed', async () => {
  // Nettoyage avant fermeture
  if (transferServiceStatus && imageTransferServer) {
    try {
      await stopImageTransferService(imageTransferServer);
      console.log("Service de transfert arrêté lors de la fermeture");
    } catch (error) {
      console.error("Erreur lors de l'arrêt du service:", error);
    }
  }

  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    // Recréer la fenêtre si nécessaire (macOS)
    app.emit('ready');
  }
});

// ========== Gestion des erreurs non capturées ==========

process.on('uncaughtException', (error) => {
  console.error('Erreur non capturée:', error);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Promesse rejetée non gérée:', reason);
});