import React, { useState, useEffect } from 'react';
import { useTheme } from '../context/ThemeContext';
import {
  ExclamationTriangleIcon,
  ClockIcon,
  AdjustmentsHorizontalIcon,
  InformationCircleIcon,
  MapPinIcon,
  TruckIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ListBulletIcon
} from '@heroicons/react/24/outline';
import type { Route, Station } from '../types';
import { fetchRoutes, fetchStationsForRoute } from '../utils/api';
import EventMarker from './EventMarker';
import { createRoot } from 'react-dom/client';
import mapboxgl from 'mapbox-gl';

interface DelaysControllerProps {
  onTimeRangeChange: (date: string, isDateFilterEnabled: boolean, isLineFilterEnabled: boolean) => void;
  onFilterChange: (filters: { [key: string]: boolean }) => void;
  onRouteSelect: (route: Route) => void;
  onStationSelect: (station: Station) => void;
  mapInstance: mapboxgl.Map | null;
}

export default function DelaysController({
  onTimeRangeChange,
  onFilterChange,
  onRouteSelect,
  onStationSelect,
  mapInstance
}: DelaysControllerProps) {
  const [activeTimeRange, setActiveTimeRange] = useState('realtime');
  const [showFilters, setShowFilters] = useState(false);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [stations, setStations] = useState<Station[]>([]);
  const [expandedRouteId, setExpandedRouteId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [routesLoading, setRoutesLoading] = useState(true);
  const [stationsLoading, setStationsLoading] = useState(false);
  const { isDarkMode } = useTheme();
  const [selectedStation, setSelectedStation] = useState<Station | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });
  const [isDateFilterEnabled, setIsDateFilterEnabled] = useState(false);
  const [isLineFilterEnabled, setIsLineFilterEnabled] = useState(false);

  // Fetch routes on component mount
  useEffect(() => {
    const loadRoutes = async () => {
      try {
        setRoutesLoading(true);
        const response = await fetch('http://40.68.198.73:8080/v1/routes/list');
        if (!response.ok) {
          throw new Error(`Failed to fetch routes: ${response.status} ${response.statusText}`);
        }
        const data = await response.json();
        // Handle both direct array and wrapped response formats
        const routesData = Array.isArray(data) ? data : (data.data || data.routes || []);
        setRoutes(routesData);
      } catch (error) {
        console.error('Error loading routes:', error);
      } finally {
        setRoutesLoading(false);
      }
    };

    loadRoutes();
  }, []);

  const handleTimeRangeChange = (range: string) => {
    setActiveTimeRange(range);
    onTimeRangeChange(selectedDate, isDateFilterEnabled, isLineFilterEnabled);
  };

  const handleRouteClick = async (route: Route) => {
    if (expandedRouteId === route.id) {
      setExpandedRouteId(null);
      setStations([]);
    } else {
      setExpandedRouteId(route.id);
      setStationsLoading(true);
      try {
        const response = await fetch(`http://40.68.198.73:8080/v1/routes/stations/${route.id}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch stations: ${response.status} ${response.statusText}`);
        }
        const data = await response.json();
        // Handle both direct array and wrapped response formats
        const stationsData = Array.isArray(data) ? data : (data.data || data.stations || []);
        setStations(stationsData);
      } catch (error) {
        console.error('Error loading stations:', error);
        setStations([]);
      } finally {
        setStationsLoading(false);
      }
    }
    onRouteSelect(route);
  };

  const handleStationClick = (station: Station) => {
    // Don't add marker if it's the same station
    if (selectedStation?.id === station.id) {
      return;
    }
    setSelectedStation(station);
    onStationSelect(station);
  };

  // Filter routes based on search term
  const filteredRoutes = routes.filter(route =>
    route.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Base classes for light/dark mode
  const containerClasses = `h-full w-80 shadow-lg ${
    isDarkMode ? 'bg-gray-800 text-gray-200' : 'bg-white text-gray-700'
  }`;

  const borderClasses = `transition-colors duration-200 ${
    isDarkMode ? 'border-gray-700' : 'border-gray-200'
  }`;

  const textClasses = `transition-colors duration-200 ${
    isDarkMode ? 'text-gray-300' : 'text-gray-700'
  }`;

  const buttonBaseClasses = `flex items-center px-3 py-2 rounded-md text-sm font-medium transition-all duration-200`;

  const getButtonClasses = (isActive: boolean) => {
    if (isActive) {
      return `${buttonBaseClasses} ${
        isDarkMode ? 'bg-blue-600 text-white' : 'bg-blue-500 text-white'
      }`;
    }
    return `${buttonBaseClasses} ${
      isDarkMode
        ? 'text-gray-300 hover:bg-gray-700'
        : 'text-gray-700 hover:bg-gray-100'
    }`;
  };

  const selectClasses = `w-full px-3 py-2 rounded-md text-sm border transition-colors duration-200 ${
    isDarkMode
      ? 'bg-gray-600 text-white border-gray-500'
      : 'bg-white text-gray-900 border-gray-300'
  } focus:outline-none focus:ring-2 focus:ring-blue-500`;

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newDate = e.target.value;
    setSelectedDate(newDate);
    onTimeRangeChange(newDate, isDateFilterEnabled, isLineFilterEnabled);
  };

  const handleDateFilterToggle = () => {
    const newState = !isDateFilterEnabled;
    setIsDateFilterEnabled(newState);
    onTimeRangeChange(selectedDate, newState, isLineFilterEnabled);
  };

  const handleLineFilterToggle = () => {
    const newState = !isLineFilterEnabled;
    setIsLineFilterEnabled(newState);
    onTimeRangeChange(selectedDate, isDateFilterEnabled, newState);
  };

  return (
    <div className={containerClasses}>
      <div className="h-full flex flex-col">
        <div className={`p-4 border-b ${borderClasses}`}>
          <h2 className="text-xl font-semibold flex items-center gap-2">
            <ExclamationTriangleIcon className="h-5 w-5" />
            Delay Analysis
          </h2>
          <div className="text-sm text-green-500 mt-1">
            Real-time monitoring
          </div>
        </div>

        {/* Date Filter */}
        <div className={`p-4 border-b ${borderClasses}`}>
          <div className="flex items-center justify-between mb-2">
            <label className={`text-sm font-medium ${textClasses}`}>Filter by date</label>
            <button
              onClick={handleDateFilterToggle}
              className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${
                isDateFilterEnabled
                  ? isDarkMode
                    ? 'bg-blue-600 text-white hover:bg-blue-700'
                    : 'bg-blue-500 text-white hover:bg-blue-600'
                  : isDarkMode
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {isDateFilterEnabled ? 'Enabled' : 'Disabled'}
            </button>
          </div>
          {isDateFilterEnabled && (
            <input
              type="date"
              value={selectedDate}
              onChange={handleDateChange}
              className={`w-full px-3 py-2 rounded-md border ${
                isDarkMode
                  ? 'bg-gray-700 border-gray-600 text-white'
                  : 'bg-white border-gray-300 text-gray-900'
              } focus:outline-none focus:ring-2 focus:ring-blue-500`}
            />
          )}
        </div>

        {/* Line Filter */}
        <div className={`p-4 border-b ${borderClasses}`}>
          <div className="flex items-center justify-between">
            <label className={`text-sm font-medium ${textClasses}`}>
              Filter by selected line
            </label>
            <button
              onClick={handleLineFilterToggle}
              className={`px-3 py-1 rounded-md text-sm font-medium transition-colors ${
                isLineFilterEnabled
                  ? isDarkMode
                    ? 'bg-blue-600 text-white'
                    : 'bg-blue-500 text-white'
                  : isDarkMode
                    ? 'bg-gray-700 text-gray-300'
                    : 'bg-gray-200 text-gray-700'
              }`}
            >
              {isLineFilterEnabled ? 'Enabled' : 'Disabled'}
            </button>
          </div>
        </div>

        {/* Route Search */}
        <div className={`p-4 border-b ${borderClasses}`}>
          <input
            type="text"
            placeholder="Search routes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className={`w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
              isDarkMode 
                ? 'bg-gray-700 border-gray-600 text-gray-200 placeholder-gray-400' 
                : 'bg-gray-100 border-gray-200 text-gray-800 placeholder-gray-500'
            }`}
          />
        </div>

        {/* Routes List */}
        <div className={`p-4 border-b ${borderClasses} flex-1 overflow-y-auto`}>
          {routesLoading ? (
            <div className="text-center py-4">Loading routes...</div>
          ) : filteredRoutes.length === 0 ? (
            <div className="text-center py-4">No routes found</div>
          ) : (
            <div className="space-y-2">
              {filteredRoutes.map((route) => (
                <div key={route.id} className="mb-2">
                  <button
                    onClick={() => handleRouteClick(route)}
                    className={`w-full p-3 text-left rounded-lg transition-colors border flex items-center justify-between ${
                      isDarkMode
                        ? 'bg-gray-700 hover:bg-gray-600 border-gray-600'
                        : 'bg-gray-100 hover:bg-gray-200 border-gray-200'
                    }`}
                  >
                    <div className="font-medium flex items-center gap-2">
                      <TruckIcon className="h-4 w-4" />
                      {route.name}
                    </div>
                    {expandedRouteId === route.id ? (
                      <ChevronUpIcon className="h-4 w-4" />
                    ) : (
                      <ChevronDownIcon className="h-4 w-4" />
                    )}
                  </button>
                  {expandedRouteId === route.id && (
                    <div className={`mt-2 ml-4 space-y-2 ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-600'
                    }`}>
                      {stationsLoading ? (
                        <div className="text-sm">Loading stations...</div>
                      ) : (
                        <div className="space-y-1">
                          <div className="flex items-center gap-2 text-sm font-medium mb-2">
                            <ListBulletIcon className="h-4 w-4" />
                            Stations ({stations.length})
                          </div>
                          {stations.map((station, index) => (
                            <button
                              key={station.id}
                              onClick={() => handleStationClick(station)}
                              className={`w-full text-left p-2 rounded-lg transition-colors ${
                                selectedStation?.id === station.id
                                  ? isDarkMode
                                    ? 'bg-blue-900 border border-blue-700'
                                    : 'bg-blue-100 border border-blue-200'
                                  : isDarkMode 
                                    ? 'hover:bg-gray-700' 
                                    : 'hover:bg-gray-100'
                              }`}
                            >
                              <div className="flex items-center gap-2">
                                <MapPinIcon className={`h-3 w-3 ${
                                  selectedStation?.id === station.id
                                    ? 'text-blue-500'
                                    : ''
                                }`} />
                                <div className="flex-1">
                                  <div className="text-sm font-medium">{station.name}</div>
                                  <div className="text-xs opacity-75">#{station.number}</div>
                                </div>
                                <div className="text-xs opacity-50">#{index + 1}</div>
                              </div>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
} 