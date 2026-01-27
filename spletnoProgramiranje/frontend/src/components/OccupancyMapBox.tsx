import React, { useRef, useEffect, useState } from 'react';
import mapboxgl, { Map, NavigationControl, GeolocateControl, FullscreenControl, ScaleControl } from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import { useTheme } from '../context/ThemeContext';

mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

const MAP_STYLES = [
  { name: 'Streets', styleId: 'mapbox://styles/mapbox/streets-v12', description: 'Default streets view' },
  { name: 'Light', styleId: 'mapbox://styles/mapbox/light-v11', description: 'Clean light theme' },
  { name: 'Dark', styleId: 'mapbox://styles/mapbox/dark-v11', description: 'Dark theme' },
  { name: 'Outdoors', styleId: 'mapbox://styles/mapbox/outdoors-v12', description: 'Outdoor/hiking style' },
  { name: 'Satellite', styleId: 'mapbox://styles/mapbox/satellite-v9', description: 'Pure satellite imagery' },
  { name: 'Satellite Streets', styleId: 'mapbox://styles/mapbox/satellite-streets-v12', description: 'Satellite with street labels' },
  { name: 'Navigation Day', styleId: 'mapbox://styles/mapbox/navigation-day-v1', description: 'Optimized for navigation' },
  { name: 'Navigation Night', styleId: 'mapbox://styles/mapbox/navigation-night-v1', description: 'Dark navigation theme' }
];

interface OccupancyMapBoxProps {
  onMapLoad: (map: mapboxgl.Map) => void;
}

export default function OccupancyMapBox({ onMapLoad }: OccupancyMapBoxProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<mapboxgl.Map | null>(null);

  const [lng, setLng] = useState<number>(15.6467);
  const [lat, setLat] = useState<number>(46.5547);
  const [zoom, setZoom] = useState<number>(10);
  const [pitch, setPitch] = useState<number>(0);
  const [bearing, setBearing] = useState<number>(0);
  const [currentStyle, setCurrentStyle] = useState<string>('Streets');

  const { isDarkMode } = useTheme();

  useEffect(() => {
    if (map.current || !mapContainer.current) return;

    try {
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

      map.current.addControl(new NavigationControl(), 'top-right');
      map.current.addControl(new GeolocateControl({
        positionOptions: { enableHighAccuracy: true },
        trackUserLocation: true,
        showUserHeading: true
      }), 'top-right');
      map.current.addControl(new FullscreenControl(), 'top-right');
      map.current.addControl(new ScaleControl(), 'bottom-left');

      map.current.on('move', () => {
        if (map.current) {
          setLng(parseFloat(map.current.getCenter().lng.toFixed(4)));
          setLat(parseFloat(map.current.getCenter().lat.toFixed(4)));
          setZoom(parseFloat(map.current.getZoom().toFixed(2)));
          setPitch(parseFloat(map.current.getPitch().toFixed(2)));
          setBearing(parseFloat(map.current.getBearing().toFixed(2)));
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

      map.current.on('load', () => {
        if (map.current && onMapLoad) {
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

  return (
    <div className="relative w-full h-full bg-gray-100 rounded-none overflow-hidden" style={{ minHeight: '600px' }}>
      <div 
        ref={mapContainer} 
        className="absolute inset-0 w-full h-full" 
        style={{ minHeight: '600px', backgroundColor: '#f3f4f6' }}
      />
      <div className="absolute top-4 left-[10px] z-10">
        <select
          value={currentStyle}
          onChange={(e) => {
            const style = MAP_STYLES.find(s => s.name === e.target.value);
            if (style && map.current) {
              map.current.setStyle(style.styleId);
              setCurrentStyle(style.name);
            }
          }}
          className={`w-full px-3 py-2 rounded-lg appearance-none focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${isDarkMode ? 'bg-gray-800 text-gray-200 border-gray-700' : 'bg-white text-gray-900 border-gray-200'}`}
        >
          {MAP_STYLES.map((style) => (
            <option key={style.name} value={style.name} title={style.description}>
              {style.name}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
} 