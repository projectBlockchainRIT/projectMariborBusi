import { useState } from 'react';
import { BellAlertIcon, XMarkIcon } from '@heroicons/react/24/outline';
import { useTheme } from '../context/ThemeContext';
import { getApiUrl } from '../config/api';
import type { Station, Route } from '../types';

interface DelayReportModalProps {
  isOpen: boolean;
  station: Station;
  route?: Route | null;
  routes?: Route[];
  onClose: () => void;
  onSuccess?: () => void;
}

interface DelayReport {
  date: string;
  delayTime: number;
  stopId: number;
  lineId: number;
  userId: number;
}

export default function DelayReportModal({
  isOpen,
  station,
  route,
  routes = [],
  onClose,
  onSuccess,
}: DelayReportModalProps) {
  const { isDarkMode } = useTheme();
  const [delayTime, setDelayTime] = useState(5);
  const [selectedLineId, setSelectedLineId] = useState<number | null>(
    route?.line_id || null
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async () => {
    if (!selectedLineId) {
      setFeedback({
        type: 'error',
        message: 'Please select a line',
      });
      return;
    }

    try {
      setIsSubmitting(true);
      setFeedback(null);

      const authToken = localStorage.getItem('authToken');
      if (!authToken) {
        setFeedback({
          type: 'error',
          message: 'Authentication token missing. Please log in again.',
        });
        return;
      }

      const delayReport: DelayReport = {
        date: new Date().toISOString(),
        delayTime: delayTime,
        stopId: station.id,
        lineId: selectedLineId,
        userId: 1,
      };

      console.log('Submitting delay report:', delayReport);

      const apiUrl = getApiUrl('delays/report');
      const response = await fetch(apiUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${authToken}`,
        },
        body: JSON.stringify(delayReport),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      setFeedback({
        type: 'success',
        message: `Thank you! Delay of ${delayTime} minutes reported for ${station.name}.`,
      });

      setTimeout(() => {
        onSuccess?.();
        handleClose();
      }, 2000);
    } catch (error) {
      console.error('Error submitting delay report:', error);
      setFeedback({
        type: 'error',
        message: error instanceof Error ? error.message : 'Failed to submit delay report',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    setDelayTime(5);
    setSelectedLineId(route?.line_id || null);
    setFeedback(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 z-[9999] flex items-center justify-center p-4">
      <div
        className={`rounded-lg shadow-xl max-w-md w-full ${
          isDarkMode ? 'bg-gray-800' : 'bg-white'
        }`}
      >
        <div className="p-6">
          <div className="flex justify-between items-center mb-4">
            <h3
              className={`text-lg font-bold flex items-center gap-2 ${
                isDarkMode ? 'text-amber-400' : 'text-amber-600'
              }`}
            >
              <BellAlertIcon className="h-5 w-5" />
              Report Delay
            </h3>
            <button
              onClick={handleClose}
              className={`p-1 rounded-full transition-colors ${
                isDarkMode
                  ? 'hover:bg-gray-700 text-gray-400'
                  : 'hover:bg-gray-200 text-gray-500'
              }`}
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          </div>

          <div className="mb-4">
            <p className={`mb-1 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
              Station: <span className="font-medium">{station.name}</span>
            </p>

            {/* Line selection */}
            {!route && routes.length > 0 && (
              <div className="mb-3">
                <label
                  className={`block text-sm font-medium mb-2 ${
                    isDarkMode ? 'text-gray-300' : 'text-gray-700'
                  }`}
                >
                  Select Line
                </label>
                <select
                  value={selectedLineId || ''}
                  onChange={(e) => setSelectedLineId(Number(e.target.value))}
                  className={`w-full px-3 py-2 border rounded-lg ${
                    isDarkMode
                      ? 'bg-gray-700 border-gray-600 text-white'
                      : 'bg-white border-gray-300 text-gray-900'
                  }`}
                >
                  <option value="">-- Select a line --</option>
                  {routes.map((r) => (
                    <option key={r.id} value={r.line_id}>
                      {r.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {route && (
              <p className={`mb-3 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                Line: <span className="font-medium">{route.name}</span>
              </p>
            )}
          </div>

          {/* Delay time slider */}
          <div className="mb-4">
            <label
              className={`block text-sm font-medium mb-2 ${
                isDarkMode ? 'text-gray-300' : 'text-gray-700'
              }`}
            >
              Delay Time: <span className="font-bold">{delayTime} minutes</span>
            </label>
            <input
              type="range"
              min="1"
              max="60"
              value={delayTime}
              onChange={(e) => setDelayTime(Number(e.target.value))}
              className="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
            />
            <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
              <span>1 min</span>
              <span>60 min</span>
            </div>
          </div>

          {/* Feedback message */}
          {feedback && (
            <div
              className={`mb-4 p-3 rounded-lg ${
                feedback.type === 'success'
                  ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                  : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-300'
              }`}
            >
              {feedback.message}
            </div>
          )}

          {/* Submit button */}
          <button
            onClick={handleSubmit}
            disabled={isSubmitting || !selectedLineId}
            className={`w-full py-2.5 px-4 rounded-lg font-medium flex items-center justify-center gap-2 transition-colors ${
              isSubmitting || !selectedLineId
                ? 'bg-gray-400 dark:bg-gray-600 cursor-not-allowed'
                : isDarkMode
                  ? 'bg-amber-600 hover:bg-amber-700 text-white'
                  : 'bg-amber-500 hover:bg-amber-600 text-white'
            }`}
          >
            {isSubmitting ? (
              <>
                <svg
                  className="animate-spin h-4 w-4 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Submitting...
              </>
            ) : (
              <>
                <BellAlertIcon className="h-4 w-4" />
                Report Delay
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
