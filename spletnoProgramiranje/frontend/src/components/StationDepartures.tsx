import { Clock, TrendingUp, Calendar } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { useMemo } from 'react';

interface Departure {
  line: string;
  direction: string;
  times: string[];
}

interface StationDeparturesProps {
  departures: Departure[];
}

interface ParsedDeparture {
  line: string;
  direction: string;
  time: string;
  minutesUntil: number;
  isPast: boolean;
  isNext: boolean;
}

export default function StationDepartures({ departures }: StationDeparturesProps) {
  const { isDarkMode } = useTheme();

  const parsedDepartures = useMemo(() => {
    const now = new Date();
    const currentMinutes = now.getHours() * 60 + now.getMinutes();

    const allDepartures: ParsedDeparture[] = [];
    const seenDepartures = new Set<string>();

    // Flatten all departures with their times
    departures.forEach((dep) => {
      dep.times.forEach((time) => {
        // Create unique key to detect duplicates
        const uniqueKey = `${dep.line}-${dep.direction}-${time}`;

        // Skip if we've already seen this exact departure
        if (seenDepartures.has(uniqueKey)) {
          return;
        }
        seenDepartures.add(uniqueKey);

        const [hours, minutes] = time.split(':').map(Number);
        const departureMinutes = hours * 60 + minutes;
        const minutesUntil = departureMinutes - currentMinutes;

        allDepartures.push({
          line: dep.line,
          direction: dep.direction,
          time,
          minutesUntil,
          isPast: minutesUntil < 0,
          isNext: false,
        });
      });
    });

    // Sort by time
    allDepartures.sort((a, b) => {
      const aMinutes = parseInt(a.time.split(':')[0]) * 60 + parseInt(a.time.split(':')[1]);
      const bMinutes = parseInt(b.time.split(':')[0]) * 60 + parseInt(b.time.split(':')[1]);
      return aMinutes - bMinutes;
    });

    // Mark the next upcoming departure
    const nextIndex = allDepartures.findIndex((d) => !d.isPast);
    if (nextIndex >= 0) {
      allDepartures[nextIndex].isNext = true;
    }

    return allDepartures;
  }, [departures]);

  const upcomingDepartures = parsedDepartures.filter((d) => !d.isPast);
  const pastDepartures = parsedDepartures.filter((d) => d.isPast);

  const formatMinutesUntil = (minutes: number) => {
    if (minutes < 0) return 'Departed';
    if (minutes === 0) return 'Now';
    if (minutes < 60) return `${minutes} min`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
  };

  return (
    <div className="space-y-4">
      {/* Next Departure - Highlighted */}
      {upcomingDepartures.length > 0 && upcomingDepartures[0].isNext && (
        <div
          className={`p-4 rounded-xl border-2 ${
            isDarkMode
              ? 'bg-gradient-to-br from-marprom-900/40 to-marprom-800/20 border-marprom-600/50'
              : 'bg-gradient-to-br from-marprom-50 to-white border-marprom-300'
          }`}
        >
          <div className="flex items-center gap-2 mb-3">
            <TrendingUp
              className={`w-5 h-5 ${isDarkMode ? 'text-marprom-400' : 'text-marprom-600'}`}
            />
            <span
              className={`text-sm font-semibold ${
                isDarkMode ? 'text-marprom-300' : 'text-marprom-700'
              }`}
            >
              NEXT DEPARTURE
            </span>
          </div>
          <div className="flex items-baseline justify-between">
            <div>
              <div
                className={`text-3xl font-bold ${
                  isDarkMode ? 'text-white' : 'text-gray-900'
                }`}
              >
                {upcomingDepartures[0].time}
              </div>
              <div
                className={`text-sm mt-1 font-medium ${
                  isDarkMode ? 'text-marprom-300' : 'text-marprom-700'
                }`}
              >
                Line {upcomingDepartures[0].line}
              </div>
              <div
                className={`text-xs mt-0.5 ${
                  isDarkMode ? 'text-gray-400' : 'text-gray-600'
                }`}
              >
                {upcomingDepartures[0].direction}
              </div>
            </div>
            <div
              className={`text-right px-3 py-2 rounded-lg ${
                isDarkMode ? 'bg-marprom-700/50' : 'bg-marprom-100'
              }`}
            >
              <div
                className={`text-2xl font-bold ${
                  isDarkMode ? 'text-marprom-200' : 'text-marprom-700'
                }`}
              >
                {formatMinutesUntil(upcomingDepartures[0].minutesUntil)}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Upcoming Departures */}
      {upcomingDepartures.length > 0 && (
        <div>
          <div className="flex items-center gap-2 mb-3">
            <Clock
              className={`w-4 h-4 ${isDarkMode ? 'text-gray-400' : 'text-gray-600'}`}
            />
            <h4
              className={`text-sm font-semibold ${
                isDarkMode ? 'text-gray-300' : 'text-gray-700'
              }`}
            >
              Upcoming ({upcomingDepartures.length})
            </h4>
          </div>
          <div className="space-y-2 max-h-[400px] overflow-y-auto">
            {upcomingDepartures.map((dep, index) => (
              <div
                key={`${dep.line}-${dep.time}-${index}`}
                className={`p-3 rounded-lg border transition-all ${
                  dep.isNext
                    ? 'hidden' // Already shown in highlight section
                    : isDarkMode
                      ? 'bg-gray-800/50 border-gray-700/50 hover:bg-gray-800'
                      : 'bg-white border-gray-200 hover:bg-gray-50'
                }`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-baseline gap-2">
                      <span
                        className={`text-lg font-bold ${
                          isDarkMode ? 'text-white' : 'text-gray-900'
                        }`}
                      >
                        {dep.time}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded text-xs font-medium ${
                          isDarkMode
                            ? 'bg-blue-900/50 text-blue-300'
                            : 'bg-blue-100 text-blue-700'
                        }`}
                      >
                        Line {dep.line}
                      </span>
                    </div>
                    <div
                      className={`text-sm mt-1 ${
                        isDarkMode ? 'text-gray-400' : 'text-gray-600'
                      }`}
                    >
                      {dep.direction}
                    </div>
                  </div>
                  <div
                    className={`text-right px-2 py-1 rounded ${
                      isDarkMode ? 'bg-gray-700/50' : 'bg-gray-100'
                    }`}
                  >
                    <div
                      className={`text-sm font-semibold ${
                        dep.minutesUntil <= 5
                          ? isDarkMode
                            ? 'text-amber-400'
                            : 'text-amber-600'
                          : isDarkMode
                            ? 'text-gray-300'
                            : 'text-gray-700'
                      }`}
                    >
                      {formatMinutesUntil(dep.minutesUntil)}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Past Departures (Collapsible) */}
      {pastDepartures.length > 0 && (
        <details className="group">
          <summary
            className={`cursor-pointer p-3 rounded-lg border list-none ${
              isDarkMode
                ? 'bg-gray-800/30 border-gray-700/50 hover:bg-gray-800/50'
                : 'bg-gray-50 border-gray-200 hover:bg-gray-100'
            }`}
          >
            <div className="flex items-center gap-2">
              <Calendar
                className={`w-4 h-4 ${isDarkMode ? 'text-gray-500' : 'text-gray-400'}`}
              />
              <span
                className={`text-sm font-medium ${
                  isDarkMode ? 'text-gray-400' : 'text-gray-600'
                }`}
              >
                Earlier Departures ({pastDepartures.length})
              </span>
              <svg
                className={`w-4 h-4 ml-auto transition-transform group-open:rotate-180 ${
                  isDarkMode ? 'text-gray-500' : 'text-gray-400'
                }`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M19 9l-7 7-7-7"
                />
              </svg>
            </div>
          </summary>
          <div className="mt-2 space-y-1 max-h-[200px] overflow-y-auto pl-2">
            {pastDepartures.map((dep, index) => (
              <div
                key={`${dep.line}-${dep.time}-past-${index}`}
                className={`p-2 rounded text-sm opacity-60 ${
                  isDarkMode ? 'text-gray-500' : 'text-gray-500'
                }`}
              >
                <span className="font-mono">{dep.time}</span>
                <span className="mx-2">•</span>
                <span>Line {dep.line}</span>
                <span className="mx-2">•</span>
                <span className="text-xs">{dep.direction}</span>
              </div>
            ))}
          </div>
        </details>
      )}

      {upcomingDepartures.length === 0 && pastDepartures.length === 0 && (
        <div
          className={`text-center py-8 ${
            isDarkMode ? 'text-gray-500' : 'text-gray-400'
          }`}
        >
          <Clock className="w-12 h-12 mx-auto mb-2 opacity-50" />
          <p className="text-sm">No departure information available</p>
        </div>
      )}
    </div>
  );
}
