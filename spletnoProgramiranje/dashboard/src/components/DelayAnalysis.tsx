import { useState, useEffect } from 'react';
import { Bar } from 'react-chartjs-2';
import { Clock, TrendingUp, AlertCircle, Calendar, MapPin, User } from 'lucide-react';
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

// Register ChartJS components
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

interface Route {
  id: string;
  name: string;
  color?: string;
}

interface RouteDelay {
  routeId: string;
  routeName: string;
  averageDelay: number;
}

// Updated interface for recent delays matching the actual API response
interface RecentDelay {
  ID: number;
  Date: string;         // "2025-06-29T00:00:00Z"
  DelayMin: number;     // The actual delay in minutes
  LineCode: string;     // "G3"
  LineID: number;       // 21
  StopID: number;       // 226
  StopName: string;     // "Novak"
  UserID: { Int64: number; Valid: boolean };
  Username: { String: string; Valid: boolean };
}

export default function DelayAnalysis() {
  const [timeRange, setTimeRange] = useState('week');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [averageDelay, setAverageDelay] = useState<number | null>(null);
  
  // State for routes and their delays
  const [routes, setRoutes] = useState<Route[]>([]);
  const [routeDelays, setRouteDelays] = useState<RouteDelay[]>([]);
  const [systemAverageDelay, setSystemAverageDelay] = useState<number>(0);
  const [highestDelay, setHighestDelay] = useState<RouteDelay | null>(null);
  
  // State for recent delays
  const [recentDelays, setRecentDelays] = useState<RecentDelay[]>([]);

  // Fetch all routes and their delays
  useEffect(() => {
    const fetchAllRouteDelays = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Step 1: Fetch all routes
        const routesResponse = await fetch('http://40.68.198.73:8080/v1/routes/list', {
          headers: { 'Accept': 'application/json' }
        });

        if (!routesResponse.ok) {
          throw new Error(`Failed to fetch routes: ${routesResponse.status}`);
        }

        const routesData = await routesResponse.json();
        const allRoutes = routesData.data || [];
        setRoutes(allRoutes);
        
        // Step 2: Fetch system-wide average delay
        const systemDelayResponse = await fetch('http://40.68.198.73:8080/v1/delays/average', {
          headers: { 'Accept': 'application/json' }
        });

        if (systemDelayResponse.ok) {
          const systemData = await systemDelayResponse.json();
          
          // Determine if the API returns a simple number or an object with AvgDelayMins
          const systemDelayValue = systemData.data && typeof systemData.data === 'object' && 'AvgDelayMins' in systemData.data
            ? systemData.data.AvgDelayMins
            : (typeof systemData.data === 'number' ? systemData.data : 0);
            
          setAverageDelay(Number(systemDelayValue));
          setSystemAverageDelay(Number(systemDelayValue));
        }
        
        // Step 3: Fetch recent delays
        const recentDelaysResponse = await fetch('http://40.68.198.73:8080/v1/delays/recent', {
          headers: { 'Accept': 'application/json' }
        });

        if (recentDelaysResponse.ok) {
          const recentData = await recentDelaysResponse.json();
          console.log('Recent delays data:', recentData);
          
          // The API returns { data: [...] }
          setRecentDelays(recentData.data || []);
        } else {
          console.warn(`Failed to fetch recent delays: ${recentDelaysResponse.status}`);
        }
        
        // Step 4: Fetch delay for each route
        const delayPromises = allRoutes.map(async (route: Route) => {
          try {
            const response = await fetch(`http://40.68.198.73:8080/v1/delays/average/${route.id}`, {
              headers: { 'Accept': 'application/json' }
            });
            
            if (response.ok) {
              const data = await response.json();
              console.log(`Delay data for route ${route.id}:`, data);
              
              // Extract the delay value from the correct path in the response
              // The API returns { data: { LineID: 14, LineCode: "P12", AvgDelayMins: 6.21 } }
              const delayValue = data.data && data.data.AvgDelayMins !== undefined 
                ? data.data.AvgDelayMins 
                : 0;
                
              return {
                routeId: route.id,
                routeName: route.name || `Line ${route.id}`,
                averageDelay: Number(delayValue) || 0
              };
            } else {
              console.warn(`Failed to fetch delay for route ${route.id}: ${response.status}`);
              return {
                routeId: route.id,
                routeName: route.name || `Line ${route.id}`,
                averageDelay: 0
              };
            }
          } catch (err) {
            console.error(`Error fetching delay for route ${route.id}:`, err);
            return {
              routeId: route.id,
              routeName: route.name || `Line ${route.id}`,
              averageDelay: 0
            };
          }
        });
        
        const delays = await Promise.all(delayPromises);
        
        // Sort by delay (descending)
        const sortedDelays = [...delays].sort((a, b) => b.averageDelay - a.averageDelay);
        
        setRouteDelays(sortedDelays);
        setHighestDelay(sortedDelays.length > 0 ? sortedDelays[0] : null);
        
      } catch (err) {
        console.error('Error fetching route delays:', err);
        setError(err instanceof Error ? err.message : 'Failed to load route delays');
      } finally {
        setLoading(false);
      }
    };

    fetchAllRouteDelays();
    
    // Refresh data periodically
    const interval = setInterval(fetchAllRouteDelays, 180000); // Every 3 minutes
    return () => clearInterval(interval);
  }, [timeRange]); // Re-fetch when time range changes
  
  // Dynamically build chart data based on route delays
  const delayByRouteData = {
    labels: routeDelays.map(route => route.routeName),
    datasets: [
      {
        label: 'Average Delay (minutes)',
        data: routeDelays.map(route => route.averageDelay),
        backgroundColor: routeDelays.map((_, i) => {
          const colors = [
            'rgba(59, 130, 246, 0.8)', // blue
            'rgba(16, 185, 129, 0.8)', // green
            'rgba(245, 158, 11, 0.8)', // orange 
            'rgba(239, 68, 68, 0.8)',  // red
            'rgba(139, 92, 246, 0.8)', // purple
          ];
          return colors[i % colors.length];
        }),
      },
    ],
  };

  const barOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'top' as const,
      },
      title: {
        display: true,
        text: 'Delays by Route',
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Average Delay (minutes)',
        },
      },
    },
  };

  // Calculate count of routes with significant delays (over 3 minutes)
  const routesWithSignificantDelays = routeDelays.filter(route => route.averageDelay > 3).length;

  // Format date for better display
  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleString('en-US', { 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  };

  // Stats cards with real data
  const stats = [
    {
      title: 'System Average Delay',
      value: averageDelay !== null ? `${Number(averageDelay).toFixed(1)} min` : 'N/A',
      icon: Clock,
      color: 'text-blue-500',
      bgColor: 'bg-blue-50 dark:bg-blue-900/50',
    },
    {
      title: 'Highest Route Delay',
      value: highestDelay ? `${Number(highestDelay.averageDelay).toFixed(1)} min` : 'N/A',
      icon: TrendingUp,
      color: 'text-orange-500',
      bgColor: 'bg-orange-50 dark:bg-orange-900/50',
      subtitle: highestDelay ? highestDelay.routeName : '',
    },
    {
      title: 'Routes with Delays',
      value: routesWithSignificantDelays.toString(),
      icon: AlertCircle,
      color: 'text-red-500',
      bgColor: 'bg-red-50 dark:bg-red-900/50',
      subtitle: `Out of ${routes.length} total routes`,
    },
  ];

  return (
    <div className="p-6 space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Delay Analysis</h1>
        <div className="flex space-x-2">
          <button
            onClick={() => setTimeRange('week')}
            className={`px-4 py-2 rounded-lg ${
              timeRange === 'week'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300'
            }`}
          >
            Week
          </button>
          <button
            onClick={() => setTimeRange('month')}
            className={`px-4 py-2 rounded-lg ${
              timeRange === 'month'
                ? 'bg-blue-600 text-white'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300'
            }`}
          >
            Month
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-100 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
        </div>
      ) : (
        <>
          {/* Stats Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {stats.map((stat, index) => (
              <div
                key={index}
                className={`p-6 rounded-xl ${stat.bgColor} flex items-center space-x-4`}
              >
                <div className={`p-3 rounded-lg ${stat.color} bg-white dark:bg-gray-800`}>
                  <stat.icon className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-gray-600 dark:text-gray-300">{stat.title}</p>
                  <p className="text-2xl font-semibold text-gray-900 dark:text-white">{stat.value}</p>
                  {stat.subtitle && (
                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{stat.subtitle}</p>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Charts and Recent Delays List */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Recent delays list */}
            <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm">
              <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center">
                <Clock className="h-5 w-5 mr-2 text-blue-500" />
                Recent Reported Delays
              </h2>
              
              {recentDelays.length > 0 ? (
                <div className="space-y-4 max-h-[350px] overflow-y-auto pr-2">
                  {recentDelays
                    .sort((a, b) => new Date(b.Date).getTime() - new Date(a.Date).getTime())
                    .map(delay => {
                      // Find matching route name
                      const routeName = routes.find(r => r.id === String(delay.LineID))?.name || delay.LineCode;
                      
                      return (
                        <div 
                          key={delay.ID}
                          className="border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                        >
                          <div className="flex items-start justify-between">
                            <div className="flex items-center">
                              <div className="flex-shrink-0 h-10 w-10 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center text-blue-600 dark:text-blue-400 font-semibold">
                                {routeName || delay.LineCode}
                              </div>
                              <div className="ml-3">
                                <p className="text-sm font-medium text-gray-900 dark:text-white">
                                  {delay.DelayMin} minute delay
                                </p>
                                <div className="flex items-center text-xs text-gray-500 dark:text-gray-400">
                                  <MapPin className="h-3 w-3 mr-1" />
                                  <span>{delay.StopName}</span>
                                </div>
                              </div>
                            </div>
                            <span className="text-xs text-gray-500 dark:text-gray-400">
                              {formatDate(delay.Date)}
                            </span>
                          </div>
                          
                          {delay.Username.Valid && (
                            <div className="mt-2 flex items-center text-xs text-gray-500 dark:text-gray-400">
                              <User className="h-3 w-3 mr-1" />
                              <span>Reported by {delay.Username.String}</span>
                            </div>
                          )}
                        </div>
                      );
                  })}
                </div>
              ) : (
                <div className="flex items-center justify-center h-[350px]">
                  <p className="text-gray-500 dark:text-gray-400">No recent delays reported</p>
                </div>
              )}
            </div>
            
            {/* Bar chart showing delays by route */}
            <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm h-full">
              <Bar options={barOptions} data={delayByRouteData} />
            </div>
          </div>

          {/* Additional Analysis */}
          <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
              Key Insights
            </h2>
            {/* Key Insights section */}
            <div className="space-y-4">
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0 w-2 h-2 mt-2 rounded-full bg-blue-500"></div>
                <p className="text-gray-600 dark:text-gray-300">
                  System-wide average delay is {averageDelay ? Number(averageDelay).toFixed(1) : 'N/A'} minutes
                </p>
              </div>
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0 w-2 h-2 mt-2 rounded-full bg-orange-500"></div>
                <p className="text-gray-600 dark:text-gray-300">
                  {highestDelay 
                    ? `${highestDelay.routeName} experiences the highest average delays (${
                        typeof highestDelay.averageDelay === 'number' 
                          ? highestDelay.averageDelay.toFixed(1) 
                          : Number(highestDelay.averageDelay).toFixed(1) || '0.0'
                      } minutes)`
                    : 'No route delay data available'}
                </p>
              </div>
              <div className="flex items-start space-x-3">
                <div className="flex-shrink-0 w-2 h-2 mt-2 rounded-full bg-green-500"></div>
                <p className="text-gray-600 dark:text-gray-300">
                  {recentDelays.length > 0
                    ? `${recentDelays.length} delays reported recently across ${new Set(recentDelays.map(d => d.LineID)).size} routes`
                    : 'No recent delay reports available'}
                </p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}