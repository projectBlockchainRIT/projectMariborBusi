import Sidebar from './Sidebar';
import { useUser } from '../../context/UserContext';
import { useTheme } from '../../context/ThemeContext';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const { isAuthenticated, isAdmin } = useUser();
  const { isDarkMode } = useTheme();

  return (
    <div className={`flex h-screen transition-colors duration-200 ${
      isDarkMode 
        ? 'bg-gray-900 text-white' 
        : 'bg-gray-50 text-gray-900'
    }`}>
      <Sidebar 
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
      />
      <main className="flex-1 overflow-y-auto">
        <div className="h-full">
          {children}
        </div>
      </main>
    </div>
  );
}