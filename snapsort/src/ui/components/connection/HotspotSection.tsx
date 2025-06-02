import React from 'react';
import { WifiIcon } from '../icons/WifiIcon';

interface HotspotSectionProps {
  wifiString: string;
  qrCode: string;
  isLoading: boolean;
  onStartHotspot: () => void;
}

export const HotspotSection: React.FC<HotspotSectionProps> = ({
  wifiString,
  qrCode,
  isLoading,
  onStartHotspot
}) => {
  // Formate la chaîne WiFi pour l'affichage
  const formatWifiString = (wifiStr: string) => {
    return wifiStr.replace(/WIFI:/, "WiFi: ").replace(/;/g, "; ");
  };

  return (
    <div className="connection-card">
      <div className="connection-card-header connection-card-header--hotspot">
        <h2 className="connection-card-title">Point d'Accès WiFi</h2>
      </div>
      
      <div className="connection-card-content">
        <button 
          onClick={onStartHotspot}
          disabled={isLoading}
          className={`connection-btn connection-btn--primary ${isLoading ? 'connection-btn--loading' : ''}`}
        >
          <WifiIcon />
          {isLoading ? 'Activation...' : 'Activer le point d\'accès'}
        </button>
        
        {wifiString && (
          <div className="connection-info-section">
            <div className="connection-info-box">
              <p className="connection-wifi-string">
                {formatWifiString(wifiString)}
              </p>
            </div>
            
            {qrCode && (
              <div className="connection-qr-container">
                <h4 className="connection-qr-title">Scanner pour se connecter:</h4>
                <div className="connection-qr-wrapper">
                  <img 
                    src={qrCode} 
                    alt="QR Code WiFi" 
                    className="connection-qr-image" 
                  />
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};