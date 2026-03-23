import { useMemo } from 'react';
import { Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  Tooltip,
  Title,
  Legend,
} from 'chart.js';
import type { DailyActivity } from '../../types/activity';

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Tooltip, Title, Legend);

interface Props {
  dailyActivity: Record<string, DailyActivity>;
}

function computeTrendLine(counts: number[]): number[] {
  const n = counts.length;
  if (n < 2) return counts;

  // Simple linear regression: y = mx + b
  let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
  for (let i = 0; i < n; i++) {
    sumX += i;
    sumY += counts[i];
    sumXY += i * counts[i];
    sumX2 += i * i;
  }
  const m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
  const b = (sumY - m * sumX) / n;

  return counts.map((_, i) => parseFloat((m * i + b).toFixed(2)));
}

export default function ActivityBarChart({ dailyActivity }: Props) {
  const { labels, counts, trend } = useMemo(() => {
    const entries = Object.entries(dailyActivity).sort(([a], [b]) => a.localeCompare(b));
    const c = entries.map(([, da]) => da.count);
    return {
      labels: entries.map(([date]) => date),
      counts: c,
      trend: computeTrendLine(c),
    };
  }, [dailyActivity]);

  if (labels.length === 0) return null;

  const data = {
    labels,
    datasets: [
      {
        type: 'bar' as const,
        label: 'Liczba PR-ów',
        data: counts,
        backgroundColor: '#40c463',
        borderRadius: 3,
        order: 2,
      },
      {
        type: 'line' as const,
        label: 'Trend',
        data: trend,
        borderColor: '#d63384',
        borderWidth: 2,
        borderDash: [6, 3],
        pointRadius: 0,
        fill: false,
        order: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      title: { display: false },
      legend: { display: true, position: 'top' as const },
      tooltip: {
        callbacks: {
          label: (ctx: any) => {
            if (ctx.dataset.type === 'line') return `Trend: ${ctx.raw}`;
            return `${ctx.raw} PR-ów`;
          },
        },
      },
    },
    scales: {
      x: {
        title: { display: true, text: 'Data' },
        ticks: { maxRotation: 45 },
      },
      y: {
        title: { display: true, text: 'Liczba PR-ów' },
        beginAtZero: true,
        ticks: { stepSize: 1 },
      },
    },
  };

  return <Bar data={data} options={options} />;
}
