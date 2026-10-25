import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconComponent } from '../icon/icon.component';

/** Champ mot de passe avec un bouton pour afficher ou masquer la saisie. */
@Component({
  selector: 'app-password-input',
  imports: [ReactiveFormsModule, IconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <input [id]="inputId()" [type]="visible() ? 'text' : 'password'" [formControl]="control()" class="input-field pr-12"
             [attr.autocomplete]="autocomplete()" [attr.aria-describedby]="describedBy() || null" />
      <button type="button" class="absolute inset-y-0 right-0 flex items-center px-3 text-gray-500 hover:text-primary-700"
              [attr.aria-controls]="inputId()" [attr.aria-pressed]="visible()"
              [attr.aria-label]="visible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'"
              [attr.title]="visible() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'" (click)="visible.set(!visible())">
        <app-icon [name]="visible() ? 'eye-off' : 'eye'" [size]="20" />
      </button>
    </div>
  `,
})
export class PasswordInputComponent {
  readonly control = input.required<FormControl<string>>();
  readonly inputId = input.required<string>();
  readonly autocomplete = input('current-password');
  readonly describedBy = input('');
  protected readonly visible = signal(false);
}
