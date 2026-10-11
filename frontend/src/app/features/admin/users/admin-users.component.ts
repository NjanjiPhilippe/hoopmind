import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal, resource, signal } from '@angular/core';

import { RouterLink } from '@angular/router';

import { Api } from '../../../core/api/generated/api';
import { deleteUser } from '../../../core/api/generated/fn/administration-des-utilisateurs/delete-user';
import { listUsers } from '../../../core/api/generated/fn/administration-des-utilisateurs/list-users';
import { setUserRoles } from '../../../core/api/generated/fn/administration-des-utilisateurs/set-user-roles';
import { setUserStatus } from '../../../core/api/generated/fn/administration-des-utilisateurs/set-user-status';
import { UserProfile } from '../../../core/api/generated/models/user-profile';
import { AuthService } from '../../../core/auth/auth.service';
import { toProblem } from '../../../core/http/problem';
import { NavbarComponent } from '../../../shared/components/navbar/navbar.component';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-admin-users',
  imports: [DatePipe, RouterLink, NavbarComponent],
  templateUrl: './admin-users.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminUsersComponent {
  private readonly api = inject(Api);
  protected readonly auth = inject(AuthService);

  protected readonly search = signal('');
  /** Revient à la première page à chaque nouvelle recherche. */
  protected readonly page = linkedSignal({ source: this.search, computation: () => 0 });

  protected readonly users = resource({
    params: () => ({ search: this.search() || undefined, page: this.page(), size: PAGE_SIZE }),
    loader: ({ params }) => this.api.invoke(listUsers, params),
  });
  protected readonly rows = computed(() => this.users.value()?.content ?? []);
  protected readonly pageInfo = computed(() => this.users.value()?.page);

  protected readonly busyUserId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  private searchTimer?: ReturnType<typeof setTimeout>;

  onSearch(value: string): void {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.search.set(value.trim()), 300);
  }

  isAdmin(user: UserProfile): boolean {
    return user.roles.includes('ROLE_ADMIN');
  }

  isSelf(user: UserProfile): boolean {
    return user.id === this.auth.user()?.id;
  }

  toggleEnabled(user: UserProfile): Promise<void> {
    return this.run(user, () => this.api.invoke(setUserStatus, { id: user.id, body: { enabled: !user.enabled } }));
  }

  toggleAdmin(user: UserProfile): Promise<void> {
    const roles: UserProfile['roles'] = this.isAdmin(user) ? ['ROLE_USER'] : ['ROLE_USER', 'ROLE_ADMIN'];
    return this.run(user, () => this.api.invoke(setUserRoles, { id: user.id, body: { roles } }));
  }

  remove(user: UserProfile): Promise<void> {
    if (!confirm(`Supprimer définitivement le compte « ${user.username} » ?`)) {
      return Promise.resolve();
    }
    return this.run(user, () => this.api.invoke(deleteUser, { id: user.id }));
  }

  private async run(user: UserProfile, action: () => Promise<unknown>): Promise<void> {
    this.busyUserId.set(user.id);
    this.error.set(null);
    try {
      await action();
      this.users.reload();
    } catch (e) {
      this.error.set(toProblem(e).detail ?? 'Action impossible');
    } finally {
      this.busyUserId.set(null);
    }
  }
}
