import { app } from 'electron';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { EventEmitter } from 'events';
import { startHotspot, getSSID, getSecurityKey, extractSSID, extractUserSecurityKey } from './connexion.js';
import { networkInterfaces } from 'os';

export const transferEvents = new EventEmitter();
const SERVER_PORT = 8080;

export function startImageServer(savePath: string): Promise<string> {
  return new Promise(async (resolve, reject) => {
    try {
      if (!fs.existsSync(savePath)) {
        await fs.promises.mkdir(savePath, { recursive: true });
      }

      const server = http.createServer(async (req, res) => {
        try {
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
                transferEvents.emit('transfer:start', {
                  fileName,
                  fileSize,
                  index: currentImageIndex + 1,
                  total: totalImages
                });
                
                // Créer le dossier pour la date
                const formattedDate = dateString.substring(0, 8); // Prendre YYYYMMDD
                const dateFolder = path.join(savePath, formattedDate);
                if (!fs.existsSync(dateFolder)) {
                  await fs.promises.mkdir(dateFolder, { recursive: true });
                }
                
                // Créer le chemin complet du fichier
                const filePath = path.join(dateFolder, fileName);
                const fileStream = fs.createWriteStream(filePath);
                
                // Réinitialiser le compteur de bytes reçus
                receivedBytes = 0;
                let fileBuffer = Buffer.alloc(0);
                
                // Si des données sont déjà dans le buffer, les utiliser d'abord
                if (remainingData.length > 0) {
                  const dataToWrite = remainingData.length > fileSize ? 
                    remainingData.slice(0, fileSize) : 
                    remainingData;
                  
                  fileStream.write(dataToWrite);
                  receivedBytes += dataToWrite.length;
                  
                  // Mettre à jour la progression
                  transferEvents.emit('transfer:progress', {
                    fileName,
                    progress: receivedBytes / fileSize,
                    receivedBytes,
                    totalBytes: fileSize
                  });
                  
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
                        transferEvents.emit('transfer:progress', {
                          fileName,
                          progress: receivedBytes / fileSize,
                          receivedBytes,
                          totalBytes: fileSize
                        });
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
                transferEvents.emit('transfer:complete', {
                  fileName,
                  filePath,
                  size: fileSize
                });
                
                currentImageIndex++;
                
              } catch (fileError) {
                console.error(`Erreur lors du traitement de l'image ${currentImageIndex + 1}:`, fileError);
                transferEvents.emit('transfer:error', {
                    error: `Erreur lors du traitement de l'image: ${fileError instanceof Error ? fileError.message : 'Erreur inconnue'}`
                });
                currentImageIndex++;
            }
            
            }
            
            // Tous les fichiers ont été traités
            res.statusCode = 200;
            res.end('Transfert terminé avec succès');
            
          } else {
            // Requête GET - simple vérification que le serveur est actif
            res.statusCode = 200;
            res.end('Serveur de transfert actif');
          }
        }catch (error) {
            console.error('Erreur:', error);
            transferEvents.emit('transfer:error', {
                error: `Erreur serveur: ${error instanceof Error ? error.message : 'Erreur inconnue'}`
            });
            res.statusCode = 500;
            res.end('Erreur serveur');
        }
            
      });

      server.on('error', (error) => {
        console.error('Erreur serveur:', error);
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
    await startHotspot();
    const ssid = extractSSID(await getSSID()) || 'Unknown';
    const securityKey = extractUserSecurityKey(await getSecurityKey()) || 'Unknown';
    const savePath = path.join(app.getPath('pictures'), 'SnapSort');
    await startImageServer(savePath);
    const serverIp = await getLocalIpAddress();
    const wifiString = `WIFI:S:${ssid};T:WPA;P:${securityKey};H:false;;`;
    return { wifiString, serverIp };
  } catch (error) {
    console.error('Erreur lors du démarrage du service:', error);
    throw error;
  }
}

// Cette fonction ajoute les informations du serveur au QR code
export function generateTransferQRCode(wifiString: string, serverIp: string): string {
  return `${wifiString}IP:${serverIp};PORT:${SERVER_PORT};`;
}