import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

/** Champ mot de passe avec un bouton pour afficher ou masquer la saisie. */
@Component({
  selector: 'app-password-input',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <input [id]="inputId()" [type]="visible() ? 'text' : 'password'" [formControl]="control()" class="input-field pr-24"
             [attr.autocomplete]="autocomplete()" [attr.aria-describedby]="describedBy() || null" />
      <button type="button" class="absolute inset-y-0 right-0 px-3 text-sm font-medium text-primary-600 hover:text-primary-700"
              [attr.aria-controls]="inputId()" [attr.aria-pressed]="visible()" (click)="visible.set(!visible())">
        {{ visible() ? 'Masquer' : 'Afficher' }}
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
