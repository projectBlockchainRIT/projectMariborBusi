// Components/Layout/InteractiveMapControls.tsx
import mapboxgl from 'mapbox-gl';

interface InteractiveMapControlsProps {
  map: mapboxgl.Map | null;
}

export default function InteractiveMapControls({ map }: InteractiveMapControlsProps) {
  return (
    <div className="w-64 bg-white dark:bg-gray-800 shadow-lg border-l border-gray-200 dark:border-gray-700">
      <div className="p-4">
        <h2 className="text-lg font-semibold text-gray-800 dark:text-white mb-4">
          Interactive Map Controls
        </h2>
        {/* Tu vstaviš dejanske kontrolnike (checkboxi, filtri, layer toggle, ipd.) */}
        <div className="text-gray-500 dark:text-gray-400">
          Interactive map controls will be implemented here
        </div>
      </div>
    </div>
  );
}