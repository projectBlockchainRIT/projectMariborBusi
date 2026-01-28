import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';
import { FolderIcon } from '@heroicons/react/24/outline';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export default function EmptyState({
  icon,
  title,
  description,
  action,
  className = '',
}: EmptyStateProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        flex flex-col items-center justify-center
        py-12 px-6 text-center
        ${className}
      `}
    >
      <div
        className={`
          p-4 rounded-2xl mb-4
          ${isDarkMode ? 'bg-slate-800' : 'bg-slate-100'}
        `}
      >
        {icon || (
          <FolderIcon
            className={`w-8 h-8 ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}
          />
        )}
      </div>
      <h3
        className={`
          text-base font-semibold mb-1
          ${isDarkMode ? 'text-white' : 'text-slate-900'}
        `}
      >
        {title}
      </h3>
      {description && (
        <p
          className={`
            text-sm max-w-sm mb-4
            ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}
          `}
        >
          {description}
        </p>
      )}
      {action && <div>{action}</div>}
    </div>
  );
}
