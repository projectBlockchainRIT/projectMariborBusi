import type { Route, Station } from '../types';

export async function fetchRoutes(): Promise<Route[]> {
  try {
    console.log('Making API request to fetch routes...');
    const response = await fetch('http://40.68.198.73:8080/v1/routes/list');
    console.log('API Response status:', response.status);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch routes: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    console.log('API Response data:', data);
    return data;
  } catch (error) {
    console.error('Error fetching routes:', error);
    throw error;
  }
}

export async function fetchStationsForRoute(routeId: number): Promise<Station[]> {
  try {
    console.log(`Making API request to fetch stations for route ${routeId}...`);
    const response = await fetch(`http://40.68.198.73:8080/v1/routes/stations/${routeId}`);
    console.log('API Response status:', response.status);
    
    if (!response.ok) {
      throw new Error(`Failed to fetch stations for route ${routeId}: ${response.status} ${response.statusText}`);
    }
    
    const data = await response.json();
    console.log('API Response data:', data);
    return data;
  } catch (error) {
    console.error(`Error fetching stations for route ${routeId}:`, error);
    throw error;
  }
} 