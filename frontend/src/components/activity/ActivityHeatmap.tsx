import { useState, useMemo } from 'react';
import type { DailyActivity } from '../../types/activity';

interface Props {
  dailyActivity: Record<string, DailyActivity>;
  onDayClick: (date: string, activity: DailyActivity | null) => void;
}

const CELL_SIZE = 13;
const CELL_GAP = 2;
const CELL_STEP = CELL_SIZE + CELL_GAP;
const WEEKS = 13;
const DAYS = 7;
const LABEL_WIDTH = 30;
const HEADER_HEIGHT = 20;

const COLORS = ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39'];

const DAY_LABELS = ['Pon', '', 'Śr', '', 'Pt', '', ''];

function getColor(count: number): string {
  if (count === 0) return COLORS[0];
  if (count === 1) return COLORS[1];
  if (count === 2) return COLORS[2];
  if (count <= 4) return COLORS[3];
  return COLORS[4];
}

/** Format date as YYYY-MM-DD using UTC to match backend LocalDate keys */
function toDateString(date: Date): string {
  return date.toISOString().split('T')[0];
}

function getWeeksData(dailyActivity: Record<string, DailyActivity>) {
  const today = new Date();
  const startDate = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
  startDate.setUTCDate(startDate.getUTCDate() - (WEEKS * 7) + 1);

  // Align to Monday (UTC)
  const dayOfWeek = startDate.getUTCDay();
  const offset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  startDate.setUTCDate(startDate.getUTCDate() + offset);

  const weeks: { date: string; count: number; dayOfWeek: number }[][] = [];
  const current = new Date(startDate);

  for (let w = 0; w < WEEKS; w++) {
    const week: { date: string; count: number; dayOfWeek: number }[] = [];
    for (let d = 0; d < DAYS; d++) {
      const dateStr = toDateString(current);
      const activity = dailyActivity[dateStr];
      week.push({
        date: dateStr,
        count: activity?.count ?? 0,
        dayOfWeek: d,
      });
      current.setUTCDate(current.getUTCDate() + 1);
    }
    weeks.push(week);
  }

  return weeks;
}

function getMonthLabels(weeks: { date: string; count: number; dayOfWeek: number }[][]) {
  const months: { label: string; x: number }[] = [];
  const monthNames = ['Sty', 'Lut', 'Mar', 'Kwi', 'Maj', 'Cze', 'Lip', 'Sie', 'Wrz', 'Paź', 'Lis', 'Gru'];
  let lastMonth = -1;

  for (let w = 0; w < weeks.length; w++) {
    const firstDay = weeks[w][0];
    const month = new Date(firstDay.date + 'T00:00:00Z').getUTCMonth();
    if (month !== lastMonth) {
      months.push({
        label: monthNames[month],
        x: LABEL_WIDTH + w * CELL_STEP,
      });
      lastMonth = month;
    }
  }

  return months;
}

function countOutsideWindow(dailyActivity: Record<string, DailyActivity>, weeks: { date: string }[][]): number {
  const visibleDates = new Set(weeks.flatMap(w => w.map(d => d.date)));
  return Object.keys(dailyActivity).filter(d => !visibleDates.has(d)).length;
}

export default function ActivityHeatmap({ dailyActivity, onDayClick }: Props) {
  const [hoveredDay, setHoveredDay] = useState<{ date: string; count: number; x: number; y: number } | null>(null);
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  const weeks = useMemo(() => getWeeksData(dailyActivity), [dailyActivity]);
  const monthLabels = useMemo(() => getMonthLabels(weeks), [weeks]);
  const outsideCount = useMemo(() => countOutsideWindow(dailyActivity, weeks), [dailyActivity, weeks]);

  const svgWidth = LABEL_WIDTH + WEEKS * CELL_STEP;
  const svgHeight = HEADER_HEIGHT + DAYS * CELL_STEP;

  function handleClick(date: string) {
    const newSelected = selectedDate === date ? null : date;
    setSelectedDate(newSelected);
    onDayClick(newSelected ?? '', newSelected ? (dailyActivity[newSelected] ?? null) : null);
  }

  function handleKeyDown(e: React.KeyboardEvent, date: string) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick(date);
    }
  }

  return (
    <div className="position-relative">
      <svg
        width={svgWidth}
        height={svgHeight}
        style={{ display: 'block' }}
        role="img"
        aria-label="Heatmapa aktywności — ostatnie 13 tygodni"
      >
        {/* Month labels */}
        {monthLabels.map((m, i) => (
          <text key={i} x={m.x} y={12} fontSize={10} fill="#767676">
            {m.label}
          </text>
        ))}

        {/* Day labels */}
        {DAY_LABELS.map((label, d) =>
          label ? (
            <text key={d} x={0} y={HEADER_HEIGHT + d * CELL_STEP + CELL_SIZE - 2} fontSize={10} fill="#767676">
              {label}
            </text>
          ) : null
        )}

        {/* Cells */}
        {weeks.map((week, w) =>
          week.map((day) => (
            <rect
              key={day.date}
              x={LABEL_WIDTH + w * CELL_STEP}
              y={HEADER_HEIGHT + day.dayOfWeek * CELL_STEP}
              width={CELL_SIZE}
              height={CELL_SIZE}
              rx={2}
              ry={2}
              fill={getColor(day.count)}
              stroke={selectedDate === day.date ? '#333' : 'none'}
              strokeWidth={selectedDate === day.date ? 2 : 0}
              style={{ cursor: day.count > 0 ? 'pointer' : 'default' }}
              tabIndex={day.count > 0 ? 0 : undefined}
              role={day.count > 0 ? 'button' : undefined}
              aria-label={`${day.date}: ${day.count} PR-ów`}
              onMouseEnter={(e) => {
                const rect = (e.target as SVGRectElement).getBoundingClientRect();
                setHoveredDay({ date: day.date, count: day.count, x: rect.left, y: rect.top });
              }}
              onMouseLeave={() => setHoveredDay(null)}
              onClick={() => handleClick(day.date)}
              onKeyDown={(e) => handleKeyDown(e, day.date)}
            />
          ))
        )}
      </svg>

      {/* Tooltip */}
      {hoveredDay && (
        <div
          className="position-fixed bg-dark text-white px-2 py-1 rounded small"
          style={{
            left: hoveredDay.x + CELL_SIZE + 4,
            top: hoveredDay.y - 4,
            pointerEvents: 'none',
            zIndex: 1000,
            whiteSpace: 'nowrap',
          }}
        >
          {hoveredDay.date}: {hoveredDay.count} PR-{hoveredDay.count === 1 ? '' : 'ów'}
        </div>
      )}

      {/* Legend + info */}
      <div className="d-flex align-items-center gap-1 mt-2" style={{ fontSize: 11 }}>
        <span className="text-muted me-1">Mniej</span>
        {COLORS.map((color, i) => (
          <div key={i} style={{ width: 12, height: 12, backgroundColor: color, borderRadius: 2 }} />
        ))}
        <span className="text-muted ms-1">Więcej</span>
        {outsideCount > 0 && (
          <span className="text-muted ms-3">
            ({outsideCount} dni z aktywnością poza widocznym zakresem)
          </span>
        )}
      </div>
    </div>
  );
}
