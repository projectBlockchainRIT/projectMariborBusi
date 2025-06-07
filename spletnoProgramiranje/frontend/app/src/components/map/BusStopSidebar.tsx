import React, { useState, useEffect } from 'react';
import { MapPin, Clock, X } from 'lucide-react';

interface FetchedBusStop {
  id: number;
  number: string;
  name: string;
  latitude: number;
  longitude: number;
}

interface BusStopSidebarProps {
  isOpen: boolean; 
  onClose: () => void;
  onBusStopClick: (busStop: FetchedBusStop) => void;
}

const BusStopSidebar = ({ isOpen, onClose, onBusStopClick }: BusStopSidebarProps) => {
  const [busStops, setBusStops] = useState<FetchedBusStop[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true; 
    const controller = new AbortController();
    const signal = controller.signal;

    const fetchBusStops = async () => {
      setLoading(true);
      setError(null);   

      try {
        const response = await fetch('http://localhost:3000/v1/stations/list', {
          signal, 
        });

        if (!isMounted) return;

        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ message: 'Failed to parse error response' }));
          throw new Error(`HTTP error! Status: ${response.status} - ${errorData.message || response.statusText}`);
        }

        const result = await response.json();

        if (!isMounted) return;
        setBusStops(result.data);
      } catch (e: unknown) {
        if (!isMounted) return;

        if (e instanceof Error) {
          if (e.name === 'AbortError') {
            console.log('Fetch aborted by cleanup');
          } else {
            setError(`Failed to fetch bus stops: ${e.message}`);
          }
        } else {
          setError("An unknown error occurred during fetch.");
        }
      } finally {
        if (isMounted) {
          setLoading(false); 
        }
      }
    };

    fetchBusStops();

    return () => {
      isMounted = false; 
      controller.abort(); 
    };
  }, []); 

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-20 lg:hidden"
          onClick={onClose}
        ></div>
      )}

      <aside
        className={`w-full sm:w-80 bg-white border-r border-gray-200 transform transition-transform duration-300 ease-in-out ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:translate-x-0 fixed lg:relative z-30 h-full`}
      >
        <div className="h-full flex flex-col">
          <div className="p-4 border-b border-gray-200 flex justify-between items-center">
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Bus Stops</h2>
              <p className="text-sm text-gray-500">Real-time updates for all stops</p>
            </div>
            <button
              onClick={onClose}
              className="lg:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors"
            >
              <X size={20} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <p className="p-4 text-gray-600">Loading bus stops...</p>
            ) : error ? (
              <p className="p-4 text-red-500">Error: {error}</p>
            ) : busStops.length === 0 ? (
              <p className="p-4 text-gray-600">No bus stops found. Try again later.</p>
            ) : (
              busStops.map((stop) => (
                <div
                  key={stop.id}
                  className="p-4 border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => onBusStopClick(stop)} 
                >
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-3">
                      <MapPin className="h-5 w-5 text-mbusi-red-600 mt-1" />
                      <div>
                        <h3 className="font-medium text-gray-900">{stop.name}</h3>
                        <div className="flex items-center mt-1">
                          <Clock className="h-4 w-4 text-gray-400 mr-1" />
                          <span className="text-sm text-gray-500">Stop number: {stop.number}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </aside>
    </>
  );
};

export default BusStopSidebar;