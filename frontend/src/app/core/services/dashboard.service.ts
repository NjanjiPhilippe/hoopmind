import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Dashboard } from '../models/dashboard.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/dashboard`;

  constructor(private http: HttpClient) {}

  getDashboard(season: string = APP_CONSTANTS.CURRENT_SEASON): Observable<ApiResponse<Dashboard>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<Dashboard>>(this.API_URL, { params });
  }

  getOverallStats(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.API_URL}/stats`);
  }
}