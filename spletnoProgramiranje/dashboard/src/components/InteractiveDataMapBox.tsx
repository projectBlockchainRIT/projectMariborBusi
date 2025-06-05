import React, { useRef, useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import mapboxgl, { Map, NavigationControl, GeolocateControl, FullscreenControl, ScaleControl } from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import type { Station } from '../types/station';
import EventMarker from './EventMarker';

// Set your Mapbox access token
mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

interface MapStyle {
  name: string;
  styleId: string;
  description: string;
}

const MAP_STYLES: MapStyle[] = [
  { name: 'Streets', styleId: 'mapbox://styles/mapbox/streets-v12', description: 'Default streets view' },
  { name: 'Light', styleId: 'mapbox://styles/mapbox/light-v11', description: 'Clean light theme' },
  { name: 'Dark', styleId: 'mapbox://styles/mapbox/dark-v11', description: 'Dark theme' },
  { name: 'Outdoors', styleId: 'mapbox://styles/mapbox/outdoors-v12', description: 'Outdoor/hiking style' },
  { name: 'Satellite', styleId: 'mapbox://styles/mapbox/satellite-v9', description: 'Pure satellite imagery' },
  { name: 'Satellite Streets', styleId: 'mapbox://styles/mapbox/satellite-streets-v12', description: 'Satellite with street labels' },
  { name: 'Navigation Day', styleId: 'mapbox://styles/mapbox/navigation-day-v1', description: 'Optimized for navigation' },
  { name: 'Navigation Night', styleId: 'mapbox://styles/mapbox/navigation-night-v1', description: 'Dark navigation theme' }
];

interface InteractiveDataMapBoxProps {
  onMapLoad?: (map: Map) => void;
  onStationClick?: (station: Station) => void;
}

export default function InteractiveDataMapBox({ onMapLoad, onStationClick }: InteractiveDataMapBoxProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<Map | null>(null);
  const markersRef = useRef<mapboxgl.Marker[]>([]);

  const [lng, setLng] = useState<number>(15.6467);
  const [lat, setLat] = useState<number>(46.5547);
  const [zoom, setZoom] = useState<number>(10);
  const [pitch, setPitch] = useState<number>(0);
  const [bearing, setBearing] = useState<number>(0);
  const [currentStyle, setCurrentStyle] = useState<string>('Streets');
  const [is3DMode, setIs3DMode] = useState<boolean>(false);
  const [terrainEnabled, setTerrainEnabled] = useState<boolean>(false);
  const [elevation, setElevation] = useState<number | null>(null);
  const [isOpen, setIsOpen] = useState<boolean>(true);

  useEffect(() => {
    if (map.current || !mapContainer.current) return;

    try {
      console.log('Initializing map...');
      map.current = new mapboxgl.Map({
        container: mapContainer.current,
        style: MAP_STYLES[0].styleId,
        center: [lng, lat],
        zoom: zoom,
        pitch: pitch,
        bearing: bearing,
        projection: 'globe',
        antialias: true
      });

      // Add controls
      map.current.addControl(new NavigationControl(), 'top-right');
      map.current.addControl(new GeolocateControl({
        positionOptions: {
          enableHighAccuracy: true
        },
        trackUserLocation: true,
        showUserHeading: true
      }), 'top-right');
      map.current.addControl(new FullscreenControl(), 'top-right');
      map.current.addControl(new ScaleControl(), 'bottom-left');

      // Event listeners
      map.current.on('move', () => {
        if (map.current) {
          setLng(parseFloat(map.current.getCenter().lng.toFixed(4)));
          setLat(parseFloat(map.current.getCenter().lat.toFixed(4)));
          setZoom(parseFloat(map.current.getZoom().toFixed(2)));
          setPitch(parseFloat(map.current.getPitch().toFixed(2)));
          setBearing(parseFloat(map.current.getBearing().toFixed(2)));
        }
      });

      map.current.on('click', (e) => {
        if (map.current && terrainEnabled) {
          const elevation = map.current.queryTerrainElevation(e.lngLat, { exaggerated: false });
          if (elevation !== null && elevation !== undefined) {
            setElevation(Math.round(elevation));
          } else {
            setElevation(null);
          }
        }
      });

      map.current.on('style.load', () => {
        if (map.current) {
          map.current.addLayer({
            'id': 'sky',
            'type': 'sky',
            'paint': {
              'sky-type': 'atmosphere',
              'sky-atmosphere-sun': [0.0, 0.0],
              'sky-atmosphere-sun-intensity': 15
            }
          });
        }
      });

      // Call onMapLoad callback when map is loaded
      map.current.on('load', () => {
        console.log('Map loaded successfully');
        if (map.current && onMapLoad) {
          // Add updateMarkers to the map instance
          (map.current as any).updateMarkers = updateMarkers;
          onMapLoad(map.current);
        }
      });

      map.current.on('error', (e) => {
        console.error('Mapbox error:', e);
      });

    } catch (error) {
      console.error('Error initializing map:', error);
    }

    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []);

  // Function to update markers
  const updateMarkers = (stations: Station[]) => {
    try {
      console.log('Updating markers with stations:', stations);
      
      // Validate stations data
      if (!Array.isArray(stations)) {
        console.error('Invalid stations data: not an array', stations);
        return;
      }

      // Remove existing markers
      markersRef.current.forEach(marker => marker.remove());
      markersRef.current = [];

      // Add new markers
      if (map.current) {
        stations.forEach(station => {
          // Validate station data
          if (!station || 
              typeof station.longitude !== 'number' || 
              typeof station.latitude !== 'number' ||
              typeof station.name !== 'string' ||
              typeof station.number !== 'string') {
            console.error('Invalid station data:', station);
            return;
          }

          // Validate coordinates
          if (isNaN(station.longitude) || isNaN(station.latitude)) {
            console.error('Invalid coordinates for station:', station);
            return;
          }

          // Validate coordinate ranges
          if (station.longitude < -180 || station.longitude > 180 || 
              station.latitude < -90 || station.latitude > 90) {
            console.error('Coordinates out of valid range for station:', station);
            return;
          }

          console.log('Creating marker for station:', station);

          // Create a container for the marker
          const el = document.createElement('div');
          el.className = 'station-marker';
          
          // Render the EventMarker component into the container
          const markerElement = document.createElement('div');
          el.appendChild(markerElement);
          
          // Create a React root and render the EventMarker
          const root = createRoot(markerElement);
          root.render(<EventMarker />);

          const marker = new mapboxgl.Marker(el)
            .setLngLat([station.longitude, station.latitude])
            .setPopup(new mapboxgl.Popup({ offset: 25 })
              .setHTML(`
                <h3 class="font-semibold">${station.name}</h3>
                <p class="text-sm text-gray-600">Station #${station.number}</p>
              `));

          if (map.current) {
            marker.addTo(map.current);
            markersRef.current.push(marker);
            console.log('Marker added successfully for station:', station.name);
          }
        });

        // Store markers on the map instance for external access
        (map.current as any).markers = markersRef.current;
        
        console.log('All markers updated successfully');
      }
    } catch (error) {
      console.error('Error updating markers:', error);
    }
  };

  return (
    <div className="relative w-full h-full bg-gray-100 rounded-none overflow-hidden" style={{ minHeight: '600px' }}>
      <div 
        ref={mapContainer} 
        className="absolute inset-0 w-full h-full" 
        style={{ minHeight: '600px', backgroundColor: '#f3f4f6' }}
      />
      
      <div className="absolute top-4 left-[10px] z-10 flex flex-col gap-2">
        <div className="flex gap-2">
          <button
            onClick={() => {
              if (map.current) {
                const newPitch = is3DMode ? 0 : 60;
                const newBearing = is3DMode ? 0 : 20;
                
                map.current.flyTo({
                  pitch: newPitch,
                  bearing: newBearing,
                  duration: 2000,
                  essential: true
                });
                
                setIs3DMode(!is3DMode);
              }
            }}
            className={`px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
              is3DMode 
                ? 'bg-blue-600 hover:bg-blue-700 text-white' 
                : 'bg-gray-800 hover:bg-gray-700 text-gray-200 border border-gray-700'
            }`}
          >
            {is3DMode ? '3D ON' : '3D OFF'}
          </button>
          
          <button
            onClick={() => {
              if (map.current) {
                if (!terrainEnabled) {
                  map.current.addSource('mapbox-dem', {
                    'type': 'raster-dem',
                    'url': 'mapbox://mapbox.terrain-rgb',
                    'tileSize': 512,
                    'maxzoom': 14
                  } as any);
                  
                  map.current.setTerrain({ 'source': 'mapbox-dem', 'exaggeration': 1.5 });
                } else {
                  map.current.setTerrain(null);
                  if (map.current.getSource('mapbox-dem')) {
                    map.current.removeSource('mapbox-dem');
                  }
                }
                setTerrainEnabled(!terrainEnabled);
              }
            }}
            className={`px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
              terrainEnabled 
                ? 'bg-blue-600 hover:bg-blue-700 text-white' 
                : 'bg-gray-800 hover:bg-gray-700 text-gray-200 border border-gray-700'
            }`}
          >
            {terrainEnabled ? 'Terrain ON' : 'Terrain OFF'}
          </button>
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => {
              if (map.current) {
                map.current.flyTo({
                  center: [lng, lat],
                  zoom: 15,
                  pitch: 75,
                  bearing: 0,
                  duration: 3000,
                  essential: true
                });
              }
            }}
            className="bg-gray-800 hover:bg-gray-700 text-gray-200 border border-gray-700 px-3 py-2 text-sm font-medium rounded-lg transition-colors"
          >
            Bird's Eye
          </button>
          
          <button
            onClick={() => {
              if (map.current) {
                map.current.flyTo({
                  center: [15.6467, 46.5547],
                  zoom: 10,
                  pitch: 0,
                  bearing: 0,
                  duration: 2000,
                  essential: true
                });
                setIs3DMode(false);
              }
            }}
            className="bg-gray-800 hover:bg-gray-700 text-gray-200 border border-gray-700 px-3 py-2 text-sm font-medium rounded-lg transition-colors"
          >
            Reset View
          </button>
        </div>

        <div className="relative">
          <select
            value={currentStyle}
            onChange={(e) => {
              const style = MAP_STYLES.find(s => s.name === e.target.value);
              if (style && map.current) {
                map.current.setStyle(style.styleId);
                setCurrentStyle(style.name);
              }
            }}
            className="w-full px-3 py-2 bg-gray-800 text-gray-200 border border-gray-700 rounded-lg appearance-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            {MAP_STYLES.map((style) => (
              <option key={style.name} value={style.name} title={style.description}>
                {style.name}
              </option>
            ))}
          </select>
          <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-gray-400">
            <svg className="fill-current h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20">
              <path d="M9.293 12.95l.707.707L15.657 8l-1.414-1.414L10 10.828 5.757 6.586 4.343 8z"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
  );
} 