import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTheme } from '../context/ThemeContext';
import {
  MapIcon,
  TruckIcon,
  UserGroupIcon,
  ClockIcon,
  AdjustmentsHorizontalIcon,
  InformationCircleIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  MapPinIcon
} from '@heroicons/react/24/outline';

interface ControlPanelProps {
  onLayerChange: (layer: string) => void;
  onTimeRangeChange: (range: string) => void;
  onFilterChange: (filters: any) => void;
  onViewChange: (view: string) => void;
}

export default function InteractiveMapControls({
  onLayerChange,
  onTimeRangeChange,
  onFilterChange,
  onViewChange
}: ControlPanelProps) {
  const [activeLayer, setActiveLayer] = useState('buses');
  const [activeTimeRange, setActiveTimeRange] = useState('realtime');
  const [activeView, setActiveView] = useState('info');
  const [showFilters, setShowFilters] = useState(false);
  const { isDarkMode } = useTheme();

  const handleLayerChange = (layer: string) => {
    setActiveLayer(layer);
    onLayerChange(layer);
  };

  const handleTimeRangeChange = (range: string) => {
    setActiveTimeRange(range);
    onTimeRangeChange(range);
  };

  const handleViewChange = (view: string) => {
    setActiveView(view);
    onViewChange(view);
  };

  const handleFilterToggle = () => {
    setShowFilters(!showFilters);
  };

  // Base classes for light/dark mode
  const containerClasses = `fixed top-4 right-4 z-50 transition-colors duration-200 ${
    isDarkMode ? 'text-white' : 'text-gray-900'
  }`;

  const panelClasses = `rounded-lg shadow-lg overflow-hidden transition-colors duration-200 ${
    isDarkMode ? 'bg-gray-800' : 'bg-white'
  }`;

  const borderClasses = `transition-colors duration-200 ${
    isDarkMode ? 'border-gray-700' : 'border-gray-200'
  }`;

  const textClasses = `transition-colors duration-200 ${
    isDarkMode ? 'text-gray-300' : 'text-gray-700'
  }`;

  const buttonBaseClasses = `flex items-center px-3 py-2 rounded-md text-sm font-medium transition-all duration-200`;

  const getButtonClasses = (isActive: boolean) => {
    if (isActive) {
      return `${buttonBaseClasses} ${
        isDarkMode ? 'bg-blue-600 text-white' : 'bg-blue-500 text-white'
      }`;
    }
    return `${buttonBaseClasses} ${
      isDarkMode
        ? 'text-gray-300 hover:bg-gray-700'
        : 'text-gray-700 hover:bg-gray-100'
    }`;
  };

  const selectClasses = `w-full px-3 py-2 rounded-md text-sm border transition-colors duration-200 ${
    isDarkMode
      ? 'bg-gray-600 text-white border-gray-500'
      : 'bg-white text-gray-900 border-gray-300'
  } focus:outline-none focus:ring-2 focus:ring-blue-500`;

  return (
    <div className={containerClasses}>
      <div className={panelClasses}>
        {/* View Selector */}
        <div className={`p-4 border-b ${borderClasses}`}>
          <h3 className={`text-sm font-medium mb-3 ${textClasses}`}>
            View Mode
          </h3>
          <div className="flex flex-col space-y-2">
            <button
              onClick={() => handleViewChange('info')}
              className={getButtonClasses(activeView === 'info')}
            >
              <MapPinIcon className="w-4 h-4 mr-2" />
              Info
            </button>
            <button
              onClick={() => handleViewChange('occupancy')}
              className={getButtonClasses(activeView === 'occupancy')}
            >
              <ChartBarIcon className="w-4 h-4 mr-2" />
              Occupancy
            </button>
            <button
              onClick={() => handleViewChange('delays')}
              className={getButtonClasses(activeView === 'delays')}
            >
              <ExclamationTriangleIcon className="w-4 h-4 mr-2" />
              Delays
            </button>
          </div>
        </div>

        {/* Layer Controls - Only show when view is not 'info' */}
        {activeView !== 'info' && (
          <div className={`p-4 border-b ${borderClasses}`}>
            <h3 className={`text-sm font-medium mb-3 ${textClasses}`}>
              Map Layers
            </h3>
            <div className="flex space-x-2">
              <button
                onClick={() => handleLayerChange('buses')}
                className={getButtonClasses(activeLayer === 'buses')}
              >
                <TruckIcon className="w-4 h-4 mr-2" />
                Buses
              </button>
              <button
                onClick={() => handleLayerChange('passengers')}
                className={getButtonClasses(activeLayer === 'passengers')}
              >
                <UserGroupIcon className="w-4 h-4 mr-2" />
                Passengers
              </button>
            </div>
          </div>
        )}

        {/* Time Range Controls - Only show when view is not 'info' */}
        {activeView !== 'info' && (
          <div className={`p-4 border-b ${borderClasses}`}>
            <h3 className={`text-sm font-medium mb-3 ${textClasses}`}>
              Time Range
            </h3>
            <div className="flex space-x-2">
              <button
                onClick={() => handleTimeRangeChange('realtime')}
                className={getButtonClasses(activeTimeRange === 'realtime')}
              >
                <ClockIcon className="w-4 h-4 mr-2" />
                Real-time
              </button>
              <button
                onClick={() => handleTimeRangeChange('historical')}
                className={getButtonClasses(activeTimeRange === 'historical')}
              >
                <ClockIcon className="w-4 h-4 mr-2" />
                Historical
              </button>
            </div>
          </div>
        )}

        {/* Filter Controls - Only show when view is not 'info' */}
        {activeView !== 'info' && (
          <div className={`p-4 ${borderClasses}`}>
            <button
              onClick={handleFilterToggle}
              className={getButtonClasses(showFilters)}
            >
              <div className="flex items-center">
                <AdjustmentsHorizontalIcon className="w-4 h-4 mr-2" />
                Filters
              </div>
              <InformationCircleIcon className="w-4 h-4" />
            </button>

            <AnimatePresence>
              {showFilters && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={{ opacity: 0, height: 0 }}
                  transition={{ duration: 0.2 }}
                  className={`mt-4 p-4 rounded-md transition-colors duration-200 ${
                    isDarkMode ? 'bg-gray-700' : 'bg-gray-50'
                  }`}
                >
                  <div className="space-y-4">
                    <div>
                      <label className={`block text-sm font-medium mb-2 ${textClasses}`}>
                        Route Filter
                      </label>
                      <select
                        className={selectClasses}
                        onChange={(e) => onFilterChange({ route: e.target.value })}
                      >
                        <option value="">All Routes</option>
                        <option value="1">Route 1</option>
                        <option value="2">Route 2</option>
                        <option value="3">Route 3</option>
                      </select>
                    </div>

                    <div>
                      <label className={`block text-sm font-medium mb-2 ${textClasses}`}>
                        Time Filter
                      </label>
                      <select
                        className={selectClasses}
                        onChange={(e) => onFilterChange({ time: e.target.value })}
                      >
                        <option value="">All Times</option>
                        <option value="morning">Morning (6-12)</option>
                        <option value="afternoon">Afternoon (12-18)</option>
                        <option value="evening">Evening (18-24)</option>
                      </select>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}
      </div>
    </div>
  );
} 