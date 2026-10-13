import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';

import { GameResponse } from '../../../core/api/generated/models/game-response';
import { teamName } from '../../../core/nba/nba-catalog';
import { CalendarGame, teamCalendar } from '../../../core/nba/team-calendar';
import { TeamLogoComponent } from '../team-logo/team-logo.component';
import { GridDay, monthGrid, shiftMonth } from './month-grid';

const WEEKDAYS = ['lun.', 'mar.', 'mer.', 'jeu.', 'ven.', 'sam.', 'dim.'];

/**
 * Agenda d'un mois, du lundi au dimanche, comme Google Agenda. Deux usages :
 * - avec une équipe ({@code teamId}) : chaque case montre son match du jour (adversaire, lieu, V ou D), un clic choisit
 *   ce match ;
 * - sans équipe : chaque case montre les affiches du jour, un clic choisit le jour.
 * Les flèches parcourent les mois de la saison (du premier au dernier match).
 */
@Component({
  selector: 'app-month-calendar',
  imports: [DatePipe, TeamLogoComponent],
  templateUrl: './month-calendar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonthCalendarComponent {
  readonly games = input<GameResponse[]>([]);
  readonly teamId = input<number | null>(null);
  readonly selectedGameId = input<number | null>(null);
  readonly selectedDay = input<string | null>(null);
  /** Mois affiché (« 2023-11 »). */
  readonly month = model.required<string>();

  readonly gameSelected = output<GameResponse>();
  readonly daySelected = output<string>();

  protected readonly weekdays = WEEKDAYS;
  protected readonly teamName = teamName;
  protected readonly weeks = computed(() => monthGrid(this.month()));

  /** Premier et dernier mois qui ont des matchs : les flèches s'arrêtent là. */
  private readonly bounds = computed(() => {
    const months = this.games().map(game => (game.scheduledDate ?? '').slice(0, 7)).filter(Boolean).sort();
    return { first: months[0] ?? this.month(), last: months.at(-1) ?? this.month() };
  });
  protected readonly canGoBack = computed(() => this.month() > this.bounds().first);
  protected readonly canGoForward = computed(() => this.month() < this.bounds().last);

  /** Matchs de l'équipe par jour (vus de son côté). */
  protected readonly teamDays = computed(() => {
    const team = this.teamId();
    const byDay = new Map<string, CalendarGame>();
    if (team !== null) {
      teamCalendar(this.games(), team).months.flatMap(month => month.games)
        .forEach(entry => byDay.set((entry.game.scheduledDate ?? '').slice(0, 10), entry));
    }
    return byDay;
  });

  /** Tous les matchs par jour. */
  protected readonly allDays = computed(() => {
    const byDay = new Map<string, GameResponse[]>();
    for (const game of this.games()) {
      const day = (game.scheduledDate ?? '').slice(0, 10);
      byDay.set(day, [...(byDay.get(day) ?? []), game]);
    }
    return byDay;
  });

  dayNumber(day: GridDay): number {
    return Number(day.date.slice(8));
  }

  go(delta: number): void {
    this.month.set(shiftMonth(this.month(), delta));
  }

  pick(day: GridDay): void {
    const entry = this.teamDays().get(day.date);
    if (this.teamId() !== null) {
      if (entry) {
        this.gameSelected.emit(entry.game);
      }
    } else if (this.allDays().has(day.date)) {
      this.daySelected.emit(day.date);
    }
  }

  /** Une case est cliquable si elle porte un match (équipe) ou au moins un match (jour). */
  clickable(day: GridDay): boolean {
    return this.teamId() !== null ? this.teamDays().has(day.date) : this.allDays().has(day.date);
  }

  selected(day: GridDay): boolean {
    const entry = this.teamDays().get(day.date);
    return this.teamId() !== null ? !!entry && entry.game.id === this.selectedGameId() : day.date === this.selectedDay();
  }

  describe(day: GridDay): string {
    const entry = this.teamDays().get(day.date);
    if (entry) {
      const result = entry.outcome ? `, ${entry.outcome === 'V' ? 'victoire' : 'défaite'} ${entry.teamScore}-${entry.opponentScore}` : ', à venir';
      return `${day.date} : ${entry.home ? 'contre' : 'chez'} ${teamName(entry.opponent)}${result}`;
    }
    const count = this.allDays().get(day.date)?.length ?? 0;
    return count ? `${day.date} : ${count} match(s)` : day.date;
  }
}
