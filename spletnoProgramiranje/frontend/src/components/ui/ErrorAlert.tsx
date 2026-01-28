import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';
import {
  ExclamationTriangleIcon,
  XCircleIcon,
  InformationCircleIcon,
  CheckCircleIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline';

type AlertVariant = 'error' | 'warning' | 'info' | 'success';

interface ErrorAlertProps {
  variant?: AlertVariant;
  title?: string;
  children: ReactNode;
  onDismiss?: () => void;
  className?: string;
}

export default function ErrorAlert({
  variant = 'error',
  title,
  children,
  onDismiss,
  className = '',
}: ErrorAlertProps) {
  const { isDarkMode } = useTheme();

  const icons = {
    error: XCircleIcon,
    warning: ExclamationTriangleIcon,
    info: InformationCircleIcon,
    success: CheckCircleIcon,
  };

  const styles = {
    error: isDarkMode
      ? 'bg-red-500/10 border-red-500/20 text-red-400'
      : 'bg-red-50 border-red-200 text-red-700',
    warning: isDarkMode
      ? 'bg-amber-500/10 border-amber-500/20 text-amber-400'
      : 'bg-amber-50 border-amber-200 text-amber-700',
    info: isDarkMode
      ? 'bg-blue-500/10 border-blue-500/20 text-blue-400'
      : 'bg-blue-50 border-blue-200 text-blue-700',
    success: isDarkMode
      ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
      : 'bg-emerald-50 border-emerald-200 text-emerald-700',
  };

  const iconStyles = {
    error: 'text-red-500',
    warning: 'text-amber-500',
    info: 'text-blue-500',
    success: 'text-emerald-500',
  };

  const Icon = icons[variant];

  return (
    <div
      className={`
        flex items-start gap-3 p-4 rounded-xl border
        ${styles[variant]}
        ${className}
      `}
      role="alert"
    >
      <Icon className={`w-5 h-5 flex-shrink-0 mt-0.5 ${iconStyles[variant]}`} />
      <div className="flex-1 min-w-0">
        {title && <h4 className="font-semibold mb-1">{title}</h4>}
        <div className="text-sm">{children}</div>
      </div>
      {onDismiss && (
        <button
          onClick={onDismiss}
          className={`
            p-1 rounded-md transition-colors flex-shrink-0
            ${isDarkMode ? 'hover:bg-white/10' : 'hover:bg-black/5'}
          `}
          aria-label="Dismiss"
        >
          <XMarkIcon className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}
