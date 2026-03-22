import { useMemo } from 'react';
import { Table, Badge, Form, Row, Col } from 'react-bootstrap';
import type { ActivityFlag, Severity } from '../../types/activity';

interface Props {
  flags: ActivityFlag[];
  severityFilter: Severity | null;
  typeFilter: string;
  onTypeFilterChange: (type: string) => void;
}

const severityVariant: Record<Severity, string> = {
  CRITICAL: 'danger',
  WARNING: 'warning',
  INFO: 'info',
};

const severityLabel: Record<Severity, string> = {
  CRITICAL: 'Krytyczny',
  WARNING: 'Ostrzeżenie',
  INFO: 'Informacja',
};

export default function FlagsList({ flags, severityFilter, typeFilter, onTypeFilterChange }: Props) {
  const uniqueTypes = useMemo(() => {
    const types = [...new Set(flags.map(f => f.displayName))];
    types.sort();
    return types;
  }, [flags]);

  const filtered = useMemo(() => {
    return flags.filter(f => {
      if (severityFilter && f.severity !== severityFilter) return false;
      if (typeFilter !== 'all' && f.displayName !== typeFilter) return false;
      return true;
    });
  }, [flags, severityFilter, typeFilter]);

  if (flags.length === 0) {
    return (
      <div className="text-center text-muted py-4">
        <p className="mb-0">Brak wykrytych nieprawidłowości</p>
      </div>
    );
  }

  return (
    <>
      <Row className="mb-3 g-2 align-items-center">
        <Col xs="auto">
          <Form.Select size="sm" value={typeFilter} onChange={e => onTypeFilterChange(e.target.value)}>
            <option value="all">Wszystkie kategorie</option>
            {uniqueTypes.map(t => (
              <option key={t} value={t}>{t}</option>
            ))}
          </Form.Select>
        </Col>
        <Col>
          <small className="text-muted">{filtered.length} z {flags.length} flag</small>
        </Col>
      </Row>
      <Table striped hover responsive>
        <thead>
          <tr>
            <th style={{ width: '120px' }}>Severity</th>
            <th style={{ width: '200px' }}>Typ</th>
            <th>Opis</th>
            <th style={{ width: '80px' }}>PR</th>
          </tr>
        </thead>
        <tbody>
          {filtered.map((flag, idx) => (
            <tr key={idx}>
              <td>
                <Badge bg={severityVariant[flag.severity]} text={flag.severity === 'WARNING' || flag.severity === 'INFO' ? 'dark' : undefined}>
                  {severityLabel[flag.severity]}
                </Badge>
              </td>
              <td>{flag.displayName}</td>
              <td>{flag.description}</td>
              <td>{flag.prReference ?? '—'}</td>
            </tr>
          ))}
          {filtered.length === 0 && (
            <tr>
              <td colSpan={4} className="text-center text-muted py-3">
                Brak flag pasujących do filtrów
              </td>
            </tr>
          )}
        </tbody>
      </Table>
    </>
  );
}
