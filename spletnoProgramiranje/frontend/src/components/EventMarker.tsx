import React from 'react';
import { MapPin } from 'lucide-react';

interface EventMarkerProps {
  color?: string;
  size?: number;
}

export default function EventMarker({ color = '#DC2626', size = 20 }: EventMarkerProps) {
  return (
    <div className="relative">
      <MapPin
        size={size}
        color={color}
        fill={color}
        className="drop-shadow-lg"
        style={{
          filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.2))',
          transform: 'scale(0.8)'
        }}
      />
    </div>
  );
} 