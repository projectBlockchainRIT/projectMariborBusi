import mapboxgl from 'mapbox-gl';
import type { Route, Station } from '../types';

// Store colors for consistency
const routeColors = new Map<number, string>();
const addedRoutes = new Set<number>();

const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  return '#' + Array.from({ length: 6 }, () => letters[Math.floor(Math.random() * 16)]).join('');
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

  // Clear previous routes and stations if requested
  if (clearPrevious) {
    // Remove all existing route layers and sources
    for (const id of addedRoutes) {
      const routeSourceId = `route-${id}-source`;
      const routeLayerId = `route-${id}-line`;
      const stationsSourceId = `stations-${id}-source`;
      const stationsLayerId = `stations-${id}-points`;

      if (map.getLayer(routeLayerId)) map.removeLayer(routeLayerId);
      if (map.getSource(routeSourceId)) map.removeSource(routeSourceId);
      if (map.getLayer(stationsLayerId)) map.removeLayer(stationsLayerId);
      if (map.getSource(stationsSourceId)) map.removeSource(stationsSourceId);
    }
    addedRoutes.clear();
  }

  try {
    setStatus(`Loading route ${route.id}`);

    // Fetch route details
    const res = await fetch(`http://40.68.198.73:8080/v1/routes/${route.id}`);
    if (!res.ok) throw new Error(`API error ${res.status}`);
    const data = await res.json();
    const routeData = data.data || data;

    // Fetch stations for the route
    const stationsRes = await fetch(`http://40.68.198.73:8080/v1/routes/stations/${route.id}`);
    if (!stationsRes.ok) throw new Error(`API error ${stationsRes.status}`);
    const stationsData = await stationsRes.json();
    const stations = Array.isArray(stationsData) ? stationsData : (stationsData.data || stationsData.stations || []);

    let coords: number[][] = [];
    const path = routeData.path || routeData.coordinates || routeData.geometry?.coordinates;

    if (Array.isArray(path) && path.length > 0) {
      const first = path[0];
      if (Array.isArray(first)) {
        coords = (first[0] > 45 && first[0] < 47) // lat/lng -> lng/lat
          ? path.map(([lat, lng]: number[]) => [lng, lat])
          : path;
      } else if (typeof first === 'object') {
        if ('lat' in first && 'lng' in first) {
          coords = path.map((p: any) => [p.lng, p.lat]);
        } else if ('latitude' in first && 'longitude' in first) {
          coords = path.map((p: any) => [p.longitude, p.latitude]);
        }
      }
    }

    // Validate coordinates
    const validCoords = coords.filter(p =>
      Array.isArray(p) && p.length === 2 && !isNaN(p[0]) && !isNaN(p[1])
    );

    if (validCoords.length < 2) {
      throw new Error(`Route ${route.id}: Not enough valid coordinates`);
    }

    // Get or create color for the route
    if (!routeColors.has(route.id)) routeColors.set(route.id, getRandomColor());
    const color = routeColors.get(route.id)!;

    // Add route to map
    const routeSourceId = `route-${route.id}-source`;
    const routeLayerId = `route-${route.id}-line`;

    if (map.getSource(routeSourceId)) {
      if (map.getLayer(routeLayerId)) map.removeLayer(routeLayerId);
      map.removeSource(routeSourceId);
    }

    map.addSource(routeSourceId, {
      type: 'geojson',
      data: {
        type: 'Feature',
        properties: {
          routeId: route.id,
          routeName: route.name
        },
        geometry: {
          type: 'LineString',
          coordinates: validCoords
        }
      }
    });

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
        'line-width': 4,
        'line-opacity': 0.8
      }
    });

    // Add stations to map
    const stationsSourceId = `stations-${route.id}-source`;
    const stationsLayerId = `stations-${route.id}-points`;

    if (map.getSource(stationsSourceId)) {
      if (map.getLayer(stationsLayerId)) map.removeLayer(stationsLayerId);
      map.removeSource(stationsSourceId);
    }

    map.addSource(stationsSourceId, {
      type: 'geojson',
      data: {
        type: 'FeatureCollection',
        features: stations.map((station: Station) => ({
          type: 'Feature',
          properties: {
            id: station.id,
            name: station.name,
            number: station.number
          },
          geometry: {
            type: 'Point',
            coordinates: [station.longitude, station.latitude]
          }
        }))
      }
    });

    map.addLayer({
      id: stationsLayerId,
      type: 'circle',
      source: stationsSourceId,
      paint: {
        'circle-radius': 6,
        'circle-color': color,
        'circle-stroke-width': 2,
        'circle-stroke-color': '#ffffff'
      }
    });

    // Add click handlers for route
    map.on('click', routeLayerId, (e) => {
      const name = e.features?.[0]?.properties?.routeName || `ID: ${route.id}`;
      new mapboxgl.Popup()
        .setLngLat(e.lngLat)
        .setHTML(`<strong>Route:</strong> ${name}`)
        .addTo(map);
    });

    // Add click handlers for stations
    map.on('click', stationsLayerId, (e) => {
      if (e.features?.[0]) {
        const props = e.features[0].properties as { name: string; number: string };
        new mapboxgl.Popup()
          .setLngLat(e.lngLat)
          .setHTML(`<strong>Station:</strong> ${props.name}<br><strong>Number:</strong> ${props.number}`)
          .addTo(map);
      }
    });

    // Add hover effects
    map.on('mouseenter', routeLayerId, () => {
      map.getCanvas().style.cursor = 'pointer';
    });

    map.on('mouseleave', routeLayerId, () => {
      map.getCanvas().style.cursor = '';
    });

    map.on('mouseenter', stationsLayerId, () => {
      map.getCanvas().style.cursor = 'pointer';
    });

    map.on('mouseleave', stationsLayerId, () => {
      map.getCanvas().style.cursor = '';
    });

    // Fit map to show the entire route
    const bounds = new mapboxgl.LngLatBounds();
    validCoords.forEach(coord => bounds.extend(coord as [number, number]));
    stations.forEach((station: Station) => bounds.extend([station.longitude, station.latitude]));

    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, {
        padding: 50,
        maxZoom: 15
      });
    }

    addedRoutes.add(route.id);
    setStatus(`Displayed route ${route.name} with ${stations.length} stations`);
  } catch (err) {
    console.error(`Error rendering route ${route.id}:`, err);
    setStatus(`Error: ${err instanceof Error ? err.message : 'Unknown error'}`);
  }
} 