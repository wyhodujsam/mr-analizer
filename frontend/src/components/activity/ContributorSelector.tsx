import { Form } from 'react-bootstrap';
import type { ContributorInfo } from '../../types/activity';

interface Props {
  contributors: ContributorInfo[];
  selected: string;
  onChange: (author: string) => void;
  loading?: boolean;
}

export default function ContributorSelector({ contributors, selected, onChange, loading }: Props) {
  return (
    <Form.Group className="mb-3">
      <Form.Label>Kontrybutor</Form.Label>
      <Form.Select
        value={selected}
        onChange={(e) => onChange(e.target.value)}
        disabled={loading || contributors.length === 0}
      >
        <option value="">— Wybierz kontrybutora —</option>
        {contributors.map((c) => (
          <option key={c.login} value={c.login}>
            {c.login} ({c.prCount} PR-ów)
          </option>
        ))}
      </Form.Select>
    </Form.Group>
  );
}
