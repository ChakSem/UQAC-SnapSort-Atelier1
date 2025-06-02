import { app } from 'electron';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { EventEmitter } from 'events';
import { startHotspot, getSSID, getSecurityKey, extractSSID, extractUserSecurityKey } from './connectionService.js';
import { networkInterfaces } from 'os';
import store from './store.js';

export const transferEvents = new EventEmitter();
const SERVER_PORT = 8080;

// Type pour les informations de transfert
interface TransferStartInfo {
  fileName: string;
  fileSize: number;
  index: number;
  total: number;
}

interface TransferProgressInfo {
  fileName: string;
  progress: number;
  receivedBytes: number;
  totalBytes: number;
}

interface TransferCompleteInfo {
  fileName: string;
  filePath: string;
  size: number;
}

interface TransferErrorInfo {
  error: string;
}

export function startImageServer(savePath: string): Promise<string> {
  return new Promise(async (resolve, reject) => {
    try {
      if (!fs.existsSync(savePath)) {
        await fs.promises.mkdir(savePath, { recursive: true });
      }

      const server = http.createServer(async (req, res) => {
        try {
          // Ajout d'un gestionnaire de CORS pour permettre les requêtes cross-origin
          res.setHeader('Access-Control-Allow-Origin', '*');
          res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
          res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

          // Gestion des requêtes OPTIONS (préflight CORS)
          if (req.method === 'OPTIONS') {
            res.statusCode = 204;
            res.end();
            return;
          }

          if (req.method === 'POST') {
            console.log('Réception d\'une requête POST');
            
            // Variables pour suivre l'état du transfert
            let totalImages = 0;
            let fileName = '';
            let dateString = '';
            let fileSize = 0;
            let receivedBytes = 0;
            let currentImageIndex = 0;
            let remainingData = Buffer.alloc(0);
            
            // Fonction pour lire une ligne depuis le flux de données
            const readLine = async (): Promise<string> => {
              return new Promise((resolve) => {
                const tryReadLine = () => {
                  const lineEnd = remainingData.indexOf('\n');
                  if (lineEnd >= 0) {
                    const line = remainingData.slice(0, lineEnd).toString().trim();
                    remainingData = remainingData.slice(lineEnd + 1);
                    resolve(line);
                    return true;
                  }
                  return false;
                };
                
                if (tryReadLine()) return;
                
                const dataHandler = (chunk: Buffer) => {
                  remainingData = Buffer.concat([remainingData, chunk]);
                  if (tryReadLine()) {
                    req.removeListener('data', dataHandler);
                  }
                };
                
                req.on('data', dataHandler);
              });
            };
            
            // Lire le nombre total d'images (première ligne envoyée par le client Android)
            totalImages = parseInt(await readLine(), 10);
            console.log(`Nombre total d'images à recevoir: ${totalImages}`);
            
            // Traiter chaque image
            while (currentImageIndex < totalImages) {
              try {
                // Lire les informations de l'image
                fileName = await readLine();
                dateString = await readLine();
                fileSize = parseInt(await readLine(), 10);
                
                console.log(`Réception de l'image ${currentImageIndex + 1}/${totalImages}: ${fileName}, taille: ${fileSize} octets`);
                
                // Émettre l'événement de début de transfert
                const startInfo: TransferStartInfo = {
                  fileName,
                  fileSize,
                  index: currentImageIndex + 1,
                  total: totalImages
                };
                transferEvents.emit('transfer:start', startInfo);
                
                // Créer le chemin complet du fichier directement dans le dossier de sauvegarde principal
                const filePath = path.join(savePath, fileName);
                const fileStream = fs.createWriteStream(filePath);
                
                // Réinitialiser le compteur de bytes reçus
                receivedBytes = 0;
                
                // Si des données sont déjà dans le buffer, les utiliser d'abord
                if (remainingData.length > 0) {
                  const dataToWrite = remainingData.length > fileSize ? 
                    remainingData.slice(0, fileSize) : 
                    remainingData;
                  
                  fileStream.write(dataToWrite);
                  receivedBytes += dataToWrite.length;
                  
                  // Mettre à jour la progression
                  const progressInfo: TransferProgressInfo = {
                    fileName,
                    progress: receivedBytes / fileSize,
                    receivedBytes,
                    totalBytes: fileSize
                  };
                  transferEvents.emit('transfer:progress', progressInfo);
                  
                  // S'il reste des données après le fichier, les conserver
                  if (remainingData.length > fileSize) {
                    remainingData = remainingData.slice(fileSize);
                  } else {
                    remainingData = Buffer.alloc(0);
                  }
                }
                
                // Continuer à lire les données jusqu'à ce que le fichier soit complet
                if (receivedBytes < fileSize) {
                  await new Promise<void>((resolveFile) => {
                    const dataHandler = (chunk: Buffer) => {
                      const remainingBytes = fileSize - receivedBytes;
                      const bytesToWrite = Math.min(chunk.length, remainingBytes);
                      
                      if (bytesToWrite > 0) {
                        const dataToWrite = chunk.slice(0, bytesToWrite);
                        fileStream.write(dataToWrite);
                        receivedBytes += bytesToWrite;
                        
                        // Mettre à jour la progression
                        const progressInfo: TransferProgressInfo = {
                          fileName,
                          progress: receivedBytes / fileSize,
                          receivedBytes,
                          totalBytes: fileSize
                        };
                        transferEvents.emit('transfer:progress', progressInfo);
                      }
                      
                      // Si on a reçu tous les bytes pour ce fichier
                      if (receivedBytes >= fileSize) {
                        req.removeListener('data', dataHandler);
                        
                        // S'il y a des données supplémentaires, les conserver pour le prochain fichier
                        if (bytesToWrite < chunk.length) {
                          remainingData = chunk.slice(bytesToWrite);
                        }
                        
                        resolveFile();
                      }
                    };
                    
                    req.on('data', dataHandler);
                  });
                }
                
                // Fermer le stream de fichier
                fileStream.end();
                
                // Émettre l'événement de fin de transfert pour ce fichier
                const completeInfo: TransferCompleteInfo = {
                  fileName,
                  filePath,
                  size: fileSize
                };
                transferEvents.emit('transfer:complete', completeInfo);
                
                currentImageIndex++;
                
              } catch (fileError) {
                console.error(`Erreur lors du traitement de l'image ${currentImageIndex + 1}:`, fileError);
                const errorInfo: TransferErrorInfo = {
                  error: `Erreur lors du traitement de l'image: ${fileError instanceof Error ? fileError.message : 'Erreur inconnue'}`
                };
                transferEvents.emit('transfer:error', errorInfo);
                currentImageIndex++;
              }
            }
            
            // Tous les fichiers ont été traités
            res.statusCode = 200;
            res.end(JSON.stringify({ status: 'success', message: 'Transfert terminé avec succès' }));
            
          } else if (req.method === 'GET') {
            // Amélioré pour retourner des informations utiles au client
            const statusInfo = {
              status: 'active',
              version: '1.0.0',
              serverTime: new Date().toISOString(),
              endpoints: {
                post: '/upload',
                get: '/status'
              }
            };
            
            res.statusCode = 200;
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(statusInfo));
          } else {
            // Méthode non supportée
            res.statusCode = 405;
            res.end(JSON.stringify({ status: 'error', message: 'Méthode non supportée' }));
          }
        } catch (error) {
          console.error('Erreur:', error);
          const errorInfo: TransferErrorInfo = {
            error: `Erreur serveur: ${error instanceof Error ? error.message : 'Erreur inconnue'}`
          };
          transferEvents.emit('transfer:error', errorInfo);
          res.statusCode = 500;
          res.end(JSON.stringify({ status: 'error', message: 'Erreur serveur' }));
        }
      });

      // Amélioration de la gestion d'erreur du serveur
      server.on('error', (error) => {
        console.error('Erreur serveur:', error);
        const errorInfo: TransferErrorInfo = {
          error: `Erreur serveur: ${error.message}`
        };
        transferEvents.emit('transfer:error', errorInfo);
        reject(`Erreur serveur: ${error.message}`);
      });

      server.listen(SERVER_PORT, () => {
        console.log(`Serveur démarré sur le port ${SERVER_PORT}`);
        resolve(`Serveur démarré sur le port ${SERVER_PORT}`);
      });

    } catch (error) {
      reject(`Erreur lors du démarrage du serveur: ${error}`);
    }
  });
}

export function getLocalIpAddress(): Promise<string> {
  return new Promise((resolve, reject) => {
    try {
      const nets = networkInterfaces();
      for (const name of Object.keys(nets)) {
        const net = nets[name];
        if (net) {
          for (const addr of net) {
            // Priorité aux adresses qui commencent par 192.168.137.x (point d'accès Windows)
            if (addr.family === 'IPv4' && !addr.internal && addr.address.startsWith('192.168.137.')) {
              return resolve(addr.address);
            }
          }
        }
      }
      
      // Si aucune adresse de point d'accès trouvée, chercher une adresse IP locale
      for (const name of Object.keys(nets)) {
        const net = nets[name];
        if (net) {
          for (const addr of net) {
            if (addr.family === 'IPv4' && !addr.internal) {
              return resolve(addr.address);
            }
          }
        }
      }
      
      reject('Aucune adresse IP trouvée');
    } catch (error) {
      reject(`Erreur lors de la récupération de l'adresse IP: ${error}`);
    }
  });
}

export async function startImageTransferService() {
  try {
    // Start hotspot first
    await startHotspot();
    
    // Wait a moment for the hotspot to initialize
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Get WiFi information with error handling
    let ssid = 'Unknown';
    let securityKey = 'Unknown';
    
    try {
      const ssidOutput = await getSSID();
      ssid = extractSSID(ssidOutput) || 'Unknown';
    } catch (error) {
      console.log('Error getting SSID, using default');
    }
    
    try {
      const keyOutput = await getSecurityKey();
      securityKey = extractUserSecurityKey(keyOutput) || 'Unknown';
    } catch (error) {
      console.log('Error getting security key, using default');
    }
    
    // Prepare save path
    const rootPath = store.get("directoryPath") as string;
    if (!rootPath) return { error: "No root directory path set" };
    
    let savePath = path.join(rootPath, "unsorted_images");

    // Nettoyer le chemin pour éviter les caractères non valides
    savePath = path.normalize(savePath).replace(/\0/g, ""); 

    // Vérifier que le chemin est valide
    if (!savePath || typeof savePath !== "string" || savePath.includes('\x00')) {
      throw new Error(`Invalid save path: ${savePath}`);
    }

    // Si le dossier n'existe pas, le créer
    if (!fs.existsSync(savePath)) {
      fs.mkdirSync(savePath, { recursive: true });
    }
    
    // Start image server
    await startImageServer(savePath);
    
    // Get server IP
    const serverIp = await getLocalIpAddress();
    
    // Prepare WiFi connection string
    const wifiString = `WIFI:S:${ssid};T:WPA;P:${securityKey};H:false;;`;
    
    return { wifiString, serverIp, savePath, port: SERVER_PORT };
  } catch (error) {
    console.error('Erreur lors du démarrage du service:', error);
    throw error;
  }
}

// Cette fonction ajoute les informations du serveur au QR code
export function generateTransferQRCode(wifiString: string, serverIp: string): string {
  return `${wifiString}IP:${serverIp};PORT:${SERVER_PORT};`;
}

// Nouvelle fonction pour arrêter le serveur si nécessaire
export function stopImageTransferService(server: http.Server): Promise<void> {
  return new Promise((resolve, reject) => {
    if (!server) {
      resolve();
      return;
    }
    
    server.close((err) => {
      if (err) {
        console.error('Erreur lors de arrêt du serveur:', err);
        reject(err);
        return;
      }
      
      console.log('Serveur arrêté avec succès');
      resolve();
    });
  });
}