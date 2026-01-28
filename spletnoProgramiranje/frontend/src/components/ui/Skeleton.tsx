import { useTheme } from '../../context/ThemeContext';

interface SkeletonProps {
  className?: string;
  width?: string | number;
  height?: string | number;
  rounded?: 'none' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';
}

export default function Skeleton({
  className = '',
  width,
  height,
  rounded = 'md',
}: SkeletonProps) {
  const { isDarkMode } = useTheme();

  const roundedStyles = {
    none: 'rounded-none',
    sm: 'rounded-sm',
    md: 'rounded-md',
    lg: 'rounded-lg',
    xl: 'rounded-xl',
    '2xl': 'rounded-2xl',
    full: 'rounded-full',
  };

  return (
    <div
      className={`
        animate-pulse
        ${isDarkMode ? 'bg-slate-700' : 'bg-slate-200'}
        ${roundedStyles[rounded]}
        ${className}
      `}
      style={{
        width: width,
        height: height,
      }}
    />
  );
}

interface SkeletonTextProps {
  lines?: number;
  className?: string;
}

export function SkeletonText({ lines = 3, className = '' }: SkeletonTextProps) {
  const { isDarkMode } = useTheme();

  return (
    <div className={`space-y-2 ${className}`}>
      {Array.from({ length: lines }).map((_, index) => (
        <div
          key={index}
          className={`
            h-4 rounded animate-pulse
            ${isDarkMode ? 'bg-slate-700' : 'bg-slate-200'}
            ${index === lines - 1 ? 'w-3/4' : 'w-full'}
          `}
        />
      ))}
    </div>
  );
}

interface SkeletonCardProps {
  className?: string;
  hasImage?: boolean;
}

export function SkeletonCard({ className = '', hasImage = false }: SkeletonCardProps) {
  const { isDarkMode } = useTheme();

  return (
    <div
      className={`
        rounded-2xl p-4
        ${isDarkMode ? 'bg-slate-800' : 'bg-white'}
        ${className}
      `}
    >
      {hasImage && (
        <Skeleton
          className="w-full h-40 mb-4"
          rounded="xl"
        />
      )}
      <Skeleton className="h-6 w-3/4 mb-3" rounded="lg" />
      <SkeletonText lines={2} />
    </div>
  );
}

interface SkeletonListProps {
  count?: number;
  className?: string;
}

export function SkeletonList({ count = 5, className = '' }: SkeletonListProps) {
  const { isDarkMode } = useTheme();

  return (
    <div className={`space-y-2 ${className}`}>
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className={`
            flex items-center gap-3 p-3 rounded-lg
            ${isDarkMode ? 'bg-slate-800' : 'bg-slate-50'}
          `}
        >
          <Skeleton className="w-10 h-10" rounded="lg" />
          <div className="flex-1">
            <Skeleton className="h-4 w-1/2 mb-2" rounded="md" />
            <Skeleton className="h-3 w-1/3" rounded="md" />
          </div>
        </div>
      ))}
    </div>
  );
}
