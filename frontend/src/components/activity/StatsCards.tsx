import { Row, Col, Card } from 'react-bootstrap';
import type { ContributorStats, Severity } from '../../types/activity';

interface Props {
  stats: ContributorStats;
  activeSeverity: Severity | null;
  onSeverityClick: (severity: Severity | null) => void;
}

const severityConfig: { key: Severity; bg: string; text?: string; label: string }[] = [
  { key: 'CRITICAL', bg: 'danger', label: 'krytycznych' },
  { key: 'WARNING', bg: 'warning', text: 'dark', label: 'ostrzeżeń' },
  { key: 'INFO', bg: 'info', text: 'dark', label: 'informacyjnych' },
];

export default function StatsCards({ stats, activeSeverity, onSeverityClick }: Props) {
  const cards = [
    { label: 'Łącznie PR-ów', value: stats.totalPrs, color: 'primary' },
    { label: 'Średni rozmiar', value: `${Math.round(stats.avgSize)} linii`, color: 'info' },
    { label: 'Średni czas review', value: formatTime(stats.avgReviewTimeMinutes), color: 'secondary' },
    { label: 'Praca weekendowa', value: `${stats.weekendPercentage.toFixed(1)}%`, color: 'warning' },
  ];

  const hasAnyFlags = severityConfig.some(s => (stats.flagCounts[s.key] ?? 0) > 0);

  return (
    <>
      <Row className="mb-3">
        {cards.map((card) => (
          <Col key={card.label} xs={6} md={3}>
            <Card className={`text-center border-${card.color}`}>
              <Card.Body>
                <Card.Title className="fs-3">{card.value}</Card.Title>
                <Card.Text className="text-muted">{card.label}</Card.Text>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>
      {hasAnyFlags && (
        <Row className="mb-3">
          <Col>
            <div className="d-flex gap-2 align-items-center">
              {severityConfig.map(({ key, bg, text, label }) => {
                const count = stats.flagCounts[key] ?? 0;
                if (count === 0) return null;
                const isActive = activeSeverity === key;
                return (
                  <span
                    key={key}
                    className={`badge bg-${bg} fs-6`}
                    style={{
                      cursor: 'pointer',
                      opacity: activeSeverity && !isActive ? 0.4 : 1,
                      outline: isActive ? '2px solid #333' : 'none',
                      outlineOffset: 2,
                      color: text === 'dark' ? '#212529' : undefined,
                    }}
                    onClick={() => onSeverityClick(isActive ? null : key)}
                    onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onSeverityClick(isActive ? null : key); } }}
                    role="button"
                    tabIndex={0}
                    title={isActive ? 'Pokaż wszystkie' : `Filtruj: ${label}`}
                  >
                    {count} {label}
                  </span>
                );
              })}
              {activeSeverity && (
                <span
                  className="text-muted small"
                  style={{ cursor: 'pointer' }}
                  onClick={() => onSeverityClick(null)}
                >
                  &times; wyczyść filtr
                </span>
              )}
            </div>
          </Col>
        </Row>
      )}
    </>
  );
}

function formatTime(minutes: number): string {
  if (minutes < 60) return `${Math.round(minutes)} min`;
  const hours = Math.floor(minutes / 60);
  const mins = Math.round(minutes % 60);
  return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
}
