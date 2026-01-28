import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

interface CardProps {
  children: ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hover?: boolean;
  onClick?: () => void;
}

export default function Card({
  children,
  className = '',
  padding = 'md',
  hover = false,
  onClick,
}: CardProps) {
  const { isDarkMode } = useTheme();

  const paddingStyles = {
    none: '',
    sm: 'p-4',
    md: 'p-6',
    lg: 'p-8',
  };

  return (
    <div
      onClick={onClick}
      className={`
        rounded-2xl
        border transition-all duration-200
        ${paddingStyles[padding]}
        ${isDarkMode
          ? 'bg-slate-800 border-slate-700'
          : 'bg-white border-slate-200'
        }
        ${hover
          ? isDarkMode
            ? 'hover:bg-slate-700/50 hover:border-slate-600 hover:shadow-lg cursor-pointer'
            : 'hover:shadow-xl hover:border-slate-300 cursor-pointer'
          : ''
        }
        ${onClick ? 'cursor-pointer' : ''}
        ${className}
      `}
    >
      {children}
    </div>
  );
}

interface CardHeaderProps {
  children: ReactNode;
  className?: string;
}

export function CardHeader({ children, className = '' }: CardHeaderProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        pb-4 mb-4 border-b
        ${isDarkMode ? 'border-slate-700' : 'border-slate-200'}
        ${className}
      `}
    >
      {children}
    </div>
  );
}

interface CardTitleProps {
  children: ReactNode;
  icon?: ReactNode;
  className?: string;
}

export function CardTitle({ children, icon, className = '' }: CardTitleProps) {
  const { isDarkMode } = useTheme();

  return (
    <h3
      className={`
        flex items-center gap-2
        text-lg font-semibold
        ${isDarkMode ? 'text-white' : 'text-slate-900'}
        ${className}
      `}
    >
      {icon && <span className="flex-shrink-0">{icon}</span>}
      {children}
    </h3>
  );
}

interface CardDescriptionProps {
  children: ReactNode;
  className?: string;
}

export function CardDescription({ children, className = '' }: CardDescriptionProps) {
  const { isDarkMode } = useTheme();

  return (
    <p
      className={`
        text-sm mt-1
        ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}
        ${className}
      `}
    >
      {children}
    </p>
  );
}

interface CardFooterProps {
  children: ReactNode;
  className?: string;
}

export function CardFooter({ children, className = '' }: CardFooterProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        pt-4 mt-4 border-t
        ${isDarkMode ? 'border-slate-700' : 'border-slate-200'}
        ${className}
      `}
    >
      {children}
    </div>
  );
}
