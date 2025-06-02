import { useState, useEffect } from "react";
import QRCode from 'qrcode';
import '../styles/connection.css';
import { HotspotSection } from './HotspotSection';
import { TransferSection } from './TransferSection';
import { TransferStatus } from './TransferStatus';
import { useHotspot } from '../hooks/useHotspot';
import { useTransferService } from '../hooks/useTransferService';

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

function Connection() {
  // Hooks personnalisés pour la logique métier
  const {
    wifiString,
    qrCode,
    phoneIp,
    isLoading: isHotspotLoading,
    startHotspot: handleStartHotspot,
    fetchIpAddress
  } = useHotspot();

  const {
    isServiceActive,
    serverIp,
    transferQrCode,
    currentTransfer,
    completedTransfers,
    connectedDevices,
    isStarting,
    startService,
    stopService,
    fetchConnectedDevices
  } = useTransferService();

  return (
    <div className="connection-container">
      <div className="connection-wrapper">
        <h1 className="connection-title">Centre de Connexion et Transfert</h1>
        
        <div className="connection-grid">
          {/* Section Point d'accès WiFi */}
          <HotspotSection
            wifiString={wifiString}
            qrCode={qrCode}
            isLoading={isHotspotLoading}
            onStartHotspot={handleStartHotspot}
          />
          
          {/* Section Service de transfert */}
          <TransferSection
            isServiceActive={isServiceActive}
            serverIp={serverIp}
            phoneIp={phoneIp}
            transferQrCode={transferQrCode}
            isStarting={isStarting}
            onStartService={startService}
            onStopService={stopService}
            onFetchIp={fetchIpAddress}
          />
        </div>
        
        {/* Section statut des transferts - visible uniquement si service actif */}
        {isServiceActive && (
          <TransferStatus
            currentTransfer={currentTransfer}
            completedTransfers={completedTransfers}
            connectedDevices={connectedDevices}
            onRefreshDevices={fetchConnectedDevices}
          />
        )}
      </div>
    </div>
  );
}

export default Connection;