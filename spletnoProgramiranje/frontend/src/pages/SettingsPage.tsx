import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';
import { useUser } from '../context/UserContext';
import { motion } from 'framer-motion';
import {
  Cog6ToothIcon,
  SunIcon,
  UserIcon,
  KeyIcon,
  ArrowRightOnRectangleIcon,
  ShieldCheckIcon,
  BellIcon,
  EyeIcon,
  ChartBarIcon,
  ArrowPathIcon,
  ShieldExclamationIcon,
  WrenchScrewdriverIcon,
  PaintBrushIcon
} from '@heroicons/react/24/outline';
import { Toggle, Select, Button, PageHeader } from '../components/ui';
import SettingsTabs, { SettingsSection } from '../components/settings/SettingsTabs';

const tabs = [
  { id: 'appearance', label: 'Appearance', icon: PaintBrushIcon },
  { id: 'account', label: 'Account', icon: UserIcon },
  { id: 'notifications', label: 'Notifications', icon: BellIcon },
  { id: 'privacy', label: 'Privacy', icon: ShieldExclamationIcon },
  { id: 'advanced', label: 'Advanced', icon: WrenchScrewdriverIcon },
];

export default function SettingsPage() {
  const { isDarkMode, toggleDarkMode } = useTheme();
  const { logout } = useUser();
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('appearance');
  const [fontSize, setFontSize] = useState('medium');
  const [dataRefreshRate, setDataRefreshRate] = useState('30');
  const [mapStyle, setMapStyle] = useState('default');
  const [gpuAcceleration, setGpuAcceleration] = useState(true);
  const [renderQuality, setRenderQuality] = useState('high');
  const [antiAliasing, setAntiAliasing] = useState(true);
  const [highContrast, setHighContrast] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const [screenReader, setScreenReader] = useState(false);
  const [dataCollection, setDataCollection] = useState(true);
  const [locationSharing, setLocationSharing] = useState(true);
  const [analytics, setAnalytics] = useState(true);
  const [debugMode, setDebugMode] = useState(false);
  const [autoUpdate, setAutoUpdate] = useState(true);
  const [cacheSize, setCacheSize] = useState('1GB');
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [pushNotifications, setPushNotifications] = useState(false);
  const [delayAlerts, setDelayAlerts] = useState(true);
  const [twoFactorAuth, setTwoFactorAuth] = useState(false);
  const [sessionTimeout, setSessionTimeout] = useState(true);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const handlePasswordChange = () => {
    alert('Password change functionality coming soon!');
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'appearance':
        return (
          <div className="space-y-6">
            <SettingsSection title="Theme" icon={SunIcon}>
              <Toggle
                checked={isDarkMode}
                onChange={toggleDarkMode}
                label="Dark Mode"
                description="Toggle between light and dark theme"
              />
            </SettingsSection>

            <SettingsSection title="Display" icon={EyeIcon}>
              <Select
                label="Font Size"
                value={fontSize}
                onChange={(e) => setFontSize(e.target.value)}
                options={[
                  { value: 'small', label: 'Small' },
                  { value: 'medium', label: 'Medium' },
                  { value: 'large', label: 'Large' },
                ]}
              />

              <Select
                label="Map Style"
                value={mapStyle}
                onChange={(e) => setMapStyle(e.target.value)}
                options={[
                  { value: 'default', label: 'Default' },
                  { value: 'satellite', label: 'Satellite' },
                  { value: 'terrain', label: 'Terrain' },
                  { value: 'minimal', label: 'Minimal' },
                ]}
              />
            </SettingsSection>

            <SettingsSection title="Accessibility" icon={EyeIcon}>
              <Toggle
                checked={highContrast}
                onChange={setHighContrast}
                label="High Contrast Mode"
                description="Increase contrast for better readability"
              />

              <Toggle
                checked={reducedMotion}
                onChange={setReducedMotion}
                label="Reduced Motion"
                description="Minimize animations and transitions"
              />

              <Toggle
                checked={screenReader}
                onChange={setScreenReader}
                label="Screen Reader Support"
                description="Optimize for screen reader compatibility"
              />
            </SettingsSection>
          </div>
        );

      case 'account':
        return (
          <div className="space-y-6">
            <SettingsSection title="Profile" icon={UserIcon}>
              <div className="space-y-4">
                <div>
                  <label className={`block text-sm font-medium mb-2 ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
                    Name
                  </label>
                  <input
                    type="text"
                    value="John Doe"
                    disabled
                    className={`
                      block w-full px-4 py-3 rounded-xl text-sm
                      border transition-colors
                      ${isDarkMode
                        ? 'bg-slate-700 border-slate-600 text-white'
                        : 'bg-slate-50 border-slate-200 text-slate-900'
                      }
                      opacity-60 cursor-not-allowed
                    `}
                  />
                </div>
                <div>
                  <label className={`block text-sm font-medium mb-2 ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
                    Email
                  </label>
                  <input
                    type="email"
                    value="john.doe@example.com"
                    disabled
                    className={`
                      block w-full px-4 py-3 rounded-xl text-sm
                      border transition-colors
                      ${isDarkMode
                        ? 'bg-slate-700 border-slate-600 text-white'
                        : 'bg-slate-50 border-slate-200 text-slate-900'
                      }
                      opacity-60 cursor-not-allowed
                    `}
                  />
                </div>
                <Button
                  variant="primary"
                  leftIcon={<KeyIcon className="w-4 h-4" />}
                  onClick={handlePasswordChange}
                >
                  Change Password
                </Button>
              </div>
            </SettingsSection>

            <SettingsSection title="Security" icon={ShieldCheckIcon}>
              <Toggle
                checked={twoFactorAuth}
                onChange={setTwoFactorAuth}
                label="Two-Factor Authentication"
                description="Add an extra layer of security to your account"
              />

              <Toggle
                checked={sessionTimeout}
                onChange={setSessionTimeout}
                label="Session Timeout"
                description="Automatically log out after inactivity"
              />
            </SettingsSection>

            <Button
              variant="danger"
              fullWidth
              leftIcon={<ArrowRightOnRectangleIcon className="w-4 h-4" />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </div>
        );

      case 'notifications':
        return (
          <div className="space-y-6">
            <SettingsSection title="Notification Preferences" icon={BellIcon}>
              <Toggle
                checked={emailNotifications}
                onChange={setEmailNotifications}
                label="Email Notifications"
                description="Receive updates about your account"
              />

              <Toggle
                checked={pushNotifications}
                onChange={setPushNotifications}
                label="Push Notifications"
                description="Get real-time updates on your device"
              />

              <Toggle
                checked={delayAlerts}
                onChange={setDelayAlerts}
                label="Delay Alerts"
                description="Notify me about bus delays"
              />
            </SettingsSection>
          </div>
        );

      case 'privacy':
        return (
          <div className="space-y-6">
            <SettingsSection title="Data & Privacy" icon={ShieldExclamationIcon}>
              <Toggle
                checked={dataCollection}
                onChange={setDataCollection}
                label="Data Collection"
                description="Allow anonymous usage data collection"
              />

              <Toggle
                checked={locationSharing}
                onChange={setLocationSharing}
                label="Location Sharing"
                description="Share location for better route planning"
              />

              <Toggle
                checked={analytics}
                onChange={setAnalytics}
                label="Analytics"
                description="Enable usage analytics and reporting"
              />
            </SettingsSection>
          </div>
        );

      case 'advanced':
        return (
          <div className="space-y-6">
            <SettingsSection title="Performance" icon={ChartBarIcon}>
              <Toggle
                checked={gpuAcceleration}
                onChange={setGpuAcceleration}
                label="Hardware Acceleration"
                description="Use GPU for improved map rendering performance"
              />

              <Select
                label="Render Quality"
                value={renderQuality}
                onChange={(e) => setRenderQuality(e.target.value)}
                options={[
                  { value: 'low', label: 'Low (Better Performance)' },
                  { value: 'medium', label: 'Medium (Balanced)' },
                  { value: 'high', label: 'High (Better Quality)' },
                  { value: 'ultra', label: 'Ultra (Best Quality)' },
                ]}
                hint="Higher quality requires more GPU resources"
              />

              <Toggle
                checked={antiAliasing}
                onChange={setAntiAliasing}
                label="Anti-Aliasing"
                description="Smooth edges for better visual quality"
              />

              <Select
                label="Data Refresh Rate"
                value={dataRefreshRate}
                onChange={(e) => setDataRefreshRate(e.target.value)}
                options={[
                  { value: '15', label: '15 seconds' },
                  { value: '30', label: '30 seconds' },
                  { value: '60', label: '1 minute' },
                  { value: '300', label: '5 minutes' },
                ]}
              />
            </SettingsSection>

            <SettingsSection title="Developer" icon={WrenchScrewdriverIcon}>
              <Toggle
                checked={debugMode}
                onChange={setDebugMode}
                label="Debug Mode"
                description="Enable advanced debugging features"
              />

              <Toggle
                checked={autoUpdate}
                onChange={setAutoUpdate}
                label="Auto Updates"
                description="Automatically install updates"
              />

              <Select
                label="Cache Size"
                value={cacheSize}
                onChange={(e) => setCacheSize(e.target.value)}
                options={[
                  { value: '256MB', label: '256 MB' },
                  { value: '512MB', label: '512 MB' },
                  { value: '1GB', label: '1 GB' },
                  { value: '2GB', label: '2 GB' },
                  { value: '4GB', label: '4 GB' },
                ]}
                hint="Maximum storage for offline data"
              />

              <div className="flex gap-3 pt-2">
                <Button
                  variant="secondary"
                  leftIcon={<ArrowPathIcon className="w-4 h-4" />}
                >
                  Clear Cache
                </Button>
                <Button
                  variant="secondary"
                  leftIcon={<ArrowPathIcon className="w-4 h-4" />}
                >
                  Reset Settings
                </Button>
              </div>
            </SettingsSection>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="p-6 max-w-4xl mx-auto"
    >
      <PageHeader
        title="Settings"
        subtitle="Manage your account settings and preferences"
        icon={<Cog6ToothIcon className="w-6 h-6 text-marprom-600" />}
      />

      <SettingsTabs
        tabs={tabs}
        activeTab={activeTab}
        onTabChange={setActiveTab}
      />

      {renderTabContent()}
    </motion.div>
  );
}
