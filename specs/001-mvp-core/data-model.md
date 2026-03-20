# Data Model: MVP Core

## Domain Entities (pure — no JPA annotations)

### MergeRequest

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Internal ID (generated) |
| externalId | String | PR number from provider |
| title | String | PR title |
| description | String (nullable) | PR body/description |
| author | String | Username of author |
| sourceBranch | String | Source branch name |
| targetBranch | String | Target branch name |
| state | String | merged / closed / open |
| createdAt | LocalDateTime | When PR was created |
| mergedAt | LocalDateTime (nullable) | When PR was merged |
| labels | List\<String\> | Labels attached to PR |
| changedFiles | List\<ChangedFile\> | Files changed in PR |
| diffStats | DiffStats | Aggregate diff statistics |
| hasTests | boolean | Whether changes include test files |
| ciPassed | boolean | Whether CI checks passed |
| approvalsCount | int | Number of approvals |
| commentsCount | int | Number of comments |
| provider | String | "github" / "gitlab" |
| url | String | Web URL to PR |
| projectSlug | String | owner/repo |

### ChangedFile

| Field | Type | Description |
|-------|------|-------------|
| path | String | File path |
| additions | int | Lines added |
| deletions | int | Lines deleted |
| status | String | added / modified / deleted / renamed |

### DiffStats

| Field | Type | Description |
|-------|------|-------------|
| additions | int | Total lines added |
| deletions | int | Total lines deleted |
| changedFilesCount | int | Number of files changed |

### AnalysisResult

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Internal ID |
| mergeRequest | MergeRequest | The analyzed PR |
| score | double | 0.0 - 1.0 |
| verdict | Verdict | AUTOMATABLE / MAYBE / NOT_SUITABLE |
| reasons | List\<String\> | Human-readable explanations |
| matchedRules | List\<String\> | Names of rules that matched |
| llmComment | String (nullable) | Optional LLM assessment text |
| analyzedAt | LocalDateTime | When analysis was performed |

### Verdict (enum)

- `AUTOMATABLE` — score >= automatable-threshold (default 0.7)
- `MAYBE` — score >= maybe-threshold (default 0.4) and < automatable-threshold
- `NOT_SUITABLE` — score < maybe-threshold or excluded

### AnalysisReport

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Internal ID |
| projectSlug | String | Analyzed repository |
| provider | String | github / gitlab |
| analyzedAt | LocalDateTime | When report was created |
| totalMrs | int | Total PRs analyzed |
| results | List\<AnalysisResult\> | Individual results |
| automatableCount | int | Count of AUTOMATABLE |
| maybeCount | int | Count of MAYBE |
| notSuitableCount | int | Count of NOT_SUITABLE |

### FetchCriteria

| Field | Type | Description |
|-------|------|-------------|
| projectSlug | String | owner/repo |
| targetBranch | String (nullable) | Filter by target branch |
| state | String | merged / closed / all |
| after | LocalDate (nullable) | PRs created after this date |
| before | LocalDate (nullable) | PRs created before this date |
| limit | int | Max PRs to fetch (default 100) |

### LlmAssessment

| Field | Type | Description |
|-------|------|-------------|
| scoreAdjustment | double | -0.5 to +0.5 |
| comment | String | LLM explanation |
| provider | String | claude-cli / none |

### RuleResult

| Field | Type | Description |
|-------|------|-------------|
| ruleName | String | Name of the rule |
| matched | boolean | Whether rule matched |
| weight | double | Score adjustment |
| reason | String | Why it matched/didn't |

## JPA Entities (adapter layer)

Persistence entities mirror domain models with JPA annotations. Mapping done in repository adapter.

### AnalysisResultEntity

- `@Entity @Table(name = "analysis_results")`
- Fields: id, externalMrId, mrTitle, mrAuthor, projectSlug, provider, score, verdict, reasons (JSON string), matchedRules (JSON string), llmComment, analyzedAt, reportId

### AnalysisReportEntity

- `@Entity @Table(name = "analysis_reports")`
- Fields: id, projectSlug, provider, analyzedAt, totalMrs, automatableCount, maybeCount, notSuitableCount
- `@OneToMany` → AnalysisResultEntity

## Relationships

```
AnalysisReport 1──* AnalysisResult
AnalysisResult *──1 MergeRequest (embedded data, not separate table)
MergeRequest 1──* ChangedFile (transient, not persisted)
MergeRequest 1──1 DiffStats (transient, not persisted)
```

Note: MergeRequest and ChangedFile are NOT separate JPA entities — their key fields are flattened into AnalysisResultEntity. Full MR data is transient (fetched from API, not stored).
