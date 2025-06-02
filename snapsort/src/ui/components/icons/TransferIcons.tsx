import React from 'react';

interface IconProps {
  className?: string;
  size?: number;
}

// Icône Play pour démarrer le service
export const PlayIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 20 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`play-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" 
      />
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
      />
    </svg>
  );
};

// Icône Stop pour arrêter le service
export const StopIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 20 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`stop-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
      />
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M9 10a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z" 
      />
    </svg>
  );
};

// Icône Globe pour récupérer l'IP
export const GlobeIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 20 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`globe-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9 3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" 
      />
    </svg>
  );
};

// Icône Refresh pour rafraîchir
export const RefreshIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 16 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`refresh-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" 
      />
    </svg>
  );
};

// Icône Check pour les transferts complétés
export const CheckIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 16 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`check-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M5 13l4 4L19 7" 
      />
    </svg>
  );
};

// Icône Spinner pour le chargement
export const SpinnerIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 20 
}) => {
  return (
    <svg 
      className={`spinner-icon animate-spin ${className}`}
      xmlns="http://www.w3.org/2000/svg" 
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24"
    >
      <circle 
        className="opacity-25" 
        cx="12" 
        cy="12" 
        r="10" 
        stroke="currentColor" 
        strokeWidth="4"
      />
      <path 
        className="opacity-75" 
        fill="currentColor" 
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
  );
};

// Icône Device pour les appareils
export const DeviceIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 24 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`device-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" 
      />
    </svg>
  );
};

// Icône No Devices pour l'état vide
export const NoDevicesIcon: React.FC<IconProps> = ({ 
  className = "", 
  size = 48 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`no-devices-icon ${className}`}
      width={size}
      height={size}
      fill="none" 
      viewBox="0 0 24 24" 
      stroke="currentColor"
      strokeWidth={2}
    >
      <path 
        strokeLinecap="round" 
        strokeLinejoin="round" 
        d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" 
      />
    </svg>
  );
};