import { Card, Badge, Row, Col } from 'react-bootstrap';
import type { ProjectSummary } from '../../types/project';

interface Props {
  summary: ProjectSummary;
}

function badgeVariant(pct: number): string {
  if (pct >= 50) return 'success';
  if (pct >= 20) return 'warning';
  return 'danger';
}

export default function BddSddCards({ summary }: Props) {
  return (
    <Row className="g-3">
      <Col md={6}>
        <Card className="h-100">
          <Card.Body className="text-center">
            <Card.Title>BDD (Behavior Driven Development)</Card.Title>
            <div className="fs-1 fw-bold">{summary.bddPercent.toFixed(0)}%</div>
            <div className="text-muted mb-2">
              {summary.bddCount} z {summary.totalPrs} PR-ów zawiera artefakty BDD
            </div>
            <Badge bg={badgeVariant(summary.bddPercent)} className="fs-6">
              {summary.bddPercent >= 50 ? 'Dobre pokrycie' : summary.bddPercent >= 20 ? 'Częściowe' : 'Niskie pokrycie'}
            </Badge>
          </Card.Body>
        </Card>
      </Col>
      <Col md={6}>
        <Card className="h-100">
          <Card.Body className="text-center">
            <Card.Title>SDD (Specification Driven Development)</Card.Title>
            <div className="fs-1 fw-bold">{summary.sddPercent.toFixed(0)}%</div>
            <div className="text-muted mb-2">
              {summary.sddCount} z {summary.totalPrs} PR-ów zawiera artefakty SDD
            </div>
            <Badge bg={badgeVariant(summary.sddPercent)} className="fs-6">
              {summary.sddPercent >= 50 ? 'Dobre pokrycie' : summary.sddPercent >= 20 ? 'Częściowe' : 'Niskie pokrycie'}
            </Badge>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
}
