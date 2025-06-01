import { Link } from 'react-router-dom';
import { Bus, Bell } from 'lucide-react';

const DashboardHeader = () => {
  return (
    <header className="bg-white shadow-sm">
      <div className="px-4 sm:px-6 lg:px-8 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <Link to="/" className="flex items-center">
            <Bus className="h-8 w-8 text-mbusi-red-600" />
            <span className="ml-2 text-2xl font-bold text-mbusi-red-600">M-busi</span>
          </Link>
          </div>
          
          <div className="flex items-center space-x-2 sm:space-x-4">
            <button className="relative p-2 text-gray-600 hover:text-mbusi-red-600 transition-colors">
              <Bell size={20} />
              <span className="absolute top-0 right-0 h-2 w-2 bg-mbusi-red-600 rounded-full"></span>
            </button>
            
            <div className="flex items-center space-x-2 sm:space-x-3">
              <div className="h-8 w-8 rounded-full bg-mbusi-red-100 flex items-center justify-center">
                <span className="text-sm font-medium text-mbusi-red-600">JD</span>
              </div>
              <span className="hidden sm:inline text-sm font-medium text-gray-700">John Doe</span>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};

export default DashboardHeader;