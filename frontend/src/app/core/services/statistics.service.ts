import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StatisticsOverview } from '../models/dashboard.model';
import { PlayerStatistic } from '../models/player.model';
import { TeamStatistic } from '../models/team.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/statistics`;

  constructor(private http: HttpClient) {}

  getTeamStatisticsOverview(teamId: number, season: string): Observable<ApiResponse<StatisticsOverview>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<StatisticsOverview>>(`${this.API_URL}/team/${teamId}`, { params });
  }

  getTopTeams(season: string, limit: number = 10): Observable<ApiResponse<TeamStatistic[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<TeamStatistic[]>>(`${this.API_URL}/season/${season}/top-teams`, { params });
  }

  getTopScorers(season: string, limit: number = 10): Observable<ApiResponse<PlayerStatistic[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<PlayerStatistic[]>>(`${this.API_URL}/season/${season}/top-scorers`, { params });
  }

  getTopAssistants(season: string, limit: number = 10): Observable<ApiResponse<PlayerStatistic[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<PlayerStatistic[]>>(`${this.API_URL}/season/${season}/top-assistants`, { params });
  }

  getConferenceStandings(conference: string, season: string): Observable<ApiResponse<TeamStatistic[]>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<TeamStatistic[]>>(`${this.API_URL}/conference/${conference}`, { params });
  }
}