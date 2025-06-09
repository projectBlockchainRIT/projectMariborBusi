import React, { useState, useCallback, useEffect, useRef } from 'react';
import InteractiveDataMapBox from './InteractiveDataMapBox';
import InteractiveMapControls from './layout/InteractiveMapControls';
import type { Map as MapboxMap } from 'mapbox-gl';
import mapboxgl from 'mapbox-gl';
import type { Station, Route } from '../types/station';
import { drawRoutesOnMap } from '../utils/drawRoutesOnMap';
import { useTheme } from '../context/ThemeContext';

// Function to generate a random color
const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

// Interface for bus location data received from WebSocket
interface BusLocation {
  id: string;
  latitude: number;
  longitude: number;
  routeId: number;
  timestamp: number;
  speed?: number;
  heading?: number;
}

export default function InteractiveMap() {
  const [mapInstance, setMapInstance] = useState<MapboxMap | null>(null);
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [stations, setStations] = useState<Station[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedStation, setSelectedStation] = useState<Station | null>(null);
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
  const [stationMetadata, setStationMetadata] = useState<any>(null);
  const [showStationInfo, setShowStationInfo] = useState(false);
  const routeColorsRef = useRef(new Map<number, string>());
  const { isDarkMode } = useTheme();
  
  // WebSocket and bus tracking
  const webSocketRef = useRef<WebSocket | null>(null);
  const [busLocations, setBusLocations] = useState<BusLocation[]>([]);
  const busMarkersRef = useRef<Map<string, mapboxgl.Marker>>(new Map());
  const [busTrackingActive, setBusTrackingActive] = useState(false);
  
  // Maintain refs for current active route elements for proper cleanup
  const currentRouteLayerId = useRef<string | null>(null);
  const currentRouteSourceId = useRef<string | null>(null);

  // Fetch initial station list
  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();
    const signal = controller.signal;

    const fetchStations = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch('http://40.68.198.73:8080/v1/stations/list', {
          signal,
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          }
        });

        if (!isMounted) return;

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ message: 'Failed to parse error response' }));
          throw new Error(`HTTP error! Status: ${response.status} - ${errorData.message || response.statusText}`);
        }

        const result = await response.json();
        console.log('Raw response data:', result);

        if (!isMounted) return;

        // Handle both array responses and nested data structures
        let stationsData;
        if (Array.isArray(result)) {
          stationsData = result;
        } else if (result && typeof result === 'object') {
          stationsData = result.data || result.stations || [];
        } else {
          stationsData = [];
        }
        console.log('Processed stations data:', stationsData);

        if (!Array.isArray(stationsData)) {
          throw new Error('Invalid response format: stations data is not an array');
        }

        setStations(stationsData);
      } catch (e: unknown) {
        if (!isMounted) return;

        if (e instanceof Error) {
          if (e.name === 'AbortError') {
            console.log('Fetch aborted by cleanup');
          } else {
            setError(`Failed to fetch stations: ${e.message}`);
          }
        } else {
          setError("An unknown error occurred during fetch.");
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    fetchStations();

    return () => {
      isMounted = false;
      controller.abort();
    };
  }, []);

  const handleMapLoad = useCallback((map: MapboxMap) => {
    console.log('Map loaded in InteractiveMap');
    setMapInstance(map);
    setIsMapLoaded(true);
  }, []);

  // Connect to WebSocket for bus tracking when a route is selected
  useEffect(() => {
    if (!selectedRoute || !selectedRoute.id) {
      // Clean up existing WebSocket if there's no selected route
      cleanupBusTracking();
      return;
    }
    
    // Clean up any previous WebSocket connection
    cleanupBusTracking();
    
    // Start new WebSocket connection for the selected route
    const routeId = selectedRoute.id;
    console.log(`Starting bus tracking for route ${routeId}`);
    
    const wsUrl = `ws://40.68.198.73:8080/v1/estimate/simulate/${routeId}`;
    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log(`WebSocket connected for route ${routeId}`);
      setBusTrackingActive(true);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        console.log('Received bus location data:', data);
        
        // Handle different data formats (single bus or array of buses)
        const busData = Array.isArray(data) ? data : [data];
        
        // If no valid data or empty array, just log and return
        if (!data || (Array.isArray(busData) && busData.length === 0)) {
          console.log('Received empty bus data, ignoring update');
          setError('Failed to load busses / no buses active on this route ');

          return;
        }
        
        if (!selectedRoute || !selectedRoute.id) {
          console.warn('No selected route available for bus data');
          setError('Failed to load busses / no buses active on this route ');

          return;
        }
        
        // Update bus locations
        setBusLocations(prevLocations => {
          // Create a map of existing locations by ID for quick lookup
          const locationMap = new Map(prevLocations.map(loc => [loc.id, loc]));
          
          // Update with new locations
          busData.forEach(bus => {
            // Skip if bus object is null or undefined
            if (!bus) {
              console.warn('Received null/undefined bus entry');
              return;
            }
            
            // Map the API's lat/lon properties to our latitude/longitude interface
            const id = bus.departure_id || bus.direction_id || bus.id || Math.random().toString();
            const latitude = bus.lat || bus.latitude;
            const longitude = bus.lon || bus.longitude;
            
            if (id && typeof latitude === 'number' && typeof longitude === 'number') {
              console.log(`Processing bus ${id} at position [${latitude}, ${longitude}]`);
              locationMap.set(id.toString(), {
                id: id.toString(),
                latitude,
                longitude,
                routeId: selectedRoute.id,
                timestamp: bus.timestamp || Date.now(),
                // Optional: add these if available
                speed: bus.speed,
                heading: bus.heading
              });
            } else {
              console.warn('Invalid bus data:', bus);
            }
          });
          
          const updatedLocations = Array.from(locationMap.values());
          console.log(`Updated bus locations (${updatedLocations.length}):`, updatedLocations);
          return updatedLocations;
        });
      } catch (err) {
        console.error('Error processing WebSocket message:', err);
      }
    };
    
    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setError(`Bus tracking error: Connection failed`);
      setBusTrackingActive(false);
    };
    
    ws.onclose = () => {
      console.log('WebSocket connection closed');
      setBusTrackingActive(false);
    };
    
    webSocketRef.current = ws;
    
    return () => {
      cleanupBusTracking();
    };
  }, [selectedRoute]);
  
  // Update bus markers on the map when bus locations change
  useEffect(() => {
    if (!mapInstance || !busLocations.length) return;
    
    console.log(`Updating ${busLocations.length} bus markers on the map`);
    
    // Get the color for the route
    const routeId = busLocations[0]?.routeId;
    let busColor = '#3388FF';
    
    if (routeId && routeColorsRef.current.has(routeId)) {
      busColor = routeColorsRef.current.get(routeId) || busColor;
    }
    
    // Update or create markers for each bus
    busLocations.forEach(bus => {
      const markerId = bus.id;
      
      // Skip invalid locations
      if (typeof bus.latitude !== 'number' || typeof bus.longitude !== 'number' ||
          isNaN(bus.latitude) || isNaN(bus.longitude) ||
          bus.latitude < -90 || bus.latitude > 90 ||
          bus.longitude < -180 || bus.longitude > 180) {
        console.warn(`Invalid bus location for ID ${markerId}:`, bus);
        return;
      }
      
      // Create HTML element for the bus marker
      const createBusMarkerElement = () => {
        const el = document.createElement('div');
        el.className = 'bus-marker';
        el.style.width = '20px';
        el.style.height = '20px';
        el.style.borderRadius = '50%';
        el.style.background = busColor;
        el.style.border = '2px solid white';
        el.style.boxShadow = '0 0 5px rgba(0,0,0,0.5)';
        
        // Add pulse animation
        const pulse = document.createElement('div');
        pulse.className = 'bus-marker-pulse';
        pulse.style.position = 'absolute';
        pulse.style.width = '20px';
        pulse.style.height = '20px';
        pulse.style.borderRadius = '50%';
        pulse.style.backgroundColor = `${busColor}50`; // Semi-transparent
        pulse.style.animation = 'pulse 1.5s infinite';
        el.appendChild(pulse);
        
        // Add ID label
        const label = document.createElement('div');
        label.className = 'bus-marker-label';
        label.textContent = '🚌';
        label.style.position = 'absolute';
        label.style.top = '50%';
        label.style.left = '50%';
        label.style.transform = 'translate(-50%, -50%)';
        label.style.color = 'white';
        label.style.fontSize = '10px';
        label.style.fontWeight = 'bold';
        el.appendChild(label);
        
        return el;
      };
      
      const lngLat: [number, number] = [bus.longitude, bus.latitude];
      
      // Check if marker already exists
      if (busMarkersRef.current.has(markerId)) {
        // Update existing marker position
        busMarkersRef.current.get(markerId)?.setLngLat(lngLat);
      } else {
        // Create new marker
        const marker = new mapboxgl.Marker({
          element: createBusMarkerElement(),
          anchor: 'center'
        })
        .setLngLat(lngLat)
        .setPopup(
          new mapboxgl.Popup({ offset: 25 })
            .setHTML(`<h3>Bus ${markerId}</h3><p>Route: ${routeId || 'Unknown'}</p>`)
        )
        .addTo(mapInstance);
        
        busMarkersRef.current.set(markerId, marker);
      }
    });
    
    // Clean up markers for buses that are no longer in the data
    const activeBusIds = new Set(busLocations.map(bus => bus.id));
    busMarkersRef.current.forEach((marker, markerId) => {
      if (!activeBusIds.has(markerId)) {
        marker.remove();
        busMarkersRef.current.delete(markerId);
      }
    });
  }, [busLocations, mapInstance]);

  

  // Add pulse animation style to document
  useEffect(() => {
    // Add the CSS animation for the pulse effect
    if (!document.getElementById('bus-marker-style')) {
      const style = document.createElement('style');
      style.id = 'bus-marker-style';
      style.textContent = `
        @keyframes pulse {
          0% {
            transform: scale(1);
            opacity: 0.8;
          }
          70% {
            transform: scale(2);
            opacity: 0;
          }
          100% {
            transform: scale(1);
            opacity: 0;
          }
        }
      `;
      document.head.appendChild(style);
    }
    
    // Clean up on unmount
    return () => {
      const styleElement = document.getElementById('bus-marker-style');
      if (styleElement) {
        styleElement.remove();
      }
    };
  }, []);
  
  // Helper function to clean up WebSocket and bus markers
  const cleanupBusTracking = useCallback(() => {
    // Close WebSocket connection
    if (webSocketRef.current) {
      console.log('Closing WebSocket connection');
      webSocketRef.current.close();
      webSocketRef.current = null;
    }
    
    // Clear bus locations
    setBusLocations([]);
    setBusTrackingActive(false);
    
    // Remove markers from map
    if (mapInstance) {
      busMarkersRef.current.forEach(marker => marker.remove());
      busMarkersRef.current.clear();
    }
  }, [mapInstance]);

  // Helper function to clean up previous route layers/sources
  const cleanupPreviousRoute = useCallback((map: MapboxMap) => {
    try {
      // Remove existing layer if it exists
      if (currentRouteLayerId.current && map.getLayer(currentRouteLayerId.current)) {
        map.removeLayer(currentRouteLayerId.current);
      }
      
      // Remove existing source if it exists
      if (currentRouteSourceId.current && map.getSource(currentRouteSourceId.current)) {
        map.removeSource(currentRouteSourceId.current);
      }
      
      // Reset the refs
      currentRouteLayerId.current = null;
      currentRouteSourceId.current = null;
    } catch (err) {
      console.error('Error cleaning up previous route:', err);
    }
  }, []);

  const handleRouteSelect = useCallback(async (routeId: number) => {
    if (!mapInstance) {
      console.log('Map not initialized yet');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      console.log(`Fetching route data for ID: ${routeId}`);
      const response = await fetch(`http://40.68.198.73:8080/v1/routes/${routeId}`, {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch route: ${response.status} ${response.statusText}`);
      }

      const rawData = await response.json();
      console.log('Raw route data received:', rawData);
      
      // Handle nested data structure
      const routeData = rawData.data || rawData;
      console.log('Processed route data:', routeData);
      setSelectedRoute(routeData);

      // Clean up previous route
      cleanupPreviousRoute(mapInstance);
      
      // Generate unique IDs for this route
      const layerId = `route-line-${routeId}`;
      const sourceId = `route-source-${routeId}`;
      
      // Store the new IDs
      currentRouteLayerId.current = layerId;
      currentRouteSourceId.current = sourceId;

      // Get or create a color for this route
      if (!routeColorsRef.current.has(routeId)) {
        routeColorsRef.current.set(routeId, getRandomColor());
      }
      const routeColor = routeColorsRef.current.get(routeId);

      // Process coordinates from different possible formats
      let coordinates = extractCoordinates(routeData);

      // If we found valid coordinates, add them to the map
      if (coordinates && Array.isArray(coordinates) && coordinates.length > 1) {
        console.log(`Adding route with ${coordinates.length} coordinates. First point:`, coordinates[0]);
        
        // Filter out invalid coordinates
        const validCoordinates = coordinates.filter(coord => 
          Array.isArray(coord) && 
          coord.length === 2 && 
          !isNaN(coord[0]) && 
          !isNaN(coord[1]) &&
          // Ensure coordinates are within valid ranges
          coord[0] >= -180 && coord[0] <= 180 &&
          coord[1] >= -90 && coord[1] <= 90
        );

        console.log(`Found ${validCoordinates.length} valid coordinates out of ${coordinates.length}`);

        if (validCoordinates.length < 2) {
          throw new Error('Not enough valid coordinates to display route');
        }

        // Add the route to the map
        mapInstance.addSource(sourceId, {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {
              routeId,
              name: routeData.name || `Route ${routeId}`
            },
            geometry: {
              type: 'LineString',
              coordinates: validCoordinates
            }
          }
        });

        mapInstance.addLayer({
          id: layerId,
          type: 'line',
          source: sourceId,
          layout: {
            'line-join': 'round',
            'line-cap': 'round'
          },
          paint: {
            'line-color': routeColor || '#FF0000',
            'line-width': 4,
            'line-opacity': 0.8
          }
        });

        // Fit the map to show the entire route
        try {
          // Create a proper LngLatBounds with first coordinate
          const firstCoord = validCoordinates[0];
          const bounds = validCoordinates.reduce((bounds: mapboxgl.LngLatBounds, coord: number[]) => {
            return bounds.extend(coord as mapboxgl.LngLatLike);
          }, new mapboxgl.LngLatBounds([firstCoord[0], firstCoord[1]], [firstCoord[0], firstCoord[1]]));

          mapInstance.fitBounds(bounds, {
            padding: 50,
            duration: 2000,
            maxZoom: 15 // Prevent zooming in too far if the route is small
          });
        } catch (error) {
          console.error('Error setting map bounds:', error);
        }
      } else {
        throw new Error('No valid coordinates found in route data');
      }
    } catch (error) {
      console.error('Error handling route selection:', error);
      setError('Failed to load route path: ' + (error instanceof Error ? error.message : 'Unknown error'));
    } finally {
      setLoading(false);
    }
  }, [mapInstance, cleanupPreviousRoute]);

  // Helper function to extract coordinates from different data formats
  function extractCoordinates(routeData: any): number[][] | null {
    // ...existing extractCoordinates function...
    let coordinates = null;
    
    // Case 1: Path array of coordinates
    if (routeData.path && Array.isArray(routeData.path) && routeData.path.length > 0) {
      console.log('Found path property with data');
      const firstPoint = routeData.path[0];
      
      if (Array.isArray(firstPoint) && firstPoint.length === 2) {
        // Check if the first coordinate is likely latitude (Slovenia is around 46°N)
        if (firstPoint[0] > 45 && firstPoint[0] < 47) {
          console.log('Coordinates appear to be in [lat, lng] format, swapping to [lng, lat]');
          coordinates = routeData.path.map((point: number[]) => [point[1], point[0]]);
        } else {
          console.log('Coordinates appear to be already in [lng, lat] format');
          coordinates = routeData.path;
        }
      } else if (typeof firstPoint === 'object' && firstPoint !== null) {
        // Case: Array of objects with lat/lng properties
        if ('lat' in firstPoint && 'lng' in firstPoint) {
          console.log('Converting from {lat,lng} format');
          coordinates = routeData.path.map((point: any) => [point.lng, point.lat]);
        } else if ('latitude' in firstPoint && 'longitude' in firstPoint) {
          console.log('Converting from {latitude,longitude} format');
          coordinates = routeData.path.map((point: any) => [point.longitude, point.latitude]);
        }
      }
    } 
    // Case 2: Direct coordinates array
    else if (routeData.coordinates && Array.isArray(routeData.coordinates)) {
      console.log('Found coordinates property');
      coordinates = routeData.coordinates;
      
      // Check if these also need swapping (if first point looks like latitude)
      if (coordinates.length > 0 && Array.isArray(coordinates[0]) && 
          coordinates[0].length === 2 && coordinates[0][0] > 45 && coordinates[0][0] < 47) {
        console.log('Coordinates in coordinates property need swapping');
        coordinates = coordinates.map((point: number[]) => [point[1], point[0]]);
      }
    } 
    // Case 3: GeoJSON format
    else if (routeData.geometry && routeData.geometry.coordinates && 
             Array.isArray(routeData.geometry.coordinates)) {
      console.log('Found GeoJSON geometry.coordinates');
      coordinates = routeData.geometry.coordinates;
    }
    // Case 4: Try to find any property that might contain an array of points
    else {
      console.log('Trying to find coordinates in any property');
      for (const key in routeData) {
        const value = routeData[key];
        if (Array.isArray(value) && value.length > 1 &&
            Array.isArray(value[0]) && value[0].length === 2 &&
            typeof value[0][0] === 'number' && typeof value[0][1] === 'number') {
          console.log(`Found possible coordinates in ${key} property`);
          coordinates = value;
          
          // Check if these coordinates need swapping
          if (value[0][0] > 45 && value[0][0] < 47) {
            console.log(`Coordinates in ${key} need swapping`);
            coordinates = value.map((point: number[]) => [point[1], point[0]]);
          }
          break;
        }
      }
    }
    
    return coordinates;
  }

  const handleStationClick = useCallback(async (station: Station) => {
    if (!mapInstance) return;

    try {
      // Fetch station metadata
      const metadataResponse = await fetch(`http://40.68.198.73:8080/v1/stations/${station.id}`, {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      let metadata;
      if (metadataResponse.ok) {
        const responseData = await metadataResponse.json();
        console.log("metadata: ", responseData);
        metadata = responseData.data || responseData;
        setStationMetadata(metadata);
      }

      // Try to fetch station location
      const response = await fetch(`http://40.68.198.73:8080/v1/stations/location/${station.id}`, {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      let locationData;
      if (response.ok) {
        const responseData = await response.json();
        console.log('Station location data response:', responseData);
        
        // Handle nested data structure
        locationData = responseData.data || responseData;
        console.log('Processed station location data:', locationData);
      } else {
        console.log('API error. Using original station coordinates');
        locationData = {
          latitude: station.latitude,
          longitude: station.longitude
        };
      }

      // Validate location data
      if (!locationData || 
          typeof locationData.latitude !== 'number' || 
          typeof locationData.longitude !== 'number' ||
          isNaN(locationData.latitude) || 
          isNaN(locationData.longitude) ||
          locationData.latitude < -90 || locationData.latitude > 90 ||
          locationData.longitude < -180 || locationData.longitude > 180) {
        console.log('Invalid location data, using original station coordinates');
        locationData = {
          latitude: station.latitude,
          longitude: station.longitude
        };
      }

      // Update selected station with location data
      const updatedStation = {
        ...station,
        latitude: locationData.latitude,
        longitude: locationData.longitude
      };
      setSelectedStation(updatedStation);
      setShowStationInfo(true);

      // Update markers on the map
      if (mapInstance && typeof (mapInstance as any).updateMarkers === 'function') {
        (mapInstance as any).updateMarkers([updatedStation]);
      } else {
        console.warn('updateMarkers method not found on map instance');
      }

      // Fly to the station location
      mapInstance.flyTo({
        center: [locationData.longitude, locationData.latitude],
        zoom: 15,
        duration: 2000
      });

    } catch (error) {
      console.error('Error handling station click:', error);
      // Use original station coordinates as fallback
      if (station.latitude && station.longitude &&
          !isNaN(station.latitude) && !isNaN(station.longitude) &&
          station.latitude >= -90 && station.latitude <= 90 &&
          station.longitude >= -180 && station.longitude <= 180) {
        
        const updatedStation = {
          ...station,
          latitude: station.latitude,
          longitude: station.longitude
        };
        setSelectedStation(updatedStation);
        setShowStationInfo(true);

        if (mapInstance && typeof (mapInstance as any).updateMarkers === 'function') {
          (mapInstance as any).updateMarkers([updatedStation]);
        }

        mapInstance.flyTo({
          center: [station.longitude, station.latitude],
          zoom: 15,
          duration: 2000
        });
      } else {
        setError('Failed to get valid station location');
      }
    }
  }, [mapInstance]);

  // Cleanup function on unmount
  useEffect(() => {
    return () => {
      // Clean up any remaining listeners or resources
      if (mapInstance && currentRouteLayerId.current && currentRouteSourceId.current) {
        cleanupPreviousRoute(mapInstance);
      }
      
      // Clean up WebSocket and bus markers
      cleanupBusTracking();
    };
  }, [mapInstance, cleanupPreviousRoute, cleanupBusTracking]);

  return (
    <div className="flex h-full min-h-[600px]">
      <InteractiveMapControls
        onRouteSelect={handleRouteSelect}
        onStationSelect={handleStationClick}
      />
      <div className="flex-1 h-full relative">
        <InteractiveDataMapBox
          onMapLoad={handleMapLoad}
          onStationClick={handleStationClick}
        />
        
        {loading && (
          <div className="absolute inset-0 bg-black bg-opacity-30 flex items-center justify-center z-10">
            <div className="bg-white p-3 rounded-md shadow-md">
              <p>Loading data...</p>
            </div>
          </div>
        )}
        
        {error && (
          <div className="absolute top-4 left-0 right-0 mx-auto w-3/4 max-w-md bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded z-10">
            <p>{error}</p>
            <button 
              className="absolute top-0 right-0 p-2"
              onClick={() => setError(null)}
            >
              ×
            </button>
          </div>
        )}
        
        {busTrackingActive && (
          <div className="absolute top-4 right-4 bg-white p-2 rounded shadow-md z-10">
            <div className="flex items-center">
              <div className="w-3 h-3 rounded-full bg-green-500 mr-2 animate-pulse"></div>
              <span className="text-sm font-medium">Live Bus Tracking</span>
            </div>
            <div className="text-xs text-gray-500 mt-1">
              {busLocations.length} buses on route
            </div>
          </div>
        )}

        {/* Station Information Panel */}
        {showStationInfo && selectedStation && (
          <div className={`absolute top-4 right-4 w-96 rounded-lg shadow-lg z-20 border backdrop-blur-sm ${
            isDarkMode 
              ? 'bg-gray-800/90 border-gray-700/50' 
              : 'bg-white/90 border-gray-200/50'
          }`}>
            <div className={`p-4 border-b flex justify-between items-center ${
              isDarkMode ? 'border-gray-700/50' : 'border-gray-200/50'
            }`}>
              <h3 className={`text-lg font-semibold ${
                isDarkMode ? 'text-gray-100' : 'text-gray-900'
              }`}>
                {selectedStation.name}
              </h3>
              <button
                onClick={() => setShowStationInfo(false)}
                className={`p-1 rounded-full hover:bg-opacity-10 transition-colors ${
                  isDarkMode 
                    ? 'text-gray-400 hover:text-gray-200 hover:bg-gray-700' 
                    : 'text-gray-500 hover:text-gray-700 hover:bg-gray-200'
                }`}
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            
            <div className="p-4 max-h-[calc(100vh-200px)] overflow-y-auto">
              <div className="mb-4">
                <p className={`text-sm ${
                  isDarkMode ? 'text-gray-400' : 'text-gray-600'
                }`}>
                  Station #{selectedStation.number}
                </p>
              </div>

              {stationMetadata && stationMetadata.departures && stationMetadata.departures.length > 0 ? (
                <div>
                  <h4 className={`font-medium mb-3 ${
                    isDarkMode ? 'text-gray-100' : 'text-gray-900'
                  }`}>
                    Upcoming Departures
                  </h4>
                  <div className="space-y-4">
                    {stationMetadata.departures.map((departure: any, index: number) => (
                      <div key={index} className={`border-b pb-3 last:border-0 ${
                        isDarkMode ? 'border-gray-700/50' : 'border-gray-200/50'
                      }`}>
                        <div className={`font-medium ${
                          isDarkMode ? 'text-gray-100' : 'text-gray-900'
                        }`}>
                          Line {departure.line} ({departure.direction})
                        </div>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {departure.times.map((time: string, timeIndex: number) => (
                            <span
                              key={timeIndex}
                              className={`px-2 py-1 rounded text-sm ${
                                isDarkMode
                                  ? 'bg-blue-900/50 text-blue-100'
                                  : 'bg-blue-100 text-blue-800'
                              }`}
                            >
                              {time}
                            </span>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div className={`text-sm ${
                  isDarkMode ? 'text-gray-400' : 'text-gray-500'
                }`}>
                  No departure information available
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}