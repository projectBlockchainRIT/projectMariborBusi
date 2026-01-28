import { useRef, useState } from "react";
import { ChevronLeftIcon, ChevronRightIcon } from "@heroicons/react/24/outline";
import OccupancyMapBox from "../components/OccupancyMapBox";
import OccupancyController from "../components/OccupancyController";
import type { Route } from "../types";
import type { Map as MapboxMap } from "mapbox-gl";
import { useTheme } from "../context/ThemeContext";

export default function OccupancyPage() {
  const mapRef = useRef<MapboxMap | null>(null);
  const [, setSelectedRoute] = useState<Route | null>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const { isDarkMode } = useTheme();

  const legend = [
    { color: "#4ade80", label: "Low (Green)" },
    { color: "#fde047", label: "Moderate (Yellow)" },
    { color: "#fb923c", label: "High (Orange)" },
    { color: "#ef4444", label: "Very High (Red)" },
  ];

  const handleMapLoad = (map: MapboxMap) => {
    mapRef.current = map;
  };

  const handleRouteSelect = (route: Route) => {
    setSelectedRoute(route);
  };

  const toggleSidebar = () => {
    setIsSidebarOpen(!isSidebarOpen);
  };

  return (
    <div className="flex h-full relative">
      {/* Mobile toggle button */}
      <button
        onClick={toggleSidebar}
        className={`
          absolute top-4 z-20 md:hidden
          p-2 rounded-r-xl shadow-lg
          transition-all duration-300
          ${isSidebarOpen ? "left-80" : "left-0 rounded-l-xl"}
          ${
            isDarkMode
              ? "bg-slate-800 text-white hover:bg-slate-700"
              : "bg-white text-slate-700 hover:bg-slate-50"
          }
        `}
        aria-label={isSidebarOpen ? "Close sidebar" : "Open sidebar"}
      >
        {isSidebarOpen ? (
          <ChevronLeftIcon className="w-5 h-5" />
        ) : (
          <ChevronRightIcon className="w-5 h-5" />
        )}
      </button>

      {/* Sidebar */}
      <div
        className={`
          absolute md:relative z-10
          h-full
          transition-transform duration-300 ease-in-out
          ${isSidebarOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"}
          w-full md:w-80 flex-shrink-0
          max-w-80
        `}
      >
        <OccupancyController
          onRouteSelect={handleRouteSelect}
          mapInstance={mapRef.current}
        />
      </div>

      {/* Map */}
      <div className="flex-1 relative">
        <OccupancyMapBox onMapLoad={handleMapLoad} />
      </div>

      {/* Mobile overlay when sidebar is open */}
      {isSidebarOpen && (
        <div
          className="absolute inset-0 bg-black/30 z-5 md:hidden"
          onClick={toggleSidebar}
        />
      )}

      {/* Floating Occupancy Legend Window */}
      <div
        className={`fixed bottom-4 right-4 z-50 rounded-lg shadow-lg px-6 py-4 flex flex-col items-center min-w-[260px] border ${
          isDarkMode
            ? "bg-gray-800 text-gray-200 border-gray-700"
            : "bg-white text-gray-900 border-gray-200"
        }`}
        style={{ pointerEvents: "auto" }}
      >
        <div className="font-semibold mb-2 text-base">
          Route Occupancy Legend
        </div>
        <div className="flex flex-col gap-1 w-full mb-2">
          {legend.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2">
              <span
                className="inline-block w-6 h-3 rounded"
                style={{ background: item.color }}
              ></span>
              <span className="text-xs">{item.label}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
