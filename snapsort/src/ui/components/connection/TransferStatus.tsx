import React from 'react';
import { DeviceList } from './DeviceList';
import { CheckIcon, SpinnerIcon } from '../icons/TransferIcons';

interface TransferInfo {
  fileName: string;
  progress: number;
  receivedBytes: number;
  totalBytes: number;
  fileSize?: number;
  index?: number;
  total?: number;
}

interface ConnectedDevice {
  ip: string;
  mac: string;
  name: string;
}

interface TransferStatusProps {
  currentTransfer: TransferInfo | null;
  completedTransfers: string[];
  connectedDevices: ConnectedDevice[];
  onRefreshDevices: () => void;
}

export const TransferStatus: React.FC<TransferStatusProps> = ({
  currentTransfer,
  completedTransfers,
  connectedDevices,
  onRefreshDevices
}) => {
  // Formate la taille des fichiers en unités lisibles
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  };

  return (
    <div className="connection-status-grid">
      {/* Section Progression du Transfert */}
      <div className="connection-card">
        <div className="connection-card-header connection-card-header--status">
          <h2 className="connection-card-title">Progression du Transfert</h2>
        </div>
        
        <div className="connection-card-content">
          {currentTransfer ? (
            <div className="connection-progress-container">
              <div className="connection-progress-info">
                <div className="connection-progress-filename">
                  <p className="connection-progress-filename">{currentTransfer.fileName}</p>
                  {currentTransfer.index && currentTransfer.total && (
                    <p className="connection-progress-file-count">
                      Fichier {currentTransfer.index} sur {currentTransfer.total}
                    </p>
                  )}
                </div>
                <span className="connection-progress-percentage">
                  {(currentTransfer.progress * 100).toFixed(1)}%
                </span>
              </div>
              
              <div className="connection-progress-bar">
                <div 
                  className="connection-progress-fill" 
                  style={{ width: `${Math.min(currentTransfer.progress * 100, 100)}%` }}
                />
              </div>
              
              <div className="connection-progress-bytes">
                <span>{formatFileSize(currentTransfer.receivedBytes)}</span>
                <span>{formatFileSize(currentTransfer.totalBytes)}</span>
              </div>
            </div>
          ) : (
            <div className="connection-waiting-state">
              <SpinnerIcon className="connection-spinner" />
              <span>En attente de transfert...</span>
            </div>
          )}
          
          {/* Section des transferts complétés */}
          {completedTransfers.length > 0 && (
            <div className="connection-completed-section">
              <div className="connection-completed-header">
                <h4 className="connection-completed-title">Transferts complétés</h4>
                <span className="connection-completed-badge">
                  {completedTransfers.length}
                </span>
              </div>
              
              <div className="connection-completed-list">
                <ul className="connection-completed-items">
                  {completedTransfers.slice(0, 10).map((file, index) => (
                    <li key={index} className="connection-completed-item">
                      <CheckIcon className="connection-completed-icon" />
                      <span className="connection-completed-filename">{file}</span>
                    </li>
                  ))}
                  {completedTransfers.length > 10 && (
                    <li className="connection-completed-item connection-completed-item--more">
                      <span>...et {completedTransfers.length - 10} de plus</span>
                    </li>
                  )}
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
      
      {/* Section Appareils Connectés */}
      <DeviceList 
        connectedDevices={connectedDevices}
        onRefreshDevices={onRefreshDevices}
      />
    </div>
  );
};