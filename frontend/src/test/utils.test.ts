import { describe, it, expect } from 'vitest';
import { formatDate } from '../utils/format';
import { verdictClass, verdictLabel } from '../utils/verdict';
import { extractApiError } from '../utils/error';

describe('formatDate', () => {
  it('formats valid ISO string', () => {
    const result = formatDate('2026-03-20T10:00:00');
    // toLocaleString output varies by locale, just check it's not a dash
    expect(result).not.toBe('\u2014');
    expect(result.length).toBeGreaterThan(0);
  });

  it('returns dash for null', () => {
    expect(formatDate(null)).toBe('\u2014');
  });

  it('returns dash for undefined', () => {
    expect(formatDate(undefined)).toBe('\u2014');
  });

  it('returns dash for empty string', () => {
    expect(formatDate('')).toBe('\u2014');
  });
});

describe('verdictClass', () => {
  it('returns text-success for AUTOMATABLE', () => {
    expect(verdictClass('AUTOMATABLE')).toBe('text-success');
  });

  it('returns text-warning for MAYBE', () => {
    expect(verdictClass('MAYBE')).toBe('text-warning');
  });

  it('returns text-danger for NOT_SUITABLE', () => {
    expect(verdictClass('NOT_SUITABLE')).toBe('text-danger');
  });

  it('returns text-muted for null', () => {
    expect(verdictClass(null)).toBe('text-muted');
  });
});

describe('verdictLabel', () => {
  it('returns Auto for AUTOMATABLE', () => {
    expect(verdictLabel('AUTOMATABLE')).toBe('Auto');
  });

  it('returns Moze for MAYBE', () => {
    expect(verdictLabel('MAYBE')).toBe('Moze');
  });

  it('returns Nie for NOT_SUITABLE', () => {
    expect(verdictLabel('NOT_SUITABLE')).toBe('Nie');
  });

  it('returns dash for null', () => {
    expect(verdictLabel(null)).toBe('\u2014');
  });
});

describe('extractApiError', () => {
  it('extracts message from axios-like error', () => {
    const err = {
      response: {
        data: {
          message: 'Rate limit exceeded',
        },
      },
    };
    expect(extractApiError(err)).toBe('Rate limit exceeded');
  });

  it('returns default message when response data has no message', () => {
    const err = {
      response: {
        data: {},
      },
    };
    expect(extractApiError(err)).toBe('Wystapil nieoczekiwany blad.');
  });

  it('returns connection error for plain Error', () => {
    const err = new Error('Network failure');
    expect(extractApiError(err)).toBe('Nie udalo sie polaczyc z serwerem.');
  });

  it('returns connection error for string', () => {
    expect(extractApiError('some error')).toBe('Nie udalo sie polaczyc z serwerem.');
  });

  it('returns connection error for null', () => {
    expect(extractApiError(null)).toBe('Nie udalo sie polaczyc z serwerem.');
  });

  it('returns connection error for undefined', () => {
    expect(extractApiError(undefined)).toBe('Nie udalo sie polaczyc z serwerem.');
  });
});
