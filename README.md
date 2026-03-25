# MR Analizer

Aplikacja webowa analizująca Pull Requesty (GitHub) pod kątem automatyzowalności przez LLM. Dwuetapowy flow: przeglądanie PR-ów → analiza ze scoringiem. Moduł aktywności kontrybutora z wykrywaniem nieprawidłowości i metrykami wydajności.

## Wymagania

| Narzędzie | Wersja | Uwagi |
|-----------|--------|-------|
| Java (JDK) | 17+ | OpenJDK lub Oracle JDK |
| Maven | 3.8+ | Do budowania backendu |
| Node.js | 18+ (testy: 22+) | Do budowania frontendu |
| npm | 9+ | Dostarczany z Node.js |
| Git | 2.x | Do klonowania repo |
| GitHub Token | — | Personal Access Token z uprawnieniem `repo` |

## Instalacja

### 1. Klonowanie repozytorium

```bash
git clone https://github.com/wyhodujsam/mr-analizer.git
cd mr-analizer
```

### 2. GitHub Token

Utwórz Personal Access Token na https://github.com/settings/tokens z uprawnieniem `repo`.

**Linux / macOS:**
```bash
export GITHUB_TOKEN=ghp_twoj_token
```

**Windows (PowerShell):**
```powershell
$env:GITHUB_TOKEN = "ghp_twoj_token"
```

**Windows (cmd):**
```cmd
set GITHUB_TOKEN=ghp_twoj_token
```

Token jest wymagany do komunikacji z GitHub API. Bez niego aplikacja zwróci błąd autoryzacji.

### 3. Backend (Spring Boot)

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

Backend startuje na **http://localhost:8083**.

Baza danych H2 (plikowa) tworzy się automatycznie w `backend/data/mranalizer`. Dane przetrwają restart.

### 4. Frontend (React)

W nowym terminalu:

```bash
cd frontend
npm install
npm run dev
```

Frontend startuje na **http://localhost:3000** i automatycznie proxy'uje requesty API do backendu na porcie 8083.

### 5. Otwórz przeglądarkę

http://localhost:3000

## Instalacja na Windows 11

### Krok po kroku

#### 1. Zainstaluj Java 17

Pobierz i zainstaluj: https://adoptium.net/temurin/releases/?version=17

Podczas instalacji zaznacz opcję **"Set JAVA_HOME variable"**.

Weryfikacja w PowerShell:
```powershell
java -version
# openjdk version "17.x.x"
```

#### 2. Zainstaluj Maven

Pobierz binary zip: https://maven.apache.org/download.cgi

Rozpakuj np. do `C:\tools\apache-maven-3.9.9` i dodaj do PATH:

```powershell
# PowerShell (jednorazowo):
$env:Path += ";C:\tools\apache-maven-3.9.9\bin"

# Trwale: System → Zaawansowane ustawienia systemu → Zmienne środowiskowe → Path → Nowy
```

Weryfikacja:
```powershell
mvn -version
```

#### 3. Zainstaluj Node.js

Pobierz LTS: https://nodejs.org/

Instalator automatycznie dodaje `node` i `npm` do PATH.

Weryfikacja:
```powershell
node -v   # v18+ (zalecane v22 dla testów)
npm -v
```

#### 4. Zainstaluj Git

Pobierz: https://git-scm.com/download/win

Podczas instalacji wybierz domyślne opcje. Git Bash zostanie dodany do menu kontekstowego.

#### 5. Klonuj i uruchom

**PowerShell:**
```powershell
git clone https://github.com/wyhodujsam/mr-analizer.git
cd mr-analizer

# Ustaw token
$env:GITHUB_TOKEN = "ghp_twoj_token"

# Backend
cd backend
mvn clean install -DskipTests
mvn spring-boot:run
```

**Nowe okno PowerShell:**
```powershell
cd mr-analizer\frontend
npm install
npm run dev
```

Otwórz http://localhost:3000

#### Alternatywa: Git Bash

Jeśli wolisz linuxowy terminal, użyj Git Bash (instaluje się z Git for Windows):

```bash
export GITHUB_TOKEN=ghp_twoj_token
cd backend && mvn spring-boot:run &
cd ../frontend && npm install && npm run dev
```

### Rozwiązywanie problemów (Windows)

| Problem | Rozwiązanie |
|---------|-------------|
| `mvn: command not found` | Dodaj Maven `bin/` do zmiennej PATH |
| `JAVA_HOME is not set` | Ustaw JAVA_HOME na katalog JDK (np. `C:\Program Files\Eclipse Adoptium\jdk-17`) |
| Port 8083 zajęty | `netstat -ano \| findstr :8083` → `taskkill /PID <pid> /F` |
| Port 3000 zajęty | `netstat -ano \| findstr :3000` → `taskkill /PID <pid> /F` |
| `GITHUB_TOKEN` nie działa | W PowerShell: `$env:GITHUB_TOKEN`, w cmd: `set GITHUB_TOKEN` — sprawdź czy jest ustawiony |
| `npm install` wolne | Użyj `npm install --prefer-offline` lub ustaw mirror: `npm config set registry https://registry.npmmirror.com` |
| Błąd kompilacji Lomboka | Upewnij się, że używasz JDK 17 (nie JRE) |

## Uruchamianie testów

**Backend** (Java — unit + BDD + integration):
```bash
cd backend
mvn test
```

**Frontend** (Vitest — unit):
```bash
cd frontend
npm test
```

**Frontend E2E** (Playwright):
```bash
cd frontend
npx playwright install chromium
npx playwright test
```

## Konfiguracja

Plik `backend/src/main/resources/application.yml`:

| Parametr | Domyślna wartość | Opis |
|----------|-----------------|------|
| `server.port` | 8083 | Port backendu |
| `mr-analizer.provider` | github | Provider VCS (github) |
| `mr-analizer.github.token` | `$GITHUB_TOKEN` | Token GitHub API |
| `mr-analizer.llm.adapter` | claude-cli | Adapter LLM (claude-cli / none) |
| `mr-analizer.scoring.base-score` | 0.5 | Bazowy score PR |
| `mr-analizer.scoring.automatable-threshold` | 0.7 | Próg "automatyzowalny" |

Profil `dev` włącza konsolę H2 i diagnostykę:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# H2 Console: http://localhost:8083/h2-console
# JDBC URL: jdbc:h2:file:./data/mranalizer
```

## Porty

| Serwis | Port | URL |
|--------|------|-----|
| Backend API | 8083 | http://localhost:8083/api/ |
| Frontend (dev) | 3000 | http://localhost:3000 |
| H2 Console (dev) | 8083 | http://localhost:8083/h2-console |
| Actuator | 8083 | http://localhost:8083/actuator/health |

## Stack technologiczny

**Backend:** Java 17, Spring Boot 3.2, Spring Data JPA, WebFlux Client, H2, Cucumber 7, JUnit 5

**Frontend:** React 18, TypeScript 5, Vite 5, Bootstrap 5, Axios, Vitest, Playwright
