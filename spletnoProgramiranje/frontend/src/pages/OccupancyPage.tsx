import React, { useRef, useState } from 'react';
import OccupancyMapBox from '../components/OccupancyMapBox';
import OccupancyController from '../components/OccupancyController';
import type { Route } from '../types';
import mapboxgl from 'mapbox-gl';

export default function OccupancyPage() {
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);

  const handleMapLoad = (map: mapboxgl.Map) => {
    mapRef.current = map;
  };

  const handleRouteSelect = (route: Route) => {
      setSelectedRoute(route);
  };

  return (
      <div className="flex h-full">
      <div className="w-80 border-r border-gray-200">
        <OccupancyController
                onRouteSelect={handleRouteSelect}
          mapInstance={mapRef.current}
              />
            </div>
        <div className="flex-1 relative">
        <OccupancyMapBox 
          onMapLoad={handleMapLoad}
        />
      </div>
    </div>
  );
}