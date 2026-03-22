#!/usr/bin/env bash
# MR Analizer — Performance Profiling Script
# Usage: bash scripts/profile.sh [--duration 30] [--with-load] [--type cpu|alloc|full]
#        [--load-iterations 10] [--load-concurrency 3]

set -euo pipefail

# ── Defaults ──────────────────────────────────────────────────────────────
DURATION=30
WITH_LOAD=false
PROFILE_TYPE="full"
LOAD_ITERATIONS=10
LOAD_CONCURRENCY=3
BASE_URL="http://localhost:8083"
ASPROF_PATH="${ASPROF_PATH:-$HOME/tools/async-profiler/bin/asprof}"
REPORT_DIR="reports"
FLAMEGRAPH_DIR="$REPORT_DIR/flamegraphs"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
EPOCH=$(date +%s)

# ── Parse arguments ──────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --duration)       DURATION="$2";          shift 2 ;;
        --with-load)      WITH_LOAD=true;         shift   ;;
        --type)           PROFILE_TYPE="$2";      shift 2 ;;
        --load-iterations)   LOAD_ITERATIONS="$2";  shift 2 ;;
        --load-concurrency)  LOAD_CONCURRENCY="$2"; shift 2 ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "  --duration N          Profiling duration in seconds (default: 30)"
            echo "  --with-load           Generate test load during profiling"
            echo "  --type TYPE           cpu|alloc|full (default: full)"
            echo "  --load-iterations N   Number of load iterations (default: 10)"
            echo "  --load-concurrency N  Concurrent requests (default: 3)"
            exit 0
            ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# ── Helper functions ─────────────────────────────────────────────────────
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
warn()  { echo -e "\033[1;33m[WARN]\033[0m  $*"; }
error() { echo -e "\033[1;31m[ERROR]\033[0m $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }

check_dependency() {
    if ! command -v "$1" &>/dev/null; then
        error "$1 is not installed. $2"
        return 1
    fi
}

# ── Pre-flight checks ───────────────────────────────────────────────────
info "MR Analizer Performance Profiler"
info "Duration: ${DURATION}s | Type: ${PROFILE_TYPE} | Load: ${WITH_LOAD}"
echo ""

# Check jq
if ! check_dependency jq "Install with: sudo apt install jq"; then
    exit 1
fi

# Health check
info "Checking server health..."
if ! curl -sf "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    error "Server is not running at ${BASE_URL}"
    echo "  Start with: cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
    exit 1
fi
ok "Server is running"

# Find JVM PID (try multiple strategies)
PID=""
# Strategy 1: jps (most reliable for Java apps)
if command -v jps &>/dev/null; then
    PID=$(jps -l 2>/dev/null | grep -i 'mranalizer\|MrAnalizer' | awk '{print $1}' | head -1 || true)
fi
# Strategy 2: ss/port-based detection
if [[ -z "$PID" ]]; then
    PID=$(ss -tlnp 2>/dev/null | grep ":8083" | grep -oP 'pid=\K[0-9]+' | head -1 || true)
fi
# Strategy 3: pgrep broad match
if [[ -z "$PID" ]]; then
    PID=$(pgrep -f 'mranalizer\|mr.analizer' 2>/dev/null | head -1 || true)
fi

if [[ -z "$PID" ]]; then
    warn "Could not find MR Analizer JVM process (PID). async-profiler will be skipped."
else
    ok "Server PID: $PID"
fi

# Check async-profiler
ASPROF_AVAILABLE=false
if [[ -x "$ASPROF_PATH" ]]; then
    ASPROF_AVAILABLE=true
    ok "async-profiler found: $ASPROF_PATH"
elif [[ -n "$PID" ]]; then
    warn "async-profiler not found at $ASPROF_PATH"
    echo "  Install:"
    echo "    mkdir -p ~/tools && cd ~/tools"
    echo "    wget https://github.com/async-profiler/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz"
    echo "    tar xzf async-profiler-3.0-linux-x64.tar.gz"
    echo "  Or set ASPROF_PATH=/path/to/asprof"
fi

# Create output directories
mkdir -p "$FLAMEGRAPH_DIR"

# ── Reset metrics ────────────────────────────────────────────────────────
info "Resetting SQL statistics..."
curl -sf -X POST "${BASE_URL}/api/diagnostics/sql-stats/reset" > /dev/null 2>&1 || warn "Could not reset SQL stats (dev profile not active?)"

# ── async-profiler: CPU ──────────────────────────────────────────────────
CPU_FLAMEGRAPH=""
ALLOC_FLAMEGRAPH=""

if [[ "$ASPROF_AVAILABLE" == true && -n "$PID" ]]; then
    if [[ "$PROFILE_TYPE" == "cpu" || "$PROFILE_TYPE" == "full" ]]; then
        CPU_FLAMEGRAPH="$FLAMEGRAPH_DIR/cpu-${EPOCH}.html"
        info "Starting CPU profiling (${DURATION}s)..."
        "$ASPROF_PATH" -d "$DURATION" -f "$CPU_FLAMEGRAPH" "$PID" 2>&1 || {
            warn "CPU profiling failed. Trying with -e itimer (no perf_events)..."
            "$ASPROF_PATH" -d "$DURATION" -e itimer -f "$CPU_FLAMEGRAPH" "$PID" 2>&1 || {
                warn "CPU profiling failed with itimer too. Skipping."
                CPU_FLAMEGRAPH=""
            }
        }
        [[ -n "$CPU_FLAMEGRAPH" && -f "$CPU_FLAMEGRAPH" ]] && ok "CPU flame graph: $CPU_FLAMEGRAPH"
    fi

    if [[ "$PROFILE_TYPE" == "alloc" || "$PROFILE_TYPE" == "full" ]]; then
        ALLOC_FLAMEGRAPH="$FLAMEGRAPH_DIR/alloc-${EPOCH}.html"
        info "Starting allocation profiling (${DURATION}s)..."
        "$ASPROF_PATH" -d "$DURATION" -e alloc -f "$ALLOC_FLAMEGRAPH" "$PID" 2>&1 || {
            warn "Allocation profiling failed. Skipping."
            ALLOC_FLAMEGRAPH=""
        }
        [[ -n "$ALLOC_FLAMEGRAPH" && -f "$ALLOC_FLAMEGRAPH" ]] && ok "Alloc flame graph: $ALLOC_FLAMEGRAPH"
    fi
else
    if [[ "$PROFILE_TYPE" != "full" && "$PROFILE_TYPE" != "cpu" && "$PROFILE_TYPE" != "alloc" ]]; then
        true  # no profiling needed
    else
        warn "Skipping async-profiler (not available or no PID)"
    fi
fi

# ── Load generation ──────────────────────────────────────────────────────
if [[ "$WITH_LOAD" == true ]]; then
    info "Generating test load (${LOAD_ITERATIONS} iterations, ${LOAD_CONCURRENCY} concurrent)..."

    # Single load iteration function
    load_iteration() {
        local i=$1
        # Browse MRs (will fail without real provider, but generates HTTP + SQL activity)
        curl -sf -X POST "${BASE_URL}/api/browse" \
            -H "Content-Type: application/json" \
            -d '{"projectSlug":"test/load-test","targetBranch":"main","limit":5}' \
            -o /dev/null 2>/dev/null || true

        # List analyses
        curl -sf "${BASE_URL}/api/analysis" -o /dev/null 2>/dev/null || true

        # List repos
        curl -sf "${BASE_URL}/api/repos" -o /dev/null 2>/dev/null || true

        # Actuator health (lightweight)
        curl -sf "${BASE_URL}/actuator/health" -o /dev/null 2>/dev/null || true
    }
    export -f load_iteration
    export BASE_URL

    # Run load iterations with concurrency
    seq 1 "$LOAD_ITERATIONS" | xargs -P "$LOAD_CONCURRENCY" -I {} bash -c 'load_iteration {}'

    ok "Load generation complete (${LOAD_ITERATIONS} iterations)"
fi

# ── Collect Actuator metrics ─────────────────────────────────────────────
info "Collecting metrics..."

# HTTP metrics
HTTP_METRICS=$(curl -sf "${BASE_URL}/actuator/metrics/http.server.requests" 2>/dev/null || echo "{}")

# JVM memory
JVM_HEAP_USED=$(curl -sf "${BASE_URL}/actuator/metrics/jvm.memory.used?tag=area:heap" 2>/dev/null | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' 2>/dev/null || echo "N/A")
JVM_HEAP_MAX=$(curl -sf "${BASE_URL}/actuator/metrics/jvm.memory.max?tag=area:heap" 2>/dev/null | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' 2>/dev/null || echo "N/A")

# GC pauses
GC_PAUSE_COUNT=$(curl -sf "${BASE_URL}/actuator/metrics/jvm.gc.pause" 2>/dev/null | jq -r '.measurements[] | select(.statistic=="COUNT") | .value' 2>/dev/null || echo "0")
GC_PAUSE_TOTAL=$(curl -sf "${BASE_URL}/actuator/metrics/jvm.gc.pause" 2>/dev/null | jq -r '.measurements[] | select(.statistic=="TOTAL_TIME") | .value' 2>/dev/null || echo "0")

# Threads
JVM_THREADS=$(curl -sf "${BASE_URL}/actuator/metrics/jvm.threads.live" 2>/dev/null | jq -r '.measurements[] | select(.statistic=="VALUE") | .value' 2>/dev/null || echo "N/A")

# SQL stats
SQL_STATS=$(curl -sf "${BASE_URL}/api/diagnostics/sql-stats" 2>/dev/null || echo "{}")

ok "Metrics collected"

# ── Format helper ────────────────────────────────────────────────────────
format_bytes() {
    local bytes=$1
    if [[ "$bytes" == "N/A" || -z "$bytes" ]]; then echo "N/A"; return; fi
    local mb
    mb=$(awk "BEGIN {printf \"%.1f\", $bytes / 1048576}" 2>/dev/null || echo "N/A")
    echo "${mb}MB"
}

format_seconds_ms() {
    local seconds=$1
    if [[ "$seconds" == "0" || "$seconds" == "N/A" ]]; then echo "${seconds}"; return; fi
    local ms
    ms=$(echo "scale=1; $seconds * 1000" | bc 2>/dev/null || echo "$seconds")
    echo "${ms}ms"
}

# ── Generate report ─────────────────────────────────────────────────────
REPORT_FILE="$REPORT_DIR/profile-${TIMESTAMP}.md"
info "Generating report: $REPORT_FILE"

JVM_VERSION=$(java -version 2>&1 | head -1 || echo "unknown")

cat > "$REPORT_FILE" <<REPORT_EOF
# Performance Profile Report — $(date '+%Y-%m-%d %H:%M:%S')

## Summary

| Parameter | Value |
|-----------|-------|
| Duration | ${DURATION}s |
| Load generated | ${WITH_LOAD} |
| Profiling type | ${PROFILE_TYPE} |
| Server PID | ${PID:-N/A} |
| JVM | ${JVM_VERSION} |

## CPU Hotspots (async-profiler)

REPORT_EOF

if [[ -n "$CPU_FLAMEGRAPH" && -f "$CPU_FLAMEGRAPH" ]]; then
    echo "Flame graph: [cpu-${EPOCH}.html](flamegraphs/cpu-${EPOCH}.html)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "> Open the HTML file in a browser for interactive flame graph." >> "$REPORT_FILE"
else
    echo "_CPU profiling was not performed (async-profiler not available or skipped)._" >> "$REPORT_FILE"
fi

cat >> "$REPORT_FILE" <<REPORT_EOF

## Memory & GC

| Metric | Value |
|--------|-------|
| Heap used | $(format_bytes "$JVM_HEAP_USED") |
| Heap max | $(format_bytes "$JVM_HEAP_MAX") |
| GC pauses (count) | ${GC_PAUSE_COUNT} |
| GC pauses (total) | $(format_seconds_ms "$GC_PAUSE_TOTAL") |
| Live threads | ${JVM_THREADS} |

REPORT_EOF

if [[ -n "$ALLOC_FLAMEGRAPH" && -f "$ALLOC_FLAMEGRAPH" ]]; then
    echo "Allocation flame graph: [alloc-${EPOCH}.html](flamegraphs/alloc-${EPOCH}.html)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
fi

cat >> "$REPORT_FILE" <<REPORT_EOF

## HTTP Latency (Actuator)

REPORT_EOF

# Per-endpoint breakdown
URIS=$(echo "$HTTP_METRICS" | jq -r '.availableTags[] | select(.tag=="uri") | .values[]' 2>/dev/null || true)
if [[ -n "$URIS" ]]; then
    echo "| Endpoint | Method | Count | Total (s) | Max (s) | Avg (ms) |" >> "$REPORT_FILE"
    echo "|----------|--------|-------|-----------|---------|----------|" >> "$REPORT_FILE"

    while IFS= read -r uri; do
        METHODS=$(curl -sf "${BASE_URL}/actuator/metrics/http.server.requests?tag=uri:${uri}" 2>/dev/null \
            | jq -r '.availableTags[] | select(.tag=="method") | .values[]' 2>/dev/null || echo "ALL")

        while IFS= read -r method; do
            if [[ "$method" == "ALL" ]]; then
                ENDPOINT_DATA=$(curl -sf "${BASE_URL}/actuator/metrics/http.server.requests?tag=uri:${uri}" 2>/dev/null || echo "{}")
            else
                ENDPOINT_DATA=$(curl -sf "${BASE_URL}/actuator/metrics/http.server.requests?tag=uri:${uri}&tag=method:${method}" 2>/dev/null || echo "{}")
            fi

            EP_COUNT=$(echo "$ENDPOINT_DATA" | jq -r '.measurements[] | select(.statistic=="COUNT") | .value' 2>/dev/null || echo "0")
            EP_TOTAL=$(echo "$ENDPOINT_DATA" | jq -r '.measurements[] | select(.statistic=="TOTAL_TIME") | .value' 2>/dev/null || echo "0")
            EP_MAX=$(echo "$ENDPOINT_DATA" | jq -r '.measurements[] | select(.statistic=="MAX") | .value' 2>/dev/null || echo "0")

            EP_AVG="0"
            if [[ "$EP_COUNT" != "0" && "$EP_COUNT" != "0.0" ]]; then
                EP_AVG=$(awk "BEGIN {printf \"%.1f\", ($EP_TOTAL / $EP_COUNT) * 1000}" 2>/dev/null || echo "0")
            fi
            EP_TOTAL_FMT=$(awk "BEGIN {printf \"%.3f\", $EP_TOTAL}" 2>/dev/null || echo "$EP_TOTAL")
            EP_MAX_FMT=$(awk "BEGIN {printf \"%.3f\", $EP_MAX}" 2>/dev/null || echo "$EP_MAX")

            echo "| ${uri} | ${method} | ${EP_COUNT} | ${EP_TOTAL_FMT} | ${EP_MAX_FMT} | ${EP_AVG} |" >> "$REPORT_FILE"
        done <<< "$METHODS"
    done <<< "$URIS"
else
    echo "_No HTTP metrics available._" >> "$REPORT_FILE"
fi

cat >> "$REPORT_FILE" <<REPORT_EOF

## SQL Analysis (Hibernate Stats)

| Metric | Value |
|--------|-------|
| Total queries | $(echo "$SQL_STATS" | jq -r '.queryExecutionCount // "N/A"' 2>/dev/null || echo "N/A") |
| Slowest query time | $(echo "$SQL_STATS" | jq -r '.queryExecutionMaxTime // "N/A"' 2>/dev/null || echo "N/A")ms |
| Slowest query | $(echo "$SQL_STATS" | jq -r '.queryExecutionMaxTimeQuery // "N/A"' 2>/dev/null || echo "N/A") |
| Entities loaded | $(echo "$SQL_STATS" | jq -r '.entityLoadCount // "N/A"' 2>/dev/null || echo "N/A") |
| Entities inserted | $(echo "$SQL_STATS" | jq -r '.entityInsertCount // "N/A"' 2>/dev/null || echo "N/A") |
| Entities updated | $(echo "$SQL_STATS" | jq -r '.entityUpdateCount // "N/A"' 2>/dev/null || echo "N/A") |
| Entities deleted | $(echo "$SQL_STATS" | jq -r '.entityDeleteCount // "N/A"' 2>/dev/null || echo "N/A") |
| Collections loaded | $(echo "$SQL_STATS" | jq -r '.collectionLoadCount // "N/A"' 2>/dev/null || echo "N/A") |
| Sessions opened | $(echo "$SQL_STATS" | jq -r '.sessionOpenCount // "N/A"' 2>/dev/null || echo "N/A") |

## Recommendations

_To be filled by Claude after analyzing the data above._
REPORT_EOF

ok "Report generated: $REPORT_FILE"
echo ""
info "Done! Review the report and flame graphs."
echo "  Report: $REPORT_FILE"
if [[ -n "$CPU_FLAMEGRAPH" && -f "$CPU_FLAMEGRAPH" ]]; then echo "  CPU:    $CPU_FLAMEGRAPH"; fi
if [[ -n "$ALLOC_FLAMEGRAPH" && -f "$ALLOC_FLAMEGRAPH" ]]; then echo "  Alloc:  $ALLOC_FLAMEGRAPH"; fi

exit 0
