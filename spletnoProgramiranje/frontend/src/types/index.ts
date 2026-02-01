export interface Route {
  id: number;
  name: string;
  path: number[][];
  line_id: number;
}

export interface Station {
  id: number;
  latitude: number;
  longitude: number;
  name: string;
  number: string;
}

export interface Delay {
  delay_time: number;
  description: string;
  id: number;
  route_id: number;
  report_time: string;
  station_id: number;
}

export type TimeRangeChangeHandler = (date: string, isDateFilterEnabled: boolean, isLineFilterEnabled: boolean) => void; 