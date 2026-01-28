import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
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
  UserGroupIcon,
  XMarkIcon
} from '@heroicons/react/24/outline';
import { Bus } from 'lucide-react';
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
  { name: 'Interactive Map', icon: MapIcon, href: '/dashboard/interactive-map', requiresAuth: true },
  { name: 'Occupancy', icon: UserGroupIcon, href: '/dashboard/occupancy', requiresAuth: true },
  { name: 'Delays', icon: ExclamationTriangleIcon, href: '/dashboard/delays', requiresAuth: true },
  { name: 'Analytics', icon: ChartBarIcon, href: '/dashboard/graphs', requiresAuth: true },
  { name: 'Settings', icon: Cog6ToothIcon, href: '/dashboard/settings', requiresAuth: true },
  { name: 'Admin Panel', icon: ShieldCheckIcon, href: '/dashboard/admin', requiresAuth: true, requiresAdmin: true },
  { name: 'Login', icon: UserIcon, href: '/login' },
];

interface SidebarProps {
  isAuthenticated: boolean;
  isAdmin: boolean;
  isExpanded?: boolean;
  onClose?: () => void;
}

export default function Sidebar({ isAuthenticated, isAdmin, isExpanded = false, onClose }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { setIsAuthenticated, setIsAdmin } = useUser();
  const { isDarkMode, toggleDarkMode } = useTheme();

  const handleLogout = () => {
    setIsAuthenticated(false);
    setIsAdmin(false);
    localStorage.removeItem('isAuthenticated');
    localStorage.removeItem('isAdmin');
    onClose?.();
    navigate('/');
  };

  const filteredNavItems = navItems.filter(item => {
    if (!isAuthenticated && item.requiresAuth) return false;
    if (!isAdmin && item.requiresAdmin) return false;
    if (isAuthenticated && item.name === 'Login') return false;
    return true;
  });

  const handleNavClick = () => {
    onClose?.();
  };

  return (
    <aside
      className={`
        flex flex-col py-4
        border-r transition-all duration-300
        h-full
        ${isExpanded ? 'w-64 px-4' : 'w-[72px] items-center'}
        ${isDarkMode
          ? 'bg-slate-900 border-slate-800'
          : 'bg-white border-slate-200'
        }
      `}
    >
      {/* Header with Logo and Close button */}
      <div className={`flex items-center ${isExpanded ? 'justify-between mb-6' : 'justify-center mb-6'}`}>
        <Link
          to="/"
          onClick={handleNavClick}
          className={`p-2 rounded-xl bg-marprom-600 hover:bg-marprom-700 transition-colors group ${isExpanded ? '' : ''}`}
        >
          <Bus className="w-6 h-6 text-white" />
        </Link>

        {isExpanded && (
          <>
            <span className={`text-lg font-bold ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
              M-busi
            </span>
            {onClose && (
              <button
                onClick={onClose}
                className={`
                  p-2 rounded-lg transition-colors
                  ${isDarkMode
                    ? 'text-slate-400 hover:text-white hover:bg-slate-800'
                    : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100'
                  }
                `}
                aria-label="Close menu"
              >
                <XMarkIcon className="w-5 h-5" />
              </button>
            )}
          </>
        )}
      </div>

      {/* Divider */}
      <div className={`h-px mb-4 ${isExpanded ? 'w-full' : 'w-8'} ${isDarkMode ? 'bg-slate-700' : 'bg-slate-200'}`} />

      {/* Navigation */}
      <nav className={`flex-1 flex flex-col gap-1 ${isExpanded ? 'w-full' : 'items-center'}`}>
        {filteredNavItems.map((item) => {
          const isActive = location.pathname === item.href;
          return (
            <Link
              key={item.name}
              to={item.href}
              onClick={handleNavClick}
              className={`
                relative group rounded-xl transition-all duration-200
                ${isExpanded ? 'flex items-center gap-3 px-3 py-2.5 w-full' : 'p-3'}
                ${isActive
                  ? isDarkMode
                    ? 'bg-slate-800 text-white'
                    : 'bg-slate-100 text-slate-900'
                  : isDarkMode
                    ? 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                    : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100'
                }
                ${isActive && 'shadow-sm'}
              `}
            >
              {/* Active indicator */}
              {isActive && (
                <span
                  className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-5 bg-marprom-600 rounded-r-full"
                />
              )}
              <item.icon className="w-5 h-5 flex-shrink-0" />

              {/* Label (always visible when expanded) */}
              {isExpanded && (
                <span className="text-sm font-medium">{item.name}</span>
              )}

              {/* Tooltip (only when collapsed) */}
              {!isExpanded && (
                <span
                  className={`
                    absolute left-full ml-3 px-3 py-1.5 rounded-lg text-xs font-medium
                    whitespace-nowrap opacity-0 invisible
                    group-hover:opacity-100 group-hover:visible
                    group-focus:opacity-100 group-focus:visible
                    transition-all duration-200 transform
                    group-hover:translate-x-0 group-focus:translate-x-0 -translate-x-1
                    z-50 shadow-lg
                    ${isDarkMode
                      ? 'bg-slate-800 text-white border border-slate-700'
                      : 'bg-slate-900 text-white'
                    }
                  `}
                >
                  {item.name}
                  {/* Arrow */}
                  <span
                    className={`
                      absolute right-full top-1/2 -translate-y-1/2
                      border-4 border-transparent
                      ${isDarkMode ? 'border-r-slate-800' : 'border-r-slate-900'}
                    `}
                  />
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      {/* Bottom section */}
      <div className={`flex flex-col gap-1 mt-auto pt-4 ${isExpanded ? 'w-full' : 'items-center'}`}>
        {/* Divider */}
        <div className={`h-px mb-3 ${isExpanded ? 'w-full' : 'w-8'} ${isDarkMode ? 'bg-slate-700' : 'bg-slate-200'}`} />

        {/* Theme toggle */}
        <button
          onClick={toggleDarkMode}
          className={`
            relative group rounded-xl transition-all duration-200
            ${isExpanded ? 'flex items-center gap-3 px-3 py-2.5 w-full' : 'p-3'}
            ${isDarkMode
              ? 'text-slate-400 hover:text-amber-400 hover:bg-slate-800/50'
              : 'text-slate-500 hover:text-amber-500 hover:bg-slate-100'
            }
          `}
          aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {isDarkMode ? (
            <SunIcon className="w-5 h-5 flex-shrink-0" />
          ) : (
            <MoonIcon className="w-5 h-5 flex-shrink-0" />
          )}

          {isExpanded && (
            <span className="text-sm font-medium">
              {isDarkMode ? 'Light mode' : 'Dark mode'}
            </span>
          )}

          {/* Tooltip (only when collapsed) */}
          {!isExpanded && (
            <span
              className={`
                absolute left-full ml-3 px-3 py-1.5 rounded-lg text-xs font-medium
                whitespace-nowrap opacity-0 invisible
                group-hover:opacity-100 group-hover:visible
                group-focus:opacity-100 group-focus:visible
                transition-all duration-200 transform
                group-hover:translate-x-0 group-focus:translate-x-0 -translate-x-1
                z-50 shadow-lg
                ${isDarkMode
                  ? 'bg-slate-800 text-white border border-slate-700'
                  : 'bg-slate-900 text-white'
                }
              `}
            >
              {isDarkMode ? 'Light mode' : 'Dark mode'}
              <span
                className={`
                  absolute right-full top-1/2 -translate-y-1/2
                  border-4 border-transparent
                  ${isDarkMode ? 'border-r-slate-800' : 'border-r-slate-900'}
                `}
              />
            </span>
          )}
        </button>

        {/* About link */}
        <Link
          to="/about"
          onClick={handleNavClick}
          className={`
            relative group rounded-xl transition-all duration-200
            ${isExpanded ? 'flex items-center gap-3 px-3 py-2.5 w-full' : 'p-3'}
            ${location.pathname === '/about'
              ? isDarkMode
                ? 'bg-slate-800 text-white'
                : 'bg-slate-100 text-slate-900'
              : isDarkMode
                ? 'text-slate-400 hover:text-white hover:bg-slate-800/50'
                : 'text-slate-500 hover:text-slate-900 hover:bg-slate-100'
            }
          `}
        >
          <InformationCircleIcon className="w-5 h-5 flex-shrink-0" />

          {isExpanded && (
            <span className="text-sm font-medium">About</span>
          )}

          {/* Tooltip (only when collapsed) */}
          {!isExpanded && (
            <span
              className={`
                absolute left-full ml-3 px-3 py-1.5 rounded-lg text-xs font-medium
                whitespace-nowrap opacity-0 invisible
                group-hover:opacity-100 group-hover:visible
                group-focus:opacity-100 group-focus:visible
                transition-all duration-200 transform
                group-hover:translate-x-0 group-focus:translate-x-0 -translate-x-1
                z-50 shadow-lg
                ${isDarkMode
                  ? 'bg-slate-800 text-white border border-slate-700'
                  : 'bg-slate-900 text-white'
                }
              `}
            >
              About
              <span
                className={`
                  absolute right-full top-1/2 -translate-y-1/2
                  border-4 border-transparent
                  ${isDarkMode ? 'border-r-slate-800' : 'border-r-slate-900'}
                `}
              />
            </span>
          )}
        </Link>

        {/* Logout button */}
        {isAuthenticated && (
          <button
            onClick={handleLogout}
            className={`
              relative group rounded-xl transition-all duration-200
              ${isExpanded ? 'flex items-center gap-3 px-3 py-2.5 w-full' : 'p-3'}
              ${isDarkMode
                ? 'text-slate-400 hover:text-red-400 hover:bg-red-500/10'
                : 'text-slate-500 hover:text-red-600 hover:bg-red-50'
              }
            `}
          >
            <ArrowRightOnRectangleIcon className="w-5 h-5 flex-shrink-0" />

            {isExpanded && (
              <span className="text-sm font-medium">Logout</span>
            )}

            {/* Tooltip (only when collapsed) */}
            {!isExpanded && (
              <span
                className={`
                  absolute left-full ml-3 px-3 py-1.5 rounded-lg text-xs font-medium
                  whitespace-nowrap opacity-0 invisible
                  group-hover:opacity-100 group-hover:visible
                  group-focus:opacity-100 group-focus:visible
                  transition-all duration-200 transform
                  group-hover:translate-x-0 group-focus:translate-x-0 -translate-x-1
                  z-50 shadow-lg
                  ${isDarkMode
                    ? 'bg-slate-800 text-white border border-slate-700'
                    : 'bg-slate-900 text-white'
                  }
                `}
              >
                Logout
                <span
                  className={`
                    absolute right-full top-1/2 -translate-y-1/2
                    border-4 border-transparent
                    ${isDarkMode ? 'border-r-slate-800' : 'border-r-slate-900'}
                  `}
                />
              </span>
            )}
          </button>
        )}
      </div>
    </aside>
  );
}
