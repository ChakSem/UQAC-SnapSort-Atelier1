import { app, BrowserWindow, dialog, ipcMain } from 'electron';
import { exec } from 'child_process';
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
// Import depuis le nouveau service
import connectionService from './connectionService.js';
import store from "./store.js";
import { getFolders } from './folderManager.js';
import { runPipeline } from './python.js';

let mainWindow: BrowserWindow | null = null;

// Variables globales pour le service de transfert
let imageTransferServer: http.Server | null = null;
let transferServiceStatus = false;

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

ipcMain.handle("get-setting", (_, key) => {
  return store.get(key);
});

ipcMain.handle("set-setting", (_, key, value) => {
  store.set(key, value);
});

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

ipcMain.handle("get-media-files", async (_, directoryPath) => {
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) {
    return { error: "Aucun chemin de dossier racine défini" };
  }

  const tempDirectoryPath = path.join(rootPath, "temp");
  
  if (fs.existsSync(tempDirectoryPath)) {
    cleanTempFolder(directoryPath, tempDirectoryPath);
  } else {
    fs.mkdirSync(tempDirectoryPath, { recursive: true });
  }

  try {
    const files = fs.readdirSync(directoryPath).filter(file => {
      const ext = path.extname(file).toLowerCase();
      return [".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov", ".avi"].includes(ext);
    });

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

ipcMain.handle("start-hotspot", async () => {
  try {
    const hotspotResult = await connectionService.startHotspot();
    
    if (hotspotResult.error) {
      return hotspotResult;
    }

    await new Promise(resolve => setTimeout(resolve, 3000));

    const wifiCredentials = await connectionService.getWifiCredentials();
    
    if (!wifiCredentials.ssid || !wifiCredentials.password) {
      return { error: "Impossible de récupérer les informations du hotspot" };
    }

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

ipcMain.handle("get-ip", async () => {
  try {
    const ipAddress = await connectionService.getConnectedPhoneIP();
    return ipAddress;
  } catch (error) {
    console.error("Erreur lors de la récupération de l'IP:", error);
    return null;
  }
});

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

// Fonction pour forcer l'arrêt du serveur sur le port 8080
const forceKillServerOnPort = async (port: number = 8080): Promise<void> => {
  return new Promise((resolve) => {
    // Sur Windows, utiliser netstat et taskkill
    if (process.platform === 'win32') {
      exec(`netstat -ano | findstr :${port}`, (error: any, stdout: any) => {
        if (stdout) {
          const lines = stdout.trim().split('\n');
          const pids = new Set<string>();
          
          lines.forEach((line: string) => {
            const parts = line.trim().split(/\s+/);
            if (parts.length >= 5 && parts[1].includes(`:${port}`)) {
              pids.add(parts[4]);
            }
          });
          
          if (pids.size > 0) {
            pids.forEach(pid => {
              exec(`taskkill /F /PID ${pid}`, (killError: any) => {
                if (killError) {
                  console.log(`Impossible de tuer le processus ${pid}`);
                }
              });
            });
          }
        }
        setTimeout(resolve, 1000); // Attendre 1 seconde
      });
    } else {
      // Sur Unix/Linux/Mac
      exec(`lsof -ti:${port}`, (error: any, stdout: any) => {
        if (stdout) {
          const pids = stdout.trim().split('\n');
          pids.forEach((pid: string) => {
            if (pid) {
              exec(`kill -9 ${pid}`, (killError: any) => {
                if (killError) {
                  console.log(`Impossible de tuer le processus ${pid}`);
                }
              });
            }
          });
        }
        setTimeout(resolve, 1000);
      });
    }
  });
};

ipcMain.handle('start-image-transfer-service', async () => {
  try {
    if (transferServiceStatus) {
      return { 
        message: "Le service est déjà actif", 
        status: true,
        serverIp: "192.168.137.1"
      };
    }
    
    // Forcer l'arrêt de tout processus utilisant le port 8080
    await forceKillServerOnPort(8080);
    
    // Arrêter le serveur existant s'il y en a un
    if (imageTransferServer) {
      await stopImageTransferService(imageTransferServer);
      imageTransferServer = null;
    }
    
    // Attendre un délai supplémentaire pour s'assurer que le port est libéré
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const serviceInfo = await startImageTransferService();
    transferServiceStatus = true;
    
    mainWindow?.webContents.send('transfer:service-started', serviceInfo);
    setupTransferEventListeners();
    
    console.log("Service de transfert démarré:", serviceInfo);
    return serviceInfo;
  } catch (error) {
    console.error("Erreur lors du démarrage du service de transfert:", error);
    transferServiceStatus = false;
    return { error: `Erreur lors du démarrage: ${error}` };
  }
});

ipcMain.handle('stop-image-transfer-service', async () => {
  try {
    if (imageTransferServer) {
      await stopImageTransferService(imageTransferServer);
      imageTransferServer = null;
    }
    
    // Forcer l'arrêt des processus sur le port
    await forceKillServerOnPort(8080);
    
    transferServiceStatus = false;
    mainWindow?.webContents.send('transfer:service-stopped');
    
    console.log("Service de transfert arrêté avec succès");
    return { 
      message: "Service arrêté avec succès", 
      status: false 
    };
  } catch (error) {
    console.error("Erreur lors de l'arrêt du service:", error);
    transferServiceStatus = false;
    return { error: `Erreur lors de l'arrêt: ${error}` };
  }
});

// Configuration améliorée des écouteurs d'événements de transfert
function setupTransferEventListeners() {
  // Nettoyer les anciens écouteurs pour éviter les doublons
  transferEvents.removeAllListeners();
  
  transferEvents.on('transfer:start', (info) => {
    console.log("Transfert démarré:", info.fileName);
    mainWindow?.webContents.send('transfer:start', info);
  });
  
  transferEvents.on('transfer:progress', (info) => {
    // Envoyer les mises à jour de progression
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

ipcMain.handle('generate-transfer-qrcode', (_, wifiString, serverIp) => {
  try {
    return generateTransferQRCode(wifiString, serverIp);
  } catch (error) {
    console.error("Erreur lors de la génération du QR code:", error);
    return { error: "Erreur lors de la génération du QR code" };
  }
});

ipcMain.handle('get-transfer-service-status', () => {
  return { 
    active: transferServiceStatus,
    timestamp: new Date().toISOString()
  };
});

// Gestionnaire temporaire pour les appareils connectés (à implémenter plus tard)
ipcMain.handle('get-connected-devices', async () => {
  try {
    // Pour l'instant, retourner un tableau vide
    // Cette fonction devra être implémentée pour scanner le réseau
    return [];
  } catch (error) {
    console.error("Erreur lors de la récupération des appareils:", error);
    return [];
  }
});

// ========== Gestionnaires Dossiers ==========

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
      await forceKillServerOnPort(8080);
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