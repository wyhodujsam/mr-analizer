import { Table, Button } from 'react-bootstrap';
import type { AnalysisResponse } from '../types';

interface Props {
  analyses: AnalysisResponse[];
  onDelete: (reportId: number) => void;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

export default function AnalysisHistory({ analyses, onDelete }: Props) {
  function handleDelete(reportId: number) {
    if (window.confirm('Usunac analize?')) {
      onDelete(reportId);
    }
  }

  if (analyses.length === 0) {
    return null;
  }

  return (
    <div className="analysis-history">
      <h5 className="text-muted mb-3">Historia analiz</h5>
      <Table hover bordered responsive size="sm">
        <thead className="table-light">
          <tr>
            <th>Data</th>
            <th>Repozytorium</th>
            <th>PRs</th>
            <th className="text-success">Auto</th>
            <th className="text-warning">Maybe</th>
            <th className="text-danger">Not Suit.</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {analyses.map((a) => (
            <tr key={a.reportId}>
              <td className="text-muted">{formatDate(a.analyzedAt)}</td>
              <td>{a.projectSlug}</td>
              <td>{a.totalMrs}</td>
              <td className="text-success">{a.automatableCount}</td>
              <td className="text-warning">{a.maybeCount}</td>
              <td className="text-danger">{a.notSuitableCount}</td>
              <td>
                <Button
                  variant="outline-danger"
                  size="sm"
                  onClick={() => handleDelete(a.reportId)}
                >
                  Usun
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
