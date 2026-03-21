import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Button, Form } from 'react-bootstrap';
import ScoreBadge from './ScoreBadge';
import type { AnalysisResponse, Verdict } from '../types';

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
  score: number;
  verdict: Verdict;
  url: string;
  hasDetailedAnalysis: boolean;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

function verdictLabel(v: Verdict): string {
  switch (v) {
    case 'AUTOMATABLE': return 'Auto';
    case 'MAYBE': return 'Moze';
    case 'NOT_SUITABLE': return 'Nie';
  }
}

function verdictClass(v: Verdict): string {
  switch (v) {
    case 'AUTOMATABLE': return 'text-success';
    case 'MAYBE': return 'text-warning';
    case 'NOT_SUITABLE': return 'text-danger';
  }
}

export default function AnalysisHistory({ analyses, onDelete }: Props) {
  const navigate = useNavigate();
  const [filterRepo, setFilterRepo] = useState<string>('all');

  function handleDelete(e: React.MouseEvent, reportId: number) {
    e.stopPropagation();
    if (window.confirm('Usunac analize?')) {
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

  const seenReports = new Set<number>();

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
            <th></th>
          </tr>
        </thead>
        <tbody>
          {filteredRows.map((r) => {
            const isFirstInGroup = !seenReports.has(r.reportId);
            const isGrouped = (reportResultCounts.get(r.reportId) ?? 0) > 1;
            seenReports.add(r.reportId);

            return (
              <tr
                key={`${r.reportId}-${r.id}`}
                className={`clickable-row ${isGrouped ? 'verdict-' + (r.verdict === 'NOT_SUITABLE' ? 'not-suitable' : r.verdict.toLowerCase()) : ''}`}
                onClick={() => navigate(r.hasDetailedAnalysis
                  ? `/analysis/${r.reportId}/${r.id}`
                  : `/mr/${r.reportId}/${r.id}`)}
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
                    onClick={(e) => handleDelete(e, r.reportId)}
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
