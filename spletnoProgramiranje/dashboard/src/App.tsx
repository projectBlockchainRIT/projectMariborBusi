import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import DashboardLayout from './components/layout/DashboardLayout';
import Map from './components/Map';
import InteractiveMap from './components/InteractiveMap';
import Login from './pages/Login';
import Register from './pages/Register';
import Graphs from './pages/Graphs';
import AboutPage from './pages/AboutPage';
import { UserProvider } from './context/UserContext';
import { ThemeProvider } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
  return (
    <ThemeProvider>
      <UserProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route
              path="/*"
              element={
                <DashboardLayout>
                  <Routes>
                    <Route path="/" element={<Map />} />
                    <Route path="/interactive-map" element={
                      <ProtectedRoute>
                        <InteractiveMap />
                      </ProtectedRoute>
                    } />
                    <Route path="/graphs" element={
                      <ProtectedRoute>
                        <Graphs />
                      </ProtectedRoute>
                    } />
                    <Route path="/settings" element={
                      <ProtectedRoute>
                        <div>Settings</div>
                      </ProtectedRoute>
                    } />
                    <Route path="/admin" element={
                      <ProtectedRoute requireAdmin>
                        <div>Admin Panel</div>
                      </ProtectedRoute>
                    } />
                    <Route path="/about" element={<AboutPage />} />
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
