import { Route, Routes } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import RealTimeSim from './pages/RealTimeSim';
import InteractiveMap from './pages/InteractiveMap';
import BusDensity from './pages/BusDensity';
import About from './pages/About';

const App = () => {
  return (
    <div className="flex min-h-screen">
      <Sidebar />

      {/* Glavna vsebina */}
      <div className="flex-1 p-8">
        <Routes>
          <Route path="/" element={<RealTimeSim />} />
          <Route path="/map" element={<InteractiveMap />} />
          <Route path="/density" element={<BusDensity />} />
          <Route path="/about" element={<About />} />
        </Routes>
      </div>
    </div>
  );
};

export default App;
