import { useState } from 'react';
import QRCode from 'qrcode';

export const useHotspot = () => {
  const [wifiString, setWifiString] = useState<string>("");
  const [qrCode, setQrCode] = useState<string>("");
  const [phoneIp, setPhoneIp] = useState<string>("");
  const [isLoading, setIsLoading] = useState<boolean>(false);

  // Génère un QR code WiFi à partir de la chaîne de connexion
  const generateQRCode = async (wifiString: string) => {
    try {
      // Nettoyage de la chaîne WiFi
      const cleanedWifiString = wifiString.trim()
        .replace(/\r/g, "")  
        .replace(/\n/g, "") 
        .replace(/\s*;\s*/g, ";")  
        .replace(/\s+/g, " ");  

      // Génération du QR Code
      const qrCodeDataUrl = await QRCode.toDataURL(cleanedWifiString, {
        errorCorrectionLevel: 'H',  
        width: 300,  
        margin: 1,  
        color: {
          dark: '#000000', 
          light: '#FFFFFF'  
        }
      });
      setQrCode(qrCodeDataUrl);

    } catch (error) {
      console.error("Erreur lors de la génération du QR Code:", error);
    }
  };

  // Démarre le hotspot WiFi
  const startHotspot = async () => {
    setIsLoading(true);
    try {
      const result = await (window as any).electron.startHotspot();
      
      if (result?.wifiString) {
        setWifiString(result.wifiString);
        console.log("Hotspot activé: ", result.wifiString);
        await generateQRCode(result.wifiString);
      } else {
        setWifiString(result?.error || "Erreur lors de l'activation du hotspot");
      }
    } catch (error) {
      console.error("Erreur lors du démarrage du hotspot:", error);
      setWifiString("Erreur lors de l'activation du hotspot");
    } finally {
      setIsLoading(false);
    }
  };

  // Récupère l'adresse IP du téléphone connecté
  const fetchIpAddress = async () => {
    try {
      const result = await (window as any).electron.getIpAdress();
      setPhoneIp(result);
      console.log("Phone IP Address:", result);
    } catch (error) {
      console.error("Erreur lors de la récupération de l'IP:", error);
    }
  };

  return {
    wifiString,
    qrCode,
    phoneIp,
    isLoading,
    startHotspot,
    fetchIpAddress
  };
};