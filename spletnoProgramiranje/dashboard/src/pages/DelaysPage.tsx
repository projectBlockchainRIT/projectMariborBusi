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
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedTimeRange, setSelectedTimeRange] = useState<string>('all');
  const [isDateFilterEnabled, setIsDateFilterEnabled] = useState<boolean>(false);

  const handleMapLoad = (map: mapboxgl.Map) => {
    mapRef.current = map;
  };

  const handleTimeRangeChange = (date: string, isDateFilterEnabled: boolean) => {
    setSelectedDate(date);
    setIsDateFilterEnabled(isDateFilterEnabled);
    // If there's a selected station, update its popup with new delay data
    if (selectedStation) {
      handleStationClick(selectedStation);
    }
  };

  const handleStationClick = async (station: Station) => {
    if (mapRef.current) {
      try {
        const response = await fetch(`http://40.68.198.73:8080/v1/delays/station/${station.id}`);
        if (!response.ok) {
          throw new Error(`Failed to fetch delays: ${response.status} ${response.statusText}`);
        }
        const responseData = await response.json();
        console.log('Received delays data:', responseData);
        
        // Extract the data array from the response
        const data = responseData.data;
        console.log('Extracted data array:', data);

        // Remove previous marker if it exists
        if (currentMarkerRef.current) {
          currentMarkerRef.current.remove();
          currentMarkerRef.current = null;
        }

        // Create popup content
        let popupContent = '';
        
        if (Array.isArray(data) && data.length > 0) {
          // Only filter by date if date filter is enabled
          const filteredData = isDateFilterEnabled 
            ? data.filter(delay => {
                // Convert delay date to DD-MM-YYYY format
                const delayDate = new Date(delay.Date);
                const delayDateStr = `${delayDate.getDate().toString().padStart(2, '0')}-${(delayDate.getMonth() + 1).toString().padStart(2, '0')}-${delayDate.getFullYear()}`;
                
                // Convert selected date from YYYY-MM-DD to DD-MM-YYYY
                const [year, month, day] = selectedDate.split('-');
                const formattedSelectedDate = `${day}-${month}-${year}`;
                
                // Selected date is already in DD-MM-YYYY format
                console.log('DEBUG - Date Comparison:', {
                  originalDelayDate: delay.Date,
                  parsedDelayDate: delayDate,
                  formattedDelayDate: delayDateStr,
                  selectedDate: selectedDate,
                  formattedSelectedDate: formattedSelectedDate,
                  areEqual: delayDateStr === formattedSelectedDate,
                  delayObject: delay
                });
                
                return delayDateStr === formattedSelectedDate;
              })
            : data;

          if (isDateFilterEnabled && filteredData.length === 0) {
            popupContent = `
              <div class="p-2 text-black">
                <p class="text-sm">No delays for ${selectedDate} on this station</p>
              </div>
            `;
          } else {
            popupContent = `
              <div class="p-2 text-black">
                <h3 class="font-semibold mb-2">Delays</h3>
                ${filteredData.map(delay => {
                  const delayDate = new Date(delay.Date).toLocaleDateString();
                  return `
                    <div class="mb-2">
                      <p class="text-sm">
                        <span class="font-medium">Delay:</span> ${delay.DelayMin} minutes
                      </p>
                      <p class="text-sm">
                        <span class="font-medium">Line:</span> ${delay.LineCode}
                      </p>
                      ${!isDateFilterEnabled ? `
                        <p class="text-sm">
                          <span class="font-medium">Date:</span> ${delayDate}
                        </p>
                      ` : ''}
                    </div>
                  `;
                }).join('')}
              </div>
            `;
          }
        } else {
          popupContent = `
            <div class="p-2 text-black">
              <p class="text-sm">No delays available for this station</p>
            </div>
          `;
        }

        // Create a container for the marker
        const el = document.createElement('div');
        el.className = 'station-marker';
        
        // Render the EventMarker component into the container
        const markerElement = document.createElement('div');
        el.appendChild(markerElement);
        
        // Create a React root and render the EventMarker
        const root = createRoot(markerElement);
        root.render(<EventMarker color="#DC2626" size={24} />);

        // Create the marker and add it to the map
        const marker = new mapboxgl.Marker(el)
          .setLngLat([station.longitude, station.latitude])
          .setPopup(new mapboxgl.Popup({ offset: 25 })
            .setHTML(popupContent));

        // Add marker to the map and open popup
        marker.addTo(mapRef.current);
        const popup = marker.getPopup();
        if (popup) {
          popup.addTo(mapRef.current);
        }
        currentMarkerRef.current = marker;
        
        // Fly to the station location
        mapRef.current.flyTo({
          center: [station.longitude, station.latitude],
          zoom: 15,
          duration: 2000
        });

        setSelectedStation(station);
      } catch (error) {
        console.error('Error fetching delays:', error);
      }
    }
  };

  const handleFilterChange = (filters: { [key: string]: boolean }) => {
    console.log('Filters changed:', filters);
  };

  const handleRouteSelect = (route: Route) => {
    // Remove station marker if it exists
    if (currentMarkerRef.current) {
      currentMarkerRef.current.remove();
      currentMarkerRef.current = null;
    }
    setSelectedStation(null);
    
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