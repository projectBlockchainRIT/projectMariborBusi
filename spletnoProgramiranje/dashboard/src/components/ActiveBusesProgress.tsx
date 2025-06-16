import { motion } from 'framer-motion';
import { TruckIcon } from '@heroicons/react/24/outline';
import { useState, useEffect } from 'react';
import { useTheme } from '../context/ThemeContext';

interface ActiveBusesProgressProps {
  className?: string;
}

interface ActiveBusesData {
  data: number;
}

export default function ActiveBusesProgress({
  className = '',
}: ActiveBusesProgressProps) {
  const [activeCount, setActiveCount] = useState<number>(0);
  const [minCount, setMinCount] = useState<number>(0);
  const [maxCount, setMaxCount] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const { isDarkMode } = useTheme();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await fetch('http://40.68.198.73:8080/v1/routes/active');
        if (!response.ok) {
          throw new Error('Failed to fetch active buses data');
        }
        const data: ActiveBusesData = await response.json();
        
        setActiveCount(data.data);
        // Update min and max counts
        setMinCount(prevMin => Math.min(prevMin || data.data, data.data));
        setMaxCount(prevMax => Math.max(prevMax || data.data, data.data));
        setError(null);
      } catch (err) {
        setError('Failed to fetch active buses data');
        console.error('Error fetching active buses:', err);
      }
    };

    // Initial fetch
    fetchData();

    // Set up interval for periodic updates
    const intervalId = setInterval(fetchData, 10000);

    // Cleanup interval on component unmount
    return () => clearInterval(intervalId);
  }, []);

  // Calculate the percentage for the progress bar
  const percentage = Math.min(Math.max((activeCount / maxCount) * 100, 0), 100);
  
  // Calculate color based on percentage
  const getColorClass = (percent: number) => {
    if (percent < 33) return 'from-green-500 to-green-400';
    if (percent < 66) return 'from-yellow-500 to-yellow-400';
    return 'from-red-500 to-red-400';
  };

  return (
    <div
      className={`rounded-xl shadow-sm p-4 border ${isDarkMode ? 'bg-gray-800 text-gray-200 border-gray-700' : 'bg-white text-gray-900 border-gray-200'} ${className}`}
      role="region"
      aria-label="Active Buses Progress"
    >
      {/* Header with icon and active count */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <TruckIcon className={`w-5 h-5 ${isDarkMode ? 'text-gray-200' : 'text-gray-600'}`} />
          <h3 className={`text-lg font-semibold ${isDarkMode ? 'text-gray-100' : 'text-gray-900'}`}>Active Buses</h3>
        </div>
        <div className={`text-2xl font-bold ${isDarkMode ? 'text-gray-100' : 'text-gray-900'}`}>{error ? '--' : activeCount}</div>
      </div>

      {/* Progress bar container */}
      <div className={`relative h-4 rounded-full overflow-hidden mb-2 ${isDarkMode ? 'bg-gray-700' : 'bg-gray-200'}`}>
        <motion.div
          className={`absolute top-0 left-0 h-full bg-gradient-to-r ${getColorClass(percentage)}`}
          initial={{ width: 0 }}
          animate={{ width: `${percentage}%` }}
          transition={{ duration: 0.5, ease: "easeOut" }}
          role="progressbar"
          aria-valuenow={percentage}
          aria-valuemin={0}
          aria-valuemax={100}
        />
      </div>

      {/* Stats row */}
      <div className={`flex justify-between text-sm ${isDarkMode ? 'text-gray-400' : 'text-gray-600'}`}>
        <div className="flex items-center space-x-1">
          <span className="font-medium">Min:</span>
          <span>{error ? '--' : minCount}</span>
        </div>
        <div className="flex items-center space-x-1">
          <span className="font-medium">Max:</span>
          <span>{error ? '--' : maxCount}</span>
        </div>
      </div>

      {/* Percentage indicator */}
      <div className={`text-right text-sm mt-1 ${isDarkMode ? 'text-gray-400' : 'text-gray-500'}`}>
        {error ? '--' : `${percentage.toFixed(1)}% of daily maximum`}
      </div>

      {/* Error message */}
      {error && (
        <div className={`mt-2 text-sm ${isDarkMode ? 'text-red-400' : 'text-red-500'}`}>{error}</div>
      )}
    </div>
  );
} 