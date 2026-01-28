import { useTheme } from '../../context/ThemeContext';

interface ToggleProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  label?: string;
  description?: string;
  className?: string;
}

export default function Toggle({
  checked,
  onChange,
  disabled = false,
  size = 'md',
  label,
  description,
  className = '',
}: ToggleProps) {
  const { isDarkMode } = useTheme();

  const sizeStyles = {
    sm: {
      track: 'w-8 h-4',
      thumb: 'w-3 h-3',
      translate: 'translate-x-4',
    },
    md: {
      track: 'w-10 h-5',
      thumb: 'w-4 h-4',
      translate: 'translate-x-5',
    },
    lg: {
      track: 'w-12 h-6',
      thumb: 'w-5 h-5',
      translate: 'translate-x-6',
    },
  };

  const handleClick = () => {
    if (!disabled) {
      onChange(!checked);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  const toggle = (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      className={`
        relative inline-flex items-center rounded-full
        transition-colors duration-200
        focus:outline-none focus:ring-2 focus:ring-marprom-600/50 focus:ring-offset-2
        ${sizeStyles[size].track}
        ${checked
          ? 'bg-marprom-600'
          : isDarkMode ? 'bg-slate-700' : 'bg-slate-200'
        }
        ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
        ${isDarkMode ? 'focus:ring-offset-slate-900' : 'focus:ring-offset-white'}
      `}
    >
      <span
        className={`
          inline-block rounded-full bg-white shadow-sm
          transition-transform duration-200
          ${sizeStyles[size].thumb}
          ${checked ? sizeStyles[size].translate : 'translate-x-0.5'}
        `}
      />
    </button>
  );

  if (label || description) {
    return (
      <div className={`flex items-center justify-between ${className}`}>
        <div className="flex-1 mr-4">
          {label && (
            <div className={`font-medium ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
              {label}
            </div>
          )}
          {description && (
            <div className={`text-sm mt-0.5 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
              {description}
            </div>
          )}
        </div>
        {toggle}
      </div>
    );
  }

  return <div className={className}>{toggle}</div>;
}
