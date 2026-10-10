import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  imports: [],
  template: `
    <div class="flex items-center justify-center" [class.h-screen]="fullScreen">
      <div class="text-center">
        <div [class]="'spinner mx-auto ' + sizeClass"></div>
        @if (message) {
          <p class="mt-4 text-gray-600">{{ message }}</p>
        }
      </div>
    </div>
  `,
})
export class LoadingSpinnerComponent {
  @Input() message?: string;
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() fullScreen = false;

  get sizeClass(): string {
    const sizes = {
      sm: 'w-8 h-8',
      md: 'w-12 h-12',
      lg: 'w-16 h-16',
    };
    return sizes[this.size];
  }
}
