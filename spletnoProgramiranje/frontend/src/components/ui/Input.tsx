import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      hint,
      leftIcon,
      rightIcon,
      fullWidth = true,
      className = '',
      id,
      ...props
    },
    ref
  ) => {
    const { isDarkMode } = useTheme();
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;

    return (
      <div className={fullWidth ? 'w-full' : ''}>
        {label && (
          <label
            htmlFor={inputId}
            className={`block text-sm font-medium mb-2 ${
              isDarkMode ? 'text-slate-200' : 'text-slate-700'
            }`}
          >
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <div className={`
              absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none
              ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}
            `}>
              {leftIcon}
            </div>
          )}
          <input
            ref={ref}
            id={inputId}
            className={`
              block w-full px-4 py-3
              rounded-xl text-sm
              border transition-all duration-200
              focus:outline-none focus:ring-2
              ${leftIcon ? 'pl-11' : ''}
              ${rightIcon ? 'pr-11' : ''}
              ${error
                ? isDarkMode
                  ? 'border-red-500 bg-red-500/10 text-white focus:ring-red-500/20 focus:border-red-500'
                  : 'border-red-500 bg-red-50 text-slate-900 focus:ring-red-500/20 focus:border-red-500'
                : isDarkMode
                  ? 'border-slate-700 bg-slate-800 text-white placeholder-slate-500 focus:ring-marprom-600/20 focus:border-marprom-600'
                  : 'border-slate-200 bg-white text-slate-900 placeholder-slate-400 focus:ring-marprom-600/20 focus:border-marprom-600'
              }
              ${props.disabled ? 'opacity-50 cursor-not-allowed' : ''}
              ${className}
            `}
            {...props}
          />
          {rightIcon && (
            <div className={`
              absolute inset-y-0 right-0 pr-3.5 flex items-center
              ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}
            `}>
              {rightIcon}
            </div>
          )}
        </div>
        {error && (
          <p className="mt-1.5 text-sm text-red-500">{error}</p>
        )}
        {hint && !error && (
          <p className={`mt-1.5 text-sm ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
            {hint}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

export default Input;
