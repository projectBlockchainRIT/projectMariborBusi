import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral';
type BadgeSize = 'sm' | 'md' | 'lg';

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  size?: BadgeSize;
  icon?: ReactNode;
  dot?: boolean;
  className?: string;
}

export default function Badge({
  children,
  variant = 'neutral',
  size = 'md',
  icon,
  dot = false,
  className = '',
}: BadgeProps) {
  const { isDarkMode } = useTheme();

  const sizeStyles = {
    sm: 'px-2 py-0.5 text-xs',
    md: 'px-2.5 py-1 text-xs',
    lg: 'px-3 py-1.5 text-sm',
  };

  const variantStyles = {
    success: isDarkMode
      ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20'
      : 'bg-emerald-50 text-emerald-700 border-emerald-200',
    warning: isDarkMode
      ? 'bg-amber-500/15 text-amber-400 border-amber-500/20'
      : 'bg-amber-50 text-amber-700 border-amber-200',
    danger: isDarkMode
      ? 'bg-red-500/15 text-red-400 border-red-500/20'
      : 'bg-red-50 text-red-700 border-red-200',
    info: isDarkMode
      ? 'bg-blue-500/15 text-blue-400 border-blue-500/20'
      : 'bg-blue-50 text-blue-700 border-blue-200',
    neutral: isDarkMode
      ? 'bg-slate-700 text-slate-300 border-slate-600'
      : 'bg-slate-100 text-slate-600 border-slate-200',
  };

  const dotColors = {
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    danger: 'bg-red-500',
    info: 'bg-blue-500',
    neutral: isDarkMode ? 'bg-slate-400' : 'bg-slate-500',
  };

  return (
    <span
      className={`
        inline-flex items-center gap-1.5
        font-medium rounded-full border
        ${sizeStyles[size]}
        ${variantStyles[variant]}
        ${className}
      `}
    >
      {dot && (
        <span className={`w-1.5 h-1.5 rounded-full ${dotColors[variant]}`} />
      )}
      {icon && <span className="flex-shrink-0">{icon}</span>}
      {children}
    </span>
  );
}
