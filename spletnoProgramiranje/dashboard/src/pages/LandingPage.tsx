import React from 'react';
import { Bus, MapPin, Clock, Route, Menu, X, Play } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';

const LandingPage = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = React.useState(false);
  const navigate = useNavigate();

  const handleLogin = () => {
    navigate('/login');
  };

  const handleGetStarted = () => {
    navigate('/register');
  };

  const handleWatchDemo = () => {
    // Open demo video in a modal or navigate to a demo page
    window.open('https://www.youtube.com/watch?v=dQw4w9WgXcQ', '_blank');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-teal-50">
      {/* Navigation */}
      <nav className="relative bg-white/80 backdrop-blur-md border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <div className="flex items-center space-x-2">
              <div className="bg-gradient-to-r from-blue-600 to-teal-600 p-2 rounded-lg">
                <Bus className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-teal-600 bg-clip-text text-transparent">
                m-busi
              </span>
            </div>

            {/* Desktop Navigation */}
            <div className="hidden md:flex items-center space-x-8">
              <a href="#features" className="text-gray-700 hover:text-blue-600 transition-colors duration-200">
                Features
              </a>
              <a href="#about" className="text-gray-700 hover:text-blue-600 transition-colors duration-200">
                About
              </a>
              <button 
                onClick={handleLogin}
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-all duration-200 hover:scale-105 hover:shadow-lg"
              >
                Login
              </button>
            </div>

            {/* Mobile menu button */}
            <div className="md:hidden">
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="text-gray-700 hover:text-blue-600 transition-colors duration-200"
              >
                {mobileMenuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>
            </div>
          </div>

          {/* Mobile Navigation */}
          {mobileMenuOpen && (
            <div className="md:hidden py-4 border-t border-gray-200 bg-white/95 backdrop-blur-md">
              <div className="flex flex-col space-y-3">
                <a href="#features" className="text-gray-700 hover:text-blue-600 px-4 py-2 transition-colors duration-200">
                  Features
                </a>
                <a href="#about" className="text-gray-700 hover:text-blue-600 px-4 py-2 transition-colors duration-200">
                  About
                </a>
                <button 
                  onClick={handleLogin}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg mx-4 transition-all duration-200"
                >
                  Login
                </button>
              </div>
            </div>
          )}
        </div>
      </nav>

      {/* Hero Section */}
      <main className="relative">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-32">
          <div className="text-center animate-fade-in">
            <div className="mb-8">
              <div className="inline-flex items-center bg-blue-50 border border-blue-200 rounded-full px-6 py-2 mb-8">
                <MapPin className="h-4 w-4 text-blue-600 mr-2" />
                <span className="text-blue-700 text-sm font-medium">Real-Time Bus Tracking</span>
              </div>
            </div>

            <h1 className="text-5xl md:text-7xl font-bold text-gray-900 mb-6 leading-tight">
              Track Your Bus.{' '}
              <span className="bg-gradient-to-r from-blue-600 via-teal-600 to-blue-800 bg-clip-text text-transparent">
                Real-Time.
              </span>{' '}
              <span className="bg-gradient-to-r from-teal-600 to-blue-600 bg-clip-text text-transparent">
                Smarter.
              </span>
            </h1>

            <p className="text-xl md:text-2xl text-gray-600 mb-12 max-w-4xl mx-auto leading-relaxed">
              m-busi is your real-time bus assistant. See live locations, delays, occupancy levels, 
              and find the optimal route instantly. Never miss your bus again.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center mb-16">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleGetStarted}
                className="bg-gradient-to-r from-orange-500 to-orange-600 hover:from-orange-600 hover:to-orange-700 text-white font-semibold px-8 py-4 rounded-xl text-lg transition-all duration-200 hover:shadow-xl transform"
              >
                Get Started Free
              </motion.button>
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={handleWatchDemo}
                className="border-2 border-gray-300 hover:border-blue-500 text-gray-700 hover:text-blue-600 font-semibold px-8 py-4 rounded-xl text-lg transition-all duration-200 hover:scale-105 hover:bg-blue-50 flex items-center gap-2"
              >
                <Play className="h-5 w-5" />
                Watch Demo
              </motion.button>
            </div>

            {/* Feature Cards */}
            <div className="grid md:grid-cols-3 gap-8 mt-20">
              <motion.div
                whileHover={{ y: -8 }}
                className="bg-white/60 backdrop-blur-sm border border-gray-200 rounded-2xl p-8 hover:shadow-xl transition-all duration-300"
              >
                <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
                  <MapPin className="h-8 w-8 text-blue-600" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">Live Location</h3>
                <p className="text-gray-600">
                  Track buses in real-time with precise GPS coordinates and arrival predictions.
                </p>
              </motion.div>

              <motion.div
                whileHover={{ y: -8 }}
                className="bg-white/60 backdrop-blur-sm border border-gray-200 rounded-2xl p-8 hover:shadow-xl transition-all duration-300"
              >
                <div className="bg-teal-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
                  <Clock className="h-8 w-8 text-teal-600" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">Delay Alerts</h3>
                <p className="text-gray-600">
                  Get instant notifications about delays and schedule changes to plan accordingly.
                </p>
              </motion.div>

              <motion.div
                whileHover={{ y: -8 }}
                className="bg-white/60 backdrop-blur-sm border border-gray-200 rounded-2xl p-8 hover:shadow-xl transition-all duration-300"
              >
                <div className="bg-orange-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6">
                  <Route className="h-8 w-8 text-orange-600" />
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-4">Smart Routes</h3>
                <p className="text-gray-600">
                  Discover optimal routes and connections based on real-time traffic and capacity.
                </p>
              </motion.div>
            </div>
          </div>
        </div>

        {/* Background Elements */}
        <div className="absolute top-1/2 left-10 w-72 h-72 bg-blue-200 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse"></div>
        <div className="absolute top-1/3 right-10 w-72 h-72 bg-teal-200 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse delay-1000"></div>
        <div className="absolute bottom-1/4 left-1/2 w-72 h-72 bg-orange-200 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse delay-2000"></div>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <div className="flex items-center space-x-2 mb-4 md:mb-0">
              <div className="bg-gradient-to-r from-blue-600 to-teal-600 p-2 rounded-lg">
                <Bus className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold">m-busi</span>
            </div>
            
            <div className="flex flex-col md:flex-row items-center space-y-4 md:space-y-0 md:space-x-8 text-center md:text-left">
              <div className="flex space-x-6">
                <a href="#privacy" className="text-gray-400 hover:text-white transition-colors duration-200">Privacy Policy</a>
                <a href="#terms" className="text-gray-400 hover:text-white transition-colors duration-200">Terms of Service</a>
                <a href="#contact" className="text-gray-400 hover:text-white transition-colors duration-200">Contact</a>
              </div>
              <div className="text-gray-400 text-sm">
                © 2025 m-busi. All rights reserved.
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default LandingPage;