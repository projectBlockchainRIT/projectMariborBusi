import { motion } from 'framer-motion';
import { TruckIcon } from '@heroicons/react/24/outline';
import { useState, useEffect } from 'react';

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

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await fetch('http://localhost:8080/v1/routes/active');
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
      className={`bg-white dark:bg-slate-800 rounded-xl shadow-sm p-4 ${className}`}
      role="region"
      aria-label="Active Buses Progress"
    >
      {/* Header with icon and active count */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <TruckIcon className="w-5 h-5 text-gray-600 dark:text-gray-300" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Active Buses
          </h3>
        </div>
        <div className="text-2xl font-bold text-gray-900 dark:text-white">
          {error ? '--' : activeCount}
        </div>
      </div>

      {/* Progress bar container */}
      <div className="relative h-4 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden mb-2">
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
      <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400">
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
      <div className="text-right text-sm text-gray-500 dark:text-gray-400 mt-1">
        {error ? '--' : `${percentage.toFixed(1)}% of daily maximum`}
      </div>

      {/* Error message */}
      {error && (
        <div className="mt-2 text-sm text-red-500 dark:text-red-400">
          {error}
        </div>
      )}
    </div>
  );
} 