import type { WeeklyCount } from '../../types/activity';

interface Props {
  breakdown: WeeklyCount[];
}

export default function VelocityChart({ breakdown }: Props) {
  if (!breakdown || breakdown.length === 0) return null;

  const max = Math.max(...breakdown.map(w => w.count), 1);
  const barWidth = 16;
  const gap = 4;
  const chartHeight = 40;
  const width = breakdown.length * (barWidth + gap);

  return (
    <svg width={width} height={chartHeight + 14} className="mt-2">
      {breakdown.map((w, i) => {
        const barHeight = (w.count / max) * chartHeight;
        const x = i * (barWidth + gap);
        const y = chartHeight - barHeight;
        return (
          <g key={w.weekStart}>
            <rect
              x={x}
              y={y}
              width={barWidth}
              height={Math.max(barHeight, 2)}
              fill={i === breakdown.length - 1 ? '#0d6efd' : '#adb5bd'}
              rx={2}
            >
              <title>{w.weekStart}: {w.count} PR</title>
            </rect>
            <text
              x={x + barWidth / 2}
              y={chartHeight + 12}
              textAnchor="middle"
              fontSize="9"
              fill="#6c757d"
            >
              {w.count}
            </text>
          </g>
        );
      })}
    </svg>
  );
}
