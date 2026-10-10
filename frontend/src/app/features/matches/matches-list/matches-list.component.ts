import { Component, OnInit } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';
import { MatchCardComponent } from '../../../shared/components/match-card/match-card.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { MatchService } from '../../../core/services/match.service';
import { Match, MatchStatus } from '../../../core/models/match.model';

@Component({
  selector: 'app-matches-list',
  imports: [
    FormsModule,
    NavbarComponent,
    MatchCardComponent,
    LoadingSpinnerComponent,
  ],
  templateUrl: './matches-list.component.html',
  styleUrls: ['./matches-list.component.css'],
})
export class MatchesListComponent {
  matches: Match[] = [];
  displayedMatches: Match[] = [];
  isLoading = false;
  activeTab: 'upcoming' | 'completed' | 'all' = 'upcoming';
  selectedSeason = '2024-2025';
  limit = 20;

  constructor(private matchService: MatchService) {}

  ngOnInit(): void {
    this.loadMatches();
  }

  loadMatches(): void {
    this.isLoading = true;

    if (this.activeTab === 'upcoming') {
      this.matchService.getUpcomingMatches(this.limit).subscribe({
        next: (response) => {
          this.matches = response.data;
          this.displayedMatches = this.matches;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.isLoading = false;
        },
      });
    } else {
      this.matchService.getMatchesBySeason(this.selectedSeason).subscribe({
        next: (response) => {
          this.matches = response.data;
          this.filterByTab();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur:', error);
          this.isLoading = false;
        },
      });
    }
  }

  filterByTab(): void {
    switch (this.activeTab) {
      case 'upcoming':
        this.displayedMatches = this.matches
          .filter((m) => m.status === MatchStatus.SCHEDULED)
          .slice(0, this.limit);
        break;
      case 'completed':
        this.displayedMatches = this.matches
          .filter((m) => m.status === MatchStatus.COMPLETED)
          .slice(0, this.limit);
        break;
      case 'all':
        this.displayedMatches = this.matches.slice(0, this.limit);
        break;
    }
  }
}
