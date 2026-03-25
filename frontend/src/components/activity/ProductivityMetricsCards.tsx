import { Row, Col, Card, Badge, ProgressBar } from 'react-bootstrap';
import type { ProductivityMetrics } from '../../types/activity';
import VelocityChart from './VelocityChart';

interface Props {
  productivity: ProductivityMetrics | null;
}

function trendArrow(trend: string) {
  switch (trend) {
    case 'rising': return <span className="text-success ms-2">&#9650;</span>;
    case 'falling': return <span className="text-danger ms-2">&#9660;</span>;
    default: return <span className="text-secondary ms-2">&#9654;</span>;
  }
}

function formatHours(h: number): string {
  if (h < 1) return `${Math.round(h * 60)} min`;
  if (h < 24) return `${h.toFixed(1)}h`;
  return `${(h / 24).toFixed(1)} dni`;
}

export default function ProductivityMetricsCards({ productivity }: Props) {
  if (!productivity) return null;

  const { velocity, cycleTime, impact, codeChurn, reviewEngagement } = productivity;

  const addPct = impact.totalLines > 0
    ? Math.round((impact.totalAdditions / impact.totalLines) * 100)
    : 50;

  return (
    <>
      <h4 className="mt-4 mb-3">Wydajność</h4>
      <Row className="g-3 mb-3">
        <Col md={6} lg={3}>
          <Card className="h-100">
            <Card.Body className="text-center">
              <Card.Subtitle className="text-muted mb-2">Velocity</Card.Subtitle>
              <div className="fs-3 fw-bold">
                {velocity.prsPerWeek.toFixed(1)}
                <small className="fs-6 text-muted"> PR/tyg</small>
                {trendArrow(velocity.trend)}
              </div>
              <VelocityChart breakdown={velocity.weeklyBreakdown} />
            </Card.Body>
          </Card>
        </Col>

        <Col md={6} lg={3}>
          <Card className="h-100">
            <Card.Body className="text-center">
              <Card.Subtitle className="text-muted mb-2">Cycle Time</Card.Subtitle>
              {cycleTime.avgHours === 0 && cycleTime.medianHours === 0 ? (
                <div className="text-muted">Brak danych</div>
              ) : (
                <>
                  <div className="fs-3 fw-bold">{formatHours(cycleTime.medianHours)}</div>
                  <small className="text-muted">
                    avg: {formatHours(cycleTime.avgHours)} | p90: {formatHours(cycleTime.p90Hours)}
                  </small>
                </>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col md={6} lg={3}>
          <Card className="h-100">
            <Card.Body className="text-center">
              <Card.Subtitle className="text-muted mb-2">Impact</Card.Subtitle>
              <div className="fs-3 fw-bold">{impact.totalLines.toLocaleString()}</div>
              <small className="text-muted">linii ({impact.avgLinesPerPr.toFixed(0)}/PR)</small>
              <ProgressBar className="mt-2" style={{ height: 8 }}>
                <ProgressBar variant="success" now={addPct} key={1} />
                <ProgressBar variant="danger" now={100 - addPct} key={2} />
              </ProgressBar>
              <small className="text-muted">
                <span className="text-success">+{impact.totalAdditions}</span>
                {' / '}
                <span className="text-danger">-{impact.totalDeletions}</span>
              </small>
            </Card.Body>
          </Card>
        </Col>

        <Col md={6} lg={3}>
          <Card className="h-100">
            <Card.Body className="text-center">
              <Card.Subtitle className="text-muted mb-2">Code Churn</Card.Subtitle>
              <div className="fs-3 fw-bold">{codeChurn.churnRatio.toFixed(2)}</div>
              <Badge bg={
                codeChurn.churnRatio < 0.2 ? 'info'
                  : codeChurn.churnRatio <= 0.8 ? 'success'
                  : 'warning'
              }>
                {codeChurn.label}
              </Badge>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {reviewEngagement && (
        <Row className="g-3 mb-3">
          <Col md={6} lg={4}>
            <Card>
              <Card.Body className="text-center">
                <Card.Subtitle className="text-muted mb-2">Review Engagement</Card.Subtitle>
                <div>
                  <span className="fw-bold">{reviewEngagement.reviewsGiven}</span> danych
                  {' / '}
                  <span className="fw-bold">{reviewEngagement.reviewsReceived}</span> otrzymanych
                </div>
                <Badge bg={
                  reviewEngagement.ratio > 1.5 ? 'success'
                    : reviewEngagement.ratio >= 0.5 ? 'primary'
                    : 'warning'
                } className="mt-1">
                  {reviewEngagement.label} (ratio: {reviewEngagement.ratio.toFixed(2)})
                </Badge>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      )}
    </>
  );
}
