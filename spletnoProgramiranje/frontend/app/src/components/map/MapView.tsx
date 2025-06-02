import React, { useRef, useEffect, useState } from 'react';
import mapboxgl, { Map, NavigationControl, GeolocateControl, FullscreenControl, ScaleControl } from 'mapbox-gl';

mapboxgl.accessToken = 'pk.eyJ1IjoiYml0LWJhbmRpdCIsImEiOiJjbWJldzQyM28wNXRmMmlzaDhleWkwNXllIn0.CcdSzZ3I4zYYe4XXeUEItQ';

interface MapStyle {
  name: string;
  styleId: string;
  description: string;
}

interface TestLocation {
  name: string;
  coords: [number, number];
  zoom: number;
  description: string;
}

const TEST_LOCATIONS: TestLocation[] = [
  { name: "Innsbruck", coords: [11.4041, 47.2692], zoom: 12, description: "Alpine valley city" },
  { name: "San Francisco", coords: [-122.4194, 37.7749], zoom: 11, description: "Hills and bay" },
  { name: "Denver", coords: [-104.9903, 39.7392], zoom: 10, description: "Rocky Mountains" },
  { name: "Zermatt", coords: [7.7491, 46.0207], zoom: 13, description: "Near Matterhorn" },
  { name: "Salt Lake City", coords: [-111.8910, 40.7608], zoom: 10, description: "Wasatch Range" },
  { name: "Vancouver", coords: [-123.1207, 49.2827], zoom: 10, description: "Coast Mountains" },
  { name: "Rio de Janeiro", coords: [-43.1729, -22.9068], zoom: 11, description: "Coastal mountains" },
  { name: "Cape Town", coords: [18.4241, -33.9249], zoom: 11, description: "Table Mountain" }
];

const MAP_STYLES: MapStyle[] = [
  { name: 'Streets', styleId: 'mapbox://styles/mapbox/streets-v12', description: 'Default streets view' },
  { name: 'Light', styleId: 'mapbox://styles/mapbox/light-v11', description: 'Clean light theme' },
  { name: 'Dark', styleId: 'mapbox://styles/mapbox/dark-v11', description: 'Dark theme' },
  { name: 'Outdoors', styleId: 'mapbox://styles/mapbox/outdoors-v12', description: 'Outdoor/hiking style' },
  { name: 'Satellite', styleId: 'mapbox://styles/mapbox/satellite-v9', description: 'Pure satellite imagery' },
  { name: 'Satellite Streets', styleId: 'mapbox://styles/mapbox/satellite-streets-v12', description: 'Satellite with street labels' },
  { name: 'Navigation Day', styleId: 'mapbox://styles/mapbox/navigation-day-v1', description: 'Optimized for navigation' },
  { name: 'Navigation Night', styleId: 'mapbox://styles/mapbox/navigation-night-v1', description: 'Dark navigation theme' }
];

function MapView(): React.JSX.Element {
  const mapContainer = useRef<HTMLDivElement>(null);
  const map = useRef<Map | null>(null);

  const [lng, setLng] = useState<number>(15.6467);
  const [lat, setLat] = useState<number>(46.5547);
  const [zoom, setZoom] = useState<number>(10);
  const [pitch, setPitch] = useState<number>(0);
  const [bearing, setBearing] = useState<number>(0);
  const [currentStyle, setCurrentStyle] = useState<string>('Streets');
  const [is3DMode, setIs3DMode] = useState<boolean>(false);
  const [terrainEnabled, setTerrainEnabled] = useState<boolean>(false);
  const [elevation, setElevation] = useState<number | null>(null);

  useEffect(() => {
    if (map.current || !mapContainer.current) {
      return;
    }

    map.current = new mapboxgl.Map({
      container: mapContainer.current,
      style: MAP_STYLES[0].styleId,
      center: [lng, lat],
      zoom: zoom,
      pitch: pitch,
      bearing: bearing,
      projection: 'globe', 
      antialias: true 
    });

    map.current.addControl(new NavigationControl(), 'top-right');
    map.current.addControl(new GeolocateControl({
      positionOptions: {
        enableHighAccuracy: true
      },
      trackUserLocation: true,
      showUserHeading: true
    }), 'top-right');
    map.current.addControl(new FullscreenControl(), 'top-right');
    map.current.addControl(new ScaleControl(), 'bottom-left');


    map.current.on('move', () => {
      if (map.current) {
        setLng(parseFloat(map.current.getCenter().lng.toFixed(4)));
        setLat(parseFloat(map.current.getCenter().lat.toFixed(4)));
        setZoom(parseFloat(map.current.getZoom().toFixed(2)));
        setPitch(parseFloat(map.current.getPitch().toFixed(2)));
        setBearing(parseFloat(map.current.getBearing().toFixed(2)));
      }
    });

    map.current.on('click', (e) => {
      if (map.current && terrainEnabled) {
        const elevation = map.current.queryTerrainElevation(e.lngLat, { exaggerated: false });
        if (elevation !== null && elevation !== undefined) {
          setElevation(Math.round(elevation));
        } else {
          setElevation(null);
        }
      }
    });

    map.current.on('style.load', () => {
      if (map.current) {
        map.current.addLayer({
          'id': 'sky',
          'type': 'sky',
          'paint': {
            'sky-type': 'atmosphere',
            'sky-atmosphere-sun': [0.0, 0.0],
            'sky-atmosphere-sun-intensity': 15
          }
        });
      }
    });

    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []);

  const switchMapStyle = (style: MapStyle) => {
    if (map.current) {
      map.current.setStyle(style.styleId);
      setCurrentStyle(style.name);
    }
  };

  const toggle3DMode = () => {
    if (map.current) {
      const newPitch = is3DMode ? 0 : 60;
      const newBearing = is3DMode ? 0 : 20;
      
      map.current.flyTo({
        pitch: newPitch,
        bearing: newBearing,
        duration: 2000,
        essential: true
      });
      
      setIs3DMode(!is3DMode);
    }
  };

  const toggleTerrain = () => {
    if (map.current) {
      if (!terrainEnabled) {
        map.current.addSource('mapbox-dem', {
          'type': 'raster-dem',
          'url': 'mapbox://mapbox.terrain-rgb',
          'tileSize': 512,
          'maxzoom': 14
        } as any);
        
        map.current.setTerrain({ 'source': 'mapbox-dem', 'exaggeration': 1.5 });
      } else {
        map.current.setTerrain(null);
        if (map.current.getSource('mapbox-dem')) {
          map.current.removeSource('mapbox-dem');
        }
      }
      setTerrainEnabled(!terrainEnabled);
    }
  };

  const enableBirdseye = () => {
    if (map.current) {
      map.current.flyTo({
        center: [lng, lat],
        zoom: 15,
        pitch: 75,
        bearing: 0,
        duration: 3000,
        essential: true
      });
    }
  };

  const jumpToLocation = (location: TestLocation) => {
    if (map.current) {
      map.current.flyTo({
        center: location.coords,
        zoom: location.zoom,
        pitch: 60,
        bearing: 20,
        duration: 3000,
        essential: true
      });
      setIs3DMode(true);
      
      if (!terrainEnabled) {
        setTimeout(() => toggleTerrain(), 1000);
      }
    }
  };

  const resetView = () => {
    if (map.current) {
      map.current.flyTo({
        center: [15.6467, 46.5547],
        zoom: 10,
        pitch: 0,
        bearing: 0,
        duration: 2000,
        essential: true
      });
      setIs3DMode(false);
    }
  };

  return (
    <div className="relative w-full h-full bg-gray-200 rounded-lg overflow-hidden">
      <div 
        ref={mapContainer} 
        className="absolute inset-0 w-full h-full" 
      />
      
      <div className="absolute top-4 left-4 z-10">
        <div className="bg-gray-900 text-white p-3 rounded-lg bg-opacity-90 text-xs font-mono">
          <div>Style: {currentStyle}</div>
          <div>Lng: {lng} | Lat: {lat}</div>
          <div>Zoom: {zoom} | Pitch: {pitch}° | Bearing: {bearing}°</div>
          <div>3D: {is3DMode ? 'ON' : 'OFF'} | Terrain: {terrainEnabled ? 'ON' : 'OFF'}</div>
          {elevation !== null && (
            <div className="text-yellow-300 font-bold">Elevation: {elevation}m</div>
          )}
          {terrainEnabled && (
            <div className="text-blue-300 text-xs mt-1">Click map to get elevation</div>
          )}
        </div>
      </div>


      <div className="absolute top-4 right-4 z-10 max-w-xs">
        <div className="bg-white rounded-lg shadow-lg p-2 max-h-80 overflow-y-auto">
          <h3 className="text-sm font-bold mb-2 text-gray-800">Map Styles</h3>
          <div className="grid grid-cols-2 gap-1">
            {MAP_STYLES.map((style) => (
              <button
                key={style.name}
                onClick={() => switchMapStyle(style)}
                className={`p-2 text-xs rounded transition-colors ${
                  currentStyle === style.name
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-100 hover:bg-gray-200 text-gray-800'
                }`}
                title={style.description}
              >
                {style.name}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="absolute top-40 left-4 z-10 flex flex-wrap gap-2">
        <button
          onClick={toggle3DMode}
          className={`px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
            is3DMode 
              ? 'bg-purple-600 hover:bg-purple-700 text-white' 
              : 'bg-white hover:bg-gray-100 text-gray-800 border'
          }`}
        >
          {is3DMode ? '3D ON' : '3D OFF'}
        </button>
        
        <button
          onClick={toggleTerrain}
          className={`px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
            terrainEnabled 
              ? 'bg-green-600 hover:bg-green-700 text-white' 
              : 'bg-white hover:bg-gray-100 text-gray-800 border'
          }`}
        >
          {terrainEnabled ? 'Terrain ON' : 'Terrain OFF'}
        </button>
        
        <button
          onClick={enableBirdseye}
          className="bg-orange-500 hover:bg-orange-600 text-white px-3 py-2 text-sm font-medium rounded-lg transition-colors"
        >
          Bird's Eye
        </button>
        
        <button
          onClick={resetView}
          className="bg-gray-500 hover:bg-gray-600 text-white px-3 py-2 text-sm font-medium rounded-lg transition-colors"
        >
          Reset View
        </button>
      </div>
    </div>
  );
}

export default MapView;