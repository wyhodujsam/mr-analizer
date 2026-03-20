# Quickstart: MVP Core

## Prerequisites

- Java 17
- Maven 3.8+
- Node.js 18+ / npm
- GitHub Personal Access Token (z uprawnieniami `repo`)

## Setup

### 1. Backend

```bash
cd backend
export GITHUB_TOKEN=ghp_your_token_here
mvn clean install
mvn spring-boot:run
```

Backend startuje na `http://localhost:8083`

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend startuje na `http://localhost:3000` (proxy do backend na 8083)

## Weryfikacja

### Test 1: Analiza repozytorium (US1 + US2)

1. Otworz `http://localhost:3000`
2. Wpisz `facebook/react` w pole "Project slug"
3. Ustaw zakres dat na ostatni miesiac
4. Kliknij "Analizuj"
5. Oczekiwany wynik: tabela z PR, kazdy z score i verdict
6. Kliknij na dowolny PR — powinna pojawic sie strona szczegulow ze score breakdown

### Test 2: Reguly exclude (US2)

1. Uruchom analize repozytorium ktore ma PR z labelem "hotfix"
2. Oczekiwany wynik: PR z "hotfix" ma verdict NOT_SUITABLE

### Test 3: LLM analiza (US3)

1. Upewnij sie ze Claude CLI jest zainstalowane (`claude --version`)
2. W `application.yml` ustaw `mr-analizer.llm.adapter: claude-cli`
3. Uruchom analize
4. Oczekiwany wynik: wyniki zawieraja komentarz LLM

### Test 4: Persystencja (US5)

1. Uruchom analize
2. Zrestartuj backend (`Ctrl+C`, `mvn spring-boot:run`)
3. Oczekiwany wynik: poprzednie wyniki sa nadal dostepne na dashboardzie

## Testy automatyczne

```bash
# Testy jednostkowe + BDD
cd backend
mvn test

# Tylko testy BDD (Cucumber)
cd backend
mvn test -Dcucumber.filter.tags="@bdd"
```

## Troubleshooting

- **401 z GitHub API**: Sprawdz czy GITHUB_TOKEN jest ustawiony i ma uprawnienia `repo`
- **Rate limit**: Zmniejsz limit PR w formularzu (np. do 20)
- **Claude CLI timeout**: Sprawdz czy `claude` jest w PATH, zwieksz timeout w application.yml
- **CORS error**: Sprawdz czy backend dziala na 8083, Vite proxy powinien to obslugiwac
