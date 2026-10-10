import { MatchStatus } from "./match.model";

export interface FilterOptions {
  season?: string;
  conference?: string;
  division?: string;
  position?: string;
  status?: MatchStatus;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  page?: number;
  limit?: number;
}