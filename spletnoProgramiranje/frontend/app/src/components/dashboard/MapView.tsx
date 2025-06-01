import React from 'react';
import { MapPin } from 'lucide-react';

const MapView = () => {
  return (
    <div className="relative h-full bg-gray-200 rounded-lg m-2 sm:m-4">
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="text-center px-4">
          <MapPin className="h-8 w-8 sm:h-12 sm:w-12 text-mbusi-red-600 mx-auto mb-2 sm:mb-4" />
          <p className="text-gray-600 text-sm sm:text-base">Google Maps will be integrated here</p>
        </div>
      </div>
    </div>
  );
};

export default MapView;