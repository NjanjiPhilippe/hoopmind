import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { problemCode, toProblem } from '../../../core/http/problem';
import { AuthLayoutComponent } from '../auth-layout.component';

/** Mêmes règles que le backend (RegisterRequest). */
export const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, AuthLayoutComponent],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(USERNAME_PATTERN)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    firstName: ['', Validators.maxLength(100)],
    lastName: ['', Validators.maxLength(100)],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.register(this.form.getRawValue());
      await this.router.navigate(['/dashboard']);
    } catch (e) {
      const problem = toProblem(e);
      switch (problemCode(problem)) {
        case 'username-taken':
          this.form.controls.username.setErrors({ taken: true });
          break;
        case 'email-taken':
          this.form.controls.email.setErrors({ taken: true });
          break;
        default:
          this.error.set(problem.detail ?? 'Inscription impossible');
      }
    } finally {
      this.submitting.set(false);
    }
  }
}
