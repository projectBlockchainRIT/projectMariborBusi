import React, { useState, useEffect } from 'react';
import { TruckIcon } from '@heroicons/react/24/outline';
import type { Route } from '../types';
import mapboxgl from 'mapbox-gl';
import { drawRouteOnMap } from '../utils/drawRouteOnMap';
import { useTheme } from '../context/ThemeContext';
import { Box, Typography, FormControl, InputLabel, Select, MenuItem, Switch, FormControlLabel, Button, Slider, Tabs, Tab } from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { PlayArrow, Pause } from '@mui/icons-material';

interface OccupancyControllerProps {
  onRouteSelect: (route: Route) => void;
  mapInstance: mapboxgl.Map | null;
}

const PRESET_HOURS = [8, 10, 12, 16, 18];

const formatDateToYYYYMMDD = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export default function OccupancyController({ onRouteSelect, mapInstance }: OccupancyControllerProps) {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [selectedRouteId, setSelectedRouteId] = useState<string | null>(null);
  const [routesLoading, setRoutesLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const { isDarkMode } = useTheme();
  const [showDateSpan, setShowDateSpan] = useState(false);
  const [startDate, setStartDate] = useState<Date | null>(null);
  const [endDate, setEndDate] = useState<Date | null>(null);
  const [occupancyData, setOccupancyData] = useState<{ [date: string]: { [hour: number]: number | null } }>({});
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [loadingOccupancy, setLoadingOccupancy] = useState(false);
  const [dateTabs, setDateTabs] = useState<string[]>([]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentHourIdx, setCurrentHourIdx] = useState<number | null>(null);
  const [animationSpeed, setAnimationSpeed] = useState(1); // seconds
  const [currentDayIdx, setCurrentDayIdx] = useState<number>(0);

  useEffect(() => {
    const loadRoutes = async () => {
      try {
        setRoutesLoading(true);
        const response = await fetch('http://40.68.198.73:8080/v1/routes/list');
        if (!response.ok) {
          throw new Error(`Failed to fetch routes: ${response.status} ${response.statusText}`);
        }
        const data = await response.json();
        const routesData = Array.isArray(data) ? data : (data.data || data.routes || []);
        setRoutes(routesData);
      } catch (error) {
        console.error('Error loading routes:', error);
      } finally {
        setRoutesLoading(false);
      }
    };
    loadRoutes();
  }, []);

  const fetchOccupancyData = async (lineId: string, date: string, hour: number) => {
    try {
      const response = await fetch(`http://40.68.198.73:8080/v1/occupancy/line/${lineId}/date/${date}/hour/${hour}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch occupancy data: ${response.status} ${response.statusText}`);
      }
      const data = await response.json();
      console.log(`[API] Occupancy response for line ${lineId}, date ${date}, hour ${hour}:`, data);
      // Handle both array and object responses
      if (Array.isArray(data?.data)) {
        // Find the entry for the correct hour
        const hourStr = hour.toString().padStart(2, '0');
        const found = data.data.find((entry: any) => entry.Time && entry.Time.includes(`T${hourStr}:`));
        return found ? found.OccupancyLevel ?? null : null;
      } else if (data?.data && typeof data.data === 'object') {
        return data.data.OccupancyLevel ?? null;
      }
      return null;
    } catch (error) {
      console.log(`[API] Error for line ${lineId}, date ${date}, hour ${hour}:`, error);
      return null;
    }
  };

  const getDatesInRange = (start: Date, end: Date): string[] => {
    const dates: string[] = [];
    const currentDate = new Date(start);
    const endDateObj = new Date(end);
    
    while (currentDate <= endDateObj) {
      dates.push(formatDateToYYYYMMDD(currentDate));
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return dates;
  };

  const handleRouteClick = async (route: Route) => {
    setSelectedRouteId(route.id);
    onRouteSelect(route);
    setLoadingOccupancy(true);
    let datesToFetch: string[] = [];
    if (showDateSpan && startDate && endDate) {
      datesToFetch = getDatesInRange(startDate, endDate);
    } else {
      datesToFetch = [formatDateToYYYYMMDD(new Date())];
    }
    setDateTabs(datesToFetch);
    setSelectedDate(datesToFetch[0]);
    const newData: { [date: string]: { [hour: number]: number | null } } = {};
    for (const date of datesToFetch) {
      newData[date] = {};
      for (const hour of PRESET_HOURS) {
        newData[date][hour] = await fetchOccupancyData(route.id, date, hour);
      }
    }
    setOccupancyData(newData);
    setLoadingOccupancy(false);
    if (mapInstance) {
      await drawRouteOnMap(mapInstance, route, { setStatus: (msg: string) => console.log(msg) });
    }
  };

  const filteredRoutes = routes.filter(route =>
    route.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Helper for color based on occupancy level
  const getOccupancyColor = (level: number | null) => {
    if (level === null || level === undefined) return isDarkMode ? 'bg-gray-700' : 'bg-gray-200';
    if (level < 1) return 'bg-green-400';
    if (level < 2) return 'bg-yellow-300';
    if (level < 3) return 'bg-orange-400';
    if (level < 4) return 'bg-orange-600';
    return 'bg-red-600';
  };

  // When toggling date span off, fetch for today
  useEffect(() => {
    if (!showDateSpan && selectedRouteId) {
      const fetchToday = async () => {
        setLoadingOccupancy(true);
        const today = formatDateToYYYYMMDD(new Date());
        setDateTabs([today]);
        setSelectedDate(today);
        const newData: { [date: string]: { [hour: number]: number | null } } = {};
        newData[today] = {};
        for (const hour of PRESET_HOURS) {
          newData[today][hour] = await fetchOccupancyData(selectedRouteId, today, hour);
        }
        setOccupancyData(newData);
        setLoadingOccupancy(false);
      };
      fetchToday();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [showDateSpan, selectedRouteId]);

  // Update: Reset animation when dateTabs or route changes
  useEffect(() => {
    setIsPlaying(false);
    setCurrentHourIdx(null);
    setCurrentDayIdx(0);
  }, [selectedRouteId, dateTabs]);

  // Animation effect for multi-day
  useEffect(() => {
    if (currentHourIdx === null || dateTabs.length === 0) return;
    // Always update route color for the current state
    if (mapInstance && selectedRouteId && dateTabs[currentDayIdx]) {
      const hour = PRESET_HOURS[currentHourIdx];
      const occ = occupancyData[dateTabs[currentDayIdx]]?.[hour];
      let color = '#60A5FA'; // default blue
      if (occ !== undefined && occ !== null) {
        if (occ < 1) color = '#4ade80'; // green
        else if (occ < 2) color = '#fde047'; // yellow
        else if (occ < 3) color = '#fb923c'; // orange
        else if (occ < 4) color = '#f59e42'; // deep orange
        else color = '#ef4444'; // red
      }
      try {
        mapInstance.setPaintProperty(`route-${selectedRouteId}-line`, 'line-color', color);
      } catch (e) { /* ignore if layer not found */ }
    }
    if (!isPlaying) return;
    const isLastHour = currentHourIdx >= PRESET_HOURS.length - 1;
    const isLastDay = currentDayIdx >= dateTabs.length - 1;
    if (isLastHour && isLastDay) {
      setIsPlaying(false);
      // Do NOT reset currentHourIdx/currentDayIdx, keep highlight and color
      return;
    }
    const timer = setTimeout(() => {
      if (isLastHour) {
        setCurrentDayIdx(idx => idx + 1);
        setCurrentHourIdx(0);
      } else {
        setCurrentHourIdx(idx => (idx !== null ? idx + 1 : 0));
      }
    }, animationSpeed * 1000);
    return () => clearTimeout(timer);
  }, [isPlaying, currentHourIdx, currentDayIdx, animationSpeed, mapInstance, selectedRouteId, occupancyData, dateTabs]);

  // Play handler
  const handlePlay = () => {
    // If at the end, or at the start (null), restart from beginning
    const isAtEnd =
      currentHourIdx === PRESET_HOURS.length - 1 &&
      currentDayIdx === dateTabs.length - 1;
    const isAtStart = currentHourIdx === null || currentDayIdx === null;
    if (isAtEnd || isAtStart) {
      setCurrentDayIdx(0);
      setCurrentHourIdx(0);
    }
    setIsPlaying(true);
  };
  // Pause handler
  const handlePause = () => setIsPlaying(false);

  const displayDate = isPlaying && dateTabs.length > 0 ? dateTabs[currentDayIdx] : selectedDate;

  // Legend colors and labels
  const legend = [
    { color: '#4ade80', label: 'Low (Green)' },
    { color: '#fde047', label: 'Moderate (Yellow)' },
    { color: '#fb923c', label: 'High (Orange)' },
    { color: '#ef4444', label: 'Very High (Red)' },
  ];
  const animatedHour = isPlaying && currentHourIdx !== null ? PRESET_HOURS[currentHourIdx] : null;

  // Always render the selected route on the map when both are available
  useEffect(() => {
    if (!mapInstance || !selectedRouteId) return;
    const route = routes.find(r => r.id === selectedRouteId);
    if (route) {
      drawRouteOnMap(mapInstance, route, { setStatus: (msg: string) => console.log(msg) });
    }
  }, [mapInstance, selectedRouteId]);

  return (
    <div className={`h-full w-80 shadow-lg transition-colors duration-200 ${isDarkMode ? 'bg-gray-800 text-gray-200' : 'bg-white text-gray-700'}`}>
      <div className="h-full flex flex-col">
        <div className={`p-4 border-b transition-colors duration-200 ${isDarkMode ? 'border-gray-700' : 'border-gray-200'}`}> 
          <h2 className="text-xl font-semibold flex items-center gap-2">
            Occupancy
          </h2>
        </div>

        <div className={`p-4 border-b transition-colors duration-200 ${isDarkMode ? 'border-gray-700' : 'border-gray-200'}`}>
          <div className="flex items-center justify-between mb-2">
            <label className={`text-sm font-medium ${isDarkMode ? 'text-gray-200' : 'text-gray-700'}`}>Select Date Span</label>
            <button
              onClick={() => setShowDateSpan((prev) => !prev)}
              className={`px-3 py-1 rounded-md text-sm font-medium transition-colors duration-150
                ${showDateSpan
                  ? isDarkMode
                    ? 'bg-blue-600 text-white hover:bg-blue-700'
                    : 'bg-blue-500 text-white hover:bg-blue-600'
                  : isDarkMode
                    ? 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                }`}
            >
              {showDateSpan ? 'Enabled' : 'Disabled'}
            </button>
          </div>

          {showDateSpan && (
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <div className="flex gap-2 mt-2">
                <DatePicker
                  label="From"
                  value={startDate}
                  onChange={(newValue: Date | null) => setStartDate(newValue)}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      size: "small",
                      className: isDarkMode ? 'bg-gray-700' : 'bg-white',
                      sx: {
                        '& .MuiInputLabel-root': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 0.9)' : 'rgba(0, 0, 0, 0.6)',
                        },
                        '& .MuiInputBase-input': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 1)' : 'rgba(0, 0, 0, 0.87)',
                          '&::placeholder': {
                            color: isDarkMode ? 'rgba(255, 255, 255, 0.7)' : 'rgba(0, 0, 0, 0.5)',
                            opacity: 1
                          }
                        },
                        '& .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.4)' : 'rgba(0, 0, 0, 0.4)',
                        },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.6)',
                        },
                        '& .MuiInputAdornment-root .MuiSvgIcon-root': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 0.9)' : 'rgba(0, 0, 0, 0.7)',
                        }
                      }
                    }
                  }}
                  sx={{
                    '& .MuiPickersDay-root': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                      '&.Mui-selected': {
                        backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.16)' : 'rgba(0, 0, 0, 0.16)',
                        color: isDarkMode ? '#fff' : '#000',
                        '&:hover': {
                          backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.24)' : 'rgba(0, 0, 0, 0.24)',
                        },
                      },
                      '&:hover': {
                        backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.08)',
                      },
                    },
                    '& .MuiPickersCalendarHeader-root': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersCalendarHeader-label': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersArrowSwitcher-button': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersDay-today': {
                      borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.5)',
                    },
                  }}
                />
                <DatePicker
                  label="To"
                  value={endDate}
                  onChange={(newValue: Date | null) => setEndDate(newValue)}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      size: "small",
                      className: isDarkMode ? 'bg-gray-700' : 'bg-white',
                      sx: {
                        '& .MuiInputLabel-root': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 0.9)' : 'rgba(0, 0, 0, 0.6)',
                        },
                        '& .MuiInputBase-input': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 1)' : 'rgba(0, 0, 0, 0.87)',
                          '&::placeholder': {
                            color: isDarkMode ? 'rgba(255, 255, 255, 0.7)' : 'rgba(0, 0, 0, 0.5)',
                            opacity: 1
                          }
                        },
                        '& .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.23)' : 'rgba(0, 0, 0, 0.23)',
                        },
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.4)' : 'rgba(0, 0, 0, 0.4)',
                        },
                        '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                          borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.6)' : 'rgba(0, 0, 0, 0.6)',
                        },
                        '& .MuiInputAdornment-root .MuiSvgIcon-root': {
                          color: isDarkMode ? 'rgba(255, 255, 255, 0.9)' : 'rgba(0, 0, 0, 0.7)',
                        }
                      }
                    }
                  }}
                  sx={{
                    '& .MuiPickersDay-root': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                      '&.Mui-selected': {
                        backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.16)' : 'rgba(0, 0, 0, 0.16)',
                        color: isDarkMode ? '#fff' : '#000',
                        '&:hover': {
                          backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.24)' : 'rgba(0, 0, 0, 0.24)',
                        },
                      },
                      '&:hover': {
                        backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.08)',
                      },
                    },
                    '& .MuiPickersCalendarHeader-root': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersCalendarHeader-label': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersArrowSwitcher-button': {
                      color: isDarkMode ? 'rgba(255, 255, 255, 0.87)' : 'rgba(0, 0, 0, 0.87)',
                    },
                    '& .MuiPickersDay-today': {
                      borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.5)' : 'rgba(0, 0, 0, 0.5)',
                    },
                  }}
                />
              </div>
            </LocalizationProvider>
          )}
        </div>

        {/* Occupancy Data Display & Animation Controls */}
        <div className={`p-4 border-b transition-colors duration-200 ${isDarkMode ? 'border-gray-700' : 'border-gray-200'}`}>
          {selectedRouteId && displayDate && (
            <>
              {dateTabs.length > 1 && (
                <Tabs
                  value={displayDate}
                  onChange={(_, v) => setSelectedDate(v)}
                  variant="scrollable"
                  scrollButtons="auto"
                  sx={{ mb: 2 }}
                >
                  {dateTabs.map(date => (
                    <Tab key={date} value={date} label={date} sx={{ color: isDarkMode ? '#fff' : '#222' }} />
                  ))}
                </Tabs>
              )}
              <div className="mb-2 text-sm font-semibold">Occupancy Data {displayDate}</div>
              <div className="flex flex-col gap-2 mb-4">
                {PRESET_HOURS.map((hour, idx) => (
                  <div
                    key={hour}
                    className={`rounded-lg px-4 py-2 text-center font-mono text-lg border-2 ${getOccupancyColor(occupancyData[displayDate]?.[hour])} ${isDarkMode ? 'border-gray-600' : 'border-gray-300'} ${currentHourIdx === idx ? 'ring-2 ring-blue-400' : ''}`}
                  >
                    {loadingOccupancy ? 'Loading...' : (occupancyData[displayDate]?.[hour] !== undefined ? `Hour ${hour}: ${occupancyData[displayDate]?.[hour]}` : `Hour ${hour}: -`)}
                  </div>
                ))}
              </div>
              {/* Animation Controls */}
              <div className="flex items-center gap-2 mb-2">
                <button
                  onClick={handlePlay}
                  disabled={isPlaying || loadingOccupancy || PRESET_HOURS.length === 0}
                  className={`flex items-center gap-1 px-4 py-2 rounded font-medium transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
                    ${isDarkMode
                      ? 'bg-blue-500 text-white hover:bg-blue-600 disabled:bg-blue-900'
                      : 'bg-blue-600 text-white hover:bg-blue-700 disabled:bg-blue-300'}
                    ${isPlaying || loadingOccupancy || PRESET_HOURS.length === 0 ? 'opacity-60 cursor-not-allowed' : ''}`}
                >
                  <PlayArrow className="w-5 h-5" /> Play
                </button>
                <button
                  onClick={handlePause}
                  disabled={!isPlaying}
                  className={`flex items-center gap-1 px-4 py-2 rounded font-medium transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
                    ${isDarkMode
                      ? 'bg-gray-700 text-gray-200 hover:bg-gray-600 disabled:bg-gray-900'
                      : 'bg-gray-200 text-gray-900 hover:bg-gray-300 disabled:bg-gray-100'}
                    ${!isPlaying ? 'opacity-60 cursor-not-allowed' : ''}`}
                >
                  <Pause className="w-5 h-5" /> Pause
                </button>
                <span className="ml-2 text-xs">Speed:</span>
                <Slider
                  min={0}
                  max={2}
                  step={0.1}
                  value={animationSpeed}
                  onChange={(_, v) => setAnimationSpeed(typeof v === 'number' ? v : 1)}
                  sx={{ width: 80, color: isDarkMode ? '#60a5fa' : '#2563eb' }}
                />
                <span className="text-xs">{animationSpeed.toFixed(1)}s</span>
              </div>
            </>
          )}
        </div>

        <div className={`p-4 border-b transition-colors duration-200 ${isDarkMode ? 'border-gray-700' : 'border-gray-200'}`}> 
          <input
            type="text"
            placeholder="Search routes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className={`w-full px-3 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${isDarkMode ? 'bg-gray-700 border-gray-600 text-gray-200 placeholder-gray-400' : 'bg-gray-100 border-gray-200 text-gray-800 placeholder-gray-500'}`}
          />
        </div>

        <div className="p-4 flex-1 overflow-y-auto">
          {routesLoading ? (
            <div className="text-center py-4">Loading routes...</div>
          ) : filteredRoutes.length === 0 ? (
            <div className="text-center py-4">No routes found</div>
          ) : (
            <div className="space-y-2">
              {filteredRoutes.map((route) => {
                const isSelected = selectedRouteId === route.id;
                return (
                  <button
                    key={route.id}
                    onClick={() => handleRouteClick(route)}
                    className={`w-full p-3 text-left rounded-lg transition-colors border flex items-center font-medium gap-2
                      ${isSelected
                        ? isDarkMode
                          ? 'bg-blue-900 text-white border-blue-700'
                          : 'bg-blue-100 text-blue-900 border-blue-200'
                        : isDarkMode
                          ? 'bg-gray-700 hover:bg-gray-600 border-gray-600'
                          : 'bg-gray-100 hover:bg-gray-200 border-gray-200'
                      }`}
                  >
                    <TruckIcon className="h-4 w-4" />
                    {route.name}
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Floating Occupancy Legend Window (always visible, legend only) */}
        <div
          className={`fixed top-4 left-1/2 transform -translate-x-1/2 z-50 rounded-lg shadow-lg px-6 py-4 flex flex-col items-center min-w-[260px] border ${isDarkMode ? 'bg-gray-800 text-gray-200 border-gray-700' : 'bg-white text-gray-900 border-gray-200'}`}
          style={{ pointerEvents: 'auto' }}
        >
          <div className="font-semibold mb-2 text-base">Route Occupancy Legend</div>
          <div className="flex flex-col gap-1 w-full mb-2">
            {legend.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2">
                <span className="inline-block w-6 h-3 rounded" style={{ background: item.color }}></span>
                <span className="text-xs">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
} 