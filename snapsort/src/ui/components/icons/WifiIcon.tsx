import React from 'react';

interface WifiIconProps {
  className?: string;
  size?: number;
}

export const WifiIcon: React.FC<WifiIconProps> = ({ 
  className = "", 
  size = 20 
}) => {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      className={`wifi-icon ${className}`}
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
        d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.142 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" 
      />
    </svg>
  );
};