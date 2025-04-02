import {app, BrowserWindow, dialog, ipcMain } from 'electron';
import path from 'path';
import fs from 'fs';
import { isDev, cleanTempFolder, generateThumbnail } from './util.js';
import { getPreloadPath, getPythonScriptPath } from './pathResolver.js';
import { startHotspot, getSSID, getSecurityKey, extractSSID, extractUserSecurityKey,getHotspotInfo,getConnectedDevices} from './connexion.js';
import { startImageTransferService, generateTransferQRCode,transferEvents,getLocalIpAddress } from './server.js';
import store from "./store.js";
import { getFolders } from './folderManager.js';
import { runPipeline } from './python.js';

let mainWindow: BrowserWindow | null = null;

app.on('ready', () => {
  const mainWindow = new BrowserWindow({
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
  }
  else {
    mainWindow.loadFile(path.join(app.getAppPath(), '/dist-react/index.html'));
  }

  if (isDev()) {
    mainWindow.webContents.openDevTools();
  }
});

// Execute Python Script Handler
ipcMain.handle('run-python', async () => {

  // Récupérer le chemin du dossier principal
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) return { error: "No root directory path set" };

  const unsortedImagesPath = path.join(rootPath, "unsorted_images");
  // Si le dossier n'existe pas, retourner "no images to sort"
  if (!fs.existsSync(unsortedImagesPath)) {
    return unsortedImagesPath;
  }

  // Vérifier que le dossier "albums" existe
  const albumsPath = path.join(rootPath, 'albums');
  if (!fs.existsSync(albumsPath)) {
    // Si le dossier n'existe pas, le créer
    fs.mkdirSync(albumsPath, { recursive: true });
  }

  // Vérifier que le script Python existe
  const pythonScriptPath = getPythonScriptPath('LLM_pipeline.py');
  if (!fs.existsSync(pythonScriptPath)) {
    return { error: "The Python script does not exist" };
  }

  // Exécuter le script Python
  try {
    const output = await runPipeline({ directory: unsortedImagesPath, destination_directory: albumsPath });
    return { output };
  } catch (error) {
    console.error("Error running Python script:", error);
    return { error: "Error running Python script" };
  }
});

// Settings Handler

// Récupérer une valeur du store
ipcMain.handle("get-setting", (_, key) => {
  return store.get(key);
});

// Enregistrer une valeur dans le store
ipcMain.handle("set-setting", (_, key, value) => {
  store.set(key, value);
});

// Ouvrir un explorateur pour sélectionner un dossier
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

// Unsorted Images Handler

// Fonction principale pour récupérer les fichiers média
ipcMain.handle("get-media-files", async (_, directoryPath) => {

  // Deal with temp directory
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) return { error: "No root directory path set" };

  const tempDirectoryPath = path.join(rootPath, "temp");
  if (fs.existsSync(tempDirectoryPath)) {
    // Si le dossier existe, on le nettoie
    cleanTempFolder(directoryPath, tempDirectoryPath);
  }
  else {
    // Sinon, on le crée
    fs.mkdirSync(tempDirectoryPath, { recursive: true });
  }


  try {
    // Lire le contenu du dossier demandé
    const files = fs.readdirSync(directoryPath).filter(file => {
      const ext = path.extname(file).toLowerCase();
      return [".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov", ".avi"].includes(ext);
    });

    // Si les fichiers sont des vidéos, on génère une miniature que l'on stocke dans le dossier temp
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
    return { error: `Failed to read directory: ${error}` };
  }
});

// Connexion to the phone mobile
ipcMain.handle('get-connected-ips', async () => {
  // Vous pouvez ici exécuter une commande pour récupérer les IP connectées
  // par exemple en utilisant `exec` pour exécuter la commande `arp -a` et retourner les résultats
  const result = await getConnectedDevices();
  if (!Array.isArray(result)) {
    return { error: "Failed to retrieve connected devices" };
  }
  // Traiter le résultat pour extraire les adresses IP
  return result;
});
ipcMain.handle("start-hotspot", async () => {
  try {
      // Démarrer le hotspot
      const hotspotResult = await startHotspot();
      console.log(hotspotResult);

      // Attendre un court instant pour s'assurer que le hotspot est bien activé
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Récupérer le SSID et la clé de sécurité
      let wifiSSID = await getSSID();
      let wifiPassword = await getSecurityKey();
      const wifiEncryption = "WPA"; // WPA, WPA2 ou NONE

      wifiSSID = extractSSID(wifiSSID) ?? '';
      wifiPassword = extractUserSecurityKey(wifiPassword) ?? '';
      
      if (wifiSSID === '' || wifiPassword === '') {
        return { error: "Impossible de récupérer les informations du hotspot" };
      }
      else
      {
        const wifiString = `WIFI:T:${wifiEncryption};S:${wifiSSID};P:${wifiPassword};;`;
        return { wifiString };
      }
  } catch (error) {
      return { error: "Erreur lors du démarrage du hotspot" };
  }
});


// Récupérer l'adresse IP
ipcMain.handle("get-ip", async () => {
  try {
    let ipAddress = await getHotspotInfo();
    if (ipAddress) {
      return ipAddress;
    } else {
      return { error: "No IP address found" };
    }
  } catch (error) {
    console.error("Error getting IP address:", error);
    return { error: "Error getting IP address" };
  }
}
);
// Récupérer les dossiers
ipcMain.handle("get-folders", async (_, rootPath) => {
  return getFolders(rootPath);
});


// Configurer les gestionnaires d'événements IPC
ipcMain.handle('start-image-transfer-service', async () => {
  try {
    const result = await startImageTransferService();
    return result;
  } catch (error) {
    console.error('Erreur lors du démarrage du service:', error);
    return { error: error instanceof Error ? error.message : String(error) };
  }
});

ipcMain.handle('generate-transfer-qrcode', async (event, wifiString, serverIp) => {
  try {
    const qrCodeData = generateTransferQRCode(wifiString, serverIp);
    return qrCodeData;
  } catch (error) {
    console.error('Erreur lors de la génération du QR code:', error);
    return null;
  }
});

// Relayer les événements de transfert vers le renderer
transferEvents.on('transfer:start', (data: { [key: string]: any }) => {
  BrowserWindow.getAllWindows().forEach(window => {
    if (!window.isDestroyed()) {
      window.webContents.send('transfer:start', data);
    }
  });
});

transferEvents.on('transfer:progress', (data: { [key: string]: any }) => {
  BrowserWindow.getAllWindows().forEach(window => {
    if (!window.isDestroyed()) {
      window.webContents.send('transfer:progress', data);
    }
  });
});

transferEvents.on('transfer:complete', (data: { [key: string]: any }) => {
  BrowserWindow.getAllWindows().forEach(window => {
    if (!window.isDestroyed()) {
      window.webContents.send('transfer:complete', data);
    }
  });
});

transferEvents.on('transfer:error', (data: { [key: string]: any }) => {
  BrowserWindow.getAllWindows().forEach(window => {
    if (!window.isDestroyed()) {
      window.webContents.send('transfer:error', data);
    }
  });
});