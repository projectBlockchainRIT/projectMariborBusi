import React from 'react';
import { MapPin, Clock, X } from 'lucide-react';

const busStops = [
  { id: 1, name: "Glavna postaja", nextBus: "5 min", status: "on-time" },
  { id: 2, name: "Koroška cesta", nextBus: "2 min", status: "delayed" },
  { id: 3, name: "Tabor", nextBus: "10 min", status: "on-time" },
  { id: 4, name: "Studenci", nextBus: "7 min", status: "on-time" },
  { id: 5, name: "Tezno", nextBus: "15 min", status: "on-time" },
  { id: 6, name: "Pobrežje", nextBus: "3 min", status: "on-time" },
  { id: 7, name: "Gosposvetska cesta", nextBus: "12 min", status: "delayed" },
  { id: 8, name: "Magdalenski park", nextBus: "8 min", status: "on-time" },
];

interface BusStopSidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

const BusStopSidebar = ({ isOpen, onClose }: BusStopSidebarProps) => {
  return (
    <>
      {/* Overlay for mobile */}
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
            {busStops.map((stop) => (
              <div
                key={stop.id}
                className="p-4 border-b border-gray-100 hover:bg-gray-50 cursor-pointer transition-colors"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <MapPin className="h-5 w-5 text-mbusi-red-600 mt-1" />
                    <div>
                      <h3 className="font-medium text-gray-900">{stop.name}</h3>
                      <div className="flex items-center mt-1">
                        <Clock className="h-4 w-4 text-gray-400 mr-1" />
                        <span className="text-sm text-gray-500">Next bus in {stop.nextBus}</span>
                      </div>
                    </div>
                  </div>
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-medium ${
                      stop.status === 'on-time'
                        ? 'bg-green-100 text-green-800'
                        : 'bg-yellow-100 text-yellow-800'
                    }`}
                  >
                    {stop.status === 'on-time' ? 'On Time' : 'Delayed'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </aside>
    </>
  );
};

export default BusStopSidebar;