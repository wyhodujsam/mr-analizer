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

function scoreBadgeColor(score: number): string {
  if (score >= 90) return 'success';
  if (score >= 70) return 'primary';
  if (score >= 50) return 'warning';
  return 'danger';
}

export default function AnalysisDetailPage() {
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
      .catch(() => {
        if (!controller.signal.aborted) setError('Nie udalo sie zaladowac szczegolow analizy.');
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [reportId, resultId]);

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" />
        <p className="mt-2">Ladowanie szczegolow analizy...</p>
      </div>
    );
  }

  if (error) return <Alert variant="danger">{error}</Alert>;
  if (!detail) return null;

  const hasDetailed = detail.hasDetailedAnalysis;
  const additions = detail.additions ?? 0;
  const deletions = detail.deletions ?? 0;
  const changedFilesCount = detail.changedFilesCount ?? 0;
  const categories = detail.categories ?? [];
  const humanOversight = detail.humanOversightRequired ?? [];
  const whyLlmFriendly = detail.whyLlmFriendly ?? [];
  const summaryTable = detail.summaryTable ?? [];

  return (
    <div>
      <div className="mb-3 d-flex gap-3">
        <Link to="/">&larr; Dashboard</Link>
        <Link to={`/mr/${reportId}/${resultId}`}>Szczegoly PR</Link>
      </div>

      {/* Header */}
      <h3 className="mb-1">{detail.title}</h3>
      <p className="text-muted mb-3">
        #{detail.externalId} by <strong>{detail.author}</strong>
        {' | '}
        {changedFilesCount} plikow,{' '}
        <span className="text-success">+{additions}</span>{' / '}
        <span className="text-danger">-{deletions}</span> linii
      </p>

      {/* Overall score + verdict */}
      <Row className="mb-4 g-3">
        <Col md={6}>
          <Card>
            <Card.Header>Ocena ogolna</Card.Header>
            <Card.Body>
              {hasDetailed && detail.overallAutomatability != null ? (
                <div className="d-flex align-items-center gap-3">
                  <h2 className="mb-0">
                    <Badge bg={scoreBadgeColor(detail.overallAutomatability)}>
                      ~{detail.overallAutomatability}%
                    </Badge>
                  </h2>
                  <span className="text-muted fs-5">
                    LLM poradzi sobie z tym zadaniem
                  </span>
                </div>
              ) : (
                <p className="text-muted mb-0">Brak danych z analizy LLM.</p>
              )}
            </Card.Body>
          </Card>
        </Col>
        <Col md={6}>
          <Card>
            <Card.Header>Score / Verdict</Card.Header>
            <Card.Body className="d-flex align-items-center gap-3">
              <ScoreBadge score={detail.score} verdict={detail.verdict} />
              <span className="text-muted">{detail.verdict ?? '—'}</span>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Categories table */}
      {hasDetailed && categories.length > 0 && (
        <Card className="mb-4">
          <Card.Header>Co LLM zrobi dobrze (kategorie zmian)</Card.Header>
          <Card.Body className="p-0">
            <Table hover bordered className="mb-0">
              <thead className="table-light">
                <tr>
                  <th style={{ width: '30%' }}>Kategoria zmian</th>
                  <th style={{ width: '10%' }}>Ocena</th>
                  <th>Uzasadnienie</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((cat, idx) => (
                  <tr key={idx}>
                    <td><strong>{cat.name}</strong></td>
                    <td>
                      <Badge bg={scoreBadgeColor(cat.score)}>{cat.score}%</Badge>
                    </td>
                    <td>{cat.reasoning}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      )}

      {/* Human oversight */}
      {hasDetailed && humanOversight.length > 0 && (
        <Card className="mb-4">
          <Card.Header>Co wymaga nadzoru czlowieka</Card.Header>
          <Card.Body>
            <ol className="mb-0">
              {humanOversight.map((item, idx) => (
                <li key={idx} className="mb-2">
                  <strong>{item.area}</strong>
                  <br />
                  <span className="text-muted">{item.reasoning}</span>
                </li>
              ))}
            </ol>
          </Card.Body>
        </Card>
      )}

      {/* Why LLM-friendly */}
      {hasDetailed && whyLlmFriendly.length > 0 && (
        <Card className="mb-4">
          <Card.Header>Dlaczego ten PR jest LLM-friendly</Card.Header>
          <Card.Body>
            <ul className="mb-0">
              {whyLlmFriendly.map((reason, idx) => (
                <li key={idx}>{reason}</li>
              ))}
            </ul>
          </Card.Body>
        </Card>
      )}

      {/* Summary table */}
      {hasDetailed && summaryTable.length > 0 && (
        <Card className="mb-4">
          <Card.Header>Podsumowanie</Card.Header>
          <Card.Body className="p-0">
            <Table hover bordered className="mb-0">
              <thead className="table-light">
                <tr>
                  <th>Aspekt</th>
                  <th style={{ width: '15%' }}>Ocena</th>
                  <th>Uwagi</th>
                </tr>
              </thead>
              <tbody>
                {summaryTable.map((row, idx) => (
                  <tr key={idx}>
                    <td>{row.aspect}</td>
                    <td>
                      {row.score != null ? (
                        <Badge bg={scoreBadgeColor(row.score)}>{row.score}%</Badge>
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </td>
                    <td>{row.note}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card.Body>
        </Card>
      )}

      {/* LLM Comment */}
      {detail.llmComment && (
        <Card className="mb-4">
          <Card.Header>Komentarz LLM</Card.Header>
          <Card.Body>
            <p style={{ whiteSpace: 'pre-wrap' }} className="mb-0">{detail.llmComment}</p>
          </Card.Body>
        </Card>
      )}

      {/* External link */}
      <div className="mb-4">
        <a href={detail.url} target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary">
          Zobacz oryginalny PR &rarr;
        </a>
      </div>
    </div>
  );
}
