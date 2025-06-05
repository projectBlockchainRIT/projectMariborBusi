import MapboxMap from './MapboxMap';

export default function InteractiveMap() {
  return (
    <div className="w-full h-full bg-white dark:bg-gray-800 rounded-lg shadow-lg">
      <MapboxMap isInteractive={true} />
    </div>
  );
} 