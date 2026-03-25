import { useState } from 'react';
import { Card, Table, ButtonGroup, Button } from 'react-bootstrap';
import type { ProjectSummary, PrAnalysisRow } from '../../types/project';

interface Props {
  summary: ProjectSummary;
  rows: PrAnalysisRow[];
}

type ViewMode = 'percent' | 'lines';
type LinesMode = 'all' | 'added' | 'deleted';

function DonutChart({ segments, centerText }: {
  segments: { value: number; color: string }[];
  centerText: string;
}) {
  const total = segments.reduce((s, seg) => s + seg.value, 0);
  if (total === 0) return null;

  const r = 40, cx = 50, cy = 50, stroke = 12;
  const circumference = 2 * Math.PI * r;

  let offset = 0;
  return (
    <svg width={100} height={100} viewBox="0 0 100 100">
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="#e9ecef" strokeWidth={stroke} />
      {[...segments].reverse().map((seg, i) => {
        const arc = (seg.value / total) * circumference;
        const el = (
          <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke={seg.color} strokeWidth={stroke}
            strokeDasharray={`${arc} ${circumference - arc}`}
            strokeDashoffset={-offset} transform={`rotate(-90 ${cx} ${cy})`} />
        );
        offset += arc;
        return el;
      })}
      <text x={cx} y={cy + 5} textAnchor="middle" fontSize="14" fontWeight="bold">{centerText}</text>
    </svg>
  );
}

function HistogramChart({ histogram }: { histogram: ProjectSummary['histogram'] }) {
  const max = Math.max(...histogram.map(b => b.count), 1);
  const barW = 30, gap = 4, h = 60;

  return (
    <svg width={histogram.length * (barW + gap)} height={h + 20}>
      {histogram.map((b, i) => {
        const barH = (b.count / max) * h;
        const x = i * (barW + gap);
        const label = `${b.rangeStart.toFixed(1)}-${b.rangeEnd.toFixed(1)}`;
        return (
          <g key={i}>
            <rect x={x} y={h - barH} width={barW} height={Math.max(barH, 2)} fill="#6c757d" rx={2}>
              <title>{label}: {b.count} PR</title>
            </rect>
            <text x={x + barW / 2} y={h + 12} textAnchor="middle" fontSize="8" fill="#6c757d">{label}</text>
            {b.count > 0 && (
              <text x={x + barW / 2} y={h - barH - 3} textAnchor="middle" fontSize="9" fill="#333">{b.count}</text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

function computeLinesByVerdict(rows: PrAnalysisRow[], linesMode: LinesMode) {
  const getLines = (r: PrAnalysisRow) => {
    switch (linesMode) {
      case 'added': return r.additions;
      case 'deleted': return r.deletions;
      default: return r.additions + r.deletions;
    }
  };

  const automatable = rows.filter(r => r.aiVerdict === 'AUTOMATABLE').reduce((s, r) => s + getLines(r), 0);
  const maybe = rows.filter(r => r.aiVerdict === 'MAYBE').reduce((s, r) => s + getLines(r), 0);
  const notSuitable = rows.filter(r => r.aiVerdict === 'NOT_SUITABLE').reduce((s, r) => s + getLines(r), 0);
  const total = automatable + maybe + notSuitable;

  return { automatable, maybe, notSuitable, total };
}

function formatNum(n: number): string {
  return n.toLocaleString('pl-PL');
}

const linesModeLabels: Record<LinesMode, string> = {
  all: 'Wszystkie',
  added: 'Dodane',
  deleted: 'Usunięte',
};

export default function AiPotentialCard({ summary, rows }: Props) {
  const [viewMode, setViewMode] = useState<ViewMode>('percent');
  const [linesMode, setLinesMode] = useState<LinesMode>('all');

  const lines = computeLinesByVerdict(rows, linesMode);

  return (
    <Card className="h-100">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-start mb-2">
          <Card.Title className="mb-0">AI Potential</Card.Title>
          <ButtonGroup size="sm">
            <Button variant={viewMode === 'percent' ? 'primary' : 'outline-primary'}
              onClick={() => setViewMode('percent')}>%</Button>
            <Button variant={viewMode === 'lines' ? 'primary' : 'outline-primary'}
              onClick={() => setViewMode('lines')}>Linie kodu</Button>
          </ButtonGroup>
        </div>

        {viewMode === 'lines' && (
          <ButtonGroup size="sm" className="mb-2">
            {(['all', 'added', 'deleted'] as LinesMode[]).map(m => (
              <Button key={m}
                variant={linesMode === m ? 'secondary' : 'outline-secondary'}
                onClick={() => setLinesMode(m)}>
                {linesModeLabels[m]}
              </Button>
            ))}
          </ButtonGroup>
        )}

        <div className="d-flex align-items-center gap-3 mb-3">
          {viewMode === 'percent' ? (
            <>
              <DonutChart
                segments={[
                  { value: summary.notSuitableCount, color: '#dc3545' },
                  { value: summary.maybeCount, color: '#ffc107' },
                  { value: summary.automatableCount, color: '#198754' },
                ]}
                centerText={`${Math.round(summary.automatablePercent)}%`}
              />
              <div>
                <div><span className="badge bg-success me-1">{summary.automatableCount}</span> Automatable ({summary.automatablePercent}%)</div>
                <div><span className="badge bg-warning text-dark me-1">{summary.maybeCount}</span> Maybe ({summary.maybePercent}%)</div>
                <div><span className="badge bg-danger me-1">{summary.notSuitableCount}</span> Not Suitable ({summary.notSuitablePercent}%)</div>
                <div className="text-muted mt-1">Avg score: {summary.avgScore.toFixed(2)}</div>
              </div>
            </>
          ) : (
            <>
              <DonutChart
                segments={[
                  { value: lines.notSuitable, color: '#dc3545' },
                  { value: lines.maybe, color: '#ffc107' },
                  { value: lines.automatable, color: '#198754' },
                ]}
                centerText={formatNum(lines.total)}
              />
              <div>
                <div>
                  <span className="badge bg-success me-1">{formatNum(lines.automatable)}</span>
                  Automatable ({lines.total > 0 ? Math.round(lines.automatable / lines.total * 100) : 0}%)
                </div>
                <div>
                  <span className="badge bg-warning text-dark me-1">{formatNum(lines.maybe)}</span>
                  Maybe ({lines.total > 0 ? Math.round(lines.maybe / lines.total * 100) : 0}%)
                </div>
                <div>
                  <span className="badge bg-danger me-1">{formatNum(lines.notSuitable)}</span>
                  Not Suitable ({lines.total > 0 ? Math.round(lines.notSuitable / lines.total * 100) : 0}%)
                </div>
                <div className="text-muted mt-1">
                  {linesModeLabels[linesMode]} linie: {formatNum(lines.total)}
                </div>
              </div>
            </>
          )}
        </div>

        {summary.histogram.length > 0 && (
          <>
            <h6>Ile PR-ów w każdym przedziale score</h6>
            <p className="text-muted small mb-1">
              Score 0.0–0.4 = niski potencjał AI, 0.4–0.7 = średni, 0.7–1.0 = wysoki
            </p>
            <HistogramChart histogram={summary.histogram} />
          </>
        )}

        {summary.topRules.length > 0 && (
          <>
            <h6 className="mt-3">Top reguły</h6>
            <Table size="sm" className="mb-0">
              <thead><tr><th>Reguła</th><th>PR-y</th><th>Waga</th></tr></thead>
              <tbody>
                {summary.topRules.slice(0, 5).map(r => (
                  <tr key={r.ruleName}>
                    <td><code>{r.ruleName}</code></td>
                    <td>{r.matchCount}</td>
                    <td className={r.avgWeight > 0 ? 'text-success' : 'text-danger'}>
                      {r.avgWeight > 0 ? '+' : ''}{r.avgWeight.toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </>
        )}
      </Card.Body>
    </Card>
  );
}
