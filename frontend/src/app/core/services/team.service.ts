import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Team, TeamStatistic, TeamForm } from '../models/team.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/teams`;

  constructor(private http: HttpClient) {}

  getAllTeams(): Observable<ApiResponse<Team[]>> {
    return this.http.get<ApiResponse<Team[]>>(this.API_URL);
  }

  getTeamById(id: number): Observable<ApiResponse<Team>> {
    return this.http.get<ApiResponse<Team>>(`${this.API_URL}/${id}`);
  }

  getTeamStatistics(id: number, season: string): Observable<ApiResponse<TeamStatistic>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<TeamStatistic>>(`${this.API_URL}/${id}/statistics`, { params });
  }

  getTeamForm(id: number, lastNMatches: number = 10): Observable<ApiResponse<TeamForm>> {
    const params = new HttpParams().set('lastNMatches', lastNMatches.toString());
    return this.http.get<ApiResponse<TeamForm>>(`${this.API_URL}/${id}/form`, { params });
  }

  createTeam(team: Team): Observable<ApiResponse<Team>> {
    return this.http.post<ApiResponse<Team>>(this.API_URL, team);
  }
}
