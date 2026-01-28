import type { ReactNode } from 'react';
import { useTheme } from '../../context/ThemeContext';

interface Tab {
  id: string;
  label: string;
  icon: React.ElementType;
}

interface SettingsTabsProps {
  tabs: Tab[];
  activeTab: string;
  onTabChange: (tabId: string) => void;
}

export default function SettingsTabs({ tabs, activeTab, onTabChange }: SettingsTabsProps) {
  const { isDarkMode } = useTheme();

  return (
    <div className={`flex gap-1 p-1 rounded-xl mb-6 ${
      isDarkMode ? 'bg-slate-800' : 'bg-slate-100'
    }`}>
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={`
              flex items-center gap-2 px-4 py-2.5 rounded-lg text-sm font-medium
              transition-all duration-200 flex-1
              ${isActive
                ? isDarkMode
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'bg-white text-slate-900 shadow-sm'
                : isDarkMode
                  ? 'text-slate-400 hover:text-white'
                  : 'text-slate-600 hover:text-slate-900'
              }
            `}
          >
            <Icon className="w-4 h-4" />
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
}

interface SettingsSectionProps {
  title: string;
  description?: string;
  icon?: React.ElementType;
  children: ReactNode;
}

export function SettingsSection({ title, description, icon: Icon, children }: SettingsSectionProps) {
  const { isDarkMode } = useTheme();

  return (
    <section className={`
      rounded-2xl p-6
      border transition-shadow duration-200
      hover:shadow-md
      ${isDarkMode
        ? 'bg-slate-800 border-slate-700'
        : 'bg-white border-slate-200'
      }
    `}>
      <div className="flex items-center gap-3 mb-6">
        {Icon && (
          <div className={`p-2.5 rounded-xl ${isDarkMode ? 'bg-slate-700' : 'bg-slate-100'}`}>
            <Icon className="w-5 h-5 text-marprom-600" />
          </div>
        )}
        <div>
          <h2 className={`text-lg font-semibold ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
            {title}
          </h2>
          {description && (
            <p className={`text-sm mt-0.5 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
              {description}
            </p>
          )}
        </div>
      </div>
      <div className="space-y-5">
        {children}
      </div>
    </section>
  );
}

interface SettingsRowProps {
  label: string;
  description?: string;
  children: ReactNode;
}

export function SettingsRow({ label, description, children }: SettingsRowProps) {
  const { isDarkMode } = useTheme();

  return (
    <div className="flex items-center justify-between gap-4">
      <div className="flex-1">
        <div className={`font-medium ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>
          {label}
        </div>
        {description && (
          <div className={`text-sm mt-0.5 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
            {description}
          </div>
        )}
      </div>
      {children}
    </div>
  );
}
