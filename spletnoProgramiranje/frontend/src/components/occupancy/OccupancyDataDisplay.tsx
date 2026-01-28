import { useTheme } from '../../context/ThemeContext';

interface OccupancyDataDisplayProps {
  dateTabs: string[];
  displayDate: string | null;
  onDateSelect: (date: string) => void;
  presetHours: number[];
  occupancyData: { [date: string]: { [hour: number]: number | null } };
  currentHourIdx: number | null;
  loading: boolean;
}

const getOccupancyColor = (level: number | null, isDarkMode: boolean): string => {
  if (level === null || level === undefined) return isDarkMode ? 'bg-slate-700' : 'bg-slate-200';
  if (level < 1) return 'bg-emerald-500';
  if (level < 2) return 'bg-yellow-400';
  if (level < 3) return 'bg-orange-500';
  if (level < 4) return 'bg-orange-600';
  return 'bg-red-500';
};

const getOccupancyLabel = (level: number | null): string => {
  if (level === null || level === undefined) return 'No data';
  if (level < 1) return 'Low';
  if (level < 2) return 'Moderate';
  if (level < 3) return 'High';
  if (level < 4) return 'Very High';
  return 'Crowded';
};

export default function OccupancyDataDisplay({
  dateTabs,
  displayDate,
  onDateSelect,
  presetHours,
  occupancyData,
  currentHourIdx,
  loading,
}: OccupancyDataDisplayProps) {
  const { isDarkMode } = useTheme();

  if (!displayDate) return null;

  return (
    <div className={`p-4 border-b ${isDarkMode ? 'border-slate-800' : 'border-slate-200'}`}>
      {/* Date tabs for multi-day */}
      {dateTabs.length > 1 && (
        <div className="flex gap-1 mb-3 overflow-x-auto pb-1">
          {dateTabs.map(date => (
            <button
              key={date}
              onClick={() => onDateSelect(date)}
              className={`
                px-2.5 py-1 rounded-md text-xs font-medium whitespace-nowrap
                transition-colors duration-200
                ${displayDate === date
                  ? 'bg-marprom-600 text-white'
                  : isDarkMode
                    ? 'bg-slate-800 text-slate-400 hover:text-white'
                    : 'bg-slate-100 text-slate-600 hover:text-slate-900'
                }
              `}
            >
              {date}
            </button>
          ))}
        </div>
      )}

      <div className={`text-xs font-medium mb-3 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
        {displayDate}
      </div>

      {/* Hour data */}
      <div className="space-y-2">
        {presetHours.map((hour, idx) => {
          const level = occupancyData[displayDate]?.[hour];
          const isActive = currentHourIdx === idx;
          return (
            <div
              key={hour}
              className={`
                flex items-center gap-3 p-2.5 rounded-lg
                transition-all duration-200
                ${isActive
                  ? isDarkMode
                    ? 'bg-slate-800 ring-2 ring-blue-500/50'
                    : 'bg-slate-100 ring-2 ring-blue-500/30'
                  : isDarkMode
                    ? 'bg-slate-800/50'
                    : 'bg-slate-50'
                }
              `}
            >
              <span className={`text-xs font-mono w-12 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
                {hour}:00
              </span>
              <div className="flex-1">
                <div className={`h-2 rounded-full overflow-hidden ${isDarkMode ? 'bg-slate-700' : 'bg-slate-200'}`}>
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${getOccupancyColor(level, isDarkMode)}`}
                    style={{ width: level !== null ? `${Math.min((level + 1) * 20, 100)}%` : '0%' }}
                  />
                </div>
              </div>
              <span className={`text-xs font-medium w-16 text-right ${isDarkMode ? 'text-slate-300' : 'text-slate-600'}`}>
                {loading ? '...' : getOccupancyLabel(level)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

interface OccupancyLegendProps {
  className?: string;
}

export function OccupancyLegend({ className = '' }: OccupancyLegendProps) {
  const { isDarkMode } = useTheme();

  const legend = [
    { color: 'bg-emerald-500', label: 'Low' },
    { color: 'bg-yellow-400', label: 'Moderate' },
    { color: 'bg-orange-500', label: 'High' },
    { color: 'bg-red-500', label: 'Crowded' },
  ];

  return (
    <div className={`p-4 border-t ${isDarkMode ? 'border-slate-800' : 'border-slate-200'} ${className}`}>
      <div className={`text-xs font-medium mb-2 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
        Occupancy Legend
      </div>
      <div className="flex gap-3">
        {legend.map((item, idx) => (
          <div key={idx} className="flex items-center gap-1.5">
            <span className={`w-2.5 h-2.5 rounded-full ${item.color}`} />
            <span className={`text-xs ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
              {item.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
