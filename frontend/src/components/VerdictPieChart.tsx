import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';

ChartJS.register(ArcElement, Tooltip, Legend);

interface Props {
  automatable: number;
  maybe: number;
  notSuitable: number;
}

export default function VerdictPieChart({ automatable, maybe, notSuitable }: Props) {
  if (automatable === 0 && maybe === 0 && notSuitable === 0) {
    return null;
  }

  const data = {
    labels: [
      `Automatable (${automatable})`,
      `Maybe (${maybe})`,
      `Not Suitable (${notSuitable})`,
    ],
    datasets: [
      {
        data: [automatable, maybe, notSuitable],
        backgroundColor: ['#198754', '#ffc107', '#dc3545'],
        borderWidth: 1,
      },
    ],
  };

  const options = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom' as const,
      },
    },
  };

  return (
    <div style={{ maxWidth: 300, margin: '0 auto' }}>
      <Doughnut data={data} options={options} />
    </div>
  );
}
