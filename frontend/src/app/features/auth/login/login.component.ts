import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { toProblem } from '../../../core/http/problem';
import { retryAfterSeconds } from '../../../core/http/retry-after';
import { AuthLayoutComponent } from '../auth-layout.component';
import { PasswordInputComponent } from '../../../shared/components/password-input/password-input.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, AuthLayoutComponent, PasswordInputComponent],
  templateUrl: './login.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Page demandée avant la redirection vers la connexion (paramètre de requête). */
  readonly returnUrl = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    login: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  /** Secondes restantes avant de pouvoir réessayer après un 429 (trop de tentatives) ; 0 si la connexion est libre. */
  protected readonly remainingSeconds = signal(0);
  protected readonly blocked = computed(() => this.remainingSeconds() > 0);
  private countdown: ReturnType<typeof setInterval> | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearInterval(this.countdown));
  }

  async submit(): Promise<void> {
    if (this.blocked()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    try {
      await this.auth.login(this.form.getRawValue());
      await this.router.navigateByUrl(this.safeReturnUrl());
    } catch (e) {
      const wait = retryAfterSeconds(e);
      if (wait !== null) {
        this.startCountdown(wait);
      } else {
        this.error.set(toProblem(e).detail ?? 'Connexion impossible');
      }
    } finally {
      this.submitting.set(false);
    }
  }

  /** Le serveur refuse toute tentative pendant ce délai : le bouton reste désactivé et le décompte s'affiche. */
  private startCountdown(seconds: number): void {
    clearInterval(this.countdown);
    const end = Date.now() + seconds * 1000;
    const tick = () => {
      const left = Math.max(0, Math.ceil((end - Date.now()) / 1000));
      this.remainingSeconds.set(left);
      if (left === 0) {
        clearInterval(this.countdown);
      }
    };
    tick();
    this.countdown = setInterval(tick, 1000);
  }

  /** N'accepte que des chemins internes, pour éviter une redirection ouverte. */
  private safeReturnUrl(): string {
    const url = this.returnUrl();
    return url && url.startsWith('/') && !url.startsWith('//') ? url : '/dashboard';
  }
}
