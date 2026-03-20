# REST API Contract: MVP Core

Base URL: `http://localhost:8083/api`

## Endpoints

### POST /api/analysis

Uruchom analize repozytorium.

**Request Body:**
```json
{
  "projectSlug": "owner/repo",
  "provider": "github",
  "targetBranch": "main",
  "state": "merged",
  "after": "2025-01-01",
  "before": "2026-03-20",
  "limit": 100,
  "useLlm": true
}
```

**Response 200:**
```json
{
  "reportId": 1,
  "projectSlug": "owner/repo",
  "provider": "github",
  "analyzedAt": "2026-03-20T14:30:00",
  "totalMrs": 87,
  "automatableCount": 23,
  "maybeCount": 31,
  "notSuitableCount": 33,
  "results": [
    {
      "id": 1,
      "externalId": "421",
      "title": "Refactor user service to use repository pattern",
      "author": "dev-user",
      "score": 0.85,
      "verdict": "AUTOMATABLE",
      "reasons": ["boost: description contains 'refactor' (+0.2)", "boost: has tests (+0.15)"],
      "matchedRules": ["description-keywords", "has-tests"],
      "llmComment": "This PR is a straightforward refactoring...",
      "url": "https://github.com/owner/repo/pull/421"
    }
  ]
}
```

**Response 400:**
```json
{
  "error": "VALIDATION_ERROR",
  "message": "projectSlug is required"
}
```

**Response 401/403:**
```json
{
  "error": "AUTH_ERROR",
  "message": "GitHub token is invalid or missing permissions for this repository"
}
```

---

### GET /api/analysis

Lista raportow analiz.

**Response 200:**
```json
[
  {
    "reportId": 1,
    "projectSlug": "owner/repo",
    "provider": "github",
    "analyzedAt": "2026-03-20T14:30:00",
    "totalMrs": 87,
    "automatableCount": 23,
    "maybeCount": 31,
    "notSuitableCount": 33
  }
]
```

---

### GET /api/analysis/{reportId}

Szczegoly raportu z wynikami.

**Response 200:** Same as POST /api/analysis response.

**Response 404:**
```json
{
  "error": "NOT_FOUND",
  "message": "Analysis report not found"
}
```

---

### GET /api/analysis/{reportId}/mrs/{resultId}

Szczegoly pojedynczego MR z score breakdown.

**Response 200:**
```json
{
  "id": 1,
  "externalId": "421",
  "title": "Refactor user service to use repository pattern",
  "author": "dev-user",
  "description": "This PR refactors the user service...",
  "sourceBranch": "refactor/user-service",
  "targetBranch": "main",
  "state": "merged",
  "createdAt": "2025-12-01T10:00:00",
  "mergedAt": "2025-12-02T15:30:00",
  "labels": ["refactoring", "tech-debt"],
  "diffStats": {
    "additions": 120,
    "deletions": 85,
    "changedFilesCount": 8
  },
  "hasTests": true,
  "score": 0.85,
  "verdict": "AUTOMATABLE",
  "scoreBreakdown": [
    {"rule": "description-keywords", "type": "boost", "weight": 0.2, "reason": "description contains 'refactor'"},
    {"rule": "has-tests", "type": "boost", "weight": 0.15, "reason": "PR includes test changes"},
    {"rule": "changed-files-range", "type": "boost", "weight": 0.1, "reason": "8 files in sweet spot (3-15)"}
  ],
  "llmComment": "This PR is a straightforward refactoring...",
  "url": "https://github.com/owner/repo/pull/421"
}
```

---

### GET /api/summary/{reportId}

Podsumowanie raportu.

**Response 200:**
```json
{
  "reportId": 1,
  "projectSlug": "owner/repo",
  "totalMrs": 87,
  "automatable": {"count": 23, "percentage": 26.4},
  "maybe": {"count": 31, "percentage": 35.6},
  "notSuitable": {"count": 33, "percentage": 37.9}
}
```
