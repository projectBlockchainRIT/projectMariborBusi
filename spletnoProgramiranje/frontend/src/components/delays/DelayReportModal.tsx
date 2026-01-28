import { useState, useEffect } from 'react';
import { BellAlertIcon } from '@heroicons/react/24/outline';
import { useTheme } from '../../context/ThemeContext';
import { useUser } from '../../context/UserContext';
import { Modal, ModalFooter, Button, ErrorAlert } from '../ui';
import type { Station, Route } from '../../types';
import { getApiUrl } from '../../config/api';

interface DelayReportModalProps {
  isOpen: boolean;
  onClose: () => void;
  station: Station | null;
  route: Route | null;
}

interface DelayReport {
  date: string;
  delayTime: number;
  stopId: number | string;
  lineId: number | string;
  userId: number | string;
}

export default function DelayReportModal({
  isOpen,
  onClose,
  station,
  route,
}: DelayReportModalProps) {
  const { isDarkMode } = useTheme();
  const { isAuthenticated } = useUser();
  const [delayTime, setDelayTime] = useState(5);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    if (feedback) {
      const timer = setTimeout(() => setFeedback(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [feedback]);

  useEffect(() => {
    if (!isOpen) {
      setDelayTime(5);
      setFeedback(null);
    }
  }, [isOpen]);

  const handleSubmit = async () => {
    if (!station || !route) return;

    try {
      setIsSubmitting(true);

      if (!isAuthenticated) {
        setFeedback({ type: 'error', message: 'You must be logged in to report delays' });
        return;
      }

      const authToken = localStorage.getItem('authToken');
      if (!authToken) {
        setFeedback({ type: 'error', message: 'Authentication token missing. Please log in again.' });
        return;
      }

      const delayReport: DelayReport = {
        date: new Date().toISOString(),
        delayTime: delayTime,
        stopId: station.id,
        lineId: route.id,
        userId: 1
      };

      const corsProxyUrl = 'https://cors-anywhere.herokuapp.com/';
      const apiUrl = getApiUrl('delays/report');

      const response = await fetch(corsProxyUrl + apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${authToken}`,
          'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify(delayReport)
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        throw new Error(errorData?.message || `Request failed with status ${response.status}`);
      }

      setFeedback({
        type: 'success',
        message: `Delay of ${delayTime} minutes reported for ${station.name}.`
      });

      setTimeout(() => onClose(), 2000);
    } catch (error) {
      setFeedback({
        type: 'error',
        message: error instanceof Error ? error.message : 'Failed to submit delay report'
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!station) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Report Delay"
      icon={<BellAlertIcon className="w-5 h-5 text-amber-500" />}
    >
      <div className={`mb-5 p-3 rounded-lg ${isDarkMode ? 'bg-slate-700/50' : 'bg-slate-50'}`}>
        <div className={`text-sm ${isDarkMode ? 'text-slate-300' : 'text-slate-600'}`}>
          <span className="font-medium">Station:</span> {station.name}
        </div>
        <div className={`text-sm mt-1 ${isDarkMode ? 'text-slate-300' : 'text-slate-600'}`}>
          <span className="font-medium">Line:</span> {route?.name || 'Unknown'}
        </div>
      </div>

      <div className="mb-5">
        <label className={`block text-sm font-medium mb-3 ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
          Delay (minutes):{' '}
          <span className={`font-bold ${
            delayTime > 15 ? 'text-red-500' : delayTime > 5 ? 'text-amber-500' : 'text-emerald-500'
          }`}>
            {delayTime}
          </span>
        </label>
        <input
          type="range"
          min="1"
          max="60"
          value={delayTime}
          onChange={(e) => setDelayTime(Number(e.target.value))}
          className="w-full h-2 rounded-lg appearance-none cursor-pointer accent-marprom-600"
          style={{
            background: `linear-gradient(to right, #E30613 0%, #E30613 ${(delayTime / 60) * 100}%, ${
              isDarkMode ? '#334155' : '#e2e8f0'
            } ${(delayTime / 60) * 100}%, ${isDarkMode ? '#334155' : '#e2e8f0'} 100%)`
          }}
        />
        <div className="flex justify-between mt-1 text-xs text-slate-500">
          <span>1 min</span>
          <span>60 min</span>
        </div>
      </div>

      {feedback && (
        <ErrorAlert
          variant={feedback.type === 'success' ? 'success' : 'error'}
          className="mb-5"
        >
          {feedback.message}
        </ErrorAlert>
      )}

      <ModalFooter>
        <Button variant="secondary" onClick={onClose} fullWidth>
          Cancel
        </Button>
        <Button
          variant="primary"
          onClick={handleSubmit}
          isLoading={isSubmitting}
          leftIcon={<BellAlertIcon className="w-4 h-4" />}
          fullWidth
        >
          Report Delay
        </Button>
      </ModalFooter>
    </Modal>
  );
}
