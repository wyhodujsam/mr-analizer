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

function formatDate(iso: string | null): string {
  if (!iso) return '\u2014';
  return new Date(iso).toLocaleString();
}

export default function MrDetailPage() {
  const { reportId, resultId } = useParams<{ reportId: string; resultId: string }>();
  const [detail, setDetail] = useState<MrDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!reportId || !resultId) return;
    getMrDetail(Number(reportId), Number(resultId))
      .then(setDetail)
      .catch((err: unknown) => {
        if (
          err &&
          typeof err === 'object' &&
          'response' in err &&
          err.response &&
          typeof err.response === 'object' &&
          'data' in err.response
        ) {
          const data = (err.response as { data?: { message?: string } }).data;
          setError(data?.message ?? 'Failed to load MR details.');
        } else {
          setError('Failed to connect to the server.');
        }
      })
      .finally(() => setLoading(false));
  }, [reportId, resultId]);

  if (loading) {
    return (
      <div className="text-center mt-5">
        <Spinner animation="border" />
        <p className="mt-2">Loading MR details...</p>
      </div>
    );
  }

  if (error) {
    return <Alert variant="danger">{error}</Alert>;
  }

  if (!detail) return null;

  const hasAnalysis = detail.score !== undefined && detail.score !== null && detail.verdict;

  return (
    <div>
      <div className="mb-3">
        <Link to="/">&larr; Back to Dashboard</Link>
      </div>

      <h3 className="mb-1">{detail.title}</h3>
      <p className="text-muted mb-3">
        #{detail.externalId} by <strong>{detail.author}</strong>
      </p>

      <Row className="mb-4 g-3">
        <Col md={6}>
          <Card>
            <Card.Header>Metadata</Card.Header>
            <Card.Body>
              <Table size="sm" borderless className="mb-0">
                <tbody>
                  <tr>
                    <th style={{ width: '140px' }}>State</th>
                    <td>
                      <Badge bg="secondary">{detail.state}</Badge>
                    </td>
                  </tr>
                  <tr>
                    <th>Source Branch</th>
                    <td><code>{detail.sourceBranch}</code></td>
                  </tr>
                  <tr>
                    <th>Target Branch</th>
                    <td><code>{detail.targetBranch}</code></td>
                  </tr>
                  <tr>
                    <th>Created</th>
                    <td>{formatDate(detail.createdAt)}</td>
                  </tr>
                  <tr>
                    <th>Merged</th>
                    <td>{formatDate(detail.mergedAt)}</td>
                  </tr>
                  <tr>
                    <th>Labels</th>
                    <td>
                      {detail.labels.length > 0
                        ? detail.labels.map((l) => (
                            <Badge key={l} bg="info" text="dark" className="me-1">
                              {l}
                            </Badge>
                          ))
                        : <span className="text-muted">none</span>}
                    </td>
                  </tr>
                  <tr>
                    <th>Has Tests</th>
                    <td>{detail.hasTests ? 'Yes' : 'No'}</td>
                  </tr>
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>

        <Col md={6}>
          <Card>
            <Card.Header>Diff Stats</Card.Header>
            <Card.Body>
              <Table size="sm" borderless className="mb-0">
                <tbody>
                  <tr>
                    <th style={{ width: '160px' }}>Additions</th>
                    <td className="text-success">+{detail.diffStats.additions}</td>
                  </tr>
                  <tr>
                    <th>Deletions</th>
                    <td className="text-danger">-{detail.diffStats.deletions}</td>
                  </tr>
                  <tr>
                    <th>Changed Files</th>
                    <td>{detail.diffStats.changedFilesCount}</td>
                  </tr>
                </tbody>
              </Table>
            </Card.Body>
          </Card>

          {hasAnalysis && (
            <Card className="mt-3">
              <Card.Header>Score</Card.Header>
              <Card.Body className="d-flex align-items-center gap-3">
                <ScoreBadge score={detail.score} verdict={detail.verdict} />
                <span className="text-muted">{detail.verdict}</span>
              </Card.Body>
            </Card>
          )}
        </Col>
      </Row>

      {detail.description && (
        <Card className="mb-4">
          <Card.Header>Description</Card.Header>
          <Card.Body>
            <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0 }}>
              {detail.description}
            </pre>
          </Card.Body>
        </Card>
      )}

      {hasAnalysis && (
        <>
          <Card className="mb-4">
            <Card.Header>Score Breakdown</Card.Header>
            <Card.Body className="p-0">
              <Table hover bordered className="mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Rule</th>
                    <th>Type</th>
                    <th>Weight</th>
                    <th>Reason</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.scoreBreakdown.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="text-center text-muted">
                        No score breakdown available.
                      </td>
                    </tr>
                  ) : (
                    detail.scoreBreakdown.map((entry, idx) => (
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
          </Card>

          {detail.llmComment && (
            <Card className="mb-4">
              <Card.Header>LLM Analysis</Card.Header>
              <Card.Body>
                <p style={{ whiteSpace: 'pre-wrap' }}>{detail.llmComment}</p>
              </Card.Body>
            </Card>
          )}
        </>
      )}

      <div className="mb-4">
        <a href={detail.url} target="_blank" rel="noopener noreferrer" className="btn btn-outline-primary">
          View Original PR &rarr;
        </a>
      </div>
    </div>
  );
}
