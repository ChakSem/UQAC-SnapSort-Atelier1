import { exec } from "child_process";

// Fonction pour démarrer le hotspot
export function startHotspot(): Promise<string> {
    return new Promise((resolve, reject) => {
        const command = `powershell -Command "Start-Process powershell -Verb runAs -ArgumentList '-Command [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile([Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile()).StartTetheringAsync()'"`;

        exec(command, (error, stdout, stderr) => {
            if (error) {
                reject(`Erreur: ${error.message}`);
                return;
            }
            if (stderr) {
                reject(`Erreur PowerShell: ${stderr}`);
                return;
            }

            resolve(stdout.trim());
        });
    });
}

// Fonction pour récupérer la langue de la console
export function getConsoleLanguage(): Promise<string> {
  return new Promise((resolve, reject) => {
    const command = `powershell -Command "(Get-UICulture).Name"`;
    exec(command, (error, stdout, stderr) => {
      if (error) {
        console.error(`Erreur: ${error.message}`);
        return reject(`Erreur: ${error.message}`);
      }
      if (stderr) {
        console.error(`Erreur PowerShell: ${stderr}`);
        return reject(`Erreur PowerShell: ${stderr}`);
      }
      const lang = stdout.trim();
      console.log(`Langue détectée: ${lang}`);
      resolve(lang);
    });
  });
}

// Fonction pour récupérer le SSID en fonction de la langue
export async function getSSID(): Promise<string> {
    try {
      const lang = await getConsoleLanguage();
      // Définir le filtre en fonction de la langue
      let matchString;
      if (lang.startsWith("fr")) {
        matchString = "Nom du SSID";
      } else {
        matchString = "SSID name";
      }
      // Forcer la sortie en UTF-8
      const command = `powershell -Command "$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; (netsh wlan show hostednetwork) -match '${matchString}'"`;
      return new Promise((resolve, reject) => {
        exec(command, (error, stdout, stderr) => {
          if (error) {
            console.error(`Erreur: ${error.message}`);
            return reject(`Erreur: ${error.message}`);
          }
          if (stderr) {
            console.error(`Erreur PowerShell: ${stderr}`);
            return reject(`Erreur PowerShell: ${stderr}`);
          }
          const output = stdout.trim();
          console.log(`SSID brut: ${output}`);
          // Extraire le SSID proprement
          const ssid = extractSSID(output);
          resolve(ssid || output);
        });
      });
    } catch (err: unknown) {
      if (err instanceof Error) {
        throw new Error(err.message);
      } else {
        throw new Error(String(err));
      }
    }
}

// Fonction pour extraire le SSID
export function extractSSID(data: string): string | null {
    // Supporte les formats français et anglais
    const frMatch = data.match(/Nom du SSID\s*:\s*«\s*([^»]+)\s*»/);
    if (frMatch) return frMatch[1];
    
    const enMatch = data.match(/SSID name\s*:\s*"([^"]+)"/);
    return enMatch ? enMatch[1] : null;
}

// Fonction pour récupérer la clé de sécurité
export async function getSecurityKey(): Promise<string> {
    try {
      const lang = await getConsoleLanguage();
      // Définir le filtre en fonction de la langue
      let matchString;
      if (lang.startsWith("fr")) {
        matchString = "Clé de sécurité utilisateur";
      } else {
        matchString = "User security key";
      }
      const command = `powershell -Command "$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8; (netsh wlan show hostednetwork setting=security) -match '${matchString}'"`;
      return new Promise((resolve, reject) => {
        exec(command, (error, stdout, stderr) => {
          if (error) {
            console.error(`Erreur: ${error.message}`);
            return reject(`Erreur: ${error.message}`);
          }
          if (stderr) {
            console.error(`Erreur PowerShell: ${stderr}`);
            return reject(`Erreur PowerShell: ${stderr}`);
          }
          const output = stdout.trim();
          console.log(`Clé de sécurité brute: ${output}`);
          // Extraire la clé proprement
          const key = extractUserSecurityKey(output);
          resolve(key || output);
        });
      });
    } catch (err: unknown) {
      if (err instanceof Error) {
        throw new Error(err.message);
      } else {
        throw new Error(String(err));
      }
    }
}

// Fonction pour extraire la clé de sécurité
export function extractUserSecurityKey(data: string): string | null {
    // Supporte les formats français et anglais
    const frMatch = data.match(/Clé de sécurité utilisateur\s*:\s*(.+)/);
    if (frMatch) return frMatch[1].trim();
    
    const enMatch = data.match(/User security key\s*:\s*(.+)/);
    return enMatch ? enMatch[1].trim() : null;
}

// Fonction pour obtenir l'interface du hotspot
export async function getHotspotInterface(): Promise<string | null> {
    return new Promise((resolve, reject) => {
        // Récupérer l'interface du hotspot (généralement "Connexion au réseau local* XX" en français ou "Local Area Connection* XX" en anglais)
        const command = `powershell -Command "Get-NetAdapter | Where-Object { $_.InterfaceDescription -like '*Microsoft Hosted Network Virtual Adapter*' } | Select-Object -ExpandProperty Name"`;
        
        exec(command, (error, stdout, stderr) => {
            if (error) {
                console.error(`Erreur: ${error.message}`);
                return reject(error);
            }
            if (stderr) {
                console.error(`Erreur PowerShell: ${stderr}`);
                return reject(stderr);
            }
            
            const interfaceName = stdout.trim();
            resolve(interfaceName || null);
        });
    });
}

// Fonction pour obtenir l'adresse IP du hotspot
export async function getHotspotIPAddress(): Promise<string | null> {
    try {
        const interfaceName = await getHotspotInterface();
        if (!interfaceName) {
            console.log("Interface du hotspot non trouvée");
            return null;
        }
        
        // Échapper le nom de l'interface pour l'utiliser dans PowerShell
        const escapedName = interfaceName.replace(/'/g, "''");
        
        // Récupérer l'adresse IP de l'interface du hotspot
        const command = `powershell -Command "Get-NetIPAddress -InterfaceAlias '${escapedName}' -AddressFamily IPv4 | Select-Object -ExpandProperty IPAddress"`;
        
        return new Promise((resolve, reject) => {
            exec(command, (error, stdout, stderr) => {
                if (error) {
                    console.error(`Erreur: ${error.message}`);
                    return reject(error);
                }
                if (stderr) {
                    console.error(`Erreur PowerShell: ${stderr}`);
                    return reject(stderr);
                }
                
                const ipAddress = stdout.trim();
                resolve(ipAddress || null);
            });
        });
    } catch (err) {
        console.error(`Erreur lors de la récupération de l'adresse IP du hotspot: ${err}`);
        return null;
    }
}

// Fonction pour récupérer uniquement les appareils connectés au hotspot
export async function getConnectedDevices(): Promise<{ip: string, mac: string}[]> {
    try {
        // 1. Obtenir l'adresse IP du hotspot pour déterminer le sous-réseau
        const hotspotIP = await getHotspotIPAddress();
        if (!hotspotIP) {
            throw new Error("Impossible de déterminer l'adresse IP du hotspot");
        }
        
        // 2. Extraire le préfixe du sous-réseau (ex: 192.168.137)
        const ipPrefix = hotspotIP.split('.').slice(0, 3).join('.');
        console.log(`Préfixe du sous-réseau du hotspot: ${ipPrefix}`);
        
        // 3. Exécuter la commande ARP pour obtenir toutes les associations IP-MAC
        return new Promise((resolve, reject) => {
            exec('arp -a', (error, stdout, stderr) => {
                if (error) {
                    reject(`Erreur lors de l'exécution de la commande ARP: ${error.message}`);
                    return;
                }
                
                if (stderr) {
                    reject(`Erreur dans la sortie standard: ${stderr}`);
                    return;
                }
                
                // 4. Analyser la sortie et filtrer par le sous-réseau du hotspot
                const lines = stdout.split('\n');
                const connectedDevices: {ip: string, mac: string}[] = [];
                
                for (const line of lines) {
                    // Rechercher les lignes qui contiennent une adresse IP et une adresse MAC
                    const ipMatch = line.match(/(\d+\.\d+\.\d+\.\d+)/);
                    const macMatch = line.match(/([0-9a-fA-F][0-9a-fA-F]-[0-9a-fA-F][0-9a-fA-F]-[0-9a-fA-F][0-9a-fA-F]-[0-9a-fA-F][0-9a-fA-F]-[0-9a-fA-F][0-9a-fA-F]-[0-9a-fA-F][0-9a-fA-F])/);
                    
                    if (ipMatch && macMatch) {
                        const ip = ipMatch[1];
                        const mac = macMatch[1];
                        
                        // Ne garder que les IPs du sous-réseau du hotspot et exclure l'adresse du hotspot lui-même
                        if (ip.startsWith(ipPrefix) && ip !== hotspotIP) {
                            connectedDevices.push({ ip, mac });
                        }
                    }
                }
                
                resolve(connectedDevices);
            });
        });
    } catch (err) {
        console.error(`Erreur lors de la récupération des appareils connectés: ${err}`);
        return [];
    }
}

// Fonction supplémentaire pour vérifier l'état du hotspot
export function getHotspotStatus(): Promise<string> {
    return new Promise((resolve, reject) => {
        const command = 'netsh wlan show hostednetwork';
        
        exec(command, (error, stdout, stderr) => {
            if (error) {
                reject(`Erreur: ${error.message}`);
                return;
            }
            if (stderr) {
                reject(`Erreur: ${stderr}`);
                return;
            }
            
            resolve(stdout.trim());
        });
    });
}

// Fonction principale pour obtenir toutes les informations du hotspot et des appareils connectés
export async function getHotspotInfo(): Promise<{
    status: string,
    ssid: string | null,
    securityKey: string | null,
    interface: string | null,
    ipAddress: string | null,
    connectedDevices: {ip: string, mac: string}[]
}> {
    try {
        // Recueillir toutes les informations en parallèle pour optimiser le temps d'exécution
        const [status, ssid, securityKey, interfaceName, ipAddress, connectedDevices] = await Promise.all([
            getHotspotStatus(),
            getSSID(),
            getSecurityKey(),
            getHotspotInterface(),
            getHotspotIPAddress(),
            getConnectedDevices()
        ]);
        
        return {
            status,
            ssid,
            securityKey,
            interface: interfaceName,
            ipAddress,
            connectedDevices
        };
    } catch (err) {
        console.error(`Erreur lors de la récupération des informations du hotspot: ${err}`);
        throw err;
    }
}