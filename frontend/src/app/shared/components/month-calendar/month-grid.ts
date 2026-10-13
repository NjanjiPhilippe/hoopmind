/** Une case de la grille d'un mois : un jour (« 2023-11-02 ») et s'il appartient au mois affiché. */
export interface GridDay {
  date: string;
  inMonth: boolean;
}

/** « 2023-11 » pour un jour « 2023-11-02 ». */
export function monthOf(day: string): string {
  return day.slice(0, 7);
}

/** Le mois suivant (+1) ou précédent (-1) : « 2023-12 » + 1 donne « 2024-01 ». */
export function shiftMonth(month: string, delta: number): string {
  const [year, value] = month.split('-').map(Number);
  const index = year * 12 + (value - 1) + delta;
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`;
}

/**
 * Les semaines d'un mois, du lundi au dimanche, comme dans un agenda : la première commence au lundi qui précède
 * (ou est) le 1er, la dernière finit au dimanche qui suit (ou est) le dernier jour.
 */
export function monthGrid(month: string): GridDay[][] {
  const [year, value] = month.split('-').map(Number);
  const first = new Date(Date.UTC(year, value - 1, 1));
  const daysInMonth = new Date(Date.UTC(year, value, 0)).getUTCDate();
  const offset = (first.getUTCDay() + 6) % 7; // lundi = 0
  const cells = Math.ceil((offset + daysInMonth) / 7) * 7;
  const weeks: GridDay[][] = [];
  for (let i = 0; i < cells; i++) {
    const date = new Date(Date.UTC(year, value - 1, 1 - offset + i)).toISOString().slice(0, 10);
    if (i % 7 === 0) {
      weeks.push([]);
    }
    weeks.at(-1)!.push({ date, inMonth: date.startsWith(month) });
  }
  return weeks;
}
