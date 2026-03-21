import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Form } from 'react-bootstrap';
import ScoreBadge from './ScoreBadge';
import type { AnalysisResponse } from '../types';
import { formatDate } from '../utils/format';
import { verdictClass, verdictLabel } from '../utils/verdict';

interface Props {
  analyses: AnalysisResponse[];
  onDelete: (reportId: number) => void;
}

interface FlatRow {
  reportId: number;
  projectSlug: string;
  analyzedAt: string;
  id: number;
  externalId: string;
  title: string;
  author: string;
  score: number | null;
  verdict: Verdict | null;
  url: string;
  hasDetailedAnalysis: boolean;
}

export default function AnalysisHistory({ analyses, onDelete }: Props) {
  const navigate = useNavigate();
  const [filterRepo, setFilterRepo] = useState<string>('all');

  function handleDelete(e: React.MouseEvent, reportId: number, mrCount: number) {
    e.stopPropagation();
    const msg = mrCount > 1
        ? `Usunac analize? Zostanie usunietych ${mrCount} wynikow MR.`
        : 'Usunac analize?';
    if (window.confirm(msg)) {
      onDelete(reportId);
    }
  }

  if (analyses.length === 0) {
    return null;
  }

  const repos = [...new Set(analyses.map(a => a.projectSlug))];

  const rows: FlatRow[] = analyses.flatMap(a =>
    a.results.map(r => ({
      reportId: a.reportId,
      projectSlug: a.projectSlug,
      analyzedAt: a.analyzedAt,
      ...r,
    }))
  );

  const filteredRows = filterRepo === 'all'
    ? rows
    : rows.filter(r => r.projectSlug === filterRepo);

  const reportResultCounts = new Map<number, number>();
  for (const a of analyses) {
    reportResultCounts.set(a.reportId, a.results.length);
  }

  const firstReportRow = new Set<string>();
  const seenReportIds = new Set<number>();
  for (const r of filteredRows) {
    if (!seenReportIds.has(r.reportId)) {
      seenReportIds.add(r.reportId);
      firstReportRow.add(`${r.reportId}-${r.id}`);
    }
  }

  return (
    <div className="analysis-history">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h5 className="text-muted mb-0">Historia analiz</h5>
        {repos.length > 1 && (
          <Form.Select
            size="sm"
            style={{ maxWidth: 250 }}
            value={filterRepo}
            onChange={e => setFilterRepo(e.target.value)}
          >
            <option value="all">Wszystkie ({rows.length})</option>
            {repos.map(repo => (
              <option key={repo} value={repo}>
                {repo} ({rows.filter(r => r.projectSlug === repo).length})
              </option>
            ))}
          </Form.Select>
        )}
      </div>
      <Table hover bordered responsive size="sm">
        <thead className="table-light">
          <tr>
            <th>Data</th>
            <th>Repo</th>
            <th>PR #</th>
            <th>Tytul</th>
            <th>Autor</th>
            <th>Wynik</th>
            <th>Werdykt</th>
            <th><span className="visually-hidden">Akcje</span></th>
          </tr>
        </thead>
        <tbody>
          {filteredRows.map((r) => {
            const isFirstInGroup = firstReportRow.has(`${r.reportId}-${r.id}`);
            const isGrouped = (reportResultCounts.get(r.reportId) ?? 0) > 1;

            return (
              <tr
                key={`${r.reportId}-${r.id}`}
                className={`clickable-row ${isGrouped && r.verdict ? 'verdict-' + (r.verdict === 'NOT_SUITABLE' ? 'not-suitable' : r.verdict.toLowerCase()) : ''}`}
                onClick={() => navigate(r.hasDetailedAnalysis
                  ? `/analysis/${r.reportId}/${r.id}`
                  : `/mr/${r.reportId}/${r.id}`)}
                onKeyDown={(e) => { if (e.key === 'Enter') navigate(r.hasDetailedAnalysis ? `/analysis/${r.reportId}/${r.id}` : `/mr/${r.reportId}/${r.id}`); }}
                tabIndex={0}
                role="link"
              >
                <td className="text-muted">
                  {formatDate(r.analyzedAt)}
                  {isFirstInGroup && isGrouped && (
                    <span className="badge bg-secondary ms-1" style={{ fontSize: '0.65rem' }}>
                      x{reportResultCounts.get(r.reportId)}
                    </span>
                  )}
                </td>
                <td>{r.projectSlug}</td>
                <td>{r.externalId}</td>
                <td className="text-truncate" style={{ maxWidth: 250 }}>{r.title}</td>
                <td>{r.author}</td>
                <td><ScoreBadge score={r.score} verdict={r.verdict} /></td>
                <td className={verdictClass(r.verdict)}>{verdictLabel(r.verdict)}</td>
                <td>
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={(e) => handleDelete(e, r.reportId, reportResultCounts.get(r.reportId) ?? 1)}
                  >
                    Usun
                  </Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </Table>
    </div>
  );
}
