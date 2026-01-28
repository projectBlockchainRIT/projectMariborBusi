import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  isLoading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  fullWidth?: boolean;
  children: ReactNode;
}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = 'primary',
      size = 'md',
      isLoading = false,
      leftIcon,
      rightIcon,
      fullWidth = false,
      children,
      className = '',
      disabled,
      ...props
    },
    ref
  ) => {
    const { isDarkMode } = useTheme();

    const baseStyles = `
      inline-flex items-center justify-center gap-2
      font-medium rounded-xl
      transition-all duration-200
      focus:outline-none focus:ring-2 focus:ring-offset-2
      disabled:opacity-50 disabled:cursor-not-allowed
    `;

    const sizeStyles = {
      sm: 'px-3 py-1.5 text-xs',
      md: 'px-4 py-2.5 text-sm',
      lg: 'px-6 py-3 text-base',
    };

    const variantStyles = {
      primary: `
        bg-marprom-600 text-white
        hover:bg-marprom-700
        focus:ring-marprom-600/50
        shadow-lg shadow-marprom-600/20
        hover:shadow-xl hover:shadow-marprom-600/25
      `,
      secondary: isDarkMode
        ? `
          bg-slate-700 text-slate-100
          hover:bg-slate-600
          focus:ring-slate-500/50
        `
        : `
          bg-slate-100 text-slate-700
          hover:bg-slate-200
          focus:ring-slate-400/50
        `,
      ghost: isDarkMode
        ? `
          bg-transparent text-slate-300
          hover:bg-slate-800 hover:text-white
          focus:ring-slate-500/50
        `
        : `
          bg-transparent text-slate-600
          hover:bg-slate-100 hover:text-slate-900
          focus:ring-slate-400/50
        `,
      danger: `
        bg-red-600 text-white
        hover:bg-red-700
        focus:ring-red-600/50
        shadow-lg shadow-red-600/20
      `,
      outline: isDarkMode
        ? `
          bg-transparent text-slate-300
          border border-slate-600
          hover:bg-slate-800 hover:border-slate-500
          focus:ring-slate-500/50
        `
        : `
          bg-transparent text-slate-700
          border border-slate-300
          hover:bg-slate-50 hover:border-slate-400
          focus:ring-slate-400/50
        `,
    };

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={`
          ${baseStyles}
          ${sizeStyles[size]}
          ${variantStyles[variant]}
          ${fullWidth ? 'w-full' : ''}
          ${className}
        `}
        {...props}
      >
        {isLoading ? (
          <>
            <div className="animate-spin rounded-full h-4 w-4 border-2 border-current border-t-transparent" />
            <span>Loading...</span>
          </>
        ) : (
          <>
            {leftIcon && <span className="flex-shrink-0">{leftIcon}</span>}
            {children}
            {rightIcon && <span className="flex-shrink-0">{rightIcon}</span>}
          </>
        )}
      </button>
    );
  }
);

Button.displayName = 'Button';

export default Button;
