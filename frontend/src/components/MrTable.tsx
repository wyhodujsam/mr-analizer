import { Table } from 'react-bootstrap';
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
          <th>Title</th>
          <th>Author</th>
          <th>Score</th>
          <th>Verdict</th>
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
            </td>
            <td>{item.verdict}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
