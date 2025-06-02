import React from 'react';
import { PlayIcon, StopIcon, GlobeIcon } from '../icons/TransferIcons';

interface TransferSectionProps {
  isServiceActive: boolean;
  serverIp: string;
  phoneIp: string;
  transferQrCode: string;
  isStarting?: boolean;
  onStartService: () => void;
  onStopService: () => void;
  onFetchIp: () => void;
}

export const TransferSection: React.FC<TransferSectionProps> = ({
  isServiceActive,
  serverIp,
  phoneIp,
  transferQrCode,
  isStarting = false,
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
        {/* Indicateur de statut */}
        <div className="connection-status-indicator">
          <div className={`connection-status-dot ${isServiceActive ? 'active' : 'inactive'}`}></div>
          <span className="connection-status-text">
            {isStarting ? 'Démarrage en cours...' : 
             isServiceActive ? 'Service actif' : 'Service inactif'}
          </span>
        </div>

        <div className="connection-btn-group">
          <button 
            onClick={onStartService} 
            disabled={isServiceActive || isStarting}
            className={`connection-btn ${(isServiceActive || isStarting)
              ? 'connection-btn--disabled' 
              : 'connection-btn--success'}`}
          >
            <PlayIcon />
            {isStarting ? 'Démarrage...' : 'Démarrer le service'}
          </button>
          
          <button 
            onClick={onStopService} 
            disabled={!isServiceActive || isStarting}
            className={`connection-btn ${(!isServiceActive || isStarting)
              ? 'connection-btn--disabled' 
              : 'connection-btn--danger'}`}
          >
            <StopIcon />
            Arrêter le service
          </button>
          
          <button 
            onClick={onFetchIp} 
            className="connection-btn connection-btn--primary"
            disabled={isStarting}
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
            
            {/* Instructions pour l'utilisateur */}
            <div className="connection-instructions">
              <h4>Instructions :</h4>
              <ol>
                <li>Connectez votre téléphone au WiFi ci-dessus</li>
                <li>Utilisez l'adresse <strong>{serverIp}:8080</strong> dans votre application</li>
                <li>Commencez le transfert depuis votre téléphone</li>
              </ol>
            </div>
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