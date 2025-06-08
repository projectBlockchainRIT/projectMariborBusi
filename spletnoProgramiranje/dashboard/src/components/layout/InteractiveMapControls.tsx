import React, { useState, useEffect, useMemo } from 'react';
import { MapPin, X, ChevronDown, ChevronUp, Bus } from 'lucide-react';
import type { Route, Station } from '../../types';
import { fetchRoutes, fetchStationsForRoute } from '../../utils/api';
import { useTheme } from '../../context/ThemeContext';

interface InteractiveMapControlsProps {
  onRouteSelect: (routeId: number) => void;
  onStationSelect: (station: Station) => void;
}

interface RoutesResponse {
  data?: Route[];
  routes?: Route[];
  [key: string]: any;
}

interface StationsResponse {
  data?: Station[];
  stations?: Station[];
  [key: string]: any;
}

export default function InteractiveMapControls({ 
  onRouteSelect, 
  onStationSelect
}: InteractiveMapControlsProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [routes, setRoutes] = useState<Route[]>([]);
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
  const [stations, setStations] = useState<Station[]>([]);
  const [loading, setLoading] = useState(false);
  const [stationsLoading, setStationsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedRouteId, setExpandedRouteId] = useState<number | null>(null);
  const { isDarkMode } = useTheme();

  // Load routes on component mount
  useEffect(() => {
    const loadRoutes = async () => {
      setLoading(true);
      try {
        const response = await fetchRoutes();
        console.log('Received routes data:', response);
        
        // Handle both array responses and nested data structures
        let routesData: Route[];
        if (Array.isArray(response)) {
          routesData = response;
        } else if (response && typeof response === 'object') {
          const typedResponse = response as RoutesResponse;
          routesData = typedResponse.data || typedResponse.routes || [];
        } else {
          routesData = [];
        }
        
        console.log('Processed routes data:', routesData);
        setRoutes(routesData);
      } catch (err) {
        setError('Failed to load routes');
        console.error('Error loading routes:', err);
      } finally {
        setLoading(false);
      }
    };

    loadRoutes();
  }, []);

  // Load stations when a route is selected
  useEffect(() => {
    if (!expandedRouteId) {
      setStations([]);
      return;
    }

    const loadStations = async () => {
      setStationsLoading(true);
      try {
        console.log('Loading stations for route ID:', expandedRouteId);
        const response = await fetchStationsForRoute(expandedRouteId);
        console.log('Received stations data:', response);
        
        // Handle both array responses and nested data structures
        let stationsData: Station[];
        if (Array.isArray(response)) {
          stationsData = response;
        } else if (response && typeof response === 'object') {
          const typedResponse = response as StationsResponse;
          stationsData = typedResponse.data || typedResponse.stations || [];
        } else {
          stationsData = [];
        }
        
        console.log('Processed stations data:', stationsData);
        setStations(stationsData);
      } catch (err) {
        setError('Failed to load stations');
        console.error('Error loading stations:', err);
      } finally {
        setStationsLoading(false);
      }
    };

    loadStations();
  }, [expandedRouteId]);

  const filteredRoutes = useMemo(() => {
    if (!Array.isArray(routes)) {
      console.error('Routes is not an array:', routes);
      return [];
    }
    
    if (routes.length === 0) {
      console.log('Routes array is empty');
      return [];
    }
    
    const firstRoute = routes[0];
    if (!firstRoute) {
      return [];
    }
    
    if (typeof firstRoute.name !== 'string') {
      console.error('Route objects missing name property:', firstRoute);
      const nameKey = Object.keys(firstRoute).find(key => 
        typeof firstRoute[key as keyof typeof firstRoute] === 'string' && 
        (firstRoute[key as keyof typeof firstRoute] as string).length > 0
      );
      
      if (nameKey) {
        console.log(`Using "${nameKey}" property as name`);
        return routes.filter(route => {
          const value = route[nameKey as keyof typeof route];
          return typeof value === 'string' && value.toLowerCase().includes(searchTerm.toLowerCase());
        });
      }
      
      return routes;
    }
    
    return routes.filter(route =>
      route.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [routes, searchTerm]);

  const handleRouteClick = (route: Route) => {
    console.log('Route clicked:', route.id);
    
    if (expandedRouteId === route.id) {
      setExpandedRouteId(null);
      setSelectedRoute(null);
      setStations([]);
    } else {
      setExpandedRouteId(route.id);
      setSelectedRoute(route);
      onRouteSelect(route.id);
    }
  };

  const handleStationClick = (station: Station) => {
    console.log('Station clicked:', station);
    onStationSelect(station);
  };

  return (
    <div className={`h-full w-80 shadow-lg ${
      isDarkMode ? 'bg-gray-800 text-gray-200' : 'bg-white text-gray-700'
    }`}>
      <div className="h-full flex flex-col">
        <div className={`p-4 border-b ${
          isDarkMode ? 'border-gray-700' : 'border-gray-200'
        }`}>
          <h2 className="text-xl font-semibold flex items-center gap-2">
            <Bus className="h-5 w-5" />
            Bus Routes
          </h2>
          {routes.length > 0 && (
            <div className="text-sm text-green-500 mt-1">
              {routes.length} routes available
            </div>
          )}
        </div>

        <div className={`p-4 border-b ${
          isDarkMode ? 'border-gray-700' : 'border-gray-200'
        }`}>
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

        <div className="flex-1 overflow-y-auto p-4">
          {loading ? (
            <div className="flex items-center justify-center h-full">
              <div className={`${isDarkMode ? 'text-gray-400' : 'text-gray-500'}`}>
                Loading routes...
              </div>
            </div>
          ) : error ? (
            <div className="text-red-400 p-4 text-center">
              {error}
            </div>
          ) : routes.length === 0 ? (
            <div className="text-center text-gray-500 dark:text-gray-400 p-4">
              No routes available
            </div>
          ) : filteredRoutes.length === 0 ? (
            <div className="text-center text-gray-500 dark:text-gray-400 p-4">
              No routes match your search
            </div>
          ) : (
            <div className="space-y-2">
              {filteredRoutes.map((route) => (
                <div key={route.id || Math.random().toString()} className="mb-2">
                  <button
                    onClick={() => handleRouteClick(route)}
                    className={`w-full p-3 text-left rounded-lg transition-colors border flex items-center justify-between ${
                      expandedRouteId === route.id
                        ? isDarkMode 
                          ? 'bg-blue-900 border-blue-700' 
                          : 'bg-blue-100 border-blue-200'
                        : isDarkMode
                          ? 'bg-gray-700 hover:bg-gray-600 border-gray-600'
                          : 'bg-gray-100 hover:bg-gray-200 border-gray-200'
                    }`}
                  >
                    <div className="font-medium flex items-center gap-2">
                      <Bus className="h-4 w-4" />
                      {route.name}
                    </div>
                    {expandedRouteId === route.id ? (
                      <ChevronUp className="h-5 w-5" />
                    ) : (
                      <ChevronDown className="h-5 w-5" />
                    )}
                  </button>
                  
                  {expandedRouteId === route.id && (
                    <div className={`mt-2 ml-4 space-y-2 ${
                      isDarkMode ? 'text-gray-300' : 'text-gray-600'
                    }`}>
                      {stationsLoading ? (
                        <div className="text-sm">Loading stations...</div>
                      ) : stations.length > 0 ? (
                        stations.map((station) => (
                          <button
                            key={station.id}
                            onClick={() => handleStationClick(station)}
                            className={`w-full text-left p-2 rounded-lg hover:bg-opacity-50 transition-colors ${
                              isDarkMode 
                                ? 'hover:bg-gray-700' 
                                : 'hover:bg-gray-100'
                            }`}
                          >
                            <div className="flex items-center gap-2">
                              <MapPin className="h-3 w-3" />
                              <span className="text-sm">{station.name}</span>
                            </div>
                          </button>
                        ))
                      ) : (
                        <div className="text-sm">No stations available</div>
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