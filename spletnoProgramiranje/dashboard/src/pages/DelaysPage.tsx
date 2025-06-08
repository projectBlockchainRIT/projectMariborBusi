import React, { useRef, useState } from 'react';
import DelaysMapBox from '../components/DelaysMapBox';
import DelaysController from '../components/DelaysController';
import type { Route, Station, Delay } from '../types';
import { drawRouteOnMap } from '../utils/drawRouteOnMap';
import { createRoot } from 'react-dom/client';
import EventMarker from '../components/EventMarker';
import mapboxgl from 'mapbox-gl';

export default function DelaysPage() {
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
  const [selectedStation, setSelectedStation] = useState<Station | null>(null);
  const currentMarkerRef = useRef<mapboxgl.Marker | null>(null);

  const handleMapLoad = (map: mapboxgl.Map) => {
    mapRef.current = map;
  };

  const handleStationClick = async (station: Station) => {
    try {
      console.log('Fetching delays for station:', station);
      const response = await fetch(`http://40.68.198.73:8080/v1/delays/station/${station.id}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch delays: ${response.status} ${response.statusText}`);
      }
      const data = await response.json();
      console.log('Received delays data:', data);
    } catch (error) {
      console.error('Error fetching delays:', error);
    }
  };

  const handleTimeRangeChange = (date: string) => {
    console.log('Time range changed:', date);
  };

  const handleFilterChange = (filters: { [key: string]: boolean }) => {
    console.log('Filters changed:', filters);
  };

  const handleRouteSelect = (route: Route) => {
    setSelectedRoute(route);
    if (mapRef.current) {
      drawRouteOnMap(mapRef.current, route, {
        setStatus: (msg: string) => console.log(msg)
      });
    }
  };

  return (
    <div className="flex h-full">
      <div className="w-80 border-r border-gray-200 dark:border-gray-700">
        <DelaysController
          onTimeRangeChange={handleTimeRangeChange}
          onFilterChange={handleFilterChange}
          onRouteSelect={handleRouteSelect}
          onStationSelect={handleStationClick}
          mapInstance={mapRef.current}
        />
      </div>
      <div className="flex-1 relative">
        <DelaysMapBox 
          onMapLoad={handleMapLoad}
          onStationClick={handleStationClick}
        />
      </div>
    </div>
  );
} 