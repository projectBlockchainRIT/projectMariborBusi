import React, { useState, useEffect } from 'react';
import { useTheme } from '../context/ThemeContext';
import { useUser } from '../context/UserContext';
import {
  ExclamationTriangleIcon,
  ClockIcon,
  AdjustmentsHorizontalIcon,
  InformationCircleIcon,
  MapPinIcon,
  TruckIcon,
  ChevronDownIcon,
  ChevronUpIcon,
  ListBulletIcon,
  BellAlertIcon, // Added for delay reporting
  XMarkIcon // For closing the modal
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

// Interface for delay report data
interface DelayReport {
  date: string;
  delayTime: number;
  stopId: number | string;
  lineId: number | string;
  userId: number | string;
}

export default function DelaysController({
  onTimeRangeChange,
  onFilterChange,
  onRouteSelect,
  onStationSelect,
  mapInstance
}: DelaysControllerProps) {
  // Get the user context
  const { user, isAuthenticated } = useUser();
  
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
  
  // Delay reporting state
  const [isDelayModalOpen, setIsDelayModalOpen] = useState(false);
  const [delayStation, setDelayStation] = useState<Station | null>(null);
  const [delayTime, setDelayTime] = useState(5); // Default 5 minutes
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [delayFeedback, setDelayFeedback] = useState<{type: 'success' | 'error'; message: string} | null>(null);

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

  // Reset delay feedback message after 5 seconds
  useEffect(() => {
    if (delayFeedback) {
      const timer = setTimeout(() => {
        setDelayFeedback(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [delayFeedback]);

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
        const response = await fetch(`http://40.68.198.73:8080/v1/routes/stations/${route.line_id}`);
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

  const openDelayReportModal = (e: React.MouseEvent, station: Station) => {
    // Prevent the click from bubbling up to the station button
    e.stopPropagation();
    setDelayStation(station);
    setIsDelayModalOpen(true);
  };

  const closeDelayReportModal = () => {
    setIsDelayModalOpen(false);
    setDelayStation(null);
    setDelayTime(5); // Reset to default
    setDelayFeedback(null);
  };

  const handleDelayTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setDelayTime(Number(e.target.value));
  };

  const submitDelayReport = async () => {
    if (!delayStation || !expandedRouteId) return;
    
    try {
      setIsSubmitting(true);
      
      // Check if user is authenticated using context
      if (!isAuthenticated) {
        setDelayFeedback({
          type: 'error',
          message: 'You must be logged in to report delays'
        });
        return;
      }
      
      // Check if we have user data and ID from the context
      /*if (!user || !user.id) {
        setDelayFeedback({
          type: 'error',
          message: 'User information not found. Please log in again.'
        });
        return;
      }*/
      
      // Get auth token from localStorage
      const authToken = localStorage.getItem('authToken');
      if (!authToken) {
        setDelayFeedback({
          type: 'error',
          message: 'Authentication token missing. Please log in again.'
        });
        return;
      }
      
      // Prepare the delay report data with userId from context
      const delayReport: DelayReport = {
        date: new Date().toISOString(),
        delayTime: delayTime,
        stopId: delayStation.id,
        lineId: expandedRouteId,
        userId: 1// Use the userId from context
      };
      
      console.log('Submitting delay report:', delayReport);
      
      const corsProxyUrl = 'https://cors-anywhere.herokuapp.com/';
      const apiUrl = 'http://40.68.198.73:8080/v1/delays/report';
      
      const response = await fetch(corsProxyUrl + apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
          'X-Requested-With': 'XMLHttpRequest' // Required by some CORS proxies
        },
        body: JSON.stringify(delayReport)
      });
      
      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        throw new Error(errorData?.message || `Request failed with status ${response.status}`);
      }
      
      // Handle success
      setDelayFeedback({
        type: 'success',
        message: `Thank you! Delay of ${delayTime} minutes reported for ${delayStation.name}.`
      });
      
      // Close modal after a short delay
      setTimeout(() => {
        closeDelayReportModal();
      }, 2000);
      
    } catch (error) {
      console.error('Error submitting delay report:', error);
      setDelayFeedback({
        type: 'error',
        message: error instanceof Error ? error.message : 'Failed to submit delay report'
      });
    } finally {
      setIsSubmitting(false);
    }
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
                            <div key={station.id} className="relative group">
                              <button
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
                              
                              {/* Delay report button */}
                              <button 
                                onClick={(e) => openDelayReportModal(e, station)}
                                className={`absolute right-2 top-2 p-1 rounded-full transition-opacity opacity-0 group-hover:opacity-100 ${
                                  isDarkMode 
                                    ? 'bg-yellow-600 hover:bg-yellow-700' 
                                    : 'bg-yellow-500 hover:bg-yellow-600'
                                } text-white`}
                                title="Report delay at this station"
                              >
                                <BellAlertIcon className="h-3 w-3" />
                              </button>
                            </div>
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

      {/* Delay Report Modal */}
      {isDelayModalOpen && delayStation && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center">
          <div className={`p-6 rounded-lg shadow-xl max-w-md w-full mx-4 ${
            isDarkMode ? 'bg-gray-800' : 'bg-white'
          }`}>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold text-yellow-500 flex items-center gap-2">
                <BellAlertIcon className="h-5 w-5" />
                Report Delay
              </h3>
              <button 
                onClick={closeDelayReportModal}
                className="p-1 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700"
              >
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
            
            <div className="mb-4">
              <p className={`mb-1 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                Station: <span className="font-medium">{delayStation.name}</span>
              </p>
              <p className={`mb-3 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                Line: <span className="font-medium">
                  {routes.find(r => r.id === expandedRouteId)?.name || `Line ${expandedRouteId}`}
                </span>
              </p>
              
              <div className="mb-4">
                <label className={`block mb-2 text-sm font-medium ${isDarkMode ? 'text-gray-200' : 'text-gray-700'}`}>
                  Delay time (in minutes):
                </label>
                <div className="flex items-center gap-4">
                  <input
                    type="range"
                    min="1"
                    max="60"
                    value={delayTime}
                    onChange={handleDelayTimeChange}
                    className={`flex-1 h-2 rounded-lg appearance-none cursor-pointer ${
                      isDarkMode ? 'bg-gray-700' : 'bg-gray-200'
                    }`}
                  />
                  <div className={`w-12 text-center font-bold ${
                    delayTime > 15 ? 'text-red-500' : delayTime > 5 ? 'text-yellow-500' : 'text-green-500'
                  }`}>
                    {delayTime}
                  </div>
                </div>
              </div>
              
              {delayFeedback && (
                <div className={`p-3 rounded-md mb-4 ${
                  delayFeedback.type === 'success' 
                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                    : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                }`}>
                  {delayFeedback.message}
                </div>
              )}
            </div>
            
            <div className="flex justify-end gap-3">
              <button
                onClick={closeDelayReportModal}
                className={`px-4 py-2 rounded-md ${
                  isDarkMode 
                    ? 'bg-gray-700 hover:bg-gray-600 text-gray-200' 
                    : 'bg-gray-200 hover:bg-gray-300 text-gray-800'
                }`}
              >
                Cancel
              </button>
              <button
                onClick={submitDelayReport}
                disabled={isSubmitting}
                className={`px-4 py-2 rounded-md flex items-center gap-1 ${
                  isSubmitting
                    ? isDarkMode
                      ? 'bg-yellow-700 text-gray-300'
                      : 'bg-yellow-400 text-gray-700'
                    : isDarkMode
                      ? 'bg-yellow-600 hover:bg-yellow-700 text-white'
                      : 'bg-yellow-500 hover:bg-yellow-600 text-white'
                }`}
              >
                {isSubmitting ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Submitting...
                  </>
                ) : (
                  <>
                    <BellAlertIcon className="h-4 w-4" />
                    Report Delay
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}