import React from 'react';
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

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-gray-50">
        <Header />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/" element={
            <main>
              <Hero />
              <Features />
              <HowItWorks />
              <Reviews />
              <About />
              <CTA />
            </main>
          } />
        </Routes>
        <Footer />
      </div>
    </Router>
  );
}

export default App