import { Link } from 'react-router-dom';
import { useUser } from '../context/UserContext';
import { useTheme } from '../context/ThemeContext';

const LandingPage = () => {
  const { isAuthenticated } = useUser();
  const { isDarkMode } = useTheme();

  return (
    <div className={`min-h-screen transition-colors duration-200 ${
      isDarkMode 
        ? 'bg-gray-900 text-white' 
        : 'bg-gradient-to-b from-blue-50 to-white text-gray-900'
    }`}>
      <div className="container mx-auto px-4 py-16">
        <div className="text-center">
          <h1 className="text-4xl md:text-6xl font-bold mb-8">
            Welcome to M-BUSI Dashboard
          </h1>
          <p className={`text-xl mb-12 max-w-2xl mx-auto ${
            isDarkMode ? 'text-gray-300' : 'text-gray-600'
          }`}>
            Explore real-time bus information, occupancy rates, and interactive maps to make your journey through Maribor smoother.
          </p>
          
          <div className="space-y-4">
            <Link
              to={isAuthenticated ? "/interactive-map" : "/login"}
              className={`inline-block px-8 py-4 rounded-lg text-lg font-semibold transition-colors ${
                isDarkMode 
                  ? 'bg-blue-600 hover:bg-blue-700 text-white' 
                  : 'bg-blue-600 hover:bg-blue-700 text-white'
              }`}
            >
              {isAuthenticated ? "Go to Interactive Map" : "Login to Access Map"}
            </Link>
            
            <div className="mt-8 grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className={`p-6 rounded-lg shadow-md ${
                isDarkMode 
                  ? 'bg-gray-800 text-gray-200' 
                  : 'bg-white text-gray-900'
              }`}>
                <h3 className="text-xl font-semibold mb-3">Real-time Updates</h3>
                <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>
                  Get live information about bus locations and schedules
                </p>
              </div>
              <div className={`p-6 rounded-lg shadow-md ${
                isDarkMode 
                  ? 'bg-gray-800 text-gray-200' 
                  : 'bg-white text-gray-900'
              }`}>
                <h3 className="text-xl font-semibold mb-3">Occupancy Tracking</h3>
                <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>
                  Check how crowded your bus is before boarding
                </p>
              </div>
              <div className={`p-6 rounded-lg shadow-md ${
                isDarkMode 
                  ? 'bg-gray-800 text-gray-200' 
                  : 'bg-white text-gray-900'
              }`}>
                <h3 className="text-xl font-semibold mb-3">Interactive Maps</h3>
                <p className={isDarkMode ? 'text-gray-300' : 'text-gray-600'}>
                  Explore routes and stops with our interactive map system
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LandingPage; 