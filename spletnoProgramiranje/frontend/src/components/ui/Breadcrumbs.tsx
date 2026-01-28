import { Link } from 'react-router-dom';
import { ChevronRightIcon, HomeIcon } from '@heroicons/react/24/outline';
import { useTheme } from '../../context/ThemeContext';

interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  showHome?: boolean;
  className?: string;
}

export default function Breadcrumbs({
  items,
  showHome = true,
  className = '',
}: BreadcrumbsProps) {
  const { isDarkMode } = useTheme();

  const allItems = showHome
    ? [{ label: 'Dashboard', href: '/dashboard' }, ...items]
    : items;

  return (
    <nav
      className={`flex items-center gap-1 text-sm ${className}`}
      aria-label="Breadcrumb"
    >
      {showHome && (
        <>
          <Link
            to="/dashboard"
            className={`
              p-1 rounded-md transition-colors
              ${isDarkMode
                ? 'text-slate-400 hover:text-white hover:bg-slate-800'
                : 'text-slate-400 hover:text-slate-700 hover:bg-slate-100'
              }
            `}
            aria-label="Dashboard"
          >
            <HomeIcon className="w-4 h-4" />
          </Link>
          <ChevronRightIcon
            className={`w-4 h-4 ${isDarkMode ? 'text-slate-600' : 'text-slate-300'}`}
          />
        </>
      )}
      {allItems.slice(showHome ? 1 : 0).map((item, index, arr) => {
        const isLast = index === arr.length - 1;

        return (
          <div key={item.label} className="flex items-center gap-1">
            {item.href && !isLast ? (
              <Link
                to={item.href}
                className={`
                  px-1.5 py-0.5 rounded-md transition-colors
                  ${isDarkMode
                    ? 'text-slate-400 hover:text-white hover:bg-slate-800'
                    : 'text-slate-500 hover:text-slate-700 hover:bg-slate-100'
                  }
                `}
              >
                {item.label}
              </Link>
            ) : (
              <span
                className={`
                  px-1.5 py-0.5
                  ${isLast
                    ? isDarkMode
                      ? 'text-white font-medium'
                      : 'text-slate-900 font-medium'
                    : isDarkMode
                      ? 'text-slate-400'
                      : 'text-slate-500'
                  }
                `}
              >
                {item.label}
              </span>
            )}
            {!isLast && (
              <ChevronRightIcon
                className={`w-4 h-4 ${isDarkMode ? 'text-slate-600' : 'text-slate-300'}`}
              />
            )}
          </div>
        );
      })}
    </nav>
  );
}
