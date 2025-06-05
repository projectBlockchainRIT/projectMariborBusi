import { useState } from 'react';
import {
  TruckIcon,
  UserIcon,
  InformationCircleIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  ShieldCheckIcon,
  SunIcon,
  MoonIcon,
  MapIcon
} from '@heroicons/react/24/outline';

interface NavItem {
  name: string;
  icon: React.ElementType;
  href: string;
  requiresAuth?: boolean;
  requiresAdmin?: boolean;
}

const navItems: NavItem[] = [
  { name: 'Realtime Map', icon: TruckIcon, href: '/' },
  { name: 'Interactive Map', icon: MapIcon, href: '/interactive-map', requiresAuth: true },
  { name: 'Graphs', icon: ChartBarIcon, href: '/graphs', requiresAuth: true },
  { name: 'Settings', icon: Cog6ToothIcon, href: '/settings', requiresAuth: true },
  { name: 'Admin Panel', icon: ShieldCheckIcon, href: '/admin', requiresAuth: true, requiresAdmin: true },
  { name: 'Login', icon: UserIcon, href: '/login' },
];

interface SidebarProps {
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

export default function Sidebar({ isDarkMode, toggleDarkMode, isAuthenticated, isAdmin }: SidebarProps) {
  const [isInteractiveMapOpen, setIsInteractiveMapOpen] = useState(false);

  const filteredNavItems = navItems.filter(item => {
    if (!isAuthenticated && item.requiresAuth) return false;
    if (!isAdmin && item.requiresAdmin) return false;
    return true;
  });

  return (
    <div className="flex h-screen">
      {/* Main Sidebar */}
      <div className="w-16 bg-white dark:bg-gray-800 shadow-lg flex flex-col items-center py-4">
        <div className="flex-1 flex flex-col items-center space-y-4">
          {filteredNavItems.map((item) => (
            <a
              key={item.name}
              href={item.href}
              className="p-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg group relative"
              onClick={(e) => {
                if (item.name === 'Interactive Map') {
                  e.preventDefault();
                  setIsInteractiveMapOpen(!isInteractiveMapOpen);
                }
              }}
            >
              <item.icon className="w-6 h-6" />
              <span className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                {item.name}
              </span>
            </a>
          ))}
        </div>

        <div className="flex flex-col items-center space-y-4">
          <button
            onClick={toggleDarkMode}
            className="p-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg group relative"
          >
            {isDarkMode ? (
              <SunIcon className="w-6 h-6" />
            ) : (
              <MoonIcon className="w-6 h-6" />
            )}
            <span className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
              {isDarkMode ? 'Light Mode' : 'Dark Mode'}
            </span>
          </button>

          <a
            href="/about"
            className="p-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg group relative"
          >
            <InformationCircleIcon className="w-6 h-6" />
            <span className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
              About
            </span>
          </a>
        </div>
      </div>

      {/* Interactive Map Sidebar */}
      {isInteractiveMapOpen && (
        <div className="w-64 bg-white dark:bg-gray-800 shadow-lg border-l border-gray-200 dark:border-gray-700">
          <div className="p-4">
            <h2 className="text-lg font-semibold text-gray-800 dark:text-white mb-4">
              Interactive Map Controls
            </h2>
            {/* Add your interactive map controls here */}
            <div className="text-gray-500 dark:text-gray-400">
              Interactive map controls will be implemented here
            </div>
          </div>
        </div>
      )}
    </div>
  );
} 