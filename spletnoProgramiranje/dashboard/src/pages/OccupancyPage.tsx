import { useState, useEffect, useRef, useCallback } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import InteractiveMapControls from '../components/layout/InteractiveMapControls';
import { useTheme } from '../context/ThemeContext';
import type { Station } from '../types';

// MapBox access token - should be in environment variables in production
mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

interface Route {
  id: string | number;
  name: string;
  color?: string;
  path?: [number, number][];
}

interface DateTimeRange {
  startDate: string;
  startTime: string;
  endDate: string;
  endTime: string;
}

// Add interface for occupancy data
interface OccupancyData {
  hour: string;
  density: number;
  OccupancyLevel?: number; // For API response compatibility
}

interface RouteOccupancy {
  lineNumber: string;
  lineId: number;
  data: OccupancyData[];
}

export default function OccupancyPage() {
  const mapContainer = useRef<HTMLDivElement | null>(null);
  const map = useRef<mapboxgl.Map | null>(null);
  const [lng, setLng] = useState(15.6467); // Maribor longitude
  const [lat, setLat] = useState(46.5547); // Maribor latitude
  const [zoom, setZoom] = useState(13);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { isDarkMode } = useTheme();
  const [selectedRoute, setSelectedRoute] = useState<Route | null>(null);
  const [routes, setRoutes] = useState<Route[]>([]);
  const [routeStops, setRouteStops] = useState<Station[]>([]);
  const [occupancyData, setOccupancyData] = useState<RouteOccupancy | null>(null);
  const [selectedHour, setSelectedHour] = useState<string | null>(null);
  
  // Date and time range state
  const [dateTimeRange, setDateTimeRange] = useState<DateTimeRange>({
    startDate: new Date().toISOString().split('T')[0],
    startTime: '00:00',
    endDate: new Date().toISOString().split('T')[0],
    endTime: '23:59'
  });

  // Function to determine color based on density levels from 1-5
  const getDensityColor = (density: number | string) => {
    // Convert to number if it's a string
    const densityNum = typeof density === 'string' ? Number(density) : density;
    
    if (densityNum >= 5) return '#FF4136'; // Red for very high occupancy (level 5)
    if (densityNum >= 4) return '#FF851B'; // Orange for high occupancy (level 4)
    if (densityNum >= 3) return '#FFDC00'; // Yellow for medium occupancy (level 3)
    if (densityNum >= 2) return '#ADFF2F'; // GreenYellow for low occupancy (level 2)
    return '#2ECC40'; // Green for very low occupancy (level 1)
  };

  // Initialize map when component mounts
  useEffect(() => {
    if (map.current) return; // Initialize map only once
    
    if (mapContainer.current) {
      map.current = new mapboxgl.Map({
        container: mapContainer.current,
        style: isDarkMode 
          ? 'mapbox://styles/mapbox/dark-v11'
          : 'mapbox://styles/mapbox/streets-v12',
        center: [lng, lat],
        zoom: zoom
      });
      
      // Add navigation controls
      map.current.addControl(new mapboxgl.NavigationControl(), 'top-right');
      
      // Save map position on move
      map.current.on('move', () => {
        if (map.current) {
          const center = map.current.getCenter();
          setLng(Number(center.lng.toFixed(4)));
          setLat(Number(center.lat.toFixed(4)));
          setZoom(Number(map.current.getZoom().toFixed(2)));
        }
      });
      
      // Add load event
      map.current.on('load', () => {
        console.log('Map loaded');
      });
    }
    
    // Cleanup on unmount
    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, [isDarkMode]);
  
  // Update map style when dark mode changes
  useEffect(() => {
    if (map.current) {
      map.current.setStyle(
        isDarkMode 
          ? 'mapbox://styles/mapbox/dark-v11'
          : 'mapbox://styles/mapbox/streets-v12'
      );
    }
  }, [isDarkMode]);

  // New function to draw route with occupancy-based coloring
  const drawRouteWithOccupancy = async (
    map: mapboxgl.Map,
    routeId: number,
    routeName: string,
    occupancyData: OccupancyData[] = [],
    selectedHour: string | null = null
  ) => {
    if (!map || !map.loaded()) {
      console.error("Map not initialized");
      return;
    }
    console.log(`Drawing route ${routeId} with occupancy data for hour: ${selectedHour}`);
    console.log(occupancyData)

    // Use fixed IDs instead of route-specific IDs
    const routeSourceId = `route-source`;
    const routeLayerId = `route-line`;
    const stationsSourceId = `stations-source`;
    const stationsLayerId = `stations-points`;

    try {
      // Clear previous layers and sources if they exist
      for (const layerId of [routeLayerId, stationsLayerId]) {
        if (map.getLayer(layerId)) {
          map.removeLayer(layerId);
        }
      }
      
      for (const sourceId of [routeSourceId, stationsSourceId]) {
        if (map.getSource(sourceId)) {
          map.removeSource(sourceId);
        }
      }
      
      
      // Fetch route geometry from API
      const geometryResponse = await fetch(
        `http://40.68.198.73:8080/v1/routes/${routeId}`,
        { headers: { 'Accept': 'application/json' } }
      );

      if (!geometryResponse.ok) {
        throw new Error(`Failed to fetch route geometry: ${geometryResponse.status}`);
      }

      const geometryData = await geometryResponse.json();
      const routeData = geometryData.data || geometryData;

      // Convert route path into GeoJSON LineString
      // The API returns coordinates as [lat, lng] but GeoJSON needs [lng, lat]
      const path = routeData.path || [];
      const geoJsonData = {
        type: "LineString",
        coordinates: path.map(coord => 
          // Swap lat/lng to lng/lat for GeoJSON format
          [coord[1], coord[0]]
        )
      };

      // Get color based on occupancy
      let routeColor = '#3388ff'; // Default blue color

      if (occupancyData && occupancyData.length > 0 && selectedHour) {
        // Debug the selected hour data
        console.log("Looking for hour:", selectedHour);
        const hourData = occupancyData.find(d => d.hour === selectedHour);
        console.log("Found hour data:", hourData);
        
        if (hourData) {
          // Get the correct density value - prioritize OccupancyLevel if it exists
          const densityValue = Number(occupancyData[0]);
          console.log("Density value:", densityValue, "type:", typeof densityValue);
          
          // Force a specific color based on the density to debug
          routeColor = getDensityColor(densityValue);
          console.log("Route color calculated:", routeColor);
        }
      }

      // Add route to map with explicit color
      map.addSource(routeSourceId, {
        type: 'geojson',
        data: geoJsonData
      });
      
      // Force the line color to be the calculated value
      console.log("Setting route color to:", routeColor);
      map.addLayer({
        id: routeLayerId,
        type: 'line',
        source: routeSourceId,
        layout: {
          'line-join': 'round',
          'line-cap': 'round'
        },
        paint: {
          'line-color': routeColor,
          'line-width': 6,
          'line-opacity': 0.8
        }
      });
      
      // Fetch and add stations
      const stationsResponse = await fetch(
        `http://40.68.198.73:8080/v1/routes/stations/${routeId}`,
        { headers: { 'Accept': 'application/json' } }
      );
      
      let stations = [];
      if (stationsResponse.ok) {
        const stationsData = await stationsResponse.json();
        stations = stationsData.data || [];
        
        if (stations.length > 0) {
          map.addSource(stationsSourceId, {
            type: 'geojson',
            data: {
              type: 'FeatureCollection',
              features: stations
                .filter((station: Station) => station.latitude && station.longitude)
                .map((station: Station) => ({
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
              'circle-color': routeColor,
              'circle-stroke-width': 2,
              'circle-stroke-color': '#ffffff'
            }
          });
          
          // Add click handlers for stations
          map.on('click', stationsLayerId, (e) => {
            if (e.features && e.features[0]) {
              const props = e.features[0].properties as { name: string; number: string };
              new mapboxgl.Popup()
                .setLngLat(e.lngLat)
                .setHTML(`<strong>Station:</strong> ${props.name}<br><strong>Number:</strong> ${props.number}`)
                .addTo(map);
            }
          });
          
          map.on('mouseenter', stationsLayerId, () => {
            map.getCanvas().style.cursor = 'pointer';
          });
          
          map.on('mouseleave', stationsLayerId, () => {
            map.getCanvas().style.cursor = '';
          });
        }
      }
      
      // Fit map to show the entire route
      // Fix: Use geoJsonData instead of undefined geometry variable
      if (geoJsonData && geoJsonData.coordinates && geoJsonData.coordinates.length > 0) {
        const bounds = new mapboxgl.LngLatBounds();
        geoJsonData.coordinates.forEach((coord: [number, number]) => {
          bounds.extend(coord);
        });
        
        map.fitBounds(bounds, {
          padding: 80,
          maxZoom: 15
        });
      }
      
      return stations;
    } catch (error) {
      console.error('Error drawing route:', error);
      throw error;
    }
  };

  // Function to handle route selection
  const handleRouteSelect = useCallback(async (routeId: number) => {
    setLoading(true);
    setError(null);
    
    console.log(`Route selected: ${routeId}`);
    
    try {
      if (!map.current || !map.current.loaded()) {
        throw new Error('Map not initialized');
      }
      
      // Step 1: Get route details
      const routeResponse = await fetch(`http://40.68.198.73:8080/v1/routes/${routeId}`, {
        headers: { 'Accept': 'application/json' }
      });
      
      if (!routeResponse.ok) {
        throw new Error(`Failed to fetch route: ${routeResponse.status}`);
      }
      
      const routeData = await routeResponse.json();
      const route = routeData.data || routeData;
      setSelectedRoute(route);
      
      // Step 2: Get occupancy data
      const currentDate = dateTimeRange.startDate;
      
      const occupancyResponse = await fetch(
        `http://40.68.198.73:8080/v1/occupancy/line/${routeId}/date/${currentDate}`, 
        { headers: { 'Accept': 'application/json' } }
      );
      
      let fetchedOccupancyData = null;
      let selectedHourValue = null;
      
      if (occupancyResponse.ok) {
        fetchedOccupancyData = await occupancyResponse.json();
        console.log("Raw occupancy data:", fetchedOccupancyData);
        
        // Make sure we have data before trying to process it
        if (fetchedOccupancyData && fetchedOccupancyData.data && 
            Array.isArray(fetchedOccupancyData.data) && fetchedOccupancyData.data.length > 0) {
          
          // Process each item to ensure density is available
          fetchedOccupancyData.data = fetchedOccupancyData.data.map(item => {
            // Make sure we use OccupancyLevel for coloring
            return {
              ...item,
              hour: item.Time,
              OccupancyLevel: Number(item.OccupancyLevel || 0),
              density: Number(item.OccupancyLevel || item.density || 0)
            };
          });
          
          console.log("Processed occupancy data:", fetchedOccupancyData.data);
          setOccupancyData(fetchedOccupancyData);
          
          // Set selected hour to the first hour in the data
          selectedHourValue = fetchedOccupancyData.data[0].hour;
          setSelectedHour(selectedHourValue);
          
          console.log("Selected hour:", selectedHourValue);
          console.log("Example density for first hour:", 
            fetchedOccupancyData.data[0].density,
            "Color would be:", getDensityColor(fetchedOccupancyData.data[0].density));
        } else {
          console.error("No occupancy data found or data is not in expected format");
          setOccupancyData(null);
          setSelectedHour(null);
        }
      }

      // Step 3: Draw the route using our new function
      const stations = await drawRouteWithOccupancy(
        map.current,
        routeId,
        route.name || `Route ${routeId}`,
        fetchedOccupancyData?.data || [],
        selectedHourValue
      );
      
      // Step 4: Set route stops from the stations returned by drawRouteWithOccupancy
      if (stations) {
        setRouteStops(stations);
      }
    } catch (err) {
      console.error('Error handling route selection:', err);
      setError(err instanceof Error ? err.message : 'Failed to load route data');
    } finally {
      setLoading(false);
    }
  }, [map, dateTimeRange.startDate]);

  // Handle station selection (fly to station)
  const handleStationSelect = (station: Station) => {
    if (!map.current || !station.latitude || !station.longitude) return;
    
    // Fly to the station without triggering a route redraw
    map.current.flyTo({
      center: [station.longitude, station.latitude],
      zoom: 16,
      duration: 1000
    });
    
    // Don't redraw the route when selecting a station
    // The selected route should remain unchanged
  };

  // Update the route color when selected hour changes
  useEffect(() => {
    if (map.current && selectedRoute && occupancyData && selectedHour) {
      const routeId = Number(selectedRoute.id);
      
      // Redraw the route with updated colors using our new function
      drawRouteWithOccupancy(
        map.current,
        routeId,
        selectedRoute.name || `Route ${routeId}`,
        occupancyData.data || [],
        selectedHour
      ).catch(err => {
        console.error("Error redrawing route:", err);
      });
    }
  }, [selectedHour, occupancyData, selectedRoute]);

  // Handle date/time range changes
  const handleDateTimeChange = (field: keyof DateTimeRange, value: string) => {
    setDateTimeRange(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle hour selection
  const handleHourSelect = (hour: string) => {
    setSelectedHour(hour);
  };

  return (
    <div className="p-0 h-[calc(100vh-64px)] overflow-hidden">
      <div className="flex h-full">
        {/* Left sidebar with route controls */}
        <div className="w-80 flex-shrink-0 h-full overflow-hidden shadow-lg border-r border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
          {/* Header */}
          <div className="h-16 flex items-center px-6 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
            <h1 className="text-xl font-bold text-gray-800 dark:text-white">
              Bus Route Map
            </h1>
          </div>
          
          <div className="h-[calc(100%-4rem)] overflow-y-auto bg-gray-50 dark:bg-gray-900">
            {/* Date and Time Range Controls */}
            <div className="pr-4 pt-4 pb-4 border-b border-gray-200 dark:border-gray-700">
              <h2 className="text-lg font-semibold mb-4 text-gray-800 dark:text-white">Time Range</h2>
              
              {/* Start Date and Time */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-600 dark:text-gray-200 mb-1">
                  Start
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="date"
                    value={dateTimeRange.startDate}
                    onChange={(e) => handleDateTimeChange('startDate', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-md shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white dark:placeholder-gray-400"
                  />
                  <input
                    type="time"
                    value={dateTimeRange.startTime}
                    onChange={(e) => handleDateTimeChange('startTime', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-md shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white dark:placeholder-gray-400"
                  />
                </div>
              </div>
              {/* End Date and Time */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-600 dark:text-gray-200 mb-1">
                  End
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="date"
                    value={dateTimeRange.endDate}
                    onChange={(e) => handleDateTimeChange('endDate', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-md shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white dark:placeholder-gray-400"
                  />
                  <input
                    type="time"
                    value={dateTimeRange.endTime}
                    onChange={(e) => handleDateTimeChange('endTime', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-200 dark:border-gray-600 rounded-md shadow-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white dark:bg-gray-700 text-gray-900 dark:text-white dark:placeholder-gray-400"
                  />
                </div>
              </div>
            </div>

            {/* Occupancy Hour Selection */}
            {occupancyData && occupancyData.data && occupancyData.data.length > 0 && (
              <div className="pr-4 pt-4 pb-4 border-b border-gray-200 dark:border-gray-700">
                <h2 className="text-lg font-semibold mb-4 text-gray-800 dark:text-white">Occupancy by Hour</h2>
                <div className="flex flex-wrap gap-2">
                  {occupancyData.data.map((hourData) => {
                    const densityNum = Number(hourData.OccupancyLevel || hourData.density || 0);
                    const bgColor = getDensityColor(densityNum);
                    return (
                      <button
                        key={hourData.hour}
                        onClick={() => handleHourSelect(hourData.hour)}
                        style={{ 
                          backgroundColor: bgColor,
                          color: densityNum >= 3 ? 'white' : 'black' // Dark text for low levels, light text for high levels
                        }}
                        className={`px-3 py-2 rounded-md text-sm font-medium ${
                          selectedHour === hourData.hour 
                            ? 'ring-2 ring-blue-500 ring-offset-2' 
                            : ''
                        }`}
                      >
                        {hourData.hour}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Route Controls */}
            <div className="pr-4 pt-4 pb-4">
              <InteractiveMapControls
                onRouteSelect={handleRouteSelect}
                onStationSelect={handleStationSelect}
              />
            </div>
          </div>
        </div>
              
        {/* Main map container */}
        <div className="flex-1 relative">
          {/* Map container */}
          <div 
            ref={mapContainer} 
            className="w-full h-full"
          />
          
          {/* Loading indicator */}
          {loading && (
            <div className="absolute inset-0 bg-black bg-opacity-20 dark:bg-opacity-40 flex items-center justify-center z-20">
              <div className="bg-white dark:bg-gray-800 p-4 rounded-lg shadow-lg flex items-center space-x-3">
                <div className="animate-spin rounded-full h-6 w-6 border-t-2 border-b-2 border-blue-500"></div>
                <p className="text-gray-700 dark:text-gray-200">Loading...</p>
              </div>
            </div>
          )}
          
          {/* Error message */}
          {error && (
            <div className="absolute top-4 left-0 right-0 mx-auto w-3/4 max-w-md bg-red-100 dark:bg-red-900/50 border border-red-400 dark:border-red-800 text-red-700 dark:text-red-300 px-4 py-3 rounded-lg z-20">
              <p className="font-medium">Error</p>
              <p className="text-sm">{error}</p>
              <button 
                className="absolute top-2 right-2 text-red-700 dark:text-red-300"
                onClick={() => setError(null)}
              >
                ✕
              </button>
            </div>
          )}
          
          {/* Route info panel */}
          {selectedRoute && (
            <div className="absolute top-6 right-6 bg-white dark:bg-gray-800 p-4 rounded-lg shadow-lg z-10 max-w-xs border border-gray-200 dark:border-gray-700">
              <h3 className="font-bold text-gray-800 dark:text-white mb-1">
                {selectedRoute.name || `Route ${selectedRoute.id}`}
              </h3>
              <div className="text-sm text-gray-600 dark:text-gray-300 mb-2">
                <span className="font-medium">{routeStops.length}</span> stops
              </div>
              
              {/* Occupancy info if available */}
              {occupancyData && selectedHour && (
                <div className="mt-2 pt-2 border-t border-gray-200 dark:border-gray-700">
                  <p className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    Occupancy at {selectedHour}:
                  </p>
                  <div className="mt-1 flex items-center">
                    {(() => {
                      const hourData = occupancyData.data.find(d => d.hour === selectedHour);
                      if (!hourData) return null;
                      
                      const density = hourData.OccupancyLevel !== undefined ? 
                        hourData.OccupancyLevel : hourData.density;
                      const color = getDensityColor(density);
                      let label = "Very Low";
                      
                      if (density >= 5) label = "Very High";
                      else if (density >= 4) label = "High";
                      else if (density >= 3) label = "Medium";
                      else if (density >= 2) label = "Low";
                      
                      return (
                        <>
                          <div className="w-3 h-3 rounded-full mr-2" style={{backgroundColor: color}}></div>
                          <span className="text-sm text-gray-700 dark:text-gray-300">
                            {label} (Level {density})
                          </span>
                        </>
                      );
                    })()}
                  </div>
                </div>
              )}
            </div>
          )}
          
          {/* Legend */}
          <div className="absolute bottom-6 left-6 bg-white dark:bg-gray-800 p-3 rounded-lg shadow-lg z-10 border border-gray-200 dark:border-gray-700">
            <h4 className="text-sm font-bold text-gray-800 dark:text-white mb-2">Occupancy Legend</h4>
            <div className="space-y-1">
              <div key="level-1" className="flex items-center">
                <div className="w-4 h-4 bg-[#2ECC40] mr-2"></div>
                <span className="text-xs text-gray-700 dark:text-gray-300">Very Low (Level 1)</span>
              </div>
              <div key="level-2" className="flex items-center">
                <div className="w-4 h-4 bg-[#ADFF2F] mr-2"></div>
                <span className="text-xs text-gray-700 dark:text-gray-300">Low (Level 2)</span>
              </div>
              <div key="level-3" className="flex items-center">
                <div className="w-4 h-4 bg-[#FFDC00] mr-2"></div>
                <span className="text-xs text-gray-700 dark:text-gray-300">Medium (Level 3)</span>
              </div>
              <div key="level-4" className="flex items-center">
                <div className="w-4 h-4 bg-[#FF851B] mr-2"></div>
                <span className="text-xs text-gray-700 dark:text-gray-300">High (Level 4)</span>
              </div>
              <div key="level-5" className="flex items-center">
                <div className="w-4 h-4 bg-[#FF4136] mr-2"></div>
                <span className="text-xs text-gray-700 dark:text-gray-300">Very High (Level 5)</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}