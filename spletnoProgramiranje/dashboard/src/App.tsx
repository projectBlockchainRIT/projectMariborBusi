import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import DashboardLayout from './components/layout/DashboardLayout';
import InteractiveMap from './components/InteractiveMap';
import Login from './pages/Login';
import Register from './pages/Register';
import Graphs from './pages/Graphs';
import AboutPage from './pages/AboutPage';
import OccupancyPage from './pages/OccupancyPage';
import DelaysPage from './pages/DelaysPage';
import LandingPage from './pages/LandingPage';
import SettingsPage from './pages/SettingsPage';
import { UserProvider } from './context/UserContext';
import { ThemeProvider } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <ThemeProvider>
      <UserProvider>
        <Router>
          <Routes>
            {/* Public routes */}
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/about" element={<AboutPage />} />

            {/* Protected routes */}
            <Route
              path="/dashboard/*"
              element={
                <DashboardLayout>
                  <Routes>
                    <Route path="/" element={<InteractiveMap />} />
                    <Route path="/interactive-map" element={
                      <ProtectedRoute>
                        <InteractiveMap />
                      </ProtectedRoute>
                    } />
                    <Route path="/occupancy" element={
                      <ProtectedRoute>
                        <OccupancyPage />
                      </ProtectedRoute>
                    } />
                    <Route path="/delays" element={
                      <ProtectedRoute>
                        <DelaysPage />
                      </ProtectedRoute>
                    } />
                    <Route path="/graphs" element={
                      <ProtectedRoute>
                        <Graphs />
                      </ProtectedRoute>
                    } />
                    <Route path="/settings" element={
                      <ProtectedRoute>
                        <SettingsPage />
                      </ProtectedRoute>
                    } />
                    <Route path="/admin" element={
                      <ProtectedRoute requireAdmin>
                        <div>Admin Panel</div>
                      </ProtectedRoute>
                    } />
                  </Routes>
                </DashboardLayout>
              }
            />
          </Routes>
        </Router>
      </UserProvider>
    </ThemeProvider>
  );
}

export default App;

