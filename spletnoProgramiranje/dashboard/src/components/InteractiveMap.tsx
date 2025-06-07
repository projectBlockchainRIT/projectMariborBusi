import React, { useState, useCallback, useEffect } from 'react';
import InteractiveDataMapBox from './InteractiveDataMapBox';
import InteractiveMapControls from './layout/InteractiveMapControls';
import type { Map as MapboxMap } from 'mapbox-gl';
import mapboxgl from 'mapbox-gl';
import type { Station, Route } from '../types/station';

// Function to generate a random color
const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

export default function InteractiveMap() {
  const [mapInstance, setMapInstance] = useState<MapboxMap | null>(null);
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [stations, setStations] = useState<Station[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedStation, setSelectedStation] = useState<Station | null>(null);
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
  const [routeColors] = useState<Map<number, string>>(new Map());

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

        const stationsData = Array.isArray(result) ? result : result.data;
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

  const handleRouteSelect = useCallback(async (routeId: number) => {
  if (!mapInstance) return;

  try {
    setLoading(true);
    
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
    
    const routeData = rawData.data || rawData;
    console.log('Processed route data:', routeData);
    setSelectedRoute(routeData);

    if (mapInstance.getLayer('route-line')) {
      mapInstance.removeLayer('route-line');
    }
    if (mapInstance.getSource('route')) {
      mapInstance.removeSource('route');
    }

    if (!routeColors.has(routeId)) {
      routeColors.set(routeId, getRandomColor());
    }
    const routeColor = routeColors.get(routeId);

    let coordinates = null;
    
    if (routeData.path && Array.isArray(routeData.path) && routeData.path.length > 0) {
      console.log('Found path property with data');
      const firstPoint = routeData.path[0];
      
      // Slovenia is around latitude 46°N and longitude 15°E
      // If our first coordinate has values like [46.xx, 15.xx], we need to swap them
      // because Mapbox expects [longitude, latitude] format
      if (Array.isArray(firstPoint) && firstPoint.length === 2) {
        // Check if the first value looks like Slovenia's latitude (around 46°)
        if (firstPoint[0] > 45 && firstPoint[0] < 47) {
          console.log('Coordinates are in [lat, lng] format, swapping to [lng, lat]');
          coordinates = routeData.path.map(point => [point[1], point[0]]);
        } else {
          coordinates = routeData.path;
        }
      } else if (typeof firstPoint === 'object') {
        if ('lat' in firstPoint && 'lng' in firstPoint) {
          coordinates = routeData.path.map(point => [point.lng, point.lat]);
        } else if ('latitude' in firstPoint && 'longitude' in firstPoint) {
          coordinates = routeData.path.map(point => [point.longitude, point.latitude]);
        }
      }
    } else if (routeData.coordinates) {
      coordinates = routeData.coordinates;
      // Check if these also need swapping
      if (coordinates.length > 0 && Array.isArray(coordinates[0]) && 
          coordinates[0].length === 2 && coordinates[0][0] > 45 && coordinates[0][0] < 47) {
        coordinates = coordinates.map(point => [point[1], point[0]]);
      }
    } else if (routeData.geometry && routeData.geometry.coordinates) {
      coordinates = routeData.geometry.coordinates;
    }

    // If we found valid coordinates, add them to the map
    if (coordinates && Array.isArray(coordinates) && coordinates.length > 1) {
      console.log('Adding route with coordinates. First point:', coordinates[0]);
      
      const validCoordinates = coordinates.filter(coord => 
        Array.isArray(coord) && coord.length === 2 && 
        !isNaN(coord[0]) && !isNaN(coord[1])
      );

      if (validCoordinates.length < 2) {
        throw new Error('Not enough valid coordinates to display route');
      }

      // Add the route to the map
      mapInstance.addSource('route', {
        type: 'geojson',
        data: {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: validCoordinates
          }
        }
      });

      mapInstance.addLayer({
        id: 'route-line',
        type: 'line',
        source: 'route',
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

      try {
        const bounds = validCoordinates.reduce((bounds: mapboxgl.LngLatBounds, coord: number[]) => {
          return bounds.extend(coord as mapboxgl.LngLatLike);
        }, new mapboxgl.LngLatBounds(validCoordinates[0], validCoordinates[0]));

        mapInstance.fitBounds(bounds, {
          padding: 50,
          duration: 2000
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
}, [mapInstance, routeColors]);

  const handleStationClick = useCallback(async (station: Station) => {
    if (!mapInstance) return;

    try {
      // Try to fetch station location
      const response = await fetch(`http://40.68.198.73:8080/v1/stations/location/${station.id}`, {
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      let locationData;
      if (response.ok) {
        locationData = await response.json();
        console.log('Station location data:', locationData);
      } else {
        console.log('Using original station coordinates');
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
          isNaN(locationData.longitude)) {
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

      // Update markers on the map
      if (mapInstance && (mapInstance as any).updateMarkers) {
        (mapInstance as any).updateMarkers([updatedStation]);
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
      if (station.latitude && station.longitude) {
        const updatedStation = {
          ...station,
          latitude: station.latitude,
          longitude: station.longitude
        };
        setSelectedStation(updatedStation);

        if (mapInstance && (mapInstance as any).updateMarkers) {
          (mapInstance as any).updateMarkers([updatedStation]);
        }

        mapInstance.flyTo({
          center: [station.longitude, station.latitude],
          zoom: 15,
          duration: 2000
        });
      } else {
        setError('Failed to get station location');
      }
    }
  }, [mapInstance]);

  return (
    <div className="flex h-full min-h-[600px]">
      <InteractiveMapControls
        onRouteSelect={handleRouteSelect}
        onStationSelect={handleStationClick}
      />
      <div className="flex-1 h-full">
        <InteractiveDataMapBox
          onMapLoad={handleMapLoad}
          onStationClick={handleStationClick}
        />
      </div>
    </div>
  );
}