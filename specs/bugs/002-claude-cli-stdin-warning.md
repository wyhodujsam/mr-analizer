# BUG-002: Claude CLI stdin warning psuje parsing JSON

## Opis

Analiza LLM kończy się błędem parsowania — `llmComment` zawiera "LLM error: Unrecognized token 'Warning'".

## Przyczyna

`ClaudeCliAdapter` używał `redirectErrorStream(true)` co mieszało stderr z stdout. Claude CLI wypisywał na stderr: `Warning: no stdin data received in 3s, proceeding without it.` — to wchodziło do JSON outputu i parser się wysypywał.

## Fix

1. Usunięto `redirectErrorStream(true)` — stderr idzie osobno
2. Dodano `redirectInput(Redirect.from(new File("/dev/null")))` — stdin jest pusty, brak warninga
