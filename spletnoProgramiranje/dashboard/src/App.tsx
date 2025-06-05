import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import DashboardLayout from './components/layout/DashboardLayout';
import Map from './components/Map';
import InteractiveMap from './components/InteractiveMap';
import Login from './pages/Login';
import Register from './pages/Register';
import { UserProvider } from './context/UserContext';

function App() {


  return (
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
                  <Route path="/interactive-map" element={<InteractiveMap />} />
                  <Route path="/graphs" element={<div>Graphs</div>} />
                  <Route path="/settings" element={<div>Settings</div>} />
                  <Route path="/admin" element={<div>Admin Panel</div>} />
                  <Route path="/about" element={<div>About Us</div>} />
                </Routes>
              </DashboardLayout>
            }
          />
        </Routes>
      </Router>
    </UserProvider>
  );
}

export default App;
