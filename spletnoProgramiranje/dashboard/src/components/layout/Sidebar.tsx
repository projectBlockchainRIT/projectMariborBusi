import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  HomeIcon,
  UserIcon,
  InformationCircleIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  ShieldCheckIcon,
  SunIcon,
  MoonIcon,
  MapIcon,
  ArrowRightOnRectangleIcon,
  ExclamationTriangleIcon,
  UserGroupIcon
} from '@heroicons/react/24/outline';
import { useUser } from '../../context/UserContext';
import { useTheme } from '../../context/ThemeContext';

interface NavItem {
  name: string;
  icon: React.ElementType;
  href: string;
  requiresAuth?: boolean;
  requiresAdmin?: boolean;
}

const navItems: NavItem[] = [
  { name: 'Interactive Map', icon: MapIcon, href: '/interactive-map', requiresAuth: true },
  { name: 'Occupancy', icon: UserGroupIcon, href: '/occupancy', requiresAuth: true },
  { name: 'Delays', icon: ExclamationTriangleIcon, href: '/delays', requiresAuth: true },
  { name: 'Graphs', icon: ChartBarIcon, href: '/graphs', requiresAuth: true },
  { name: 'Settings', icon: Cog6ToothIcon, href: '/settings', requiresAuth: true },
  { name: 'Admin Panel', icon: ShieldCheckIcon, href: '/admin', requiresAuth: true, requiresAdmin: true },
  { name: 'Login', icon: UserIcon, href: '/login', },
];

interface SidebarProps {
  isAuthenticated: boolean;
  isAdmin: boolean;
}

export default function Sidebar({ isAuthenticated, isAdmin }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { setIsAuthenticated, setIsAdmin } = useUser();
  const { isDarkMode, toggleDarkMode } = useTheme();

  const handleLogout = () => {
    // Clear authentication state
    setIsAuthenticated(false);
    setIsAdmin(false);
    // Clear localStorage
    localStorage.removeItem('isAuthenticated');
    localStorage.removeItem('isAdmin');
    // Redirect to landing page
    navigate('/');
  };

  const handleThemeToggle = () => {
    toggleDarkMode();
  };

  const filteredNavItems = navItems.filter(item => {
    if (!isAuthenticated && item.requiresAuth) return false;
    if (!isAdmin && item.requiresAdmin) return false;
    if (isAuthenticated && item.name === 'Login') return false;
    return true;
  });

  // Base classes for light/dark mode
  const sidebarClasses = `w-16 shadow-lg flex flex-col items-center py-4 transition-colors duration-200 ${
    isDarkMode ? 'bg-gray-800 text-gray-200' : 'bg-white text-gray-700'
  }`;

  const getNavLinkClasses = (isActive: boolean) => {
    const baseClasses = 'p-2 rounded-lg group relative transition-colors duration-200';
    if (isActive) {
      return `${baseClasses} ${
        isDarkMode ? 'bg-gray-700' : 'bg-gray-100'
      }`;
    }
    return `${baseClasses} ${
      isDarkMode
        ? 'hover:bg-gray-700 text-gray-200'
        : 'hover:bg-gray-100 text-gray-700'
    }`;
  };

  const tooltipClasses = `absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap`;

  return (
    <div className={sidebarClasses}>
      <div className="flex-1 flex flex-col items-center space-y-4">
        {filteredNavItems.map((item) => (
          <Link
            key={item.name}
            to={item.href}
            className={getNavLinkClasses(location.pathname === item.href)}
          >
            <item.icon className="w-6 h-6" />
            <span className={tooltipClasses}>
              {item.name}
            </span>
          </Link>
        ))}
      </div>

      <div className="flex flex-col items-center space-y-4">
        <button
          onClick={handleThemeToggle}
          className={getNavLinkClasses(false)}
        >
          {isDarkMode ? (
            <SunIcon className="w-6 h-6" />
          ) : (
            <MoonIcon className="w-6 h-6" />
          )}
          <span className={tooltipClasses}>
            {isDarkMode ? 'Light Mode' : 'Dark Mode'}
          </span>
        </button>

        <Link
          to="/about"
          className={getNavLinkClasses(location.pathname === '/about')}
        >
          <InformationCircleIcon className="w-6 h-6" />
          <span className={tooltipClasses}>
            About
          </span>
        </Link>

        {isAuthenticated && (
          <button
            onClick={handleLogout}
            className={getNavLinkClasses(false)}
          >
            <ArrowRightOnRectangleIcon className="w-6 h-6" />
            <span className={tooltipClasses}>
              Logout
            </span>
          </button>
        )}
      </div>
    </div>
  );
} 