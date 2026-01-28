import { useTheme } from '../../context/ThemeContext';

type SpinnerSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

interface SpinnerProps {
  size?: SpinnerSize;
  color?: 'primary' | 'white' | 'current';
  className?: string;
  label?: string;
}

export default function Spinner({
  size = 'md',
  color = 'primary',
  className = '',
  label,
}: SpinnerProps) {
  const { isDarkMode } = useTheme();

  const sizeStyles = {
    xs: 'h-3 w-3 border',
    sm: 'h-4 w-4 border-2',
    md: 'h-6 w-6 border-2',
    lg: 'h-8 w-8 border-2',
    xl: 'h-12 w-12 border-3',
  };

  const colorStyles = {
    primary: 'border-marprom-600 border-t-transparent',
    white: 'border-white border-t-transparent',
    current: 'border-current border-t-transparent',
  };

  const spinner = (
    <div
      className={`
        animate-spin rounded-full
        ${sizeStyles[size]}
        ${colorStyles[color]}
        ${className}
      `}
      role="status"
      aria-label={label || 'Loading'}
    />
  );

  if (label) {
    return (
      <div className="flex items-center gap-3">
        {spinner}
        <span className={`text-sm ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
          {label}
        </span>
      </div>
    );
  }

  return spinner;
}

interface SpinnerOverlayProps {
  label?: string;
}

export function SpinnerOverlay({ label = 'Loading...' }: SpinnerOverlayProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        absolute inset-0 flex flex-col items-center justify-center
        ${isDarkMode ? 'bg-slate-900/80' : 'bg-white/80'}
        backdrop-blur-sm z-10
      `}
    >
      <Spinner size="lg" />
      {label && (
        <p className={`mt-3 text-sm ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
          {label}
        </p>
      )}
    </div>
  );
}
