import { useState } from 'react';
import { Alert } from 'react-bootstrap';
import AnalysisForm from '../components/AnalysisForm';
import SummaryCard from '../components/SummaryCard';
import MrTable from '../components/MrTable';
import { runAnalysis } from '../api/analysisApi';
import type { AnalysisRequest, AnalysisResponse } from '../types';

export default function DashboardPage() {
  const [loading, setLoading] = useState(false);
  const [analysisResponse, setAnalysisResponse] = useState<AnalysisResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(request: AnalysisRequest) {
    setLoading(true);
    setError(null);
    setAnalysisResponse(null);
    try {
      const result = await runAnalysis(request);
      setAnalysisResponse(result);
    } catch (err: unknown) {
      if (
        err &&
        typeof err === 'object' &&
        'response' in err &&
        err.response &&
        typeof err.response === 'object' &&
        'data' in err.response
      ) {
        const data = (err.response as { data?: { message?: string } }).data;
        setError(data?.message ?? 'An unexpected error occurred.');
      } else {
        setError('Failed to connect to the server. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  const automatable = analysisResponse
    ? {
        count: analysisResponse.automatableCount,
        percentage:
          analysisResponse.totalMrs > 0
            ? (analysisResponse.automatableCount / analysisResponse.totalMrs) * 100
            : 0,
      }
    : { count: 0, percentage: 0 };

  const maybe = analysisResponse
    ? {
        count: analysisResponse.maybeCount,
        percentage:
          analysisResponse.totalMrs > 0
            ? (analysisResponse.maybeCount / analysisResponse.totalMrs) * 100
            : 0,
      }
    : { count: 0, percentage: 0 };

  const notSuitable = analysisResponse
    ? {
        count: analysisResponse.notSuitableCount,
        percentage:
          analysisResponse.totalMrs > 0
            ? (analysisResponse.notSuitableCount / analysisResponse.totalMrs) * 100
            : 0,
      }
    : { count: 0, percentage: 0 };

  return (
    <div>
      <h2 className="mb-4">MR Analysis Dashboard</h2>

      <AnalysisForm onSubmit={handleSubmit} loading={loading} />

      {error && (
        <Alert variant="danger" className="mt-3" dismissible onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {analysisResponse && (
        <div className="mt-4">
          <SummaryCard
            totalMrs={analysisResponse.totalMrs}
            automatable={automatable}
            maybe={maybe}
            notSuitable={notSuitable}
          />

          {analysisResponse.results.length === 0 ? (
            <Alert variant="info">No PRs match the analysis criteria.</Alert>
          ) : (
            <MrTable
              results={analysisResponse.results}
              reportId={analysisResponse.reportId}
            />
          )}
        </div>
      )}
    </div>
  );
}
