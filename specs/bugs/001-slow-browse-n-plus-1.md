# BUG-001: Wolne pobieranie MR — N+1 problem z GitHub API

## Opis

Browse MR jest bardzo wolny dla repozytoriów z wieloma PR-ami. Pobranie 10 PR-ów trwa ~15-20 sekund.

## Przyczyna

`GitHubAdapter.fetchMergeRequests()` (linia 52) robi osobny `client.fetchFiles()` call dla **każdego** PR-a w pętli stream:

```java
.map(pr -> {
    List<GitHubFile> files = client.fetchFiles(owner, repo, pr.getNumber());
    return mapper.toDomain(pr, files, projectSlug);
})
```

10 PR-ów = 1 API call na listę + 10 API callów na pliki = **11 requestów HTTP** do GitHub API. Każdy ~1-2s.

## Wpływ

- UX: użytkownik czeka 15-20s na wyniki browse
- Rate limit: szybko wyczerpuje limity GitHub API (5000/h authenticated)

## Możliwe rozwiązania

1. **Równoległe fetchowanie plików** — `CompletableFuture.supplyAsync()` dla każdego PR, potem `allOf().join()`. Zmniejszy czas z N*T do ~T.
2. **Lazy loading plików** — nie pobierać plików przy browse, tylko przy analizie. Browse potrzebuje tylko metadanych PR (tytuł, autor, daty). Pliki potrzebne dopiero przy scoringu.
3. **Cache per-PR files** — jeśli PR był już analizowany, nie pobierać plików ponownie.

## Rekomendacja

Opcja 2 (lazy loading) — najczystsza. Browse nie potrzebuje plików, a `changedFilesCount` jest dostępny w samym PR response z GitHub (pole `changed_files`). Pliki pobierać tylko w `fetchMergeRequest()` (per-MR analysis).

## Priorytet

Średni — nie blokuje, ale znacząco psuje UX.
