import {app, BrowserWindow, dialog, ipcMain } from 'electron';
import http from 'http';
import { startImageTransferService, stopImageTransferService, generateTransferQRCode, transferEvents } from './server.js';
import path from 'path';
import fs from 'fs';
import { isDev, cleanTempFolder, generateThumbnail, lireListeImages } from './util.js';
import { getPreloadPath } from './pathResolver.js';
import { startHotspot, getWifiInfo, extractWifiInfo, getPhoneIpAddress, extractIpAddress } from './connexion.js';
import { store, globalStore } from "./store.js";
import { getFolders } from './folderManager.js';
import { runImageRetrieval, runPythonFile } from './python/runMain.js';
import { setupPythonEnv } from './python/setupPythonEnv.js';
import { getScriptsPath } from './pathResolver.js';

let mainWindow: BrowserWindow | null = null;

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

  // Set global variable AIProcessing to true
  globalStore.set("AIProcessing", false);

  if (isDev()) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  }
  else {
    mainWindow.loadFile(path.join(app.getAppPath(), '/dist-react/index.html'));
  }
});

// Execute Python Script Handler
ipcMain.handle('run-python', async (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (!win) return { error: "No window found" };

  // Set global variable AIProcessing to true
  globalStore.set("AIProcessing", true);

  // Define the log/error forwarding functions ONCE
  const forwardLog = (msg: string) => win.webContents.send('log', msg);

  // Récupérer le chemin du dossier principal
  const rootPath = store.get("directoryPath") as string;
  if (!rootPath) return { error: "No root directory path set" };

  const unsortedImagesPath = path.join(rootPath, "unsorted_images");
  // Si le dossier n'existe pas, retourner "no images to sort"
  if (!fs.existsSync(unsortedImagesPath)) {
    console.log("No images to sort : unsorted_images folder not found");
    return unsortedImagesPath;
  }
  // Si le dossier existe, vérifier qu'il n'est pas vide
  const files = fs.readdirSync(unsortedImagesPath);
  if (files.length === 0) {
    console.log("No images to sort : unsorted_images folder is empty");
    return unsortedImagesPath;
  }

  // Vérifier que le dossier "albums" existe
  const albumsPath = path.join(rootPath, 'albums');
  if (!fs.existsSync(albumsPath)) {
    // Si le dossier n'existe pas, le créer
    fs.mkdirSync(albumsPath, { recursive: true });
  }

  // Vérifier que le dossier "all_images" existe
  const allImagesPath = path.join(rootPath, 'all_images');
  if (!fs.existsSync(allImagesPath)) {
    // Si le dossier n'existe pas, le créer
    fs.mkdirSync(allImagesPath, { recursive: true });
  }

  // Vérifier que l'environnement Python est prêt
  await setupPythonEnv({ onLog: forwardLog });

  // Exécuter le script Python
  forwardLog("[COMMENT]: unsortedImagesPath:" + unsortedImagesPath);
  forwardLog("[COMMENT]: albumsPath: " + albumsPath);
  forwardLog("[COMMENT]: allImagesPath: " + allImagesPath);
  forwardLog("[COMMENT]: Running Python script...");

  await runPythonFile({
    directory: unsortedImagesPath,
    destination_directory: albumsPath,
    copy_directory: allImagesPath,
    onLog: forwardLog,
  });

  // Set global variable AIProcessing to false
  globalStore.set("AIProcessing", false);
  win.webContents.send('python-end');
});

// Execute Python Script Handler
ipcMain.handle('run-image-retrieval', async (event, prompt: string) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  if (!win) return { error: "No window found" };

  // Define the log/error forwarding functions ONCE
  const forwardLog = (msg: string) => win.webContents.send('log', msg);

  // Vérifier que l'environnement Python est prêt
  await setupPythonEnv({ onLog: forwardLog });

  let output = await runImageRetrieval({
    prompt: prompt,
    onLog: forwardLog,
  });

  // Get temp files
  const tempFilesPath = getScriptsPath("temp_files");
  //check if directory exists
  if (!fs.existsSync(tempFilesPath)) {
    return { error: "The directory 'temp_files' has not been found" };
  }

  // Get json file
  const jsonPath = path.join(tempFilesPath, "similar_images.json");
  if (!fs.existsSync(jsonPath)) {
    return { error: "No json file found" };
  }

  // Read the json file
  const ImagesList = lireListeImages(jsonPath);

  return ImagesList;
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

// Récupérer une valeur du store
ipcMain.handle("get-global-variables", (_, key) => {
  return globalStore.get(key);
});

// Enregistrer une valeur dans le store
ipcMain.handle("set-global-variables", (_, key, value) => {
  globalStore.set(key, value);
});

// Connexion to the phone mobile

ipcMain.handle("start-hotspot", async () => {
  try {
      // Démarrer le hotspot
      const hotspotResult = await startHotspot();

      // Attendre un court instant pour s'assurer que le hotspot est bien activé
      await new Promise(resolve => setTimeout(resolve, 3000));

      // Récupérer le SSID et la clé de sécurité
      let data = await getWifiInfo();

      const wifiEncryption = "WPA"; // WPA, WPA2 ou NONE
      const wifiInfo = extractWifiInfo(data);
      
      if (!wifiInfo.ssid || !wifiInfo.password) {
        return { error: "Impossible de récupérer les informations du hotspot" };
      }
      else
      {
        const wifiString = `WIFI:T:${wifiEncryption};S:${wifiInfo.ssid};P:${wifiInfo.password};;`;
        return { wifiString };
      }
  } catch (error) {
      return { error: "Erreur lors du démarrage du hotspot" };
  }
});

// Récupérer l'adresse IP
ipcMain.handle("get-ip", async () => {
  try {
    let ipAddress = await getPhoneIpAddress();
    if (ipAddress) {
      ipAddress = extractIpAddress(ipAddress);
    }
    return ipAddress; // This will be sent back to the renderer process
  } catch (error) {
    console.error("Error fetching IP:", error);
    return null;
  }
});

// Récupérer les dossiers
ipcMain.handle("get-folders", async (_, rootPath) => {
  return getFolders(rootPath);
});

// Service de transfert d'images
let imageTransferServer: http.Server | null = null;
let transferServiceStatus = false;

// Démarrer le service de transfert d'images
ipcMain.handle('start-image-transfer-service', async () => {
  try {
    // Vérifier si le service est déjà en cours d'exécution
    if (transferServiceStatus) {
      return { message: "Le service est déjà actif", status: true };
    }
    
    // Démarrer le service
    const serviceInfo = await startImageTransferService();
    transferServiceStatus = true;
    
    // Envoyer l'événement de démarrage du service
    mainWindow?.webContents.send('transfer:service-started', serviceInfo);
    
    // Configurer les redirections d'événements
    transferEvents.on('transfer:start', (info) => {
      mainWindow?.webContents.send('transfer:start', info);
    });
    
    transferEvents.on('transfer:progress', (info) => {
      mainWindow?.webContents.send('transfer:progress', info);
    });
    
    transferEvents.on('transfer:complete', (info) => {
      mainWindow?.webContents.send('transfer:complete', info);
    });
    
    transferEvents.on('transfer:error', (info) => {
      mainWindow?.webContents.send('transfer:error', info);
    });
    
    return serviceInfo;
  } catch (error) {
    console.error("Erreur lors du démarrage du service de transfert:", error);
    return { error: `Erreur: ${error}` };
  }
});

// Arrêter le service de transfert d'images
ipcMain.handle('stop-image-transfer-service', async () => {
  try {
    if (!transferServiceStatus) {
      return { message: "Le service est déjà arrêté", status: false };
    }
    
    // Arrêter le serveur si existant
    if (imageTransferServer) {
      await stopImageTransferService(imageTransferServer);
      imageTransferServer = null;
    }
    
    transferServiceStatus = false;
    mainWindow?.webContents.send('transfer:service-stopped');
    
    return { message: "Service arrêté avec succès", status: false };
  } catch (error) {
    console.error("Erreur lors de l'arrêt du service:", error);
    return { error: `Erreur: ${error}` };
  }
});

// Générer un QR code pour le transfert d'images
ipcMain.handle('generate-transfer-qrcode', (_, wifiString, serverIp) => {
  return generateTransferQRCode(wifiString, serverIp);
});

// Obtenir le statut du service de transfert
ipcMain.handle('get-transfer-service-status', () => {
  return { active: transferServiceStatus };
});

// Récupérer les informations WiFi
ipcMain.handle('get-wifi-info', async () => {
  try {
    const data = await getWifiInfo();
    return extractWifiInfo(data);
  } catch (error) {
    console.error("Erreur lors de la récupération des informations WiFi:", error);
    return { error: `Erreur: ${error}` };
  }
});
