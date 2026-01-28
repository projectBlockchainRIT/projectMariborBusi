import type { Route, Station } from '../types';
import { getApiUrl } from '../config/api';

export async function fetchRoutes(): Promise<Route[]> {
  try {
    const response = await fetch(getApiUrl('routes/list'));

    if (!response.ok) {
      throw new Error(`Failed to fetch routes: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    throw error;
  }
}

export async function fetchStationsForRoute(routeId: number): Promise<Station[]> {
  try {
    const response = await fetch(getApiUrl(`routes/stations/${routeId}`));

    if (!response.ok) {
      throw new Error(`Failed to fetch stations for route ${routeId}: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    throw error;
  }
} 