import axios, { AxiosError } from 'axios';

// Types
export interface BusStop {
  id: number;
  latitude: number;
  longitude: number;
  name: string;
  number: string;
}

export interface BusStopsByLine {
  [lineId: string]: BusStop[];
}

export interface FetchBusStopsOptions {
  signal?: AbortSignal;
  onLoadingChange?: (isLoading: boolean) => void;
}

// API configuration
const API_BASE_URL = 'http://localhost:8080/v1';

// Error types
export class BusStopsError extends Error {
  constructor(message: string, public lineId?: string) {
    super(message);
    this.name = 'BusStopsError';
  }
}

/**
 * Fetches bus stops for multiple lines in parallel
 * @param lineIds Array of line IDs to fetch stops for
 * @param options Optional configuration including AbortController signal and loading state callback
 * @returns Promise resolving to an object mapping line IDs to their stops
 */
export async function fetchBusStopsByLines(
  lineIds: string[],
  options: FetchBusStopsOptions = {}
): Promise<BusStopsByLine> {
  const { signal, onLoadingChange } = options;

  try {
    onLoadingChange?.(true);

    const promises = lineIds.map(async (lineId) => {
      try {
        const response = await axios.get<BusStop[]>(`${API_BASE_URL}/routes/stations/${lineId}`, {
          signal,
          timeout: 5000, // 5 second timeout
        });
        return { lineId, stops: response.data };
      } catch (err) {
        const error = err as AxiosError;
        if (error.code === 'ERR_CANCELED') {
          throw new BusStopsError('Request was cancelled', lineId);
        }
        if (error.code === 'ECONNABORTED') {
          throw new BusStopsError('Request timed out', lineId);
        }
        throw new BusStopsError(
          `Failed to fetch stops for line ${lineId}: ${error.message}`,
          lineId
        );
      }
    });

    const results = await Promise.all(promises);

    // Group stops by line ID
    const stopsByLine = results.reduce<BusStopsByLine>((acc, { lineId, stops }) => {
      acc[lineId] = stops;
      return acc;
    }, {});

    return stopsByLine;
  } catch (err) {
    if (err instanceof BusStopsError) {
      throw err;
    }
    throw new BusStopsError('Failed to fetch bus stops');
  } finally {
    onLoadingChange?.(false);
  }
}

/**
 * Example usage in a React component:
 * 
 * ```tsx
 * const BusStopsComponent = () => {
 *   const [stops, setStops] = useState<BusStopsByLine>({});
 *   const [isLoading, setIsLoading] = useState(false);
 *   const [error, setError] = useState<BusStopsError | null>(null);
 * 
 *   useEffect(() => {
 *     const controller = new AbortController();
 * 
 *     const fetchStops = async () => {
 *       try {
 *         const result = await fetchBusStopsByLines(['1', '2', '3'], {
 *           signal: controller.signal,
 *           onLoadingChange: setIsLoading
 *         });
 *         setStops(result);
 *       } catch (err) {
 *         if (err instanceof BusStopsError) {
 *           setError(err);
 *         }
 *       }
 *     };
 * 
 *     fetchStops();
 * 
 *     return () => controller.abort();
 *   }, []);
 * 
 *   if (isLoading) return <div>Loading...</div>;
 *   if (error) return <div>Error: {error.message}</div>;
 * 
 *   return (
 *     <div>
 *       {Object.entries(stops).map(([lineId, lineStops]) => (
 *         <div key={lineId}>
 *           <h3>Line {lineId}</h3>
 *           <ul>
 *             {lineStops.map(stop => (
 *               <li key={stop.id}>{stop.name}</li>
 *             ))}
 *           </ul>
 *         </div>
 *       ))}
 *     </div>
 *   );
 * };
 * ```
 */ 