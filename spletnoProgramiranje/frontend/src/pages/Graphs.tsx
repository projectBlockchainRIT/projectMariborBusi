import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import BusDensityHeatmap from '../components/BusDensityHeatmap';
import PassengerDensityGraph from '../components/PassengerDensityGraph';
import ActiveBusesProgress from '../components/ActiveBusesProgress';
import DelayAnalysis from '../components/DelayAnalysis';
import { useTheme } from '../context/ThemeContext';
import {
  ChartBarIcon,
  UserGroupIcon,
  ClockIcon,
} from '@heroicons/react/24/outline';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  BarElement,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend
);

interface Tab {
  id: string;
  name: string;
  description: string;
  icon: React.ElementType;
}

const tabs: Tab[] = [
  {
    id: 'bus-density',
    name: 'Bus Density',
    description: 'Real-time visualization of bus distribution',
    icon: ChartBarIcon,
  },
  {
    id: 'passenger-flow',
    name: 'Passenger Flow',
    description: 'Analysis of passenger movement patterns',
    icon: UserGroupIcon,
  },
  {
    id: 'delay-analysis',
    name: 'Delay Analysis',
    description: 'Breakdown of service delays',
    icon: ClockIcon,
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
      case 'delay-analysis':
        return <DelayAnalysis />;
      default:
        return null;
    }
  };

  return (
    <div className="p-6 lg:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className={`text-2xl font-semibold tracking-tight ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
          Analytics
        </h1>
        <p className={`mt-1 text-sm ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
          Insights into Maribor's public transportation system
        </p>
      </div>

      {/* Active Buses Card */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="mb-6"
      >
        <div
          className={`
            rounded-xl border p-5 transition-colors
            ${isDarkMode
              ? 'bg-slate-800/50 border-slate-700/50'
              : 'bg-white border-slate-200'
            }
          `}
        >
          <ActiveBusesProgress />
        </div>
      </motion.div>

      {/* Main content card */}
      <div
        className={`
          rounded-xl border overflow-hidden transition-colors
          ${isDarkMode
            ? 'bg-slate-800/50 border-slate-700/50'
            : 'bg-white border-slate-200'
          }
        `}
      >
        {/* Tabs */}
        <div className={`border-b ${isDarkMode ? 'border-slate-700/50' : 'border-slate-200'}`}>
          <div className="flex overflow-x-auto">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`
                    relative flex items-center gap-2.5 px-5 py-4
                    text-sm font-medium whitespace-nowrap
                    transition-colors duration-200
                    ${isActive
                      ? isDarkMode
                        ? 'text-white'
                        : 'text-slate-900'
                      : isDarkMode
                        ? 'text-slate-400 hover:text-slate-200'
                        : 'text-slate-500 hover:text-slate-700'
                    }
                  `}
                >
                  <Icon className={`w-4 h-4 ${isActive ? 'text-marprom-600' : ''}`} />
                  <span>{tab.name}</span>

                  {/* Active indicator */}
                  {isActive && (
                    <motion.div
                      layoutId="activeTab"
                      className="absolute bottom-0 left-0 right-0 h-0.5 bg-marprom-600"
                      initial={false}
                      transition={{ type: "spring", stiffness: 500, damping: 30 }}
                    />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Tab content */}
        <div className="p-6">
          {/* Tab description */}
          <p className={`text-sm mb-6 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
            {tabs.find(t => t.id === activeTab)?.description}
          </p>

          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
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
