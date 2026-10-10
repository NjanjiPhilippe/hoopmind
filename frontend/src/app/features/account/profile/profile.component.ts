import { ChangeDetectionStrategy, Component, OnInit, inject, resource, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Api } from '../../../core/api/generated/api';
import { changeMyPassword } from '../../../core/api/generated/fn/mon-compte/change-my-password';
import { updateMyProfile } from '../../../core/api/generated/fn/mon-compte/update-my-profile';
import { getAllTeams } from '../../../core/api/generated/fn/teams/get-all-teams';
import { AuthService } from '../../../core/auth/auth.service';
import { toProblem } from '../../../core/http/problem';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, NavbarComponent],
  templateUrl: './profile.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly teams = resource({ loader: () => this.api.invoke(getAllTeams) });

  protected readonly profileForm = this.fb.group({
    firstName: ['', Validators.maxLength(100)],
    lastName: ['', Validators.maxLength(100)],
    bio: ['', Validators.maxLength(1000)],
    favoriteTeamId: this.fb.control<number | null>(null),
  });
  protected readonly savingProfile = signal(false);
  protected readonly profileMessage = signal<{ ok: boolean; text: string } | null>(null);

  protected readonly passwordForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
  });
  protected readonly savingPassword = signal(false);
  protected readonly passwordMessage = signal<{ ok: boolean; text: string } | null>(null);

  ngOnInit(): void {
    const user = this.auth.user();
    if (user) {
      this.profileForm.reset({
        firstName: user.firstName ?? '',
        lastName: user.lastName ?? '',
        bio: user.bio ?? '',
        favoriteTeamId: user.favoriteTeamId ?? null,
      });
    }
  }

  async saveProfile(): Promise<void> {
    if (this.profileForm.invalid) return;
    this.savingProfile.set(true);
    this.profileMessage.set(null);
    try {
      const { favoriteTeamId, ...names } = this.profileForm.getRawValue();
      const user = await this.api.invoke(updateMyProfile, {
        body: { ...names, favoriteTeamId: favoriteTeamId ?? undefined },
      });
      this.auth.updateUser(user);
      this.profileForm.markAsPristine();
      this.profileMessage.set({ ok: true, text: 'Profil enregistré' });
    } catch (e) {
      this.profileMessage.set({ ok: false, text: toProblem(e).detail ?? 'Enregistrement impossible' });
    } finally {
      this.savingProfile.set(false);
    }
  }

  async changePassword(): Promise<void> {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }
    this.savingPassword.set(true);
    this.passwordMessage.set(null);
    const { newPassword } = this.passwordForm.getRawValue();
    try {
      await this.api.invoke(changeMyPassword, { body: this.passwordForm.getRawValue() });
      // Le backend ferme toutes les sessions : on en rouvre une avec le nouveau mot de passe.
      await this.auth.login({ login: this.auth.user()!.username, password: newPassword });
      this.passwordForm.reset();
      this.passwordMessage.set({ ok: true, text: 'Mot de passe modifié. Vos autres sessions ont été fermées.' });
    } catch (e) {
      this.passwordMessage.set({ ok: false, text: toProblem(e).detail ?? 'Modification impossible' });
    } finally {
      this.savingPassword.set(false);
    }
  }
}
