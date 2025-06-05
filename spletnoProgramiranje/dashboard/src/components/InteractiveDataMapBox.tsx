import { useEffect, useRef } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';

// Set your Mapbox access token
mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

interface InteractiveDataMapBoxProps {
  onMapLoad?: (map: mapboxgl.Map) => void;
}

export default function InteractiveDataMapBox({ onMapLoad }: InteractiveDataMapBoxProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<mapboxgl.Map | null>(null);
  const onMapLoadRef = useRef(onMapLoad);

  // Update the ref when onMapLoad changes
  useEffect(() => {
    onMapLoadRef.current = onMapLoad;
  }, [onMapLoad]);

  useEffect(() => {
    if (!mapContainer.current || map.current) return;

    // Initialize map
    map.current = new mapboxgl.Map({
      container: mapContainer.current,
      style: 'mapbox://styles/mapbox/streets-v12',
      center: [15.645, 46.554], // Maribor coordinates
      zoom: 12
    });

    // Add navigation controls
    map.current.addControl(new mapboxgl.NavigationControl());

    // Call onMapLoad callback when map is loaded
    map.current.on('load', () => {
      if (map.current && onMapLoadRef.current) {
        onMapLoadRef.current(map.current);
      }
    });

    // Cleanup on unmount
    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []); // Empty dependency array since we're using refs

  return (
    <div className="w-full h-full">
      <div ref={mapContainer} className="w-full h-full" />
    </div>
  );
} 