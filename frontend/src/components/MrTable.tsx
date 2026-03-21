import { Badge, Button, Table } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import type { AnalysisResultItem } from '../types';
import ScoreBadge from './ScoreBadge';

interface Props {
  results: AnalysisResultItem[];
  reportId: number;
}

function verdictClass(verdict: string): string {
  switch (verdict) {
    case 'AUTOMATABLE':
      return 'verdict-automatable';
    case 'MAYBE':
      return 'verdict-maybe';
    case 'NOT_SUITABLE':
      return 'verdict-not-suitable';
    default:
      return '';
  }
}

export default function MrTable({ results, reportId }: Props) {
  const navigate = useNavigate();

  return (
    <Table hover bordered responsive className="mr-table">
      <thead className="table-dark">
        <tr>
          <th>#</th>
          <th>Tytul</th>
          <th>Autor</th>
          <th>Wynik</th>
          <th>Werdykt</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {results.map((item) => (
          <tr
            key={item.id}
            className={`${verdictClass(item.verdict)} clickable-row`}
            onClick={() => navigate(`/mr/${reportId}/${item.id}`)}
            style={{ cursor: 'pointer' }}
          >
            <td>{item.externalId}</td>
            <td>{item.title}</td>
            <td>{item.author}</td>
            <td>
              <ScoreBadge score={item.score} verdict={item.verdict} />
              {item.hasDetailedAnalysis && (
                <Badge bg="info" className="ms-1" style={{ fontSize: '0.6rem' }}>LLM</Badge>
              )}
            </td>
            <td>{item.verdict}</td>
            <td onClick={(e) => e.stopPropagation()}>
              {item.hasDetailedAnalysis && (
                <Button
                  size="sm"
                  variant="outline-info"
                  onClick={() => navigate(`/analysis/${reportId}/${item.id}`)}
                >
                  Szczegoly analizy
                </Button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
