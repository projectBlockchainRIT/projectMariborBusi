import { useState, useCallback } from 'react';
import InteractiveDataMapBox from './InteractiveDataMapBox';
import InteractiveMapControls from './layout/InteractiveMapControls';
import mapboxgl from 'mapbox-gl';

export default function InteractiveMap() {
  const [map, setMap] = useState<mapboxgl.Map | null>(null);

  const handleMapLoad = useCallback((loadedMap: mapboxgl.Map) => {
    setMap(loadedMap);
  }, []);

  return (
    <div className="flex h-full">
      <div className="flex-1 bg-white dark:bg-gray-800 rounded-lg shadow-lg">
        <InteractiveDataMapBox onMapLoad={handleMapLoad} />
      </div>
      <InteractiveMapControls map={map} />
    </div>
  );
} 