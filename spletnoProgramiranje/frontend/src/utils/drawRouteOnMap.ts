import mapboxgl from 'mapbox-gl';
import type { Route, Station } from '../types';

// Store colors for consistency
const routeColors = new Map<number, string>();
const addedRoutes = new Set<number>();

// Slovenia's approximate coordinate boundaries
const SLOVENIA_BOUNDS = {
  lng: { min: 13.3, max: 16.6 },
  lat: { min: 45.4, max: 47.0 }
};

// Helper function to check and fix coordinates for Slovenia
const ensureCorrectCoordinates = (coord: number[]): number[] => {
  if (!coord || coord.length !== 2) return coord;
  
  // Check if coordinates are likely reversed based on Slovenia's boundaries
  const [x, y] = coord;
  
  // If first value looks like latitude and second like longitude (reversed)
  if (x >= SLOVENIA_BOUNDS.lat.min && x <= SLOVENIA_BOUNDS.lat.max && 
      y >= SLOVENIA_BOUNDS.lng.min && y <= SLOVENIA_BOUNDS.lng.max) {
    // Swap them to be [longitude, latitude]
    return [y, x];
  }
  
  return coord;
};

const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

export async function drawRouteOnMap(
  map: mapboxgl.Map,
  route: Route,
  opts?: {
    setStatus?: (msg: string) => void;
    clearPrevious?: boolean;
  }
) {
  const setStatus = opts?.setStatus || (() => {});
  const clearPrevious = opts?.clearPrevious ?? true;

  if (!map || !route) return;

  // Get the proper route ID (use line_id if available, fall back to id)
  const routeId = route.line_id !== undefined ? route.line_id : route.id;

  // Clear previous routes and stations if requested
  if (clearPrevious) {
    // Remove all existing route layers and sources
    for (const id of addedRoutes) {
      if (map.getLayer(`route-${id}-line`)) {
        map.removeLayer(`route-${id}-line`);
      }
      if (map.getLayer(`route-${id}-stations`)) {
        map.removeLayer(`route-${id}-stations`);
      }
      if (map.getSource(`route-${id}-source`)) {
        map.removeSource(`route-${id}-source`);
      }
      if (map.getSource(`route-${id}-stations-source`)) {
        map.removeSource(`route-${id}-stations-source`);
      }
    }
    addedRoutes.clear();
  }

  try {
    setStatus(`Loading route ${routeId}`);

    // Fetch route details
    const res = await fetch(`http://40.68.198.73:8080/v1/routes/${routeId}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch route data: ${res.statusText}`);
    }
    const data = await res.json();
    const routeData = data.data || data;

    // Fetch stations for the route - using line_id instead of id
    const stationsRes = await fetch(`http://40.68.198.73:8080/v1/routes/${routeId}/stations`);
    if (!stationsRes.ok) {
      // Try alternative endpoint format if the first one fails
      const alternativeRes = await fetch(`http://40.68.198.73:8080/v1/routes/stations/${routeId}`);
      if (!alternativeRes.ok) {
        throw new Error(`Failed to fetch stations data: ${stationsRes.statusText}`);
      }
      var stationsData = await alternativeRes.json();
    } else {
      var stationsData = await stationsRes.json();
    }
    
    const stations = Array.isArray(stationsData) ? stationsData : (stationsData.data || stationsData.stations || []);

    let coords: number[][] = [];
    const path = routeData.path || routeData.coordinates || routeData.geometry?.coordinates;

    if (Array.isArray(path) && path.length > 0) {
      if (Array.isArray(path[0])) {
        if (typeof path[0][0] === 'number') {
          // Format: [[lng, lat], [lng, lat], ...] or possibly [[lat, lng], [lat, lng], ...]
          coords = path.map(ensureCorrectCoordinates);
        } else if (Array.isArray(path[0][0])) {
          // Format: [[[lng, lat], [lng, lat], ...]] or possibly [[[lat, lng], [lat, lng], ...]]
          coords = path[0].map(ensureCorrectCoordinates);
        }
      } else if (typeof path[0] === 'object' && 'lat' in path[0] && 'lng' in path[0]) {
        // Format: [{lat, lng}, {lat, lng}, ...]
        // This is already correct format naming, but ensure proper ordering
        coords = path.map((p: any) => [p.lng, p.lat]);
      }
    }

    // Validate coordinates
    const validCoords = coords.filter(p =>
      Array.isArray(p) && p.length === 2 && !isNaN(p[0]) && !isNaN(p[1])
    );

    if (validCoords.length < 2) {
      throw new Error('Not enough valid coordinates to draw route');
    }

    // Get or create color for the route
    if (!routeColors.has(routeId)) {
      routeColors.set(routeId, getRandomColor());
    }
    const color = routeColors.get(routeId)!;

    // Add route to map
    const routeSourceId = `route-${routeId}-source`;
    const routeLayerId = `route-${routeId}-line`;

    // Add or update the route source
    if (map.getSource(routeSourceId)) {
      (map.getSource(routeSourceId) as mapboxgl.GeoJSONSource).setData({
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: validCoords
        }
      });
    } else {
      map.addSource(routeSourceId, {
        type: 'geojson',
        data: {
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates: validCoords
          }
        }
      });
    }

    // Add or update the route line layer
    if (!map.getLayer(routeLayerId)) {
      map.addLayer({
        id: routeLayerId,
        type: 'line',
        source: routeSourceId,
        layout: {
          'line-join': 'round',
          'line-cap': 'round'
        },
        paint: {
          'line-color': color,
          'line-width': 5,
          'line-opacity': 0.8
        }
      });
    }

    // Add stations to map
    if (stations && stations.length > 0) {
      console.log(`Adding ${stations.length} stations for route ${routeId}`);
      const stationsSourceId = `route-${routeId}-stations-source`;
      const stationsLayerId = `route-${routeId}-stations`;

      // Create GeoJSON features for stations
      const features = stations.map((station: any) => {
        const lat = station.latitude || station.lat;
        const lng = station.longitude || station.lng;

        if (typeof lat !== 'number' || typeof lng !== 'number') {
          console.warn('Invalid station location:', station);
          return null;
        }
        
        // Ensure coordinates are in correct GeoJSON order [lng, lat]
        const coordinates = ensureCorrectCoordinates([lng, lat]);
        
        return {
          type: 'Feature',
          properties: {
            id: station.id || station.station_id,
            name: station.name || station.station_name,
            code: station.code || station.station_code || '',
            routeId: routeId
          },
          geometry: {
            type: 'Point',
            coordinates: coordinates
          }
        };
      }).filter(Boolean);

      console.log(`Created ${features.length} valid station features`);

      // Add or update the stations source
      if (map.getSource(stationsSourceId)) {
        (map.getSource(stationsSourceId) as mapboxgl.GeoJSONSource).setData({
          type: 'FeatureCollection',
          features: features
        });
      } else {
        map.addSource(stationsSourceId, {
          type: 'geojson',
          data: {
            type: 'FeatureCollection',
            features: features
          }
        });
      }

      // Add or update the stations layer
      if (!map.getLayer(stationsLayerId)) {
        map.addLayer({
          id: stationsLayerId,
          type: 'circle',
          source: stationsSourceId,
          paint: {
            'circle-radius': 6,
            'circle-color': color,
            'circle-stroke-width': 2,
            'circle-stroke-color': '#ffffff',
            'circle-opacity': 0.9
          }
        });

        // Add click handler for stations
        map.on('click', stationsLayerId, (e) => {
          const features = map.queryRenderedFeatures(e.point, { layers: [stationsLayerId] });
          if (features.length > 0) {
            const station = features[0].properties;
            if (station) {
              // Display station information
              new mapboxgl.Popup()
                .setLngLat((features[0].geometry as any).coordinates)
                .setHTML(`<h3>${station.name}</h3><p>Station ID: ${station.id}</p>`)
                .addTo(map);
            }
          }
        });

        // Change cursor on hover
        map.on('mouseenter', stationsLayerId, () => {
          map.getCanvas().style.cursor = 'pointer';
        });
        map.on('mouseleave', stationsLayerId, () => {
          map.getCanvas().style.cursor = '';
        });
      }
    } else {
      console.warn(`No stations found for route ${routeId}`);
    }

    // Track this route as added
    addedRoutes.add(routeId);

    // Fit the map to the route bounds if this is the first route
    if (addedRoutes.size === 1 && validCoords.length > 0) {
      const bounds = validCoords.reduce(
        (bounds, coord) => bounds.extend(coord as [number, number]),
        new mapboxgl.LngLatBounds(validCoords[0] as [number, number], validCoords[0] as [number, number])
      );

      map.fitBounds(bounds, {
        padding: 50,
        maxZoom: 15,
        duration: 500
      });
    }

    setStatus(`Route ${routeId} loaded with ${stations.length} stations`);
    return { routeData, stations };

  } catch (err) {
    console.error('Error drawing route on map:', err);
    setStatus(`Error: ${err instanceof Error ? err.message : String(err)}`);
    return null;
  }
}