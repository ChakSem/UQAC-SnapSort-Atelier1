import { app } from 'electron';
import http from 'http';
import fs from 'fs';
import path from 'path';
import { EventEmitter } from 'events';
import connectionService from './connectionService.js';
import { networkInterfaces } from 'os';
import store from './store.js';

export const transferEvents = new EventEmitter();
const SERVER_PORT = 8080;

// Variables globales pour le serveur
let currentServer: http.Server | null = null;

// Types pour les informations de transfert
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

export function startImageServer(savePath: string): Promise<{ server: http.Server; port: number }> {
  return new Promise(async (resolve, reject) => {
    try {
      // S'assurer que le dossier de sauvegarde existe
      if (!fs.existsSync(savePath)) {
        await fs.promises.mkdir(savePath, { recursive: true });
      }

      const server = http.createServer(async (req, res) => {
        try {
          // Configuration CORS
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
            console.log('=== DÉBUT DU TRANSFERT ===');
            
            // Variables pour le transfert
            let totalImages = 0;
            let currentImageIndex = 0;
            let receivedData = Buffer.alloc(0);
            
            // Fonction pour extraire une ligne du buffer
            const extractLine = (): string | null => {
              const lineEndIndex = receivedData.indexOf('\n');
              if (lineEndIndex === -1) return null;
              
              const line = receivedData.slice(0, lineEndIndex).toString().trim();
              receivedData = receivedData.slice(lineEndIndex + 1);
              return line;
            };

            // Fonction pour extraire des données d'une taille spécifique
            const extractData = (size: number): Buffer | null => {
              if (receivedData.length < size) return null;
              
              const data = receivedData.slice(0, size);
              receivedData = receivedData.slice(size);
              return data;
            };

            // Collecte des données
            req.on('data', (chunk: Buffer) => {
              receivedData = Buffer.concat([receivedData, chunk]);
            });

            req.on('end', async () => {
              try {
                console.log(`Données totales reçues: ${receivedData.length} bytes`);

                // Lire le nombre total d'images
                const totalImagesLine = extractLine();
                if (!totalImagesLine) {
                  throw new Error('Impossible de lire le nombre total d\'images');
                }
                
                totalImages = parseInt(totalImagesLine, 10);
                console.log(`Nombre total d'images: ${totalImages}`);

                // Traitement de chaque image
                for (let i = 0; i < totalImages; i++) {
                  try {
                    // Attendre que les informations de l'image soient disponibles
                    let fileName: string | null = null;
                    let dateString: string | null = null;
                    let fileSize: number = 0;

                    // Attendre les métadonnées de l'image
                    while (!fileName || !dateString || fileSize === 0) {
                      if (!fileName) fileName = extractLine();
                      if (fileName && !dateString) dateString = extractLine();
                      if (fileName && dateString && fileSize === 0) {
                        const fileSizeLine = extractLine();
                        if (fileSizeLine) {
                          fileSize = parseInt(fileSizeLine, 10);
                        }
                      }
                      
                      // Si on n'a pas toutes les infos, attendre un peu
                      if (!fileName || !dateString || fileSize === 0) {
                        await new Promise(resolve => setTimeout(resolve, 10));
                      }
                    }

                    console.log(`\n--- Image ${i + 1}/${totalImages} ---`);
                    console.log(`Nom: ${fileName}`);
                    console.log(`Date: ${dateString}`);
                    console.log(`Taille: ${fileSize} bytes`);

                    // Émettre l'événement de début de transfert
                    const startInfo: TransferStartInfo = {
                      fileName,
                      fileSize,
                      index: i + 1,
                      total: totalImages
                    };
                    transferEvents.emit('transfer:start', startInfo);

                    // Attendre que toutes les données de l'image soient disponibles
                    while (receivedData.length < fileSize) {
                      await new Promise(resolve => setTimeout(resolve, 10));
                    }

                    // Extraire les données de l'image
                    const imageData = extractData(fileSize);
                    if (!imageData) {
                      throw new Error(`Impossible d'extraire les données pour ${fileName}`);
                    }

                    // Créer le chemin du fichier
                    const filePath = path.join(savePath, fileName);
                    
                    // Écrire le fichier
                    await fs.promises.writeFile(filePath, imageData);

                    // Émettre les événements de progression et de fin
                    const progressInfo: TransferProgressInfo = {
                      fileName,
                      progress: 1.0,
                      receivedBytes: fileSize,
                      totalBytes: fileSize
                    };
                    transferEvents.emit('transfer:progress', progressInfo);

                    const completeInfo: TransferCompleteInfo = {
                      fileName,
                      filePath,
                      size: fileSize
                    };
                    transferEvents.emit('transfer:complete', completeInfo);

                    console.log(`✓ Image ${fileName} sauvegardée avec succès`);

                  } catch (imageError) {
                    console.error(`Erreur lors du traitement de l'image ${i + 1}:`, imageError);
                    const errorInfo: TransferErrorInfo = {
                      error: `Erreur lors du traitement de l'image ${i + 1}: ${imageError instanceof Error ? imageError.message : 'Erreur inconnue'}`
                    };
                    transferEvents.emit('transfer:error', errorInfo);
                  }
                }

                console.log('\n=== TRANSFERT TERMINÉ ===');
                res.statusCode = 200;
                res.setHeader('Content-Type', 'application/json');
                res.end(JSON.stringify({ 
                  status: 'success', 
                  message: `${totalImages} images transférées avec succès`,
                  totalImages: totalImages
                }));

              } catch (error) {
                console.error('Erreur lors du traitement des données:', error);
                const errorInfo: TransferErrorInfo = {
                  error: `Erreur lors du traitement: ${error instanceof Error ? error.message : 'Erreur inconnue'}`
                };
                transferEvents.emit('transfer:error', errorInfo);
                
                res.statusCode = 500;
                res.setHeader('Content-Type', 'application/json');
                res.end(JSON.stringify({ 
                  status: 'error', 
                  message: 'Erreur lors du traitement des données' 
                }));
              }
            });

            req.on('error', (error) => {
              console.error('Erreur de requête:', error);
              const errorInfo: TransferErrorInfo = {
                error: `Erreur de requête: ${error.message}`
              };
              transferEvents.emit('transfer:error', errorInfo);
            });

          } else if (req.method === 'GET') {
            // Point de terminaison de statut
            const statusInfo = {
              status: 'active',
              version: '1.0.0',
              serverTime: new Date().toISOString(),
              port: SERVER_PORT,
              endpoints: {
                upload: 'POST /',
                status: 'GET /'
              }
            };
            
            res.statusCode = 200;
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify(statusInfo));
          } else {
            // Méthode non supportée
            res.statusCode = 405;
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ status: 'error', message: 'Méthode non supportée' }));
          }
        } catch (error) {
          console.error('Erreur dans le gestionnaire de requête:', error);
          const errorInfo: TransferErrorInfo = {
            error: `Erreur serveur: ${error instanceof Error ? error.message : 'Erreur inconnue'}`
          };
          transferEvents.emit('transfer:error', errorInfo);
          
          if (!res.headersSent) {
            res.statusCode = 500;
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ status: 'error', message: 'Erreur serveur interne' }));
          }
        }
      });

      // Gestion des erreurs du serveur
      server.on('error', (error: NodeJS.ErrnoException) => {
        console.error('Erreur serveur:', error);
        
        if (error.code === 'EADDRINUSE') {
          reject(new Error(`Le port ${SERVER_PORT} est déjà utilisé. Veuillez arrêter le service existant ou changer de port.`));
        } else {
          reject(new Error(`Erreur serveur: ${error.message}`));
        }
      });

      // Démarrer le serveur
      server.listen(SERVER_PORT, () => {
        console.log(`🚀 Serveur de transfert démarré sur le port ${SERVER_PORT}`);
        currentServer = server;
        resolve({ server, port: SERVER_PORT });
      });

    } catch (error) {
      reject(new Error(`Erreur lors de l'initialisation du serveur: ${error}`));
    }
  });
}

export function getLocalIpAddress(): Promise<string> {
  return new Promise((resolve, reject) => {
    try {
      const nets = networkInterfaces();
      
      // Priorité à l'adresse IP du hotspot Windows (192.168.137.x)
      for (const name of Object.keys(nets)) {
        const net = nets[name];
        if (net) {
          for (const addr of net) {
            if (addr.family === 'IPv4' && !addr.internal && addr.address.startsWith('192.168.137.')) {
              console.log(`IP du hotspot trouvée: ${addr.address}`);
              return resolve(addr.address);
            }
          }
        }
      }
      
      // Fallback vers toute adresse IP locale
      for (const name of Object.keys(nets)) {
        const net = nets[name];
        if (net) {
          for (const addr of net) {
            if (addr.family === 'IPv4' && !addr.internal) {
              console.log(`IP locale trouvée: ${addr.address}`);
              return resolve(addr.address);
            }
          }
        }
      }
      
      // Utiliser l'IP par défaut du hotspot Windows
      console.log('Utilisation de l\'IP par défaut du hotspot Windows');
      resolve('192.168.137.1');
      
    } catch (error) {
      console.error('Erreur lors de la récupération de l\'IP:', error);
      resolve('192.168.137.1'); // IP par défaut
    }
  });
}

export async function startImageTransferService() {
  try {
    console.log('🔄 Démarrage du service de transfert d\'images...');

    // Préparer le chemin de sauvegarde
    const rootPath = store.get("directoryPath") as string;
    if (!rootPath) {
      throw new Error("Aucun chemin de dossier racine défini");
    }
    
    const savePath = path.join(rootPath, "unsorted_images");
    console.log(`📁 Dossier de sauvegarde: ${savePath}`);

    // Vérifier et créer le dossier si nécessaire
    if (!fs.existsSync(savePath)) {
      await fs.promises.mkdir(savePath, { recursive: true });
      console.log('📁 Dossier de sauvegarde créé');
    }
    
    // Démarrer le serveur d'images
    const { server } = await startImageServer(savePath);
    currentServer = server;
    
    // Obtenir l'IP du serveur
    const serverIp = await getLocalIpAddress();
    console.log(`🌐 Serveur accessible à l'adresse: ${serverIp}:${SERVER_PORT}`);
    
    return { 
      serverIp, 
      savePath, 
      port: SERVER_PORT,
      message: 'Service de transfert démarré avec succès'
    };
  } catch (error) {
    console.error('❌ Erreur lors du démarrage du service:', error);
    throw error;
  }
}

export function generateTransferQRCode(wifiString: string, serverIp: string): string {
  const transferInfo = `${wifiString}IP:${serverIp};PORT:${SERVER_PORT};`;
  console.log(`📱 QR Code généré: ${transferInfo}`);
  return transferInfo;
}

export function stopImageTransferService(server?: http.Server): Promise<void> {
  return new Promise((resolve, reject) => {
    const serverToStop = server || currentServer;
    
    if (!serverToStop) {
      console.log('✅ Aucun serveur à arrêter');
      resolve();
      return;
    }
    
    console.log('🔄 Arrêt du serveur de transfert...');
    
    serverToStop.close((err) => {
      if (err) {
        console.error('❌ Erreur lors de l\'arrêt du serveur:', err);
        reject(err);
        return;
      }
      
      currentServer = null;
      console.log('✅ Serveur de transfert arrêté avec succès');
      resolve();
    });
  });
}