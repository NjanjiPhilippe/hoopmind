import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Cadre commun des pages de connexion et d'inscription. */
@Component({
  selector: 'app-auth-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex min-h-screen items-center justify-center bg-gradient-to-br from-primary-600 to-primary-900 px-4 py-12">
      <div class="w-full max-w-md rounded-2xl bg-white p-8 shadow-2xl">
        <div class="mb-8 text-center">
          <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br from-primary-600 to-primary-800 text-3xl">🏀</div>
          <h1 class="text-3xl font-bold text-gray-900">{{ heading() }}</h1>
          <p class="mt-2 text-gray-600">{{ subheading() }}</p>
        </div>
        <ng-content />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {
  readonly heading = input.required<string>();
  readonly subheading = input('');
}
