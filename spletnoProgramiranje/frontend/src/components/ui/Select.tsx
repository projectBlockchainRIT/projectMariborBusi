import { forwardRef, type SelectHTMLAttributes, type ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';
import { ChevronDownIcon } from '@heroicons/react/24/outline';

interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  label?: string;
  error?: string;
  hint?: string;
  options: SelectOption[];
  placeholder?: string;
  leftIcon?: ReactNode;
  fullWidth?: boolean;
}

const Select = forwardRef<HTMLSelectElement, SelectProps>(
  (
    {
      label,
      error,
      hint,
      options,
      placeholder,
      leftIcon,
      fullWidth = true,
      className = '',
      id,
      ...props
    },
    ref
  ) => {
    const { isDarkMode } = useTheme();
    const selectId = id || `select-${Math.random().toString(36).substr(2, 9)}`;

    return (
      <div className={fullWidth ? 'w-full' : ''}>
        {label && (
          <label
            htmlFor={selectId}
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
          <select
            ref={ref}
            id={selectId}
            className={`
              block w-full px-4 py-3 pr-10
              rounded-xl text-sm appearance-none
              border transition-all duration-200
              focus:outline-none focus:ring-2
              ${leftIcon ? 'pl-11' : ''}
              ${error
                ? isDarkMode
                  ? 'border-red-500 bg-red-500/10 text-white focus:ring-red-500/20 focus:border-red-500'
                  : 'border-red-500 bg-red-50 text-slate-900 focus:ring-red-500/20 focus:border-red-500'
                : isDarkMode
                  ? 'border-slate-700 bg-slate-800 text-white focus:ring-marprom-600/20 focus:border-marprom-600'
                  : 'border-slate-200 bg-white text-slate-900 focus:ring-marprom-600/20 focus:border-marprom-600'
              }
              ${props.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
              ${className}
            `}
            {...props}
          >
            {placeholder && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options.map((option) => (
              <option
                key={option.value}
                value={option.value}
                disabled={option.disabled}
              >
                {option.label}
              </option>
            ))}
          </select>
          <div className={`
            absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none
            ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}
          `}>
            <ChevronDownIcon className="w-4 h-4" />
          </div>
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

Select.displayName = 'Select';

export default Select;
