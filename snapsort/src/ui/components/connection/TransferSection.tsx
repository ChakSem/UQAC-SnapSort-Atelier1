import React from 'react';
import { PlayIcon, StopIcon, GlobeIcon } from '../icons/TransferIcons';

interface TransferSectionProps {
  isServiceActive: boolean;
  serverIp: string;
  phoneIp: string;
  transferQrCode: string;
  onStartService: () => void;
  onStopService: () => void;
  onFetchIp: () => void;
}

export const TransferSection: React.FC<TransferSectionProps> = ({
  isServiceActive,
  serverIp,
  phoneIp,
  transferQrCode,
  onStartService,
  onStopService,
  onFetchIp
}) => {
  return (
    <div className="connection-card">
      <div className="connection-card-header connection-card-header--transfer">
        <h2 className="connection-card-title">Service de Transfert d'Images</h2>
      </div>
      
      <div className="connection-card-content">
        <div className="connection-btn-group">
          <button 
            onClick={onStartService} 
            disabled={isServiceActive}
            className={`connection-btn ${isServiceActive 
              ? 'connection-btn--disabled' 
              : 'connection-btn--success'}`}
          >
            <PlayIcon />
            Démarrer le service
          </button>
          
          <button 
            onClick={onStopService} 
            disabled={!isServiceActive}
            className={`connection-btn ${!isServiceActive 
              ? 'connection-btn--disabled' 
              : 'connection-btn--danger'}`}
          >
            <StopIcon />
            Arrêter le service
          </button>
          
          <button 
            onClick={onFetchIp} 
            className="connection-btn connection-btn--primary"
          >
            <GlobeIcon />
            Récupérer l'IP
          </button>
        </div>
        
        {serverIp && (
          <div className="connection-server-info">
            <div className="connection-info-row">
              <span className="connection-info-label">Serveur IP:</span>
              <span className="connection-info-value">{serverIp}</span>
            </div>
            <div className="connection-info-row">
              <span className="connection-info-label">Port:</span>
              <span className="connection-info-value">8080</span>
            </div>
            {phoneIp && (
              <div className="connection-info-row">
                <span className="connection-info-label">Téléphone IP:</span>
                <span className="connection-info-value">{phoneIp}</span>
              </div>
            )}
          </div>
        )}
        
        {isServiceActive && transferQrCode && (
          <div className="connection-qr-container">
            <h4 className="connection-qr-title">Scanner pour configurer l'application mobile:</h4>
            <div className="connection-qr-wrapper">
              <img 
                src={transferQrCode} 
                alt="QR Code Transfert" 
                className="connection-qr-image" 
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};