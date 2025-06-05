import { useState } from 'react';
import Sidebar from './Sidebar';
import { useUser } from '../../context/UserContext';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const { isAuthenticated, isAdmin } = useUser();

  const toggleDarkMode = () => {
    setIsDarkMode(!isDarkMode);
    document.documentElement.classList.toggle('dark');
  };

  return (
    <div className="flex h-screen bg-gray-100 dark:bg-gray-900">
      <Sidebar 
        isDarkMode={isDarkMode} 
        toggleDarkMode={toggleDarkMode}
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
      />
      <main className="flex-1 overflow-hidden">
        <div className="h-full p-4">
          {children}
        </div>
      </main>
    </div>
  );
}