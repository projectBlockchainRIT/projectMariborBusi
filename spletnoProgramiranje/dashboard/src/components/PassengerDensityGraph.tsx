import { motion } from 'framer-motion';
import { useState } from 'react';

interface PassengerData {
  hour: string;
  density: number;
}

interface LineData {
  lineNumber: string;
  data: PassengerData[];
}

const mockData: LineData[] = [
  {
    lineNumber: "1",
    data: [
      { hour: "4h", density: 5 },
      { hour: "6h", density: 25 },
      { hour: "8h", density: 85 },
      { hour: "10h", density: 45 },
      { hour: "12h", density: 65 },
      { hour: "14h", density: 35 },
      { hour: "16h", density: 75 },
      { hour: "18h", density: 90 },
      { hour: "20h", density: 40 },
      { hour: "22h", density: 15 }
    ]
  },
  {
    lineNumber: "2",
    data: [
      { hour: "4h", density: 10 },
      { hour: "6h", density: 35 },
      { hour: "8h", density: 95 },
      { hour: "10h", density: 55 },
      { hour: "12h", density: 75 },
      { hour: "14h", density: 45 },
      { hour: "16h", density: 85 },
      { hour: "18h", density: 100 },
      { hour: "20h", density: 50 },
      { hour: "22h", density: 20 }
    ]
  },
  {
    lineNumber: "3",
    data: [
      { hour: "4h", density: 8 },
      { hour: "6h", density: 30 },
      { hour: "8h", density: 90 },
      { hour: "10h", density: 50 },
      { hour: "12h", density: 70 },
      { hour: "14h", density: 40 },
      { hour: "16h", density: 80 },
      { hour: "18h", density: 95 },
      { hour: "20h", density: 45 },
      { hour: "22h", density: 18 }
    ]
  }
];

export default function PassengerDensityGraph() {
  const [selectedLine, setSelectedLine] = useState<string>("1");

  const maxDensity = Math.max(...mockData.flatMap(line => line.data.map(d => d.density)));
  const hours = mockData[0].data.map(d => d.hour);
  const selectedLineData = mockData.find(line => line.lineNumber === selectedLine)?.data || [];

  return (
    <div className="p-8 bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg border border-gray-200 dark:border-slate-700/50">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100">Passenger Density by Line</h2>
        <div className="flex items-center space-x-4">
          <span className="text-sm text-gray-600 dark:text-gray-400">Last updated: Today</span>
          <button className="px-3 py-1.5 text-sm bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors">
            Refresh Data
          </button>
        </div>
      </div>

      {/* Line Selection */}
      <div className="flex space-x-2 mb-6">
        {mockData.map((line) => (
          <button
            key={line.lineNumber}
            onClick={() => setSelectedLine(line.lineNumber)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
              selectedLine === line.lineNumber
                ? 'bg-blue-500 text-white'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
            }`}
          >
            Line {line.lineNumber}
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

          {/* Data points and lines */}
          <div className="relative h-full px-4 pl-8 pr-12">
            {/* Draw the columns */}
            {selectedLineData.map((point, index) => {
              const y = 100 - (point.density / maxDensity) * 100;
              const barWidth = 100 / hours.length; // Equal width for each bar
              const barLeft = index * barWidth; // Position each bar next to the previous one
              
              return (
                <motion.div
                  key={point.hour}
                  initial={{ height: 0, opacity: 0 }}
                  animate={{ height: `${100 - y}%`, opacity: 1 }}
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

          {/* X-axis labels */}
          <div className="absolute bottom-0 left-8 right-8 flex justify-between text-xs text-gray-600 dark:text-gray-400">
            {hours.map((hour) => (
              <div key={hour} className="text-center w-12">
                {hour}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="mt-6 text-sm text-gray-600 dark:text-gray-400">
        <span className="font-medium">Note:</span> Graph shows passenger density as a percentage of maximum capacity
      </div>
    </div>
  );
}