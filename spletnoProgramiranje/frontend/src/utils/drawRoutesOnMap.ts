// utils/map/drawRoutesOnMap.ts
import mapboxgl from 'mapbox-gl';
import type { Route } from '../types';

// Shrani barve po route.id za konsistentnost
const routeColors = new Map<number, string>();
const addedRoutes = new Set<number>();

const getRandomColor = () => {
  const letters = '0123456789ABCDEF';
  return '#' + Array.from({ length: 6 }, () => letters[Math.floor(Math.random() * 16)]).join('');
};

export async function drawRoutesOnMap(
  map: mapboxgl.Map,
  routes: Route[],
  opts?: {
    setStatus?: (msg: string) => void;
    clearPrevious?: boolean;
  }
) {
  const setStatus = opts?.setStatus || (() => {});
  const clearPrevious = opts?.clearPrevious ?? false;

  if (!map || !routes?.length) return;

  if (clearPrevious) {
    // Odstrani prejšnje linije, če je zahtevano
    for (const id of addedRoutes) {
      const sourceId = `route-${id}-source`;
      const layerId = `route-${id}-line`;
      if (map.getLayer(layerId)) map.removeLayer(layerId);
      if (map.getSource(sourceId)) map.removeSource(sourceId);
    }
    addedRoutes.clear();
  }

  const bounds = new mapboxgl.LngLatBounds();
  let addedCount = 0;

  for (const route of routes) {
    if (addedRoutes.has(route.id)) continue;

    try {
      setStatus(`Loading route ${route.id}`);

      const res = await fetch(`http://40.68.198.73:8080/v1/routes/${route.id}`);
      if (!res.ok) throw new Error(`API error ${res.status}`);
      const data = await res.json();
      const routeData = data.data || data;

      let coords: number[][] = [];

      // Poskusi ujeti formate
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

      // Validacija koordinat
      const validCoords = coords.filter(p =>
        Array.isArray(p) && p.length === 2 && !isNaN(p[0]) && !isNaN(p[1])
      );

      if (validCoords.length < 2) {
        console.warn(`Route ${route.id}: Not enough valid coordinates`);
        continue;
      }

      if (!routeColors.has(route.id)) routeColors.set(route.id, getRandomColor());
      const color = routeColors.get(route.id)!;

      const sourceId = `route-${route.id}-source`;
      const layerId = `route-${route.id}-line`;

      if (map.getSource(sourceId)) {
        if (map.getLayer(layerId)) map.removeLayer(layerId);
        map.removeSource(sourceId);
      }

      map.addSource(sourceId, {
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
        id: layerId,
        type: 'line',
        source: sourceId,
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

      map.on('click', layerId, (e) => {
        const name = e.features?.[0]?.properties?.routeName || `ID: ${route.id}`;
        new mapboxgl.Popup()
          .setLngLat(e.lngLat)
          .setHTML(`<strong>Route:</strong> ${name}`)
          .addTo(map);
      });

      map.on('mouseenter', layerId, () => {
        map.getCanvas().style.cursor = 'pointer';
      });

      map.on('mouseleave', layerId, () => {
        map.getCanvas().style.cursor = '';
      });

      validCoords.forEach(coord => bounds.extend(coord as [number, number]));
      addedRoutes.add(route.id);
      addedCount++;
    } catch (err) {
      console.error(`Error rendering route ${route.id}:`, err);
    }
  }

  if (!bounds.isEmpty()) {
    try {
      map.fitBounds(bounds, {
        padding: 50,
        maxZoom: 15
      });
    } catch (e) {
      console.warn('Error fitting bounds', e);
    }
  }

  setStatus(`Displayed ${addedCount} route(s)`);
}
