export interface Station {
  id: number;
  name: string;
  number: string;
  latitude: number;
  longitude: number;
}

export interface Route {
  id: number;
  name: string;
  path: number[][];
} 