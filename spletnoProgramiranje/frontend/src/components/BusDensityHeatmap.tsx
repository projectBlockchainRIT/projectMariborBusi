import { motion } from 'framer-motion';
import { useState } from 'react';

type DensityLevel = 'empty' | 'low' | 'medium' | 'high';

interface DensityData {
  hour: string;
  monday: DensityLevel;
  tuesday: DensityLevel;
  wednesday: DensityLevel;
  thursday: DensityLevel;
  friday: DensityLevel;
  saturday: DensityLevel;
  sunday: DensityLevel;
}

const densityData: DensityData[] = [
  { hour: '4h', monday: 'empty', tuesday: 'empty', wednesday: 'empty', thursday: 'empty', friday: 'empty', saturday: 'empty', sunday: 'empty' },
  { hour: '6h', monday: 'low', tuesday: 'low', wednesday: 'low', thursday: 'low', friday: 'low', saturday: 'empty', sunday: 'empty' },
  { hour: '8h', monday: 'medium', tuesday: 'high', wednesday: 'high', thursday: 'high', friday: 'high', saturday: 'low', sunday: 'empty' },
  { hour: '10h', monday: 'high', tuesday: 'high', wednesday: 'high', thursday: 'high', friday: 'high', saturday: 'low', sunday: 'low' },
  { hour: '12h', monday: 'medium', tuesday: 'medium', wednesday: 'medium', thursday: 'medium', friday: 'medium', saturday: 'medium', sunday: 'low' },
  { hour: '14h', monday: 'low', tuesday: 'low', wednesday: 'low', thursday: 'low', friday: 'low', saturday: 'medium', sunday: 'low' },
  { hour: '16h', monday: 'medium', tuesday: 'medium', wednesday: 'medium', thursday: 'medium', friday: 'medium', saturday: 'low', sunday: 'low' },
  { hour: '18h', monday: 'high', tuesday: 'high', wednesday: 'high', thursday: 'high', friday: 'high', saturday: 'low', sunday: 'empty' },
  { hour: '20h', monday: 'medium', tuesday: 'low', wednesday: 'low', thursday: 'low', friday: 'medium', saturday: 'empty', sunday: 'empty' },
  { hour: '22h', monday: 'empty', tuesday: 'empty', wednesday: 'empty', thursday: 'empty', friday: 'low', saturday: 'empty', sunday: 'empty' },
];

const densityColors: Record<DensityLevel, string> = {
  empty: 'bg-gray-100 dark:bg-gray-800/50',
  low: 'bg-orange-200 dark:bg-orange-900/70',
  medium: 'bg-orange-500 dark:bg-orange-700/90',
  high: 'bg-red-600 dark:bg-red-800',
};

const densityDescriptions: Record<DensityLevel, string> = {
  empty: 'No buses in operation',
  low: '1-3 buses in operation',
  medium: '4-6 buses in operation',
  high: '7+ buses in operation',
};

const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];

export default function BusDensityHeatmap() {
  const [hoveredCell, setHoveredCell] = useState<{ day: string; hour: string; level: DensityLevel } | null>(null);

  return (
    <div className="p-8 bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg border border-gray-200 dark:border-slate-700/50">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100">Bus Density Heatmap</h2>
        <div className="flex items-center space-x-4">
          <span className="text-sm text-gray-600 dark:text-gray-400">Last updated: Today</span>
          <button className="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
            Refresh Data
          </button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-slate-700/50">
        <div className="min-w-full">
          {/* Header */}
          <div className="grid grid-cols-8 gap-0.5 bg-gray-50 dark:bg-slate-700/30 p-2">
            <div className="col-span-1"></div>
            {days.map((day) => (
              <div
                key={day}
                className="text-center text-sm font-semibold text-gray-700 dark:text-slate-300 py-3"
              >
                {day}
              </div>
            ))}
          </div>

          {/* Heatmap Grid */}
          <div className="grid grid-cols-8 gap-0.5 p-2">
            {densityData.map((row, index) => (
              <motion.div
                key={row.hour}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="contents"
              >
                {/* Hour Label */}
                <div className="text-sm font-medium text-gray-700 dark:text-slate-300 flex items-center justify-center bg-gray-50 dark:bg-slate-700/30 py-4">
                  {row.hour}
                </div>

                {/* Density Cells */}
                {days.map((day) => (
                  <motion.div
                    key={`${row.hour}-${day}`}
                    whileHover={{ 
                      scale: 1.05,
                      transition: { duration: 0.2 }
                    }}
                    className={`${densityColors[row[day.toLowerCase() as keyof Omit<DensityData, 'hour'>]]} 
                      flex items-center justify-center py-4 relative group cursor-pointer`}
                    onMouseEnter={() => setHoveredCell({ 
                      day, 
                      hour: row.hour,
                      level: row[day.toLowerCase() as keyof Omit<DensityData, 'hour'>]
                    })}
                    onMouseLeave={() => setHoveredCell(null)}
                  >
                    {hoveredCell?.day === day && hoveredCell?.hour === row.hour && (
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="absolute inset-0 flex items-center justify-center bg-black/10 dark:bg-white/10"
                      >
                        <div className="text-xs font-medium text-gray-900 dark:text-white text-center px-1">
                          {day}<br/>{row.hour}<br/>
                          <span className="text-[10px] opacity-90">
                            {densityDescriptions[hoveredCell.level]}
                          </span>
                        </div>
                      </motion.div>
                    )}
                  </motion.div>
                ))}
              </motion.div>
            ))}
          </div>
        </div>
      </div>

      {/* Legend and Info */}
      <div className="mt-8 flex items-center justify-between">
        <div className="flex items-center space-x-6">
          {Object.entries(densityColors).map(([level, color]) => (
            <motion.div
              key={level}
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 1 }}
              className="flex items-center"
            >
              <div className={`w-5 h-5 rounded-md ${color} mr-2 shadow-sm`}></div>
              <span className={`text-sm font-medium ${
                ['medium', 'high'].includes(level)
                  ? 'text-white dark:text-white' 
                  : 'text-gray-900 dark:text-gray-100'
              } capitalize`}>
                {level}
              </span>
            </motion.div>
          ))}
        </div>
        <div className="text-sm text-gray-600 dark:text-gray-400">
          <span className="font-medium">Note:</span> Density levels indicate the number of buses in operation per line
        </div>
      </div>
    </div>
  );
} 