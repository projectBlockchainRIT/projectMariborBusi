import { PlayIcon, PauseIcon } from '@heroicons/react/24/outline';
import { useTheme } from '../../context/ThemeContext';

interface AnimationControlsProps {
  isPlaying: boolean;
  onPlay: () => void;
  onPause: () => void;
  animationSpeed: number;
  onSpeedChange: (speed: number) => void;
  disabled?: boolean;
}

export default function AnimationControls({
  isPlaying,
  onPlay,
  onPause,
  animationSpeed,
  onSpeedChange,
  disabled = false,
}: AnimationControlsProps) {
  const { isDarkMode } = useTheme();

  return (
    <div className="flex items-center gap-3">
      <button
        onClick={isPlaying ? onPause : onPlay}
        disabled={disabled}
        className={`
          p-2 rounded-lg transition-colors duration-200
          disabled:opacity-50 disabled:cursor-not-allowed
          ${isDarkMode
            ? 'bg-blue-500/10 hover:bg-blue-500/20 text-blue-400'
            : 'bg-blue-50 hover:bg-blue-100 text-blue-600'
          }
        `}
      >
        {isPlaying ? <PauseIcon className="w-4 h-4" /> : <PlayIcon className="w-4 h-4" />}
      </button>
      <div className="flex-1">
        <input
          type="range"
          min={0.5}
          max={3}
          step={0.5}
          value={animationSpeed}
          onChange={(e) => onSpeedChange(Number(e.target.value))}
          className="w-full h-1 rounded-lg appearance-none cursor-pointer accent-marprom-600"
          style={{
            background: `linear-gradient(to right, #E30613 0%, #E30613 ${((animationSpeed - 0.5) / 2.5) * 100}%, ${
              isDarkMode ? '#334155' : '#e2e8f0'
            } ${((animationSpeed - 0.5) / 2.5) * 100}%, ${isDarkMode ? '#334155' : '#e2e8f0'} 100%)`
          }}
        />
      </div>
      <span className={`text-xs ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
        {animationSpeed}s
      </span>
    </div>
  );
}
