import { useState } from 'react';
import { Table, Badge, Form, Collapse, Row, Col } from 'react-bootstrap';
import type { PrAnalysisRow } from '../../types/project';

interface Props {
  rows: PrAnalysisRow[];
}

type SortKey = 'title' | 'author' | 'aiScore' | 'createdAt';
type SortDir = 'asc' | 'desc';

const verdictBg: Record<string, string> = {
  AUTOMATABLE: 'success',
  MAYBE: 'warning',
  NOT_SUITABLE: 'danger',
};

function scoreBadge(score: number, verdict: string) {
  return (
    <Badge bg={verdictBg[verdict] ?? 'secondary'}>
      {score.toFixed(2)}
    </Badge>
  );
}

export default function ProjectPrTable({ rows }: Props) {
  const [sortKey, setSortKey] = useState<SortKey>('aiScore');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [verdictFilter, setVerdictFilter] = useState('ALL');
  const [bddFilter, setBddFilter] = useState('all');
  const [sddFilter, setSddFilter] = useState('all');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  function toggleSort(key: SortKey) {
    if (sortKey === key) {
      setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('desc');
    }
  }

  const sortIndicator = (key: SortKey) =>
    sortKey === key ? (sortDir === 'asc' ? ' ▲' : ' ▼') : '';

  let filtered = rows;
  if (verdictFilter !== 'ALL') filtered = filtered.filter(r => r.aiVerdict === verdictFilter);
  if (bddFilter === 'tak') filtered = filtered.filter(r => r.hasBdd);
  if (bddFilter === 'nie') filtered = filtered.filter(r => !r.hasBdd);
  if (sddFilter === 'tak') filtered = filtered.filter(r => r.hasSdd);
  if (sddFilter === 'nie') filtered = filtered.filter(r => !r.hasSdd);

  const sorted = [...filtered].sort((a, b) => {
    let cmp = 0;
    switch (sortKey) {
      case 'title': cmp = a.title.localeCompare(b.title); break;
      case 'author': cmp = (a.author ?? '').localeCompare(b.author ?? ''); break;
      case 'aiScore': cmp = a.aiScore - b.aiScore; break;
      case 'createdAt': cmp = (a.createdAt ?? '').localeCompare(b.createdAt ?? ''); break;
    }
    return sortDir === 'asc' ? cmp : -cmp;
  });

  return (
    <>
      <Row className="g-2 mb-3">
        <Col xs="auto">
          <Form.Select size="sm" value={verdictFilter} onChange={e => setVerdictFilter(e.target.value)}>
            <option value="ALL">Verdict: wszystkie</option>
            <option value="AUTOMATABLE">AUTOMATABLE</option>
            <option value="MAYBE">MAYBE</option>
            <option value="NOT_SUITABLE">NOT_SUITABLE</option>
          </Form.Select>
        </Col>
        <Col xs="auto">
          <Form.Select size="sm" value={bddFilter} onChange={e => setBddFilter(e.target.value)}>
            <option value="all">BDD: wszystkie</option>
            <option value="tak">BDD: tak</option>
            <option value="nie">BDD: nie</option>
          </Form.Select>
        </Col>
        <Col xs="auto">
          <Form.Select size="sm" value={sddFilter} onChange={e => setSddFilter(e.target.value)}>
            <option value="all">SDD: wszystkie</option>
            <option value="tak">SDD: tak</option>
            <option value="nie">SDD: nie</option>
          </Form.Select>
        </Col>
        <Col xs="auto" className="d-flex align-items-center text-muted">
          {sorted.length} / {rows.length} PR-ów
        </Col>
      </Row>

      <Table striped hover size="sm" responsive>
        <thead>
          <tr>
            <th role="button" onClick={() => toggleSort('title')}>PR{sortIndicator('title')}</th>
            <th role="button" onClick={() => toggleSort('author')}>Autor{sortIndicator('author')}</th>
            <th role="button" onClick={() => toggleSort('createdAt')}>Data{sortIndicator('createdAt')}</th>
            <th role="button" onClick={() => toggleSort('aiScore')}>AI Score{sortIndicator('aiScore')}</th>
            <th>Verdict</th>
            <th>BDD</th>
            <th>SDD</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map(row => (
            <PrRow key={row.prId} row={row}
              expanded={expandedId === row.prId}
              onToggle={() => setExpandedId(expandedId === row.prId ? null : row.prId)}
            />
          ))}
        </tbody>
      </Table>
    </>
  );
}

function PrRow({ row, expanded, onToggle }: { row: PrAnalysisRow; expanded: boolean; onToggle: () => void }) {
  return (
    <>
      <tr role="button" onClick={onToggle} className={expanded ? 'table-active' : ''}>
        <td>
          <span className="fw-semibold">#{row.prId}</span>{' '}
          {row.title}
        </td>
        <td>{row.author}</td>
        <td>{row.createdAt?.substring(0, 10) ?? '—'}</td>
        <td>{scoreBadge(row.aiScore, row.aiVerdict)}</td>
        <td><Badge bg={verdictBg[row.aiVerdict] ?? 'secondary'}>{row.aiVerdict}</Badge></td>
        <td>{row.hasBdd ? <span className="text-success fw-bold">✓</span> : <span className="text-muted">✗</span>}</td>
        <td>{row.hasSdd ? <span className="text-success fw-bold">✓</span> : <span className="text-muted">✗</span>}</td>
      </tr>
      <tr>
        <td colSpan={7} className="p-0 border-0">
          <Collapse in={expanded}>
            <div className="p-3 bg-light">
              <DrillDown row={row} />
            </div>
          </Collapse>
        </td>
      </tr>
    </>
  );
}

const verdictBg2: Record<string, string> = verdictBg;

function DrillDown({ row }: { row: PrAnalysisRow }) {
  return (
    <Row>
      <Col md={6}>
        <h6>Score Breakdown</h6>
        {row.ruleResults.length === 0 ? (
          <p className="text-muted">Brak dopasowanych reguł</p>
        ) : (
          <Table size="sm" className="mb-3">
            <thead><tr><th>Reguła</th><th>Waga</th><th>Powód</th></tr></thead>
            <tbody>
              {row.ruleResults.map((rr, i) => (
                <tr key={i}>
                  <td><code>{rr.ruleName}</code></td>
                  <td className={rr.weight > 0 ? 'text-success' : 'text-danger'}>
                    {rr.weight > 0 ? '+' : ''}{rr.weight.toFixed(2)}
                  </td>
                  <td className="small">{rr.reason}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        {row.llmComment && (
          <div className="mb-2"><strong>LLM:</strong> {row.llmComment}</div>
        )}
      </Col>
      <Col md={3}>
        <h6>BDD</h6>
        {row.bddFiles.length > 0 ? (
          <ul className="small mb-0">
            {row.bddFiles.map((f, i) => <li key={i}><code>{f}</code></li>)}
          </ul>
        ) : <p className="text-muted">Brak plików BDD</p>}
      </Col>
      <Col md={3}>
        <h6>SDD</h6>
        {row.sddFiles.length > 0 ? (
          <ul className="small mb-0">
            {row.sddFiles.map((f, i) => <li key={i}><code>{f}</code></li>)}
          </ul>
        ) : <p className="text-muted">Brak plików SDD</p>}
      </Col>
    </Row>
  );
}
