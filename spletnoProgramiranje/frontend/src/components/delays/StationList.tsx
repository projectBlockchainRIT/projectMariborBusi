import { MapPinIcon, BellAlertIcon } from '@heroicons/react/24/outline';
import { useTheme } from '../../context/ThemeContext';
import type { Station } from '../../types';

interface StationListProps {
  stations: Station[];
  loading: boolean;
  selectedStation: Station | null;
  onStationClick: (station: Station) => void;
  onReportDelay?: (station: Station) => void;
}

export default function StationList({
  stations,
  loading,
  selectedStation,
  onStationClick,
  onReportDelay,
}: StationListProps) {
  const { isDarkMode } = useTheme();

  if (loading) {
    return (
      <div className={`py-3 text-sm ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
        Loading stations...
      </div>
    );
  }

  return (
    <div className={`mt-2 ml-4 pl-4 border-l ${isDarkMode ? 'border-slate-700' : 'border-slate-200'}`}>
      <div className="space-y-1 py-1">
        <div className={`text-xs font-medium mb-2 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
          {stations.length} stations
        </div>
        {stations.map((station, index) => (
          <div key={station.id} className="relative group">
            <button
              onClick={() => onStationClick(station)}
              className={`
                w-full text-left p-2.5 rounded-lg
                transition-all duration-200
                ${selectedStation?.id === station.id
                  ? isDarkMode
                    ? 'bg-marprom-600/10 ring-1 ring-marprom-600/30'
                    : 'bg-marprom-50 ring-1 ring-marprom-200'
                  : isDarkMode
                    ? 'hover:bg-slate-800/50'
                    : 'hover:bg-slate-50'
                }
              `}
            >
              <div className="flex items-center gap-2">
                <MapPinIcon
                  className={`w-3.5 h-3.5 flex-shrink-0 ${
                    selectedStation?.id === station.id
                      ? 'text-marprom-600'
                      : isDarkMode ? 'text-slate-500' : 'text-slate-400'
                  }`}
                />
                <div className="flex-1 min-w-0">
                  <div className={`text-sm truncate ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
                    {station.name}
                  </div>
                  <div className={`text-xs ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
                    #{station.number}
                  </div>
                </div>
                <span className={`text-xs ${isDarkMode ? 'text-slate-600' : 'text-slate-300'}`}>
                  {index + 1}
                </span>
              </div>
            </button>

            {onReportDelay && (
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onReportDelay(station);
                }}
                className={`
                  absolute right-2 top-1/2 -translate-y-1/2
                  p-1.5 rounded-md
                  opacity-0 group-hover:opacity-100
                  transition-all duration-200
                  ${isDarkMode
                    ? 'bg-amber-500/10 hover:bg-amber-500/20 text-amber-400'
                    : 'bg-amber-50 hover:bg-amber-100 text-amber-600'
                  }
                `}
                title="Report delay"
              >
                <BellAlertIcon className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
