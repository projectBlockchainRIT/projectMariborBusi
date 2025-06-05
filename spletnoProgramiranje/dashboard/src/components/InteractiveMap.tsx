import React, { useState, useCallback, useEffect } from 'react';
import InteractiveDataMapBox from './InteractiveDataMapBox';
import InteractiveMapControls from './layout/InteractiveMapControls';
import type { Map } from 'mapbox-gl';
import type { Station } from '../types/station';

export default function InteractiveMap() {
  const [mapInstance, setMapInstance] = useState<Map | null>(null);
  const [isMapLoaded, setIsMapLoaded] = useState(false);
  const [stations, setStations] = useState<Station[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const controller = new AbortController();
    const signal = controller.signal;

    const fetchStations = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch('http://localhost:8080/v1/stations/list', {
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

        if (mapInstance && (mapInstance as any).updateMarkers) {
          console.log('Map and updateMarkers function are available, calling updateMarkers...');
          (mapInstance as any).updateMarkers(stationsData);
        }
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
  }, [mapInstance]);

  const handleMapLoad = useCallback((map: Map) => {
    console.log('Map loaded in InteractiveMap');
    setMapInstance(map);
    setIsMapLoaded(true);
  }, []);

  const handleStationClick = useCallback((station: Station) => {
    if (mapInstance) {
      mapInstance.flyTo({
        center: [station.longitude, station.latitude],
        zoom: 15,
        duration: 2000
      });
    }
  }, [mapInstance]);

  return (
    <div className="flex h-full min-h-[600px]">
      <InteractiveMapControls
        stations={stations}
        onStationClick={handleStationClick}
        loading={loading}
        error={error}
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