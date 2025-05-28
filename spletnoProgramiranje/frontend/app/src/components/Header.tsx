import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bus, Menu, X } from 'lucide-react';

const Header = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const location = useLocation();

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 10) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, []);

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      isScrolled ? 'bg-white shadow-md py-2' : 'bg-transparent py-4'
    }`}>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          <Link to="/" className="flex items-center">
            <Bus className="h-8 w-8 text-mbusi-red-600" />
            <span className="ml-2 text-2xl font-bold text-mbusi-red-600">M-busi</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center space-x-8">
            <Link to="/#features" className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors">Features</Link>
            <Link to="/#how-it-works" className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors">How It Works</Link>
            <Link to="/#reviews" className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors">Reviews</Link>
            <Link to="/#about" className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors">About</Link>
            <Link to="/login" className="text-mbusi-red-600 font-medium hover:text-mbusi-red-700 transition-colors">Login</Link>
            <Link 
              to="/register" 
              className="bg-mbusi-red-600 text-white px-4 py-2 rounded-lg hover:bg-mbusi-red-700 transition-colors shadow-sm"
            >
              Register
            </Link>
          </nav>

          {/* Mobile Menu Button */}
          <button 
            className="md:hidden text-gray-700"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isMenuOpen && (
        <div className="md:hidden bg-white shadow-lg py-4 px-4 animate-fade-in">
          <div className="flex flex-col space-y-4">
            <Link 
              to="/#features" 
              className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors"
              onClick={() => setIsMenuOpen(false)}
            >
              Features
            </Link>
            <Link 
              to="/#how-it-works" 
              className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors"
              onClick={() => setIsMenuOpen(false)}
            >
              How It Works
            </Link>
            <Link 
              to="/#reviews" 
              className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors"
              onClick={() => setIsMenuOpen(false)}
            >
              Reviews
            </Link>
            <Link 
              to="/#about" 
              className="text-gray-700 hover:text-mbusi-red-600 font-medium transition-colors"
              onClick={() => setIsMenuOpen(false)}
            >
              About
            </Link>
            <Link 
              to="/login" 
              className="text-mbusi-red-600 font-medium hover:text-mbusi-red-700 transition-colors"
              onClick={() => setIsMenuOpen(false)}
            >
              Login
            </Link>
            <Link 
              to="/register" 
              className="bg-mbusi-red-600 text-white px-4 py-2 rounded-lg hover:bg-mbusi-red-700 transition-colors text-center shadow-sm"
              onClick={() => setIsMenuOpen(false)}
            >
              Register
            </Link>
          </div>
        </div>
      )}
    </header>
  );
};

export default Header;