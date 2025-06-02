import React, { useState } from 'react';
import { Menu, X, LogOut, Search } from 'lucide-react';
import MapView from './MapView';
import BusStopSidebar from './BusStopSidebar';
import MapHeader from './MapHeader';

const Map = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      <MapHeader />
      
      <div className="flex-1 flex overflow-hidden">
        <BusStopSidebar isOpen={isSidebarOpen} onClose={() => setIsSidebarOpen(false)} />
        
        <main className="flex-1 flex flex-col overflow-hidden">
          <div className="p-4 flex items-center justify-between bg-white shadow-sm">
            <button
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="p-2 rounded-lg hover:bg-gray-100 transition-colors lg:hidden"
              aria-label="Toggle sidebar"
            >
              {isSidebarOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
            
            <div className="flex-1 max-w-xl mx-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  placeholder="Search for bus stops..."
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-mbusi-red-600 focus:border-transparent"
                />
              </div>
            </div>

            <button
              onClick={() => {/* Handle logout */}}
              className="flex items-center space-x-2 text-gray-600 hover:text-mbusi-red-600 transition-colors"
            >
              <LogOut size={20} />
              <span className="hidden sm:inline">Logout</span>
            </button>
          </div>

          <div className="h-screen w-screen sm:p-3 bg-gray-100">
            <MapView />
          </div>
        </main>
      </div>
    </div>
  );
};

export default Map;