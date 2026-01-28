import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  icon?: ReactNode;
  actions?: ReactNode;
  badge?: ReactNode;
  className?: string;
}

export default function PageHeader({
  title,
  subtitle,
  icon,
  actions,
  badge,
  className = '',
}: PageHeaderProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        flex flex-col sm:flex-row sm:items-center sm:justify-between
        gap-4 mb-6
        ${className}
      `}
    >
      <div className="flex items-start gap-3">
        {icon && (
          <div
            className={`
              p-2.5 rounded-xl flex-shrink-0
              ${isDarkMode ? 'bg-slate-800' : 'bg-slate-100'}
            `}
          >
            {icon}
          </div>
        )}
        <div>
          <div className="flex items-center gap-2">
            <h1
              className={`
                text-xl sm:text-2xl font-bold tracking-tight
                ${isDarkMode ? 'text-white' : 'text-slate-900'}
              `}
            >
              {title}
            </h1>
            {badge}
          </div>
          {subtitle && (
            <p
              className={`
                text-sm mt-1
                ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}
              `}
            >
              {subtitle}
            </p>
          )}
        </div>
      </div>
      {actions && <div className="flex items-center gap-3">{actions}</div>}
    </div>
  );
}
