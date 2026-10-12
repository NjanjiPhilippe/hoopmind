import { Injector, resource, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { currentValue } from './resource';

describe('currentValue', () => {
  it('returns undefined instead of throwing when the resource failed', async () => {
    const failing = runInInjectionContext(TestBed.inject(Injector), () =>
      resource({ loader: () => Promise.reject(new Error('serveur injoignable')) }));
    await new Promise(resolve => setTimeout(resolve));
    TestBed.tick();

    expect(failing.error()).toBeTruthy();
    expect(() => failing.value()).toThrow();
    expect(currentValue(failing)).toBeUndefined();
  });

  it('returns the loaded value', async () => {
    const loaded = runInInjectionContext(TestBed.inject(Injector), () => resource({ loader: async () => 42 }));
    await new Promise(resolve => setTimeout(resolve));
    TestBed.tick();

    expect(currentValue(loaded)).toBe(42);
  });
});
