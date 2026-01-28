import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Bus, Menu, X } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const Header = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const { isDarkMode } = useTheme();

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { href: '#features', label: 'Features' },
    { href: '#how-it-works', label: 'How It Works' },
    { href: '#reviews', label: 'Reviews' },
    { href: '#about', label: 'About' },
  ];

  return (
    <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
      isScrolled
        ? isDarkMode
          ? 'bg-slate-900/95 backdrop-blur-md shadow-sm shadow-slate-800/50 py-3'
          : 'bg-white/95 backdrop-blur-md shadow-sm py-3'
        : 'bg-transparent py-5'
    }`}>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="bg-marprom-600 p-2 rounded-xl group-hover:bg-marprom-700 transition-colors shadow-lg shadow-marprom-600/20">
              <Bus className="h-5 w-5 text-white" />
            </div>
            <span className={`text-xl font-bold ${
              isDarkMode ? 'text-white' : 'text-slate-900'
            }`}>M-busi</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <a
                key={link.href}
                href={link.href}
                className={`font-medium transition-colors relative group text-sm ${
                  isDarkMode
                    ? 'text-slate-400 hover:text-marprom-500'
                    : 'text-slate-600 hover:text-marprom-600'
                }`}
              >
                {link.label}
                <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-marprom-600 group-hover:w-full transition-all duration-300"></span>
              </a>
            ))}
          </nav>

          {/* Desktop Auth Buttons */}
          <div className="hidden md:flex items-center gap-3">
            <Link
              to="/login"
              className={`font-medium transition-colors text-sm ${
                isDarkMode
                  ? 'text-slate-300 hover:text-marprom-500'
                  : 'text-slate-700 hover:text-marprom-600'
              }`}
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="bg-marprom-600 text-white px-5 py-2.5 rounded-xl font-medium hover:bg-marprom-700 transition-colors shadow-md shadow-marprom-600/20 text-sm"
            >
              Get Started
            </Link>
          </div>

          {/* Mobile Menu Button */}
          <button
            className={`md:hidden p-2 transition-colors ${
              isDarkMode
                ? 'text-slate-300 hover:text-marprom-500'
                : 'text-slate-700 hover:text-marprom-600'
            }`}
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </div>

      {/* Mobile Navigation */}
      {isMenuOpen && (
        <div className={`md:hidden absolute top-full left-0 right-0 shadow-lg animate-fade-in ${
          isDarkMode
            ? 'bg-slate-900 border-t border-slate-800'
            : 'bg-white border-t border-slate-100'
        }`}>
          <div className="container mx-auto px-4 py-6 space-y-4">
            {navLinks.map((link) => (
              <a
                key={link.href}
                href={link.href}
                className={`block font-medium py-2 transition-colors ${
                  isDarkMode
                    ? 'text-slate-300 hover:text-marprom-500'
                    : 'text-slate-700 hover:text-marprom-600'
                }`}
                onClick={() => setIsMenuOpen(false)}
              >
                {link.label}
              </a>
            ))}
            <div className={`pt-4 space-y-3 ${
              isDarkMode ? 'border-t border-slate-800' : 'border-t border-slate-100'
            }`}>
              <Link
                to="/login"
                className={`block text-center font-medium py-2 transition-colors ${
                  isDarkMode
                    ? 'text-slate-300 hover:text-marprom-500'
                    : 'text-slate-700 hover:text-marprom-600'
                }`}
                onClick={() => setIsMenuOpen(false)}
              >
                Sign In
              </Link>
              <Link
                to="/register"
                className="block text-center bg-marprom-600 text-white px-5 py-3 rounded-xl font-medium hover:bg-marprom-700 transition-colors"
                onClick={() => setIsMenuOpen(false)}
              >
                Get Started
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  );
};

export default Header;
