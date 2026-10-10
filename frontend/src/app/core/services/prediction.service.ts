import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MatchPrediction } from '../models/match.model';
import { ApiResponse } from '../models/api-response.model';
import { APP_CONSTANTS } from '@core/constants/app-constants';
@Injectable({
  providedIn: 'root'
})
export class PredictionService {
  private readonly API_URL = `${APP_CONSTANTS.API_BASE_URL}/predictions`;

  constructor(private http: HttpClient) {}

  predictMatch(matchId: number): Observable<ApiResponse<MatchPrediction>> {
    return this.http.post<ApiResponse<MatchPrediction>>(`${this.API_URL}/match/${matchId}`, null);
  }

  getPredictionById(id: number): Observable<ApiResponse<MatchPrediction>> {
    return this.http.get<ApiResponse<MatchPrediction>>(`${this.API_URL}/${id}`);
  }

  getUpcomingPredictions(limit: number = 10): Observable<ApiResponse<MatchPrediction[]>> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<ApiResponse<MatchPrediction[]>>(`${this.API_URL}/upcoming`, { params });
  }

  getHighConfidencePredictions(minConfidence: number = 70, limit: number = 10): Observable<ApiResponse<MatchPrediction[]>> {
    const params = new HttpParams()
      .set('minConfidence', minConfidence.toString())
      .set('limit', limit.toString());
    return this.http.get<ApiResponse<MatchPrediction[]>>(`${this.API_URL}/high-confidence`, { params });
  }
}