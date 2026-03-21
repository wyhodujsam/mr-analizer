import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  Alert,
  Badge,
  Card,
  Col,
  Row,
  Spinner,
  Table,
} from 'react-bootstrap';
import { getMrDetail } from '../api/analysisApi';
import ScoreBadge from '../components/ScoreBadge';
import type { MrDetailResponse } from '../types';
import { formatDate } from '../utils/format';

export default function MrDetailPage() {
  const { reportId, resultId } = useParams<{ reportId: string; resultId: string }>();
  const [detail, setDetail] = useState<MrDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!reportId || !resultId) return;
    const controller = new AbortController();
    getMrDetail(Number(reportId), Number(resultId))
      .then((data) => {
        if (!controller.signal.aborted) setDetail(data);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted) return;
        if (
          err &&
          typeof err === 'object' &&
          'response' in err &&
          err.response &&
          typeof err.response === 'object' &&
          'data' in err.response
        ) {
          const data = (err.response as { data?: { message?: string } }).data;
          setError(data?.message ?? 'Nie udalo sie zaladowac szczegolow MR.');
        } else {
          setError('Nie udalo sie polaczyc z serwerem.');
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [reportId, resultId]);

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Ladowanie...</span>
        </Spinner>
        <p className="mt-2">Ladowanie szczegolow MR...</p>
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger">{error}</Alert>;
  }

  if (!detail) return null;

  const hasAnalysis = detail.score !== undefined && detail.score !== null && detail.verdict;
  const labels = detail.labels ?? [];
  const scoreBreakdown = detail.scoreBreakdown ?? [];
  const hasUsefulBreakdown = scoreBreakdown.length > 0 && scoreBreakdown.some(e => e.weight !== 0 || e.reason);
  const additions = detail.additions ?? 0;
  const deletions = detail.deletions ?? 0;
  const changedFilesCount = detail.changedFilesCount ?? 0;

  return (
    <div>
      <div className="mb-3 d-flex gap-3">
        <Link to="/">&larr; Powrot do dashboardu</Link>
        {detail.hasDetailedAnalysis && (
          <Link to={`/analysis/${reportId}/${resultId}`}>Szczegoly analizy LLM</Link>
        )}
      </div>

      <h3 className="mb-1">{detail.title}</h3>
      <p className="text-muted mb-3">
        #{detail.externalId} autor: <strong>{detail.author}</strong>
      </p>

      <Row className="mb-4 g-3">
        <Col md={6}>
          <Card>
            <Card.Header>Metadane</Card.Header>
            <Card.Body>
              <Table size="sm" borderless className="mb-0">
                <tbody>
                  <tr>
                    <th style={{ width: '140px' }}>Status</th>
                    <td>
                      <Badge bg="secondary">{detail.state ?? '\u2014'}</Badge>
                    </td>
                  </tr>
                  <tr>
                    <th>Branch zrodlowy</th>
                    <td><code>{detail.sourceBranch ?? '\u2014'}</code></td>
                  </tr>
                  <tr>
                    <th>Branch docelowy</th>
                    <td><code>{detail.targetBranch ?? '\u2014'}</code></td>
                  </tr>
                  <tr>
                    <th>Utworzono</th>
                    <td>{formatDate(detail.createdAt)}</td>
                  </tr>
                  <tr>
                    <th>Zmergowano</th>
                    <td>{formatDate(detail.mergedAt)}</td>
                  </tr>
                  <tr>
                    <th>Etykiety</th>
                    <td>
                      {labels.length > 0
                        ? labels.map((l) => (
                            <Badge key={l} bg="info" text="dark" className="me-1">
                              {l}
                            </Badge>
                          ))
                        : <span className="text-muted">brak</span>}
                    </td>
                  </tr>
                  <tr>
                    <th>Testy</th>
                    <td>{detail.hasTests ? 'Tak' : 'Nie'}</td>
                  </tr>
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>

        <Col md={6}>
          <Card>
            <Card.Header>Statystyki zmian</Card.Header>
            <Card.Body>
              <Table size="sm" borderless className="mb-0">
                <tbody>
                  <tr>
                    <th style={{ width: '160px' }}>Dodane linie</th>
                    <td className="text-success">+{additions}</td>
                  </tr>
                  <tr>
                    <th>Usuniete linie</th>
                    <td className="text-danger">-{deletions}</td>
                  </tr>
                  <tr>
                    <th>Zmienione pliki</th>
                    <td>{changedFilesCount}</td>
                  </tr>
                </tbody>
              </Table>
            </Card.Body>
          </Card>

          {hasAnalysis && (
            <Card className="mt-3">
              <Card.Header>Wynik</Card.Header>
              <Card.Body>
                <div className="d-flex align-items-center gap-3 mb-2">
                  <ScoreBadge score={detail.score} verdict={detail.verdict} />
                  <span className="text-muted">{detail.verdict ?? '—'}</span>
                </div>
                <small className={detail.hasDetailedAnalysis ? 'text-success' : 'text-muted'}>
                  {detail.hasDetailedAnalysis
                    ? 'Analiza z LLM (szczegoly dostepne)'
                    : 'Analiza regul (bez LLM)'}
                </small>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>

      {detail.description && (
        <Card className="mb-4">
          <Card.Header>Opis</Card.Header>
          <Card.Body>
            <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0 }}>
              {detail.description}
            </pre>
          </Card.Body>
        </Card>
      )}

      {hasAnalysis && (
        <>
          {hasUsefulBreakdown && <Card className="mb-4">
            <Card.Header>Rozbicie punktacji</Card.Header>
            <Card.Body className="p-0">
              <Table hover bordered className="mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Regula</th>
                    <th>Typ</th>
                    <th>Waga</th>
                    <th>Powod</th>
                  </tr>
                </thead>
                <tbody>
                  {scoreBreakdown.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="text-center text-muted">
                        Brak danych o rozbiciu punktacji.
                      </td>
                    </tr>
                  ) : (
                    scoreBreakdown.map((entry, idx) => (
                      <tr key={idx}>
                        <td><code>{entry.rule}</code></td>
                        <td>
                          <Badge bg={entry.type === 'boost' ? 'success' : 'danger'}>
                            {entry.type}
                          </Badge>
                        </td>
                        <td>{entry.weight >= 0 ? '+' : ''}{entry.weight.toFixed(2)}</td>
                        <td>{entry.reason}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>}

          {detail.llmComment && (
            <Card className="mb-4">
              <Card.Header>Komentarz LLM</Card.Header>
              <Card.Body>
                <p style={{ whiteSpace: 'pre-wrap' }}>{detail.llmComment}</p>
              </Card.Body>
            </Card>
          )}
        </>
      )}

      <div className="mb-4">
        <a href={detail.url} target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary">
          Zobacz oryginalny PR &rarr;
        </a>
      </div>
    </div>
  );
}
