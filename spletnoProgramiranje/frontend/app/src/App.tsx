import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Hero from './components/Hero';
import Features from './components/Features';
import HowItWorks from './components/HowItWorks';
import Reviews from './components/Reviews';
import About from './components/About';
import Footer from './components/Footer';
import CTA from './components/CTA';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import Map from './components/map/Map';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/map" element={<Map />} />
        <Route
          path="/"
          element={
            <div className="min-h-screen bg-gray-50">
              <Header />
              <main>
                <Hero />
                <Features />
                <HowItWorks />
                <Reviews />
                <About />
                <CTA />
              </main>
              <Footer />
            </div>
          }
        />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      </Routes>
    </Router>
  );
}

export default App;