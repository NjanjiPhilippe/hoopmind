import { EnvironmentInjector, createEnvironmentInjector, runInInjectionContext, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { pollWhile } from './poll';

describe('pollWhile', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  /** Un job suivi : chaque relecture produit un nouvel état, comme une ressource rechargée. */
  function job(initial: { running: boolean; failed: boolean }) {
    const state = signal(initial);
    const reload = vi.fn(() => state.update(current => ({ ...current })));
    return { state, reload };
  }

  function start(tracked: ReturnType<typeof job>, injector = TestBed.inject(EnvironmentInjector)): void {
    runInInjectionContext(injector, () =>
      pollWhile(() => tracked.state().running, tracked.reload, 1000, () => tracked.state().failed));
    TestBed.tick();
  }

  function tick(ms: number): void {
    vi.advanceTimersByTime(ms);
    TestBed.tick();
  }

  it('relit tant que le travail tourne, puis s’arrête', () => {
    const tracked = job({ running: true, failed: false });
    start(tracked);

    tick(1000);
    tick(1000);
    expect(tracked.reload).toHaveBeenCalledTimes(2);

    tracked.state.set({ running: false, failed: false });
    TestBed.tick();
    tick(5000);
    expect(tracked.reload).toHaveBeenCalledTimes(2);
  });

  it('continue après une relecture en échec si le travail tournait', () => {
    const tracked = job({ running: true, failed: false });
    start(tracked);

    tracked.state.set({ running: false, failed: true });
    TestBed.tick();
    tick(1000);
    expect(tracked.reload).toHaveBeenCalledTimes(1);

    tracked.state.set({ running: false, failed: false });
    TestBed.tick();
    tick(5000);
    expect(tracked.reload).toHaveBeenCalledTimes(1);
  });

  it('arrête le minuteur quand la page est quittée', () => {
    const tracked = job({ running: true, failed: false });
    const injector = createEnvironmentInjector([], TestBed.inject(EnvironmentInjector));
    start(tracked, injector);

    injector.destroy();
    tick(5000);
    expect(tracked.reload).not.toHaveBeenCalled();
  });
});
