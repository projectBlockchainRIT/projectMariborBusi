export interface Route {
  id: number;
  name: string;
  path: number[][];
}

export interface Station {
  id: number;
  latitude: number;
  longitude: number;
  name: string;
  number: string;
} 