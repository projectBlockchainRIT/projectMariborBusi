import { useState, useEffect } from "react";
import { MagnifyingGlassIcon, CalendarIcon } from "@heroicons/react/24/outline";
import { MapPinIcon } from "@heroicons/react/24/solid";
import type { Route } from "../types";
import mapboxgl from "mapbox-gl";
import { drawRouteOnMap } from "../utils/drawRouteOnMap";
import { useTheme } from "../context/ThemeContext";
import { getApiUrl } from "../config/api";
import AnimationControls from "./occupancy/AnimationControls";
import OccupancyDataDisplay, {
  OccupancyLegend,
} from "./occupancy/OccupancyDataDisplay";

interface OccupancyControllerProps {
  onRouteSelect: (route: Route) => void;
  mapInstance: mapboxgl.Map | null;
}

const PRESET_HOURS = [8, 10, 12, 16, 18];

const formatDateToYYYYMMDD = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

export default function OccupancyController({
  onRouteSelect,
  mapInstance,
}: OccupancyControllerProps) {
  const [routes, setRoutes] = useState<Route[]>([]);
  const [selectedRouteId, setSelectedRouteId] = useState<number | null>(null);
  const [routesLoading, setRoutesLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const { isDarkMode } = useTheme();
  const [showDateSpan, setShowDateSpan] = useState(false);
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [occupancyData, setOccupancyData] = useState<{
    [date: string]: { [hour: number]: number | null };
  }>({});
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [loadingOccupancy, setLoadingOccupancy] = useState(false);
  const [dateTabs, setDateTabs] = useState<string[]>([]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentHourIdx, setCurrentHourIdx] = useState<number | null>(null);
  const [animationSpeed, setAnimationSpeed] = useState(1);
  const [currentDayIdx, setCurrentDayIdx] = useState<number>(0);

  // Load routes
  useEffect(() => {
    const loadRoutes = async () => {
      try {
        setRoutesLoading(true);
        const response = await fetch(getApiUrl("routes/list"));
        if (!response.ok) throw new Error(`Failed to fetch routes`);
        const data = await response.json();
        setRoutes(Array.isArray(data) ? data : data.data || data.routes || []);
      } catch (error) {
        console.error("Error loading routes:", error);
      } finally {
        setRoutesLoading(false);
      }
    };
    loadRoutes();
  }, []);

  const fetchOccupancyData = async (
    lineId: number,
    date: string,
    hour: number,
  ) => {
    try {
      const response = await fetch(
        getApiUrl(`occupancy/line/${lineId}/date/${date}/hour/${hour}`),
      );
      if (!response.ok) return null;
      const data = await response.json();
      if (Array.isArray(data?.data)) {
        const hourStr = hour.toString().padStart(2, "0");
        const found = data.data.find((entry: any) =>
          entry.Time?.includes(`T${hourStr}:`),
        );
        return found?.OccupancyLevel ?? null;
      }
      return data?.data?.OccupancyLevel ?? null;
    } catch {
      return null;
    }
  };

  const getDatesInRange = (start: string, end: string): string[] => {
    const dates: string[] = [];
    const currentDate = new Date(start);
    const endDateObj = new Date(end);
    while (currentDate <= endDateObj) {
      dates.push(formatDateToYYYYMMDD(currentDate));
      currentDate.setDate(currentDate.getDate() + 1);
    }
    return dates;
  };

  const handleRouteClick = async (route: Route) => {
    const routeId = route.id;
    setSelectedRouteId(routeId);
    onRouteSelect(route);
    setLoadingOccupancy(true);

    const datesToFetch =
      showDateSpan && startDate && endDate
        ? getDatesInRange(startDate, endDate)
        : [formatDateToYYYYMMDD(new Date())];

    setDateTabs(datesToFetch);
    setSelectedDate(datesToFetch[0]);

    const newData: { [date: string]: { [hour: number]: number | null } } = {};
    for (const date of datesToFetch) {
      newData[date] = {};
      for (const hour of PRESET_HOURS) {
        newData[date][hour] = await fetchOccupancyData(routeId, date, hour);
      }
    }
    setOccupancyData(newData);
    setLoadingOccupancy(false);

    if (mapInstance) {
      await drawRouteOnMap(mapInstance, route, { setStatus: () => {} });
    }
  };

  const filteredRoutes = routes.filter((route) =>
    route.name.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  useEffect(() => {
    if (!showDateSpan && selectedRouteId) {
      const fetchToday = async () => {
        setLoadingOccupancy(true);
        const today = formatDateToYYYYMMDD(new Date());
        setDateTabs([today]);
        setSelectedDate(today);
        const newData: { [date: string]: { [hour: number]: number | null } } =
          {};
        newData[today] = {};
        for (const hour of PRESET_HOURS) {
          newData[today][hour] = await fetchOccupancyData(
            selectedRouteId,
            today,
            hour,
          );
        }
        setOccupancyData(newData);
        setLoadingOccupancy(false);
      };
      fetchToday();
    }
  }, [showDateSpan, selectedRouteId]);

  // Reset animation when route/dates change
  useEffect(() => {
    setIsPlaying(false);
    setCurrentHourIdx(null);
    setCurrentDayIdx(0);
  }, [selectedRouteId, dateTabs]);

  // Animation effect
  useEffect(() => {
    if (currentHourIdx === null || dateTabs.length === 0) return;

    // Update route color on map
    if (mapInstance && selectedRouteId && dateTabs[currentDayIdx]) {
      const hour = PRESET_HOURS[currentHourIdx];
      const occ = occupancyData[dateTabs[currentDayIdx]]?.[hour];
      let color = "#60A5FA";
      if (occ !== undefined && occ !== null) {
        if (occ < 1) color = "#10b981";
        else if (occ < 2) color = "#eab308";
        else if (occ < 3) color = "#f97316";
        else color = "#ef4444";
      }
      try {
        mapInstance.setPaintProperty(
          `route-${selectedRouteId}-line`,
          "line-color",
          color,
        );
      } catch {}
    }

    if (!isPlaying) return;

    const isLastHour = currentHourIdx >= PRESET_HOURS.length - 1;
    const isLastDay = currentDayIdx >= dateTabs.length - 1;

    if (isLastHour && isLastDay) {
      setIsPlaying(false);
      return;
    }

    const timer = setTimeout(() => {
      if (isLastHour) {
        setCurrentDayIdx((idx) => idx + 1);
        setCurrentHourIdx(0);
      } else {
        setCurrentHourIdx((idx) => (idx !== null ? idx + 1 : 0));
      }
    }, animationSpeed * 1000);

    return () => clearTimeout(timer);
  }, [
    isPlaying,
    currentHourIdx,
    currentDayIdx,
    animationSpeed,
    mapInstance,
    selectedRouteId,
    occupancyData,
    dateTabs,
  ]);

  const handlePlay = () => {
    const isAtEnd =
      currentHourIdx === PRESET_HOURS.length - 1 &&
      currentDayIdx === dateTabs.length - 1;
    if (isAtEnd || currentHourIdx === null) {
      setCurrentDayIdx(0);
      setCurrentHourIdx(0);
    }
    setIsPlaying(true);
  };

  const displayDate =
    isPlaying && dateTabs.length > 0 ? dateTabs[currentDayIdx] : selectedDate;

  // Keep route rendered on map
  useEffect(() => {
    if (!mapInstance || !selectedRouteId) return;
    const route = routes.find((r) => r.id === selectedRouteId);
    if (route) {
      drawRouteOnMap(mapInstance, route, { setStatus: () => {} });
    }
  }, [mapInstance, selectedRouteId, routes]);

  const selectedRoute = routes.find((r) => r.id === selectedRouteId);

  return (
    <div
      className={`h-full flex flex-col transition-colors duration-200 ${
        isDarkMode ? "bg-slate-900 text-white" : "bg-white text-slate-900"
      }`}
    >
      {/* Header */}
      <div
        className={`px-5 py-4 border-b ${isDarkMode ? "border-slate-800" : "border-slate-200"}`}
      >
        <h2 className="text-lg font-semibold tracking-tight">
          Route Occupancy
        </h2>
        <p
          className={`text-sm mt-0.5 ${isDarkMode ? "text-slate-400" : "text-slate-500"}`}
        >
          View passenger density by route
        </p>
      </div>

      {/* Search */}
      <div
        className={`px-4 py-3 border-b ${isDarkMode ? "border-slate-800" : "border-slate-200"}`}
      >
        <div className="relative">
          <MagnifyingGlassIcon
            className={`absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 ${
              isDarkMode ? "text-slate-500" : "text-slate-400"
            }`}
          />
          <input
            type="text"
            placeholder="Search routes..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className={`w-full pl-9 pr-4 py-2 text-sm rounded-lg border transition-colors ${
              isDarkMode
                ? "bg-slate-800 border-slate-700 text-white placeholder-slate-500 focus:border-blue-500"
                : "bg-slate-50 border-slate-200 text-slate-900 placeholder-slate-400 focus:border-blue-500"
            } focus:outline-none focus:ring-1 focus:ring-blue-500/30`}
          />
        </div>
      </div>

      {/* Date Range Toggle */}
      <div
        className={`px-4 py-3 border-b ${isDarkMode ? "border-slate-800" : "border-slate-200"}`}
      >
        <button
          onClick={() => setShowDateSpan((prev) => !prev)}
          className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
            showDateSpan
              ? "bg-marprom-600/10 text-marprom-600 border border-marprom-600/30"
              : isDarkMode
                ? "bg-slate-800 text-slate-400 hover:text-white"
                : "bg-slate-50 text-slate-600 hover:text-slate-900"
          }`}
        >
          <span className="flex items-center gap-2">
            <CalendarIcon className="w-4 h-4" />
            Date Range
          </span>
          <span
            className={`text-xs px-2 py-0.5 rounded-full ${
              showDateSpan
                ? "bg-marprom-600/20 text-marprom-600"
                : isDarkMode
                  ? "bg-slate-700 text-slate-500"
                  : "bg-slate-200 text-slate-500"
            }`}
          >
            {showDateSpan ? "On" : "Off"}
          </span>
        </button>

        {showDateSpan && (
          <div className="flex gap-1.5 mt-3">
            <div className="flex-1 min-w-0">
              <label
                className={`text-xs font-medium mb-1 block ${isDarkMode ? "text-slate-500" : "text-slate-500"}`}
              >
                From
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className={`w-full px-2 py-1.5 text-xs rounded-lg border ${
                  isDarkMode
                    ? "bg-slate-800 border-slate-700 text-white"
                    : "bg-white border-slate-200 text-slate-900"
                } focus:outline-none focus:ring-1 focus:ring-blue-500/30`}
              />
            </div>
            <div className="flex-1 min-w-0">
              <label
                className={`text-xs font-medium mb-1 block ${isDarkMode ? "text-slate-500" : "text-slate-500"}`}
              >
                To
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className={`w-full px-2 py-1.5 text-xs rounded-lg border ${
                  isDarkMode
                    ? "bg-slate-800 border-slate-700 text-white"
                    : "bg-white border-slate-200 text-slate-900"
                } focus:outline-none focus:ring-1 focus:ring-blue-500/30`}
              />
            </div>
          </div>
        )}
      </div>

      {/* Selected Route Info & Data Display */}
      {selectedRouteId && displayDate && (
        <>
          <div
            className={`px-4 py-3 border-b ${isDarkMode ? "border-slate-800" : "border-slate-200"}`}
          >
            <div
              className={`flex items-center gap-2 px-3 py-2 rounded-lg ${
                isDarkMode ? "bg-blue-500/10" : "bg-blue-50"
              }`}
            >
              <MapPinIcon className="w-4 h-4 text-blue-500" />
              <span className="text-sm font-medium text-blue-500 truncate">
                {selectedRoute?.name}
              </span>
            </div>
          </div>

          <OccupancyDataDisplay
            dateTabs={dateTabs}
            displayDate={displayDate}
            onDateSelect={setSelectedDate}
            presetHours={PRESET_HOURS}
            occupancyData={occupancyData}
            currentHourIdx={currentHourIdx}
            loading={loadingOccupancy}
          />

          {/* Animation Controls */}
          <div
            className={`px-4 py-3 border-b ${isDarkMode ? "border-slate-800" : "border-slate-200"}`}
          >
            <div
              className={`text-xs font-medium mb-2 ${isDarkMode ? "text-slate-500" : "text-slate-500"}`}
            >
              Animation
            </div>
            <AnimationControls
              isPlaying={isPlaying}
              onPlay={handlePlay}
              onPause={() => setIsPlaying(false)}
              animationSpeed={animationSpeed}
              onSpeedChange={setAnimationSpeed}
              disabled={loadingOccupancy}
            />
          </div>
        </>
      )}

      {/* Routes List */}
      <div className="flex-1 overflow-y-auto p-4">
        {routesLoading ? (
          <div className="space-y-2">
            {[...Array(5)].map((_, i) => (
              <div
                key={i}
                className={`h-11 rounded-lg animate-pulse ${isDarkMode ? "bg-slate-800" : "bg-slate-100"}`}
              />
            ))}
          </div>
        ) : filteredRoutes.length === 0 ? (
          <div
            className={`text-center py-8 ${isDarkMode ? "text-slate-500" : "text-slate-400"}`}
          >
            <MagnifyingGlassIcon className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No routes found</p>
          </div>
        ) : (
          <div className="space-y-1.5">
            {filteredRoutes.map((route) => {
              const isSelected = selectedRouteId === route.id;
              return (
                <button
                  key={`route-${route.id}`}
                  onClick={() => handleRouteClick(route)}
                  className={`w-full px-3 py-2.5 text-left rounded-lg transition-all duration-200 flex items-center gap-3 ${
                    isSelected
                      ? "bg-marprom-600 text-white"
                      : isDarkMode
                        ? "bg-slate-800/50 hover:bg-slate-800 text-slate-300"
                        : "bg-slate-50 hover:bg-slate-100 text-slate-700"
                  }`}
                >
                  <div
                    className={`w-7 h-7 rounded-md flex items-center justify-center ${
                      isSelected
                        ? "bg-white/20"
                        : isDarkMode
                          ? "bg-slate-700"
                          : "bg-slate-200"
                    }`}
                  >
                    <MapPinIcon
                      className={`w-3.5 h-3.5 ${isSelected ? "text-white" : "text-marprom-600"}`}
                    />
                  </div>
                  <span className="font-medium text-sm truncate">
                    {route.name}
                  </span>
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Legend at bottom */}
      <OccupancyLegend />
    </div>
  );
}
