import React, { useState, useEffect, useMemo } from 'react';
import { MapPin, Clock, X } from 'lucide-react';
import type { Station } from '../../types/station';
import type { Map } from 'mapbox-gl';
import { useTheme } from '../../context/ThemeContext';

interface InteractiveMapControlsProps {
  stations: Station[];
  onStationClick: (station: Station) => void;
  loading: boolean;
  error: string | null;
}

export default function InteractiveMapControls({ stations, onStationClick, loading, error }: InteractiveMapControlsProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const { isDarkMode } = useTheme();

  const filteredStations = useMemo(() => {
    return stations.filter(station =>
      station.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      station.number.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [stations, searchTerm]);

  return (
    <div className={`h-full w-80 shadow-lg ${
      isDarkMode ? 'bg-gray-800 text-gray-200' : 'bg-white text-gray-700'
    }`}>
      <div className="h-full flex flex-col">
        <div className={`p-4 border-b ${
          isDarkMode ? 'border-gray-700' : 'border-gray-200'
        }`}>
          <h2 className="text-xl font-semibold">Interactive Map</h2>
        </div>

        <div className={`p-4 border-b ${
          isDarkMode ? 'border-gray-700' : 'border-gray-200'
        }`}>
          <input
            type="text"
            placeholder="Search stations..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className={`w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              isDarkMode 
                ? 'bg-gray-700 border-gray-600 text-gray-200 placeholder-gray-400' 
                : 'bg-gray-100 border-gray-200 text-gray-800 placeholder-gray-500'
            }`}
          />
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className={`${isDarkMode ? 'text-gray-400' : 'text-gray-500'}`}>
                Loading stations...
              </div>
            </div>
          ) : error ? (
            <div className="text-red-400 p-4 text-center">
              {error}
            </div>
          ) : (
            <div className="space-y-2">
              {filteredStations.map((station) => (
                <button
                  key={station.id}
                  onClick={() => onStationClick(station)}
                  className={`w-full p-3 text-left rounded-lg transition-colors border ${
                    isDarkMode
                      ? 'bg-gray-700 hover:bg-gray-600 border-gray-600'
                      : 'bg-gray-100 hover:bg-gray-200 border-gray-200'
                  }`}
                >
                  <div className="font-medium">{station.name}</div>
                  <div className={`text-sm ${
                    isDarkMode ? 'text-gray-400' : 'text-gray-500'
                  }`}>Station #{station.number}</div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}