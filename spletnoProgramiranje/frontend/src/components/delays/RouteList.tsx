import { ChevronDownIcon, ChevronUpIcon } from '@heroicons/react/24/outline';
import { Bus } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import { Spinner, EmptyState } from '../ui';
import type { Route } from '../../types';

interface RouteListProps {
  routes: Route[];
  loading: boolean;
  expandedRouteId: string | null;
  onRouteClick: (route: Route) => void;
  children?: (route: Route) => React.ReactNode;
}

export default function RouteList({
  routes,
  loading,
  expandedRouteId,
  onRouteClick,
  children,
}: RouteListProps) {
  const { isDarkMode } = useTheme();

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Spinner size="md" />
      </div>
    );
  }

  if (routes.length === 0) {
    return (
      <EmptyState
        title="No routes found"
        description="Try adjusting your search"
      />
    );
  }

  return (
    <div className="space-y-2">
      {routes.map((route) => (
        <div key={route.id}>
          <button
            onClick={() => onRouteClick(route)}
            className={`
              w-full p-3 rounded-lg text-left
              flex items-center justify-between gap-2
              transition-all duration-200
              ${expandedRouteId === route.id
                ? isDarkMode
                  ? 'bg-slate-800 ring-1 ring-slate-700'
                  : 'bg-slate-100 ring-1 ring-slate-200'
                : isDarkMode
                  ? 'hover:bg-slate-800/50'
                  : 'hover:bg-slate-50'
              }
            `}
          >
            <div className="flex items-center gap-2.5">
              <Bus className={`w-4 h-4 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} />
              <span className={`text-sm font-medium ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
                {route.name}
              </span>
            </div>
            {expandedRouteId === route.id ? (
              <ChevronUpIcon className={`w-4 h-4 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} />
            ) : (
              <ChevronDownIcon className={`w-4 h-4 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} />
            )}
          </button>

          {expandedRouteId === route.id && children && children(route)}
        </div>
      ))}
    </div>
  );
}
