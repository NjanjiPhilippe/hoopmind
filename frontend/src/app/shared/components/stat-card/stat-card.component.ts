import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-stat-card',
  imports: [],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.css'],
})
export class StatCardComponent {
  @Input() label!: string;
  @Input() value!: string | number;
  @Input() change?: string;
  @Input() icon?: string;
  @Input() trend?: 'up' | 'down' | 'neutral';
  @Input() color: 'blue' | 'green' | 'red' | 'orange' = 'blue';

  get changeClass(): string {
    if (!this.trend) return 'text-gray-600';
    return {
      up: 'text-green-600',
      down: 'text-red-600',
      neutral: 'text-gray-600',
    }[this.trend];
  }

  get iconBgClass(): string {
    const colors = {
      blue: 'bg-blue-100 text-blue-600',
      green: 'bg-green-100 text-green-600',
      red: 'bg-red-100 text-red-600',
      orange: 'bg-orange-100 text-orange-600',
    };
    return colors[this.color];
  }
}
