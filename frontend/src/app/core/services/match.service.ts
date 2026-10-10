import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Match, HeadToHead } from '../models/match.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Injectable({
  providedIn: 'root'
})
export class MatchService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/matches`;

  constructor(private http: HttpClient) {}

  getUpcomingMatches(limit: number = 20): Observable<ApiResponse<Match[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<Match[]>>(`${this.API_URL}/upcoming`, { params });
  }

  getMatchesBySeason(season: string): Observable<ApiResponse<Match[]>> {
    return this.http.get<ApiResponse<Match[]>>(`${this.API_URL}/season/${season}`);
  }

  getMatchById(id: number): Observable<ApiResponse<Match>> {
    return this.http.get<ApiResponse<Match>>(`${this.API_URL}/${id}`);
  }

  getTeamMatches(teamId: number, season: string): Observable<ApiResponse<Match[]>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<Match[]>>(`${this.API_URL}/team/${teamId}`, { params });
  }

  getHeadToHead(team1Id: number, team2Id: number, limit: number = 5): Observable<ApiResponse<HeadToHead>> {
    const params = new HttpParams()
      .set('team1Id', team1Id.toString())
      .set('team2Id', team2Id.toString())
      .set('limit', limit.toString());
    return this.http.get<ApiResponse<HeadToHead>>(`${this.API_URL}/head-to-head`, { params });
  }

  createMatch(match: Match): Observable<ApiResponse<Match>> {
    return this.http.post<ApiResponse<Match>>(this.API_URL, match);
  }

  updateMatchScore(id: number, homeScore: number, awayScore: number): Observable<ApiResponse<Match>> {
    const params = new HttpParams()
      .set('homeScore', homeScore.toString())
      .set('awayScore', awayScore.toString());
    return this.http.patch<ApiResponse<Match>>(`${this.API_URL}/${id}/score`, null, { params });
  }
}