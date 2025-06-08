import { motion } from 'framer-motion';
import { useState, useEffect, useMemo } from 'react';

interface OccupancyData {
  occupancyLevel: number;
  time: string;
}

interface PassengerData {
  hour: string;
  density: number;
}

interface Route {
  id: number;
  name: string;
  description?: string;
  [key: string]: any;
}

interface LineData {
  lineNumber: string;
  lineId: number;
  data: PassengerData[];
}

// Format hour from API time string (e.g., "14:30:00" -> "14h")
const formatHour = (timeString: string): string => {
  if (!timeString) return '';
  const match = timeString.match(/^(\d{1,2}):/);
  return match ? `${match[1]}h` : '';
};

// Get current date in YYYY-MM-DD format
const getCurrentDate = (): string => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

// Format date for display (e.g., "2025-06-08" -> "June 8, 2025")
const formatDateForDisplay = (dateStr: string): string => {
  try {
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    
    const date = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  } catch (e) {
    return dateStr;
  }
};

// Get date string for a day offset from current date
const getOffsetDate = (offset: number): string => {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

export default function PassengerDensityGraph() {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [selectedLineId, setSelectedLineId] = useState<number | null>(null);
  const [lineData, setLineData] = useState<LineData[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [currentDate, setCurrentDate] = useState<string>(getCurrentDate());
  const [showDatePicker, setShowDatePicker] = useState<boolean>(false);
  
  // Updated hours to match only available API hours
  const hours = ["6", "8", "10", "12", "16", "18"];

  // Fetch available routes on component mount
  useEffect(() => {
    const fetchRoutes = async () => {
      try {
        setLoading(true);
        // Convert WebSocket URL to HTTP URL
        const response = await fetch('http://40.68.198.73:8080/v1/routes/list', {
          headers: {
            'Accept': 'application/json'
          }
        });

        if (!response.ok) {
          throw new Error(`Failed to fetch routes: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();
        console.log('Routes data:', data);

        let routesArray: Route[] = [];
        if (Array.isArray(data)) {
          routesArray = data;
        } else if (data && typeof data === 'object') {
          // Handle case where API returns {data: [...]} structure
          routesArray = data.data || data.routes || [];
        }

        if (routesArray.length > 0) {
          setRoutes(routesArray);
          // Select first route by default
          setSelectedLineId(routesArray[0].id);
        } else {
          setError('No routes available');
        }
      } catch (err) {
        console.error('Error fetching routes:', err);
        setError(err instanceof Error ? err.message : 'Failed to load routes');
      } finally {
        setLoading(false);
      }
    };

    fetchRoutes();
  }, []);
// Fetch occupancy data when selectedLineId or date changes
useEffect(() => {
  if (!selectedLineId) return;
  
  const fetchOccupancyData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Make a single fetch for all hours instead of multiple fetches
      const response = await fetch(`http://40.68.198.73:8080/v1/occupancy/line/${selectedLineId}/date/${currentDate}`, {
        headers: {
          'Accept': 'application/json'
        }
      });
      
      if (!response.ok) {
        if (response.status === 404) {
          console.log('No occupancy data available');
          setError(`No passenger data available for ${selectedRouteName} on ${formatDateForDisplay(currentDate)}`);
          setLoading(false);
          return;
        }
        throw new Error(`Failed to fetch occupancy data: ${response.status}`);
      }
      
      const responseData = await response.json();
      console.log('Raw occupancy data:', responseData);
      
      // Extract data array from response
      const occupancyData = responseData.data || [];
      
      if (!Array.isArray(occupancyData) || occupancyData.length === 0) {
        console.log('No occupancy data found in response');
        setError(`No passenger data available for ${selectedRouteName} on ${formatDateForDisplay(currentDate)}`);
        setLoading(false);
        return;
      }
      
      console.log(`Found ${occupancyData.length} data points`);
      
      // Process the occupancy data
      const newLineData: LineData = {
        lineNumber: String(selectedLineId),
        lineId: selectedLineId,
        data: []
      };

      // Map the API data to our format
      occupancyData.forEach(item => {
        if (!item.Time || typeof item.OccupancyLevel !== 'number') return;
        
        // Extract hour from the time string (format: "0000-01-01THH:00:00Z")
        const hourMatch = item.Time.match(/T(\d{2}):/);
        if (!hourMatch) return;
        
        const hour = hourMatch[1];
        console.log(`Extracted hour ${hour} with occupancy level ${item.OccupancyLevel}`);
        
        newLineData.data.push({
          hour: `${hour}h`,
          density: Math.round(item.OccupancyLevel * 25) // Convert 0-4 scale to 0-100%
        });
      });
      
      // Sort data by hour
      newLineData.data.sort((a, b) => 
        parseInt(a.hour.replace('h', '')) - parseInt(b.hour.replace('h', ''))
      );
      
      console.log('Processed occupancy data:', newLineData);
      
      // Update line data, replacing previous data for this line
      setLineData(prevData => {
        const otherLines = prevData.filter(line => line.lineId !== selectedLineId);
        return [...otherLines, newLineData];
      });
      
    } catch (err) {
      console.error('Error fetching occupancy data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load occupancy data');
    } finally {
      setLoading(false);
    }
  };

  fetchOccupancyData();
}, [selectedLineId, currentDate]);
  
  // Find the name of the selected route
  const selectedRouteName = useMemo(() => {
    const route = routes.find(r => r.id === selectedLineId);
    return route ? route.name : `Line ${selectedLineId}`;
  }, [routes, selectedLineId]);
  
  const refreshData = () => {
    // Update date to current and refresh data
    setCurrentDate(getCurrentDate());
    setShowDatePicker(false);
  };

  // Handle date change
  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCurrentDate(e.target.value);
    setShowDatePicker(false);
  };

  // Navigate to previous/next day
  const navigateDate = (offset: number) => {
    const date = new Date(currentDate);
    date.setDate(date.getDate() + offset);
    setCurrentDate(
      `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    );
  };

  // Find data for currently selected line
  const selectedLineData = useMemo(() => {
    const line = lineData.find(line => line.lineId === selectedLineId);
    return line?.data || [];
  }, [lineData, selectedLineId]);
  
  // Calculate max density for graph scaling
  const maxDensity = useMemo(() => {
    if (selectedLineData.length === 0) return 100; // Default max if no data
    const maxValue = Math.max(...selectedLineData.map(d => d.density));
    return Math.max(100, maxValue); // Always at least 100% for consistent scaling
  }, [selectedLineData]);

  // Get list of hours from the data
  const dataHours = useMemo(() => {
    if (selectedLineData.length === 0) return hours.map(h => `${h}h`);
    return selectedLineData.map(d => d.hour);
  }, [selectedLineData, hours]);

  return (
    <div className="p-8 bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg border border-gray-200 dark:border-slate-700/50">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100">Passenger Density by Line</h2>
        
        {/* Date Selection UI */}
        <div className="flex items-center space-x-2">
          <button 
            className="p-1.5 rounded-lg bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600"
            onClick={() => navigateDate(-1)}
            aria-label="Previous day"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-600 dark:text-gray-300" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
          </button>
          
          <div className="relative">
            <button
              className="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 flex items-center space-x-1"
              onClick={() => setShowDatePicker(!showDatePicker)}
              disabled={loading}
            >
              <span>{formatDateForDisplay(currentDate)}</span>
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-600 dark:text-gray-300" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
            
            {showDatePicker && (
              <div className="absolute top-full mt-1 right-0 z-10 bg-white dark:bg-gray-800 shadow-lg rounded-lg p-3 border border-gray-200 dark:border-gray-700">
                <div className="space-y-3">
                  <input 
                    type="date" 
                    value={currentDate}
                    onChange={handleDateChange}
                    className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-800 dark:text-gray-200"
                  />
                  
                  <div className="grid grid-cols-2 gap-2">
                    <button 
                      className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
                      onClick={() => {
                        setCurrentDate(getCurrentDate());
                        setShowDatePicker(false);
                      }}
                    >
                      Today
                    </button>
                    <button 
                      className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600"
                      onClick={() => {
                        setCurrentDate(getOffsetDate(-1));
                        setShowDatePicker(false);
                      }}
                    >
                      Yesterday
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
          
          <button 
            className="p-1.5 rounded-lg bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600"
            onClick={() => navigateDate(1)}
            aria-label="Next day"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-600 dark:text-gray-300" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
            </svg>
          </button>
          
          <button 
            className="px-3 py-1.5 ml-2 text-sm bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors"
            onClick={refreshData}
            disabled={loading}
          >
            {loading ? 'Loading...' : 'Today'}
          </button>
        </div>
      </div>

      {/* Show error if any */}
      {error && (
        <div className="mb-4 p-3 bg-red-100 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {/* Line Selection */}
      <div className="flex flex-wrap gap-2 mb-6">
        {routes.map((route) => (
          <button
            key={route.id}
            onClick={() => setSelectedLineId(route.id)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              selectedLineId === route.id
                ? 'bg-blue-500 text-white'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
            }`}
          >
            {route.name || `Line ${route.id}`}
          </button>
        ))}
      </div>

      {/* Graph */}
      <div className="relative h-[400px] w-full">
        {/* Y-axis labels */}
        <div className="absolute left-0 top-0 bottom-0 w-12 flex flex-col justify-between text-xs text-gray-600 dark:text-gray-400">
          {[100, 75, 50, 25, 0].map((value) => (
            <div key={value} className="text-right pr-2">
              {value}%
            </div>
          ))}
        </div>

        {/* Graph area */}
        <div className="absolute left-12 right-0 top-0 bottom-0">
          {/* Grid lines */}
          <div className="absolute inset-0 flex flex-col justify-between">
            {[0, 1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className={`w-full ${i === 4 ? 'border-t border-gray-200 dark:border-gray-700' : 'border-t border-gray-100 dark:border-gray-800'}`}
              />
            ))}
          </div>

          {loading ? (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-gray-500 dark:text-gray-400">Loading data...</div>
            </div>
          ) : selectedLineData.length === 0 ? (
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-gray-500 dark:text-gray-400">No data available for {selectedRouteName} on {formatDateForDisplay(currentDate)}</div>
            </div>
          ) : (
            /* Data points and lines */
            <div className="relative h-full px-4 pl-8 pr-12">
              {/* Draw the columns */}
              {selectedLineData.map((point, index) => {
                const y = 100 - (point.density / maxDensity) * 100;
                const barWidth = 100 / dataHours.length; // Equal width for each bar
                const barLeft = index * barWidth; // Position each bar next to the previous one
                
                return (
                  <motion.div
                    key={point.hour}
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: `${Math.max(1, 100 - y)}%`, opacity: 1 }}
                    transition={{ duration: 0.5, delay: index * 0.1 }}
                    className="absolute bottom-0 bg-blue-500/70 hover:bg-blue-500 transition-colors"
                    style={{
                      left: `${barLeft}%`,
                      width: `${barWidth}%`,
                      minWidth: '24px',
                      maxWidth: '48px',
                    }}
                  >
                    <div className="absolute -top-6 left-1/2 transform -translate-x-1/2 text-xs font-medium text-gray-700 dark:text-gray-300 whitespace-nowrap">
                      {point.density}%
                    </div>
                  </motion.div>
                );
              })}
            </div>
          )}

          {/* X-axis labels */}
          <div className="absolute bottom-0 left-8 right-8 flex justify-between text-xs text-gray-600 dark:text-gray-400">
            {dataHours.map((hour) => (
              <div key={hour} className="text-center w-12">
                {hour}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="mt-6 text-sm text-gray-600 dark:text-gray-400 flex justify-between items-center">
        <div>
          <span className="font-medium">Note:</span> Graph shows passenger density as a percentage of maximum capacity
        </div>
        <div className="text-xs text-gray-500">
          Data for {formatDateForDisplay(currentDate)}
        </div>
      </div>
    </div>
  );
}