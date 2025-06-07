import React, { useState, useEffect, useMemo } from 'react';
import { MapPin, Clock, X, ChevronDown, ChevronUp, Bus } from 'lucide-react';
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

export default function InteractiveMapControls({ onRouteSelect, onStationSelect }: InteractiveMapControlsProps) {
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
      try {
        setLoading(true);
        setError(null);
        console.log('Fetching routes...');
        const response = await fetchRoutes();
        console.log('Received routes data:', response);
        
        // More detailed validation of the response
        if (!response) {
          throw new Error('No data received from API');
        }
        
        if (Array.isArray(response)) {
          console.log(`Received ${response.length} routes`);
          // Check if routes have the expected structure
          if (response.length > 0) {
            console.log('First route sample:', response[0]);
          }
          setRoutes(response);
        } else if (typeof response === 'object') {
          // Handle case where API might return {data: [...]} structure
          // Type assertion to help TypeScript understand the structure
          const routesData = response as RoutesResponse;
          
          // Try to extract the routes array from various possible properties
          let dataArray: any[] | undefined;
          
          if (routesData.data && Array.isArray(routesData.data)) {
            dataArray = routesData.data;
          } else if (routesData.routes && Array.isArray(routesData.routes)) {
            dataArray = routesData.routes;
          } else {
            dataArray = Object.values(routesData).find(Array.isArray);
          }
          
          if (dataArray && Array.isArray(dataArray)) {
            console.log(`Extracted ${dataArray.length} routes from object response`);
            setRoutes(dataArray as Route[]);
          } else {
            console.error('Could not extract routes array from response:', routesData);
            setRoutes([]);
          }
        } else {
          console.error('Unexpected data format:', typeof response);
          setRoutes([]);
        }
      } catch (err) {
        console.error('Error in loadRoutes:', err);
        setError('Failed to load routes');
        setRoutes([]);
      } finally {
        setLoading(false);
      }
    };

    loadRoutes();
  }, []);

  // Load stations for selected route
  useEffect(() => {
    if (!expandedRouteId) {
      setStations([]);
      return;
    }

    const loadStations = async () => {
      try {
        setStationsLoading(true);
        setError(null);
        
        console.log('Loading stations for route ID:', expandedRouteId);
        const stationsData = await fetchStationsForRoute(expandedRouteId);
        console.log('Received stations data:', stationsData);
        
        if (Array.isArray(stationsData)) {
          console.log(`Setting ${stationsData.length} stations`);
          setStations(stationsData);
        } else if (stationsData && typeof stationsData === 'object') {
          const stationsArray = (stationsData as any).data || [];
          console.log(`Setting ${stationsArray.length} stations from data property`);
          setStations(Array.isArray(stationsArray) ? stationsArray : []);
        } else {
          console.log('No stations data found');
          setStations([]);
        }
      } catch (err) {
        console.error('Error loading stations:', err);
        setError('Failed to load stations');
        setStations([]);
      } finally {
        setStationsLoading(false);
      }
    };

    loadStations();
  }, [expandedRouteId]);

  const filteredRoutes = useMemo(() => {
    console.log('Current routes state:', routes);
    if (!Array.isArray(routes)) {
      console.error('Routes is not an array:', routes);
      return [];
    }
    
    if (routes.length === 0) {
      console.log('Routes array is empty');
      return [];
    }
    
    // Make sure we have at least one route to check properties
    const firstRoute = routes[0];
    if (!firstRoute) {
      return [];
    }
    
    // Check if routes have name property
    if (typeof firstRoute.name !== 'string') {
      console.error('Route objects missing name property:', firstRoute);
      // Try to find alternative property that might contain the name
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
      // If already expanded, collapse it
      setExpandedRouteId(null);
      setSelectedRoute(null);
      setStations([]);
    } else {
      // If not expanded, expand it and trigger route selection
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
                    <div className="mt-1 ml-2 border-l-2 pl-2 space-y-1">
                      {stationsLoading ? (
                        <div className="p-2 text-sm text-gray-500 flex items-center gap-2">
                          <Clock className="h-4 w-4 animate-spin" />
                          Loading stations...
                        </div>
                      ) : stations.length === 0 ? (
                        <div className="p-2 text-sm text-gray-500">
                          No stations available for this route
                        </div>
                      ) : (
                        stations.map((station, index) => (
                          <button
                            key={station.id || Math.random().toString()}
                            onClick={() => handleStationClick(station)}
                            className={`w-full p-2 text-left rounded-lg transition-colors flex items-center space-x-2 ${
                              isDarkMode
                                ? 'hover:bg-gray-600 text-gray-200'
                                : 'hover:bg-gray-200 text-gray-700'
                            }`}
                          >
                            <div className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-blue-500 text-white text-xs">
                              {index + 1}
                            </div>
                            <div>
                              <div className="font-medium">{station.name}</div>
                              {station.number && (
                                <div className={`text-sm ${
                                  isDarkMode ? 'text-gray-400' : 'text-gray-500'
                                }`}>Station #{station.number}</div>
                              )}
                            </div>
                          </button>
                        ))
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