import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import BusDensityHeatmap from '../components/BusDensityHeatmap';
import PassengerDensityGraph from '../components/PassengerDensityGraph';
import ActiveBusesProgress from '../components/ActiveBusesProgress';
import { useTheme } from '../context/ThemeContext';

interface Tab {
  id: string;
  name: string;
  description: string;
}

const tabs: Tab[] = [
  {
    id: 'bus-density',
    name: 'Bus Density',
    description: 'Real-time visualization of bus distribution across the network'
  },
  {
    id: 'passenger-flow',
    name: 'Passenger Flow',
    description: 'Analysis of passenger movement patterns and peak hours'
  },
  {
    id: 'route-performance',
    name: 'Route Performance',
    description: 'Detailed metrics on route efficiency and reliability'
  },
  {
    id: 'delay-analysis',
    name: 'Delay Analysis',
    description: 'Comprehensive breakdown of service delays and their causes'
  }
];

export default function Graphs() {
  const [activeTab, setActiveTab] = useState('bus-density');
  const { isDarkMode } = useTheme();

  const renderContent = () => {
    switch (activeTab) {
      case 'bus-density':
        return <BusDensityHeatmap />;
      case 'passenger-flow':
        return <PassengerDensityGraph />;
      case 'route-performance':
        return (
          <div className="flex items-center justify-center h-64">
            <p className={`text-lg ${isDarkMode ? 'text-gray-400' : 'text-gray-600'}`}>
              Route performance metrics coming soon...
            </p>
          </div>
        );
      case 'delay-analysis':
        return (
          <div className="flex items-center justify-center h-64">
            <p className={`text-lg ${isDarkMode ? 'text-gray-400' : 'text-gray-600'}`}>
              Delay analysis dashboard coming soon...
            </p>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="p-6">
      <div className="mb-8">
        <h1 className={`text-2xl font-bold mb-2 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
          Analytics Dashboard
        </h1>
        <p className={`${isDarkMode ? 'text-gray-400' : 'text-gray-600'}`}>
          Comprehensive insights into Maribor's public transportation system
        </p>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="mb-8"
      >
        <ActiveBusesProgress />
      </motion.div>

      <div className={`rounded-lg shadow-lg overflow-hidden ${isDarkMode ? 'bg-gray-800' : 'bg-white'}`}>
        <div className={`border-b ${isDarkMode ? 'border-gray-700' : 'border-gray-200'}`}>
          <nav className="flex space-x-8 px-6" aria-label="Tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`
                  py-4 px-1 border-b-2 font-medium text-sm transition-colors duration-200
                  ${activeTab === tab.id
                    ? isDarkMode
                      ? 'border-blue-500 text-blue-400'
                      : 'border-blue-600 text-blue-600'
                    : isDarkMode
                      ? 'border-transparent text-gray-400 hover:text-gray-300 hover:border-gray-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }
                `}
              >
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        <div className="p-6">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              transition={{ duration: 0.2 }}
            >
              {renderContent()}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
} 