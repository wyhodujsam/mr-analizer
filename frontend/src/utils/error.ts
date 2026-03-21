export function extractApiError(err: unknown): string {
  if (
    err &&
    typeof err === 'object' &&
    'response' in err &&
    err.response &&
    typeof err.response === 'object' &&
    'data' in err.response
  ) {
    const data = (err.response as { data?: { message?: string } }).data;
    return data?.message ?? 'Wystapil nieoczekiwany blad.';
  }
  return 'Nie udalo sie polaczyc z serwerem.';
}
