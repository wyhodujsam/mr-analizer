import { Table, Badge } from 'react-bootstrap';
import type { DailyActivity } from '../../types/activity';

interface Props {
  date: string;
  activity: DailyActivity;
}

export default function DayDrillDown({ date, activity }: Props) {
  return (
    <div className="mt-3 p-3 border rounded bg-light">
      <h6>
        PR-y z dnia {date}{' '}
        <Badge bg="secondary">{activity.count}</Badge>
      </h6>
      <Table size="sm" striped>
        <thead>
          <tr>
            <th>PR</th>
            <th>Tytuł</th>
            <th>Rozmiar</th>
          </tr>
        </thead>
        <tbody>
          {activity.pullRequests.map((pr) => (
            <tr key={pr.id}>
              <td>#{pr.id}</td>
              <td>{pr.title}</td>
              <td>{pr.size} linii</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </div>
  );
}
