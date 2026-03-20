import { Badge } from 'react-bootstrap';
import type { Verdict } from '../types';

interface Props {
  score: number;
  verdict: Verdict;
}

export default function ScoreBadge({ score, verdict }: Props) {
  const bg =
    verdict === 'AUTOMATABLE'
      ? 'success'
      : verdict === 'MAYBE'
      ? 'warning'
      : 'danger';

  return (
    <Badge bg={bg} text={verdict === 'MAYBE' ? 'dark' : undefined}>
      {score.toFixed(2)}
    </Badge>
  );
}
