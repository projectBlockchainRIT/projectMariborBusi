import { useState } from 'react';
import { Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline';
import Sidebar from './Sidebar';
import { useUser } from '../../context/UserContext';
import { useTheme } from '../../context/ThemeContext';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isAuthenticated, isAdmin } = useUser();
  const { isDarkMode } = useTheme();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <div
      className={`
        flex h-screen overflow-hidden
        transition-colors duration-300
        ${isDarkMode
          ? 'bg-slate-950 text-slate-100'
          : 'bg-slate-50 text-slate-900'
        }
      `}
    >
      {/* Mobile menu button */}
      <button
        onClick={toggleMobileMenu}
        className={`
          fixed top-4 left-4 z-50 md:hidden
          p-2 rounded-xl shadow-lg
          transition-colors duration-200
          ${isDarkMode
            ? 'bg-slate-800 text-white hover:bg-slate-700'
            : 'bg-white text-slate-700 hover:bg-slate-50'
          }
        `}
        aria-label={isMobileMenuOpen ? 'Close menu' : 'Open menu'}
      >
        {isMobileMenuOpen ? (
          <XMarkIcon className="w-6 h-6" />
        ) : (
          <Bars3Icon className="w-6 h-6" />
        )}
      </button>

      {/* Mobile overlay */}
      {isMobileMenuOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 backdrop-blur-sm md:hidden animate-fade-in"
          onClick={closeMobileMenu}
        />
      )}

      {/* Sidebar - Desktop */}
      <div className="hidden md:block">
        <Sidebar
          isAuthenticated={isAuthenticated}
          isAdmin={isAdmin}
        />
      </div>

      {/* Sidebar - Mobile (slide-out drawer) */}
      <div
        className={`
          fixed inset-y-0 left-0 z-40 md:hidden
          transform transition-transform duration-300 ease-in-out
          ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        <Sidebar
          isAuthenticated={isAuthenticated}
          isAdmin={isAdmin}
          isExpanded={true}
          onClose={closeMobileMenu}
        />
      </div>

      {/* Main content */}
      <main
        className={`
          flex-1 overflow-hidden
          ${isDarkMode ? 'bg-slate-900' : 'bg-slate-50'}
        `}
      >
        <div className="h-full overflow-y-auto">
          {/* Add padding on mobile for the menu button */}
          <div className="md:hidden h-16" />
          {children}
        </div>
      </main>
    </div>
  );
}
