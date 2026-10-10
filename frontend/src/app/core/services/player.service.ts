import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Player, PlayerStatistic, PlayerForm } from '../models/player.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Injectable({
  providedIn: 'root'
})
export class PlayerService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/players`;

  constructor(private http: HttpClient) {}

  getPlayersByTeam(teamId: number): Observable<ApiResponse<Player[]>> {
    return this.http.get<ApiResponse<Player[]>>(`${this.API_URL}/team/${teamId}`);
  }

  getPlayerById(id: number): Observable<ApiResponse<Player>> {
    return this.http.get<ApiResponse<Player>>(`${this.API_URL}/${id}`);
  }

  getPlayerStatistics(id: number, season: string): Observable<ApiResponse<PlayerStatistic>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<PlayerStatistic>>(`${this.API_URL}/${id}/statistics`, { params });
  }

  getPlayerForm(id: number, season: string): Observable<ApiResponse<PlayerForm>> {
    const params = new HttpParams().set('season', season);
    return this.http.get<ApiResponse<PlayerForm>>(`${this.API_URL}/${id}/form`, { params });
  }

  searchPlayers(name: string): Observable<ApiResponse<Player[]>> {
    const params = new HttpParams().set('name', name);
    return this.http.get<ApiResponse<Player[]>>(`${this.API_URL}/search`, { params });
  }
}