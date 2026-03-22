---
description: Run performance profiling on mr_analizer server and generate report with recommendations
---

## User Input

```text
$ARGUMENTS
```

## Instructions

You are running a performance profiling session for the mr_analizer application. Follow these steps:

### 1. Parse parameters

Extract from user input (defaults in parentheses):
- `--duration N` (30) — profiling duration in seconds
- `--with-load` (false) — generate test load
- `--type cpu|alloc|full` (full) — profiling type
- `--load-iterations N` (10) — load test iterations
- `--load-concurrency N` (3) — concurrent requests

### 2. Run the profiling script

Execute from the mr_analizer project root:

```bash
cd ~/mr_analizer && bash scripts/profile.sh [parsed parameters]
```

If the script reports the server is not running, inform the user and suggest:
```bash
cd ~/mr_analizer/backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

If async-profiler is not installed, show the installation instructions from the script output.

### 3. Read and analyze the report

After the script completes:
1. Read the generated report file from `reports/profile-*.md` (the most recent one)
2. Analyze the collected metrics:
   - **HTTP Latency**: Identify slow endpoints (>500ms), high request counts
   - **Memory**: Check heap usage ratio, GC pressure (many pauses = memory pressure)
   - **SQL**: Check total query count (high = potential N+1), slow queries (>100ms), entity load count vs query count
3. If flame graphs were generated, note their paths for the user to open in a browser

### 4. Write recommendations

Replace the "Recommendations" section in the report with 3-5 concrete recommendations. Each recommendation must follow this format:

```markdown
### N. [HIGH|MEDIUM|LOW] Title

**Problem:** What is wrong and where (reference specific metrics)
**Impact:** How this affects performance (quantify if possible)
**Fix:** Specific code change or configuration adjustment
```

Prioritize by severity:
- **HIGH**: >1s latency, N+1 queries (entityLoadCount >> queryExecutionCount), heap >80%
- **MEDIUM**: 200ms-1s latency, >50 queries per request, GC pauses >100ms total
- **LOW**: Minor inefficiencies, potential optimizations

### 5. Present results

Show the user:
1. A brief summary (2-3 sentences) of the profiling results
2. The top 3 findings
3. Path to the full report
4. Paths to flame graphs (if generated) — remind user to open HTML in browser
