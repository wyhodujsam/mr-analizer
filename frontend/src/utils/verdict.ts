import type { Verdict } from '../types';

export function verdictClass(v: Verdict | null): string {
  switch (v) {
    case 'AUTOMATABLE': return 'text-success';
    case 'MAYBE': return 'text-warning';
    case 'NOT_SUITABLE': return 'text-danger';
    default: return 'text-muted';
  }
}

export function verdictLabel(v: Verdict | null): string {
  switch (v) {
    case 'AUTOMATABLE': return 'Auto';
    case 'MAYBE': return 'Moze';
    case 'NOT_SUITABLE': return 'Nie';
    default: return '\u2014';
  }
}
