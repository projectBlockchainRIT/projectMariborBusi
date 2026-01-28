/**
 * API Configuration
 *
 * This file centralizes all API-related configuration.
 * Use the BACKEND_URL constant for all API calls instead of hardcoding URLs.
 */

// Get backend URL from environment variable or use fallback
export const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8080';

// API version prefix
export const API_VERSION = '/v1';

// Full base URL for API calls
export const API_BASE_URL = `${BACKEND_URL}${API_VERSION}`;

/**
 * Helper function to construct API endpoint URLs
 * @param endpoint - The endpoint path (e.g., '/routes/list')
 * @returns Full URL for the endpoint
 */
export const getApiUrl = (endpoint: string): string => {
  // Remove leading slash if present
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
  return `${API_BASE_URL}/${cleanEndpoint}`;
};

/**
 * Common fetch options for API calls
 */
export const defaultFetchOptions: RequestInit = {
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
};

/**
 * Get fetch options with authentication token
 */
export const getAuthFetchOptions = (): RequestInit => {
  const token = localStorage.getItem('authToken');
  return {
    ...defaultFetchOptions,
    headers: {
      ...defaultFetchOptions.headers,
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
    },
  };
};

/**
 * Convert HTTP/HTTPS backend URL to WebSocket URL
 * @param endpoint - The WebSocket endpoint path (e.g., 'estimate/simulate/123')
 * @returns WebSocket URL for the endpoint
 */
export const getWebSocketUrl = (endpoint: string): string => {
  const cleanEndpoint = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
  const wsUrl = BACKEND_URL.replace(/^http/, 'ws');
  return `${wsUrl}${API_VERSION}/${cleanEndpoint}`;
};
