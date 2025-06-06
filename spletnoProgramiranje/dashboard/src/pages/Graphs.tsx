import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import BusDensityHeatmap from '../components/BusDensityHeatmap';
import PassengerDensityGraph from '../components/PassengerDensityGraph';
import ActiveBusesProgress from '../components/ActiveBusesProgress';

interface Tab {
  id: string;
  label: string;
  description: string;
}

const tabs: Tab[] = [
  {
    id: 'bus-density',
    label: 'Bus Density',
    description: 'Real-time bus density across different routes'
  },
  {
    id: 'passenger-flow',
    label: 'Passenger Flow',
    description: 'Passenger traffic patterns throughout the day'
  },
  {
    id: 'route-performance',
    label: 'Route Performance',
    description: 'Performance metrics for each bus route'
  },
  {
    id: 'delay-analysis',
    label: 'Delay Analysis',
    description: 'Analysis of delays and their causes'
  }
];

export default function Graphs() {
  const [activeTab, setActiveTab] = useState(tabs[0].id);

  const renderContent = () => {
    switch (activeTab) {
      case 'bus-density':
        return <BusDensityHeatmap />;
      case 'passenger-flow':
        return <PassengerDensityGraph />;
      default:
        return (
          <div className="h-96 bg-gray-100 dark:bg-gray-700 rounded-lg flex items-center justify-center">
            <p className="text-gray-500 dark:text-gray-400">Graph will be implemented here</p>
          </div>
        );
    }
  };

  return (
    <div className="flex h-screen bg-gray-50 dark:bg-slate-900">
      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <header className="bg-white dark:bg-slate-800/50 backdrop-blur-sm border-b border-gray-200 dark:border-slate-700/50">
          <div className="px-4 py-3">
            <h1 className="text-xl font-semibold text-gray-800 dark:text-slate-100">Graphs & Analytics</h1>
          </div>
        </header>

        {/* Scrollable Content Area */}
        <main className="flex-1 overflow-y-auto p-4 bg-gray-50 dark:bg-slate-900">
          <div className="max-w-7xl mx-auto space-y-6">
            {/* Active Buses Progress Section */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
            >
              <ActiveBusesProgress
                activeCount={15}
                minCount={5}
                maxCount={20}
                className="col-span-1"
              />
            </motion.div>

            {/* Bus Density Heatmap Section */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
            >
              <BusDensityHeatmap />
            </motion.div>

            {/* Additional Graph Sections */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.4 }}
              className="bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg p-6 border border-gray-200 dark:border-slate-700/50"
            >
              <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100 mb-6">Bus Frequency Analysis</h2>
              <div className="h-96 bg-gray-50 dark:bg-slate-700/30 rounded-lg flex items-center justify-center border border-gray-200 dark:border-slate-700/50">
                <p className="text-gray-500 dark:text-slate-400">Bus frequency graph will be displayed here</p>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.6 }}
              className="bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg p-6 border border-gray-200 dark:border-slate-700/50"
            >
              <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100 mb-6">Passenger Load Distribution</h2>
              <div className="h-96 bg-gray-50 dark:bg-slate-700/30 rounded-lg flex items-center justify-center border border-gray-200 dark:border-slate-700/50">
                <p className="text-gray-500 dark:text-slate-400">Passenger load distribution graph will be displayed here</p>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.8 }}
              className="bg-white dark:bg-slate-800/50 backdrop-blur-sm rounded-xl shadow-lg p-6 border border-gray-200 dark:border-slate-700/50"
            >
              <h2 className="text-2xl font-bold text-gray-800 dark:text-slate-100 mb-6">Route Performance Metrics</h2>
              <div className="h-96 bg-gray-50 dark:bg-slate-700/30 rounded-lg flex items-center justify-center border border-gray-200 dark:border-slate-700/50">
                <p className="text-gray-500 dark:text-slate-400">Route performance metrics will be displayed here</p>
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 1 }}
            >
              <PassengerDensityGraph />
            </motion.div>
          </div>
        </main>
      </div>
    </div>
  );
} 