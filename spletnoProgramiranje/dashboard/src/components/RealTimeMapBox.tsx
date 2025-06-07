import { useEffect, useRef, useState, useCallback } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import { fetchRoutes } from '../utils/api';
import type { Route } from '../types';

// Set your Mapbox access token
mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

// Helper function to generate random colors
const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

// Type for API response
interface RoutesResponse {
  data?: Route[];
  routes?: Route[];
  [key: string]: any;
}

export default function RealTimeMapBox() {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<mapboxgl.Map | null>(null);
  const mapLoaded = useRef<boolean>(false);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState('Initializing...');
  
  // Store route colors to keep them consistent
  const routeColors = useRef(new Map<number, string>());
  
  // Track added routes to avoid duplicates
  const addedRoutes = useRef(new Set<number>());

  // Add a single route to the map
  const addRouteToMap = useCallback(async (route: Route, mapInstance: mapboxgl.Map, bounds: mapboxgl.LngLatBounds) => {
    if (addedRoutes.current.has(route.id)) {
      console.log(`Route ${route.id} already added, skipping`);
      return;
    }
    
    try {
      setStatus(`Loading route: ${route.name || route.id}`);
      
      const response = await fetch(`http://40.68.198.73:8080/v1/routes/${route.id}`, {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch route ${route.id}: ${response.status}`);
      }

      const rawData = await response.json();
      console.log(`Raw data for route ${route.id}:`, rawData);
      
      const routeData = rawData.data || rawData;
      let coordinates = null;
      
      // Extract coordinates from the route data
      if (routeData.path && Array.isArray(routeData.path) && routeData.path.length > 0) {
        console.log(`Found path for route ${route.id} with ${routeData.path.length} points`);
        const firstPoint = routeData.path[0];
        
        // Check format of coordinates
        if (Array.isArray(firstPoint) && firstPoint.length === 2) {
          // Check if the coordinates need to be swapped (latitude, longitude) -> (longitude, latitude)
          // Slovenia is around latitude 46°N and longitude 15°E
          if (firstPoint[0] > 45 && firstPoint[0] < 47) {
            console.log(`Route ${route.id}: Swapping coordinates from [lat,lng] to [lng,lat]`);
            coordinates = routeData.path.map((point: number[]) => [point[1], point[0]]);
            console.log('First few coordinates after swap:', coordinates.slice(0, 3));
          } else {
            coordinates = routeData.path;
            console.log('First few coordinates (no swap needed):', coordinates.slice(0, 3));
          }
        } else if (typeof firstPoint === 'object') {
          // Handle format with lat/lng properties
          if ('lat' in firstPoint && 'lng' in firstPoint) {
            coordinates = routeData.path.map((point: any) => [point.lng, point.lat]);
          } else if ('latitude' in firstPoint && 'longitude' in firstPoint) {
            coordinates = routeData.path.map((point: any) => [point.longitude, point.latitude]);
          }
        }
      } else if (routeData.coordinates) {
        coordinates = routeData.coordinates;
        // Check if coordinates need swapping
        if (coordinates.length > 0 && Array.isArray(coordinates[0]) && 
            coordinates[0].length === 2 && coordinates[0][0] > 45 && coordinates[0][0] < 47) {
          coordinates = coordinates.map((point: number[]) => [point[1], point[0]]);
        }
      } else if (routeData.geometry && routeData.geometry.coordinates) {
        coordinates = routeData.geometry.coordinates;
      }
      
      // If we found coordinates, add the route to the map
      if (coordinates && Array.isArray(coordinates) && coordinates.length > 1) {
        // Filter out invalid coordinates
        const validCoordinates = coordinates.filter((coord: any) => 
          Array.isArray(coord) && coord.length === 2 && 
          !isNaN(coord[0]) && !isNaN(coord[1])
        );
        
        console.log(`Route ${route.id}: Found ${validCoordinates.length} valid coordinates`);
        
        if (validCoordinates.length < 2) {
          console.warn(`Route ${route.id}: Not enough valid coordinates`);
          return;
        }
        
        // Get or generate color for this route
        if (!routeColors.current.has(route.id)) {
          routeColors.current.set(route.id, getRandomColor());
        }
        const routeColor = routeColors.current.get(route.id);
        
        // Unique source and layer IDs for this route
        const sourceId = `route-${route.id}-source`;
        const layerId = `route-${route.id}-line`;
        
        // Check if source already exists and remove it if it does
        if (mapInstance.getSource(sourceId)) {
          if (mapInstance.getLayer(layerId)) {
            mapInstance.removeLayer(layerId);
          }
          mapInstance.removeSource(sourceId);
        }
        
        // Add source and layer
        mapInstance.addSource(sourceId, {
          type: 'geojson',
          data: {
            type: 'Feature',
            properties: {
              routeId: route.id,
              routeName: route.name
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
        
        // Add click event for the route line
        mapInstance.on('click', layerId, (e) => {
          const routeId = e.features?.[0]?.properties?.routeId;
          const routeName = e.features?.[0]?.properties?.routeName;
          if (routeId) {
            new mapboxgl.Popup()
              .setLngLat(e.lngLat)
              .setHTML(`<strong>Route:</strong> ${routeName || routeId}`)
              .addTo(mapInstance);
          }
        });
        
        // Change cursor when hovering over route
        mapInstance.on('mouseenter', layerId, () => {
          mapInstance.getCanvas().style.cursor = 'pointer';
        });
        
        mapInstance.on('mouseleave', layerId, () => {
          mapInstance.getCanvas().style.cursor = '';
        });
        
        // Extend map bounds to include this route
        validCoordinates.forEach((coord: number[]) => {
          bounds.extend(coord as [number, number]);
        });
        
        addedRoutes.current.add(route.id);
        console.log(`Successfully added route ${route.id} to map`);
        return true; // Successfully added
      } else {
        console.warn(`Route ${route.id}: No valid coordinates found`);
        return false;
      }
    } catch (err) {
      console.error(`Error adding route ${route.id} to map:`, err);
      return false;
    }
  }, []);

  // Initialize map
  useEffect(() => {
    if (!mapContainer.current) return;

    console.log('Initializing map...');
    setStatus('Initializing map...');

    map.current = new mapboxgl.Map({
      container: mapContainer.current,
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [15.645, 46.554], // Maribor coordinates
      zoom: 12
    });

    // Add navigation controls
    map.current.addControl(new mapboxgl.NavigationControl());

    // Wait for map to load before adding routes
    map.current.on('load', () => {
      console.log('✅ Map loaded and ready');
      setStatus('Map loaded successfully');
      mapLoaded.current = true;
    });
    
    // Additional event handlers for debugging
    map.current.on('error', (e) => {
      console.error('Mapbox error:', e);
      setError(`Map error: ${e.error?.message || 'Unknown error'}`);
    });

    // Cleanup on unmount
    return () => {
      console.log('Cleaning up map');
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
      mapLoaded.current = false;
      addedRoutes.current.clear();
    };
  }, []);

  // Fetch routes
  useEffect(() => {
    const loadRoutes = async () => {
      try {
        setLoading(true);
        setError(null);
        setStatus('Fetching routes...');
        console.log('Fetching routes for map...');
        
        const response = await fetchRoutes();
        console.log('Received routes data:', response);
        
        if (!response) {
          throw new Error('No data received from API');
        }
        
        let routesData: Route[] = [];
        
        if (Array.isArray(response)) {
          routesData = response;
        } else if (typeof response === 'object') {
          // Handle case where API might return {data: [...]} structure
          const responseData = response as RoutesResponse;
          
          if (responseData.data && Array.isArray(responseData.data)) {
            routesData = responseData.data;
          } else if (responseData.routes && Array.isArray(responseData.routes)) {
            routesData = responseData.routes;
          } else {
            const dataArray = Object.values(responseData).find(Array.isArray);
            if (dataArray) routesData = dataArray as Route[];
          }
        }
        
        console.log(`Processed ${routesData.length} routes`);
        setStatus(`Found ${routesData.length} routes`);
        setRoutes(routesData);
      } catch (err) {
        console.error('Error loading routes:', err);
        const errorMessage = err instanceof Error ? err.message : 'Failed to load routes';
        setError(errorMessage);
        setStatus(`Error: ${errorMessage}`);
      } finally {
        setLoading(false);
      }
    };
    
    loadRoutes();
  }, []);

  // Add routes to map when both map is loaded and routes are available
  useEffect(() => {
    const addRoutesToMap = async () => {
      // Wait for a few seconds before adding routes to reduce server load
      if (!mapLoaded.current) {
        console.log('Waiting a few seconds before adding routes to map...');
        const timer = setTimeout(() => {
          addRoutesToMap();
        }, 500);
        return () => clearTimeout(timer);
      }
      if (!map.current || !mapLoaded.current || !routes.length) {
        console.log('Not ready to add routes yet:', { 
          mapExists: !!map.current,
          mapLoaded: mapLoaded.current,
          routesCount: routes.length
        });
        return;
      }
      
      setStatus('Adding routes to map...');
      console.log(`Ready to add ${routes.length} routes to map`);
      const mapInstance = map.current;
      
      // Create a bounds object to fit all routes
      const bounds = new mapboxgl.LngLatBounds();
      let boundsSet = false;
      
      // Process routes sequentially to avoid overwhelming the API
      const processRoutes = async () => {
        let successCount = 0;
        
        for (const route of routes) {
          if (await addRouteToMap(route, mapInstance, bounds)) {
            successCount++;
          }
        }
        
        // Once all routes are processed, fit the map to the bounds
        if (successCount > 0) {
          console.log(`Successfully added ${successCount} routes, fitting map to bounds`);
          boundsSet = true;
          try {
            // Only fit bounds if we have at least 2 points
            if (!bounds.isEmpty()) {
              mapInstance.fitBounds(bounds, {
                padding: 50,
                maxZoom: 15
              });
              console.log('✅ Fitted map to all routes');
            } else {
              console.log('Bounds are empty, not fitting map');
            }
          } catch (e) {
            console.error('Error fitting bounds:', e);
          }
        } else {
          console.warn('No routes were successfully added to the map');
        }
        
        setStatus(successCount > 0 ? `Showing ${successCount} routes` : 'No routes to display');
      };
      
      await processRoutes();
    };
    
    addRoutesToMap();
  }, [routes, addRouteToMap]);

  return (
    <div className="w-full h-full relative">
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-30 z-10">
          <div className="bg-white p-4 rounded-lg shadow-lg">
            <p className="text-center font-medium">Loading routes...</p>
          </div>
        </div>
      )}
      
      {error && (
        <div className="absolute top-4 left-4 right-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded z-10">
          <strong className="font-bold">Error:</strong>
          <span className="block sm:inline"> {error}</span>
        </div>
      )}
      
      <div ref={mapContainer} className="w-full h-full" />
    </div>
  );
}