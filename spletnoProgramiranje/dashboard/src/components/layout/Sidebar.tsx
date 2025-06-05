import { Link, useLocation } from 'react-router-dom';
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
  { name: 'Login', icon: UserIcon, href: '/login', },
];

interface SidebarProps {
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
}

export default function Sidebar({ isDarkMode, toggleDarkMode, isAuthenticated, isAdmin }: SidebarProps) {
  const location = useLocation();


  const filteredNavItems = navItems.filter(item => {
    if (!isAuthenticated && item.requiresAuth) return false;
    if (!isAdmin && item.requiresAdmin) return false;
    if (isAuthenticated && item.name === 'Login') return false;
    return true;
  });

  return (
    <div className="w-16 bg-white dark:bg-gray-800 shadow-lg flex flex-col items-center py-4">
      <div className="flex-1 flex flex-col items-center space-y-4">
        {filteredNavItems.map((item) => (
          <Link
            key={item.name}
            to={item.href}
            className={`p-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg group relative ${
              location.pathname === item.href ? 'bg-gray-100 dark:bg-gray-700' : ''
            }`}
          >
            <item.icon className="w-6 h-6" />
            <span className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
              {item.name}
            </span>
          </Link>
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

        <Link
          to="/about"
          className={`p-2 text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg group relative ${
            location.pathname === '/about' ? 'bg-gray-100 dark:bg-gray-700' : ''
          }`}
        >
          <InformationCircleIcon className="w-6 h-6" />
          <span className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
            About
          </span>
        </Link>
      </div>
    </div>
  );
} 