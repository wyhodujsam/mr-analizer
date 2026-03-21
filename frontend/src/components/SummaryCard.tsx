import { Card, Row, Col } from 'react-bootstrap';

interface Props {
  totalMrs: number;
  automatable: { count: number; percentage: number };
  maybe: { count: number; percentage: number };
  notSuitable: { count: number; percentage: number };
}

export default function SummaryCard({ totalMrs, automatable, maybe, notSuitable }: Props) {
  return (
    <>
      <p className="text-muted mb-3">
        Przeanalizowanych PR-ow: <strong>{totalMrs}</strong>
      </p>
      <Row className="mb-4 g-3">
        <Col md={4}>
          <Card border="success" className="h-100">
            <Card.Header className="bg-success text-white fw-semibold">
              Do automatyzacji
            </Card.Header>
            <Card.Body className="text-center">
              <div className="display-6 text-success fw-bold">{automatable.count}</div>
              <div className="text-muted">{automatable.percentage.toFixed(1)}%</div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card border="warning" className="h-100">
            <Card.Header className="bg-warning text-dark fw-semibold">
              Moze
            </Card.Header>
            <Card.Body className="text-center">
              <div className="display-6 text-warning fw-bold">{maybe.count}</div>
              <div className="text-muted">{maybe.percentage.toFixed(1)}%</div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card border="danger" className="h-100">
            <Card.Header className="bg-danger text-white fw-semibold">
              Nie nadaje sie
            </Card.Header>
            <Card.Body className="text-center">
              <div className="display-6 text-danger fw-bold">{notSuitable.count}</div>
              <div className="text-muted">{notSuitable.percentage.toFixed(1)}%</div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </>
  );
}
