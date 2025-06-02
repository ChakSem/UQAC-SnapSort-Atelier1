import React from 'react';
import { RefreshIcon, DeviceIcon, NoDevicesIcon } from '../icons/TransferIcons';

interface ConnectedDevice {
  ip: string;
  mac: string;
  name: string;
}

interface DeviceListProps {
  connectedDevices: ConnectedDevice[];
  onRefreshDevices: () => void;
}

export const DeviceList: React.FC<DeviceListProps> = ({
  connectedDevices,
  onRefreshDevices
}) => {
  return (
    <div className="connection-card">
      <div className="connection-card-header connection-card-header--devices">
        <h2 className="connection-card-title">Appareils Connectés</h2>
        <button 
          onClick={onRefreshDevices}
          className="connection-refresh-btn"
          title="Rafraîchir la liste des appareils"
        >
          <RefreshIcon />
          Rafraîchir
        </button>
      </div>
      
      <div className="connection-card-content">
        {connectedDevices.length > 0 ? (
          <div className="connection-device-list">
            {connectedDevices.map((device, index) => (
              <div key={index} className="connection-device-item">
                <div className="connection-device-icon">
                  <DeviceIcon />
                </div>
                
                <div className="connection-device-info">
                  <p className="connection-device-name">
                    {device.name || 'Appareil inconnu'}
                  </p>
                  <div className="connection-device-details">
                    <div className="connection-device-detail">
                      <span>IP:</span>
                      <span className="connection-device-detail-value">{device.ip}</span>
                    </div>
                    <div className="connection-device-detail">
                      <span>MAC:</span>
                      <span className="connection-device-detail-value">{device.mac}</span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="connection-no-devices">
            <NoDevicesIcon className="connection-no-devices-icon" />
            <p className="connection-no-devices-text">Aucun appareil connecté détecté</p>
            <p className="connection-no-devices-subtext">
              Les appareils apparaîtront ici une fois connectés
            </p>
          </div>
        )}
      </div>
    </div>
  );
};