import { exec } from "child_process";
import { getScriptsPath } from './pathResolver.js';

interface WifiCredentials {
  ssid: string | null;
  password: string | null;
}

interface HotspotResult {
  wifiString?: string;
  error?: string;
}

interface IpExtractionResult {
  ip: string | null;
  interface: string | null;
}

class ConnectionService {
  
  // Démarre le hotspot Windows
  async startHotspot(): Promise<HotspotResult> {
    try {
      const command = `powershell -Command "Start-Process powershell -Verb runAs -ArgumentList '-Command [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType=WindowsRuntime]::CreateFromConnectionProfile([Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType=WindowsRuntime]::GetInternetConnectionProfile()).StartTetheringAsync()'"`;

      const stdout = await this.executeCommand(command);
      return { wifiString: stdout.trim() };
    } catch (error) {
      console.error("Erreur lors du démarrage du hotspot:", error);
      return { error: `Erreur lors du démarrage du hotspot: ${error}` };
    }
  }

  // Récupère le SSID du point d'accès
  async getHotspotSSID(): Promise<string> {
    const scriptPath = getScriptsPath('getSSID.ps1');
    const command = `powershell -ExecutionPolicy Bypass -File "${scriptPath}"`;
    
    try {
      const output = await this.executeCommand(command);
      return this.extractSSID(output);
    } catch (error) {
      console.error("Erreur lors de la récupération du SSID:", error);
      throw new Error(`Erreur lors de la récupération du SSID: ${error}`);
    }
  }

  // Récupère la clé de sécurité du point d'accès
  async getHotspotSecurityKey(): Promise<string> {
    const scriptPath = getScriptsPath('getSecurityKey.ps1');
    const command = `powershell -ExecutionPolicy Bypass -File "${scriptPath}"`;
    
    try {
      const output = await this.executeCommand(command);
      return this.extractSecurityKey(output);
    } catch (error) {
      console.error("Erreur lors de la récupération de la clé:", error);
      throw new Error(`Erreur lors de la récupération de la clé: ${error}`);
    }
  }

  // Récupère les informations WiFi complètes
  async getWifiCredentials(): Promise<WifiCredentials> {
    const scriptPath = getScriptsPath('getWifiInfo.ps1');
    const command = `powershell -ExecutionPolicy Bypass -File "${scriptPath}"`;
    
    try {
      const output = await this.executeCommand(command);
      return this.parseWifiCredentials(output);
    } catch (error) {
      console.error("Erreur lors de la récupération des informations WiFi:", error);
      throw new Error(`Erreur lors de la récupération des informations WiFi: ${error}`);
    }
  }

  // Récupère l'adresse IP du téléphone connecté
  async getConnectedPhoneIP(): Promise<string | null> {
    try {
      const arpOutput = await this.executeCommand('arp -a');
      return this.extractPhoneIPFromARP(arpOutput);
    } catch (error) {
      console.error("Erreur lors de la récupération de l'IP:", error);
      throw new Error(`Erreur lors de la récupération de l'IP: ${error}`);
    }
  }

  // Méthodes utilitaires privées

  // Exécute une commande système de manière asynchrone
  private executeCommand(command: string): Promise<string> {
    return new Promise((resolve, reject) => {
      exec(command, (error, stdout, stderr) => {
        if (error) {
          reject(error.message);
          return;
        }
        if (stderr) {
          reject(stderr);
          return;
        }
        resolve(stdout);
      });
    });
  }

  // Extrait le SSID de la sortie PowerShell
  private extractSSID(output: string): string {
    const match = output.match(/SSID:\s*(.+)/i);
    if (!match || !match[1]) {
      throw new Error("SSID non trouvé dans la sortie");
    }
    return match[1].trim();
  }

  // Extrait la clé de sécurité de la sortie PowerShell
  private extractSecurityKey(output: string): string {
    const match = output.match(/Key:\s*(.+)/i);
    if (!match || !match[1]) {
      throw new Error("Clé de sécurité non trouvée dans la sortie");
    }
    return match[1].trim();
  }

  // Parse les informations WiFi complètes
  private parseWifiCredentials(data: string): WifiCredentials {
    let ssid: string | null = null;
    let password: string | null = null;
    
    // Recherche du SSID
    const ssidMatch = data.match(/ssid:\s*(.+)/i);
    if (ssidMatch && ssidMatch[1]) {
      ssid = ssidMatch[1].trim();
    }
    
    // Recherche du mot de passe
    const passwordMatch = data.match(/password:\s*(.+)/i);
    if (passwordMatch && passwordMatch[1]) {
      password = passwordMatch[1].trim();
    }
    
    return { ssid, password };
  }

  // Extrait l'IP du téléphone depuis la table ARP
  private extractPhoneIPFromARP(arpOutput: string): string | null {
    const interfaces = arpOutput.split(/Interface:/).slice(1);
    const targetInterface = interfaces.find(block => block.includes("192.168.137."));

    if (!targetInterface) {
      console.log("Interface hotspot non trouvée dans la table ARP");
      return null;
    }

    const lines = targetInterface.split("\n").map(line => line.trim());
    const headerIndex = lines.findIndex(line => line.startsWith("Internet Address"));

    if (headerIndex === -1 || headerIndex + 1 >= lines.length) {
      console.log("En-tête de table ARP non trouvé");
      return null;
    }

    // Recherche de la première adresse IP valide après l'en-tête
    for (let i = headerIndex + 1; i < lines.length; i++) {
      const match = lines[i].match(/^192\.168\.137\.\d+/);
      if (match) {
        console.log(`IP du téléphone trouvée: ${match[0]}`);
        return match[0];
      }
    }

    console.log("Aucune IP de téléphone trouvée dans l'interface hotspot");
    return null;
  }

  // Génère une chaîne WiFi formatée pour QR code
  generateWifiQRString(ssid: string, password: string, encryption: string = "WPA"): string {
    return `WIFI:T:${encryption};S:${ssid};P:${password};;`;
  }
}

// Export des fonctions pour compatibilité avec l'API existante
const connectionService = new ConnectionService();

export const startHotspot = () => connectionService.startHotspot();
export const getSSID = () => connectionService.getHotspotSSID();
export const getSecurityKey = () => connectionService.getHotspotSecurityKey();
export const getWifiInfo = () => connectionService.getWifiCredentials();
export const getPhoneIpAddress = () => connectionService.getConnectedPhoneIP();

// Fonctions d'extraction pour compatibilité
export const extractSSID = (output: string): string | null => {
  try {
    return connectionService['extractSSID'](output);
  } catch {
    return null;
  }
};

export const extractUserSecurityKey = (output: string): string | null => {
  try {
    return connectionService['extractSecurityKey'](output);
  } catch {
    return null;
  }
};

export const extractWifiInfo = (data: string) => {
  return connectionService['parseWifiCredentials'](data);
};

export const extractIpAddress = (arpOutput: string): string | null => {
  return connectionService['extractPhoneIPFromARP'](arpOutput);
};

export default connectionService;