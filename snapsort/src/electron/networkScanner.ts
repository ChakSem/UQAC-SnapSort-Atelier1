import { exec } from 'child_process';
import { networkInterfaces } from 'os';

interface ConnectedDevice {
  ip: string;
  mac: string;
  name: string;
  vendor?: string;
  status: 'active' | 'inactive';
}

interface NetworkScanResult {
  devices: ConnectedDevice[];
  totalFound: number;
  scanTime: number;
}

class NetworkScanner {
  private readonly HOTSPOT_NETWORK = '192.168.137';
  private readonly SCAN_TIMEOUT = 10000; // 10 secondes

  // Fonction principale pour récupérer les appareils connectés
  async getConnectedDevices(): Promise<ConnectedDevice[]> {
    try {
      console.log('🔍 Scan des appareils connectés au hotspot...');
      const startTime = Date.now();

      // Vérifier que le hotspot est actif
      const hotspotIp = await this.getHotspotIP();
      if (!hotspotIp) {
        console.log('❌ Hotspot non actif ou introuvable');
        return [];
      }

      console.log(`📡 Hotspot actif sur: ${hotspotIp}`);

      // Scanner le réseau
      const devices = await this.scanNetwork();
      
      const scanTime = Date.now() - startTime;
      console.log(`✅ Scan terminé en ${scanTime}ms - ${devices.length} appareil(s) trouvé(s)`);

      return devices;
    } catch (error) {
      console.error('❌ Erreur lors du scan des appareils:', error);
      return [];
    }
  }

  // Récupère l'IP du hotspot Windows
  private async getHotspotIP(): Promise<string | null> {
    try {
      const nets = networkInterfaces();
      
      for (const name of Object.keys(nets)) {
        const net = nets[name];
        if (net) {
          for (const addr of net) {
            if (addr.family === 'IPv4' && !addr.internal && addr.address.startsWith(this.HOTSPOT_NETWORK)) {
              return addr.address;
            }
          }
        }
      }
      
      return null;
    } catch (error) {
      console.error('Erreur lors de la récupération de l\'IP du hotspot:', error);
      return null;
    }
  }

  // Scanne le réseau pour trouver les appareils connectés
  private async scanNetwork(): Promise<ConnectedDevice[]> {
    try {
      // Utiliser la table ARP pour obtenir les appareils connectés
      const arpDevices = await this.getARPDevices();
      
      // Ping les appareils pour vérifier leur statut
      const activeDevices = await this.verifyDevicesStatus(arpDevices);
      
      return activeDevices;
    } catch (error) {
      console.error('Erreur lors du scan réseau:', error);
      return [];
    }
  }

  // Récupère les appareils depuis la table ARP
  private async getARPDevices(): Promise<ConnectedDevice[]> {
    return new Promise((resolve) => {
      const command = process.platform === 'win32' ? 'arp -a' : 'arp -a';
      
      exec(command, { timeout: this.SCAN_TIMEOUT }, (error, stdout, stderr) => {
        if (error) {
          console.error('Erreur ARP:', error);
          resolve([]);
          return;
        }

        const devices = this.parseARPOutput(stdout);
        resolve(devices);
      });
    });
  }

  // Parse la sortie de la commande ARP
  private parseARPOutput(arpOutput: string): ConnectedDevice[] {
    const devices: ConnectedDevice[] = [];
    
    try {
      // Séparer par interfaces
      const interfaces = arpOutput.split(/Interface:/);
      
      // Trouver l'interface du hotspot
      const hotspotInterface = interfaces.find(block => 
        block.includes(this.HOTSPOT_NETWORK)
      );

      if (!hotspotInterface) {
        console.log('Interface hotspot non trouvée dans ARP');
        return devices;
      }

      const lines = hotspotInterface.split('\n');
      
      for (const line of lines) {
        const trimmedLine = line.trim();
        
        // Chercher les lignes avec des adresses IP du hotspot
        const match = trimmedLine.match(/^(192\.168\.137\.\d+)\s+([0-9a-fA-F-]{17})\s+(\w+)/);
        
        if (match) {
          const [, ip, mac, type] = match;
          
          // Ignorer l'IP du hotspot lui-même
          if (ip !== '192.168.137.1') {
            devices.push({
              ip,
              mac: this.formatMacAddress(mac),
              name: this.getDeviceName(ip, mac),
              vendor: this.getVendorFromMac(mac),
              status: type.toLowerCase() === 'dynamic' ? 'active' : 'inactive'
            });
          }
        }
      }
    } catch (error) {
      console.error('Erreur lors du parsing ARP:', error);
    }

    return devices;
  }

  // Vérifie le statut des appareils avec ping
  private async verifyDevicesStatus(devices: ConnectedDevice[]): Promise<ConnectedDevice[]> {
    const verifiedDevices: ConnectedDevice[] = [];

    for (const device of devices) {
      try {
        const isActive = await this.pingDevice(device.ip);
        verifiedDevices.push({
          ...device,
          status: isActive ? 'active' : 'inactive'
        });
      } catch (error) {
        // En cas d'erreur, considérer l'appareil comme inactif
        verifiedDevices.push({
          ...device,
          status: 'inactive'
        });
      }
    }

    return verifiedDevices.filter(device => device.status === 'active');
  }

  // Ping un appareil pour vérifier s'il est actif
  private async pingDevice(ip: string): Promise<boolean> {
    return new Promise((resolve) => {
      const command = process.platform === 'win32' 
        ? `ping -n 1 -w 1000 ${ip}` 
        : `ping -c 1 -W 1 ${ip}`;

      exec(command, { timeout: 2000 }, (error, stdout) => {
        if (error) {
          resolve(false);
          return;
        }

        // Vérifier si le ping a réussi
        const success = process.platform === 'win32'
          ? stdout.includes('TTL=')
          : stdout.includes('1 received');

        resolve(success);
      });
    });
  }

  // Formate l'adresse MAC
  private formatMacAddress(mac: string): string {
    // Convertir les formats Windows (xx-xx-xx-xx-xx-xx) vers le format standard (xx:xx:xx:xx:xx:xx)
    return mac.replace(/-/g, ':').toLowerCase();
  }

  // Essaie de déterminer le nom de l'appareil
  private getDeviceName(ip: string, mac: string): string {
    // Essayer de résoudre le nom via DNS inverse (optionnel)
    // Pour l'instant, utiliser un nom générique basé sur le vendor
    const vendor = this.getVendorFromMac(mac);
    
    if (vendor) {
      return `${vendor} Device`;
    }
    
    return `Device ${ip.split('.').pop()}`;
  }

  // Identifie le fabricant à partir de l'adresse MAC
  private getVendorFromMac(mac: string): string | undefined {
    // Base de données simplifiée des préfixes MAC des fabricants courants
    const vendorPrefixes: { [key: string]: string } = {
      // Apple
      '00:03:93': 'Apple',
      '00:05:02': 'Apple',
      '00:0a:27': 'Apple',
      '00:0a:95': 'Apple',
      '00:11:24': 'Apple',
      '00:14:51': 'Apple',
      '00:16:cb': 'Apple',
      '00:17:f2': 'Apple',
      '00:19:e3': 'Apple',
      '00:1b:63': 'Apple',
      '00:1e:c2': 'Apple',
      '00:21:e9': 'Apple',
      '00:23:12': 'Apple',
      '00:23:df': 'Apple',
      '00:25:00': 'Apple',
      '00:25:4b': 'Apple',
      '00:26:08': 'Apple',
      '00:26:4a': 'Apple',
      '00:26:b0': 'Apple',
      '00:26:bb': 'Apple',
      
      // Samsung
      '00:02:78': 'Samsung',
      '00:07:ab': 'Samsung',
      '00:09:18': 'Samsung',
      '00:0d:e5': 'Samsung',
      '00:12:47': 'Samsung',
      '00:13:77': 'Samsung',
      '00:15:99': 'Samsung',
      '00:16:32': 'Samsung',
      '00:17:c9': 'Samsung',
      '00:1a:8a': 'Samsung',
      '00:1b:98': 'Samsung',
      '00:1d:25': 'Samsung',
      '00:1e:7d': 'Samsung',
      '00:21:19': 'Samsung',
      '00:23:39': 'Samsung',
      
      // Huawei
      '00:e0:fc': 'Huawei',
      '00:25:9e': 'Huawei',
      '28:6e:d4': 'Huawei',
      '34:6b:d3': 'Huawei',
      '4c:54:99': 'Huawei',
      '50:8f:4c': 'Huawei',
      '68:3e:34': 'Huawei',
      '78:f8:82': 'Huawei',
      '84:a4:23': 'Huawei',
      '9c:28:ef': 'Huawei',
      
      // Xiaomi
      '34:ce:00': 'Xiaomi',
      // '50:8f:4c': 'Xiaomi', // Removed duplicate key, already used for Huawei
      '68:df:dd': 'Xiaomi',
      '74:51:ba': 'Xiaomi',
      '78:11:dc': 'Xiaomi',
      '8c:be:be': 'Xiaomi',
      '98:fa:9b': 'Xiaomi',
      'f8:a4:5f': 'Xiaomi',
      
      // OnePlus
      'ac:37:43': 'OnePlus',
      'e8:b2:ac': 'OnePlus',
      
      // Google
      '00:1a:11': 'Google',
      'f4:f5:e8': 'Google',
      
      // Microsoft
      '00:15:5d': 'Microsoft',
      '00:03:ff': 'Microsoft'
    };

    const macPrefix = mac.substring(0, 8).toLowerCase();
    return vendorPrefixes[macPrefix];
  }

  // Méthode utilitaire pour obtenir des statistiques
  async getNetworkStats(): Promise<{
    hotspotActive: boolean;
    hotspotIP: string | null;
    connectedDevices: number;
    lastScanTime: Date;
  }> {
    const hotspotIP = await this.getHotspotIP();
    const devices = await this.getConnectedDevices();
    
    return {
      hotspotActive: hotspotIP !== null,
      hotspotIP,
      connectedDevices: devices.length,
      lastScanTime: new Date()
    };
  }
}

// Export de l'instance du scanner
const networkScanner = new NetworkScanner();

export default networkScanner;
export { ConnectedDevice, NetworkScanResult };
export const getConnectedDevices = () => networkScanner.getConnectedDevices();
export const getNetworkStats = () => networkScanner.getNetworkStats();