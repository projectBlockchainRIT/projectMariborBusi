import { useState, useEffect, useRef, useCallback } from 'react';
import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import InteractiveMapControls from '../components/layout/InteractiveMapControls';
import { useTheme } from '../context/ThemeContext';
import type { Station } from '../types';
import { Play, Pause, RotateCcw, Clock, Calendar } from 'lucide-react'; // Added Calendar icon

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
  Time?: string; // For API response compatibility
  date?: string; // Added date field for multi-day support
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
  const [currentDate, setCurrentDate] = useState<string | null>(null); // New state for tracking current date
  
  // Animation state
  const [isAnimating, setIsAnimating] = useState(false);
  const [animationSpeed, setAnimationSpeed] = useState(1000); // 1 second per step
  const [currentAnimationIndex, setCurrentAnimationIndex] = useState(0);
  const animationIntervalRef = useRef<NodeJS.Timeout | null>(null);
  
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

  // Helper function to generate dates between start and end
  const getDatesBetween = (startDate: string, endDate: string): string[] => {
    const dates: string[] = [];
    const currentDate = new Date(startDate);
    const lastDate = new Date(endDate);
    
    // Set time to midnight to avoid DST issues
    currentDate.setHours(0, 0, 0, 0);
    lastDate.setHours(0, 0, 0, 0);
    
    // Add each date in the range
    while (currentDate <= lastDate) {
      dates.push(currentDate.toISOString().split('T')[0]);
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return dates;
  };

  // Animation controls - updated for multi-day support
  const startAnimation = () => {
    if (!occupancyData || !occupancyData.data || occupancyData.data.length === 0) {
      setError("No occupancy data to animate");
      return;
    }
    
    // Stop any existing animation
    if (animationIntervalRef.current) {
      clearInterval(animationIntervalRef.current);
    }
    
    // Reset animation index if we're at the end
    if (currentAnimationIndex >= occupancyData.data.length - 1) {
      setCurrentAnimationIndex(0);
    }
    
    // Set the initial hour and date
    const initialData = occupancyData.data[currentAnimationIndex];
    setSelectedHour(initialData.hour);
    setCurrentDate(initialData.date || null);
    setIsAnimating(true);
    
    // Start the animation interval
    animationIntervalRef.current = setInterval(() => {
      setCurrentAnimationIndex(prevIndex => {
        const nextIndex = prevIndex + 1;
        
        // If we've reached the end, stop the animation
        if (nextIndex >= occupancyData.data.length) {
          stopAnimation();
          return prevIndex; // Keep the last index
        }
        
        // Update the selected hour and date to the next one in the sequence
        const nextData = occupancyData.data[nextIndex];
        setSelectedHour(nextData.hour);
        setCurrentDate(nextData.date || null);
        return nextIndex;
      });
    }, animationSpeed);
  };

  const stopAnimation = () => {
    if (animationIntervalRef.current) {
      clearInterval(animationIntervalRef.current);
      animationIntervalRef.current = null;
    }
    setIsAnimating(false);
  };

  const resetAnimation = () => {
    stopAnimation();
    setCurrentAnimationIndex(0);
    if (occupancyData && occupancyData.data && occupancyData.data.length > 0) {
      const firstData = occupancyData.data[0];
      setSelectedHour(firstData.hour);
      setCurrentDate(firstData.date || null);
    }
  };

  // Clean up interval on unmount
  useEffect(() => {
    return () => {
      if (animationIntervalRef.current) {
        clearInterval(animationIntervalRef.current);
      }
    };
  }, []);

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
    selectedHour: string | null = null,
    selectedDate: string | null = null
  ) => {
    if (!map || !map.loaded()) {
      console.error("Map not initialized");
      return;
    }
    console.log(`Drawing route ${routeId} with occupancy data for date: ${selectedDate}, hour: ${selectedHour}`);
    console.log(occupancyData)

    // Use fixed IDs instead of route-specific IDs
    const routeSourceId = `route-source`;
    const routeLayerId = `route-line`;
    const stationsSourceId = `stations-source`;
    const stationsLayerId = `stations-points`;

    try {
      // Clear previous layers and sources if they exist
      // IMPORTANT: First remove layers before removing sources
      for (const layerId of [routeLayerId, stationsLayerId]) {
        try {
          if (map.getLayer(layerId)) {
            map.removeLayer(layerId);
          }
        } catch (err) {
          console.warn(`Error removing layer ${layerId}:`, err);
        }
      }
      
      // Now remove sources after layers have been removed
      for (const sourceId of [routeSourceId, stationsSourceId]) {
        try {
          if (map.getSource(sourceId)) {
            map.removeSource(sourceId);
          }
        } catch (err) {
          console.warn(`Error removing source ${sourceId}:`, err);
        }
      }
      
      // Add a small delay to ensure removal is complete
      await new Promise(resolve => setTimeout(resolve, 10));
      
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
        console.log("Looking for hour:", selectedHour, "and date:", selectedDate);
        
        // Find the current hour data, filtering by date if provided
        const hourData = occupancyData.find(d => {
          if (selectedDate) {
            return d.hour === selectedHour && d.date === selectedDate;
          }
          return d.hour === selectedHour;
        });
        
        console.log("Found hour data:", hourData);
        
        if (hourData) {
          // Get the correct density value - prioritize OccupancyLevel if it exists
          const densityValue = Number(hourData.OccupancyLevel || hourData.density || 0);
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

  // Function to handle route selection - updated for multi-day support
  const handleRouteSelect = useCallback(async (routeId: number) => {
    setLoading(true);
    setError(null);
    
    // Stop any running animation when selecting a new route
    stopAnimation();
    setCurrentAnimationIndex(0);
    
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
      
      // Step 2: Get occupancy data for all dates in the range
      const datesList = getDatesBetween(dateTimeRange.startDate, dateTimeRange.endDate);
      console.log("Fetching occupancy data for dates:", datesList);
      
      // Create an array to hold all data
      let allOccupancyData: OccupancyData[] = [];
      
      // Fetch data for each date in parallel
      const occupancyPromises = datesList.map(async (date) => {
        const occupancyResponse = await fetch(
          `http://40.68.198.73:8080/v1/occupancy/line/${routeId}/date/${date}`, 
          { headers: { 'Accept': 'application/json' } }
        );
        
        if (occupancyResponse.ok) {
          const fetchedData = await occupancyResponse.json();
          
          if (fetchedData && fetchedData.data && 
              Array.isArray(fetchedData.data) && fetchedData.data.length > 0) {
            
            // Process items and add date information to each entry
            return fetchedData.data.map(item => ({
              ...item,
              hour: item.Time,
              date: date, // Add date for multi-day support
              OccupancyLevel: Number(item.OccupancyLevel || 0),
              density: Number(item.OccupancyLevel || item.density || 0)
            }));
          }
        }
        return [];
      });
      
      // Wait for all occupancy data fetches to complete
      const occupancyResults = await Promise.all(occupancyPromises);
      
      // Combine all results
      allOccupancyData = occupancyResults.flat();
      
      // Sort by date and then by hour
      allOccupancyData.sort((a, b) => {
        if (a.date !== b.date) {
          return a.date!.localeCompare(b.date!);
        }
        return a.hour.localeCompare(b.hour);
      });
      
      // Check if we have any data
      if (allOccupancyData.length > 0) {
        console.log(`Loaded ${allOccupancyData.length} occupancy entries across ${datesList.length} days`);
        
        const combinedOccupancyData = {
          lineNumber: route.name,
          lineId: routeId,
          data: allOccupancyData
        };
        
        setOccupancyData(combinedOccupancyData);
        
        // Set selected hour to the first hour in the data
        const firstEntry = allOccupancyData[0];
        setSelectedHour(firstEntry.hour);
        setCurrentDate(firstEntry.date || null);
        
        console.log("First entry:", firstEntry);
        console.log("Selected hour:", firstEntry.hour, "Selected date:", firstEntry.date);
        
        // Draw initial route
        const stations = await drawRouteWithOccupancy(
          map.current,
          routeId,
          route.name || `Route ${routeId}`,
          allOccupancyData,
          firstEntry.hour,
          firstEntry.date
        );
        
        // Set route stops
        if (stations) {
          setRouteStops(stations);
        }
      } else {
        console.error("No occupancy data found for any selected dates");
        setOccupancyData(null);
        setSelectedHour(null);
        setCurrentDate(null);
        setError("No occupancy data available for the selected date range");
        
        // Still draw the route but without occupancy data
        const stations = await drawRouteWithOccupancy(
          map.current,
          routeId,
          route.name || `Route ${routeId}`,
          [],
          null,
          null
        );
        
        if (stations) {
          setRouteStops(stations);
        }
      }
    } catch (err) {
      console.error('Error handling route selection:', err);
      setError(err instanceof Error ? err.message : 'Failed to load route data');
    } finally {
      setLoading(false);
    }
  }, [map, dateTimeRange.startDate, dateTimeRange.endDate]);

  // Handle station selection (fly to station)
  const handleStationSelect = (station: Station) => {
    if (!map.current || !station.latitude || !station.longitude) return;
    
    // Fly to the station without triggering a route redraw
    map.current.flyTo({
      center: [station.longitude, station.latitude],
      zoom: 16,
      duration: 1000
    });
  };

  // Update the route color when selected hour or date changes
  useEffect(() => {
    let isActive = true; // For cleanup/prevent race conditions
    
    const updateRoute = async () => {
      if (!map.current || !map.current.loaded() || !selectedRoute || !occupancyData || !selectedHour) {
        return;
      }
      
      const routeId = Number(selectedRoute.id);
      
      try {
        // First ensure we're not in a loading state
        if (loading) return;
        
        // Use requestAnimationFrame to ensure we're in the render cycle
        // This helps prevent race conditions with map operations
        requestAnimationFrame(() => {
          if (isActive) {
            // Redraw the route with updated colors using our new function
            drawRouteWithOccupancy(
              map.current!,
              routeId,
              selectedRoute.name || `Route ${routeId}`,
              occupancyData.data || [],
              selectedHour,
              currentDate
            ).catch(err => {
              console.error("Error redrawing route:", err);
            });
          }
        });
      } catch (err) {
        console.error("Error updating route for hour change:", err);
      }
    };
    
    updateRoute();
    
    // Cleanup function to prevent updates if component unmounts during async operation
    return () => {
      isActive = false;
    };
  }, [selectedHour, currentDate, loading]);

  // Handle date/time range changes
  const handleDateTimeChange = (field: keyof DateTimeRange, value: string) => {
    setDateTimeRange(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle hour selection
  const handleHourSelect = (hour: string, date: string | null) => {
    setSelectedHour(hour);
    setCurrentDate(date);
    
    // Find the index of this hour+date combination for animation purposes
    if (occupancyData && occupancyData.data) {
      const dataIndex = occupancyData.data.findIndex(d => 
        d.hour === hour && (!date || d.date === date)
      );
      if (dataIndex !== -1) {
        setCurrentAnimationIndex(dataIndex);
      }
    }
  };

  // Reload route when date range changes
  useEffect(() => {
    if (selectedRoute) {
      // Reload the current route with new date range
      handleRouteSelect(Number(selectedRoute.id)).catch(console.error);
    }
  }, [dateTimeRange.startDate, dateTimeRange.endDate]);

  // Format date for display
  const formatDate = (dateStr: string | null | undefined): string => {
    if (!dateStr) return "";
    
    const date = new Date(dateStr);
    return date.toLocaleDateString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric'
    });
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

            {/* Occupancy Hour Selection - Updated for multi-day */}
            {occupancyData && occupancyData.data && occupancyData.data.length > 0 && (
              <div className="pr-4 pt-4 pb-4 border-b border-gray-200 dark:border-gray-700">
                <h2 className="text-lg font-semibold mb-4 text-gray-800 dark:text-white">
                  Occupancy Data
                  {currentDate && (
                    <span className="ml-2 text-sm font-medium bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 py-0.5 px-2 rounded-full">
                      {formatDate(currentDate)}
                    </span>
                  )}
                </h2>
                
                {/* Date tabs if multiple dates */}
                {(() => {
                  // Extract unique dates
                  const uniqueDates = Array.from(
                    new Set(occupancyData.data.map(d => d.date))
                  ).filter(Boolean) as string[];
                  
                  if (uniqueDates.length > 1) {
                    return (
                      <div className="mb-4 border-b border-gray-200 dark:border-gray-700">
                        <div className="flex overflow-x-auto pb-2">
                          {uniqueDates.map(date => (
                            <button
                              key={date}
                              onClick={() => {
                                // Find first hour for this date
                                const firstHourForDate = occupancyData.data.find(d => d.date === date);
                                if (firstHourForDate) {
                                  handleHourSelect(firstHourForDate.hour, date);
                                }
                              }}
                              className={`flex items-center px-3 py-1.5 whitespace-nowrap mr-2 rounded-t-md text-xs font-medium
                                ${currentDate === date 
                                  ? 'bg-blue-500 text-white' 
                                  : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                                }`}
                            >
                              <Calendar size={12} className="mr-1" />
                              {formatDate(date)}
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  }
                  return null;
                })()}
                
                {/* Hour buttons for current date */}
                <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Hours</h3>
                <div className="flex flex-wrap gap-2 mb-4">
                  {occupancyData.data
                    // Filter for current date if one is selected
                    .filter(hourData => !currentDate || hourData.date === currentDate)
                    .map((hourData, index) => {
                      const densityNum = Number(hourData.OccupancyLevel || hourData.density || 0);
                      const bgColor = getDensityColor(densityNum);
                      
                      // Check if this is the current animation step
                      const hourIndex = occupancyData.data.findIndex(d => 
                        d.hour === hourData.hour && d.date === hourData.date
                      );
                      const isCurrentAnimationStep = hourIndex === currentAnimationIndex && isAnimating;
                      
                      return (
                        <button
                          key={`${hourData.date}-${hourData.hour}`}
                          onClick={() => handleHourSelect(hourData.hour, hourData.date || null)}
                          style={{ 
                            backgroundColor: bgColor,
                            color: densityNum >= 3 ? 'white' : 'black',
                            position: 'relative' // For animation indicator
                          }}
                          className={`px-3 py-2 rounded-md text-sm font-medium ${
                            (selectedHour === hourData.hour && 
                             (!currentDate || currentDate === hourData.date)) 
                              ? 'ring-2 ring-blue-500 ring-offset-2' 
                              : ''
                          }`}
                        >
                          {hourData.hour}
                          {isCurrentAnimationStep && (
                            <div className="absolute -top-2 -right-2 w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
                          )}
                        </button>
                      );
                    })}
                </div>
                
                {/* Animation Controls */}
                <div className="mt-4">
                  <h3 className="text-sm font-medium mb-2 text-gray-700 dark:text-gray-300">Animation Controls</h3>
                  <div className="flex items-center space-x-3 mb-3">
                    {!isAnimating ? (
                      <button
                        onClick={startAnimation}
                        className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-2 rounded-md text-sm font-medium flex items-center"
                      >
                        <Play size={16} className="mr-1" />
                        Start
                      </button>
                    ) : (
                      <button
                        onClick={stopAnimation}
                        className="bg-yellow-500 hover:bg-yellow-600 text-white px-3 py-2 rounded-md text-sm font-medium flex items-center"
                      >
                        <Pause size={16} className="mr-1" />
                        Pause
                      </button>
                    )}
                    
                    <button
                      onClick={resetAnimation}
                      className="bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-800 dark:text-white px-3 py-2 rounded-md text-sm font-medium flex items-center"
                    >
                      <RotateCcw size={16} className="mr-1" />
                      Reset
                    </button>
                  </div>
                  
                  {/* Animation Speed Control */}
                  <div className="flex items-center">
                    <Clock size={16} className="mr-2 text-gray-600 dark:text-gray-400" />
                    <label className="text-sm text-gray-600 dark:text-gray-400 mr-2">
                      Speed:
                    </label>
                    <input
                      type="range"
                      min="200"
                      max="3000"
                      step="100"
                      value={animationSpeed}
                      onChange={(e) => setAnimationSpeed(Number(e.target.value))}
                      className="w-24 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer dark:bg-gray-700"
                    />
                    <span className="ml-2 text-sm text-gray-600 dark:text-gray-400">
                      {(animationSpeed / 1000).toFixed(1)}s
                    </span>
                  </div>
                  
                  {/* Current Animation Progress */}
                  {isAnimating && occupancyData.data.length > 0 && (
                    <div className="mt-3">
                      <div className="flex justify-between items-center text-xs text-gray-500 dark:text-gray-400 mb-1">
                        <span>{formatDate(occupancyData.data[0].date)} - {occupancyData.data[0].hour}</span>
                        <span>{formatDate(occupancyData.data[occupancyData.data.length-1].date)} - {occupancyData.data[occupancyData.data.length-1].hour}</span>
                      </div>
                      <div className="bg-gray-200 dark:bg-gray-700 rounded-full h-2.5 overflow-hidden">
                        <div 
                          className="bg-blue-600 h-2.5 rounded-full transition-all duration-300" 
                          style={{ 
                            width: `${(currentAnimationIndex / (occupancyData.data.length - 1)) * 100}%` 
                          }}
                        ></div>
                      </div>
                    </div>
                  )}
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
                    {currentDate ? (
                      <>Occupancy on {formatDate(currentDate)} at {selectedHour}:</>
                    ) : (
                      <>Occupancy at {selectedHour}:</>
                    )}
                  </p>
                  <div className="mt-1 flex items-center">
                    {(() => {
                      const hourData = occupancyData.data.find(d => 
                        d.hour === selectedHour && 
                        (!currentDate || d.date === currentDate)
                      );
                      
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