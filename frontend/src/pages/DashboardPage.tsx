import { useState, useEffect, useCallback } from 'react';
import { Alert, Button, Form, Row, Col, Spinner, Badge } from 'react-bootstrap';
import RepoSelector from '../components/RepoSelector';
import MrBrowseTable from '../components/MrBrowseTable';
import SummaryCard from '../components/SummaryCard';
import MrTable from '../components/MrTable';
import AnalysisHistory from '../components/AnalysisHistory';
import {
  getRepos,
  addRepo,
  deleteRepo,
  browseMrs,
  runAnalysis,
  getAnalyses,
  getAnalysisBySlug,
  deleteAnalysis,
} from '../api/analysisApi';
import type {
  SavedRepository,
  MrBrowseItem,
  AnalysisRequest,
  AnalysisResponse,
} from '../types';

type Step = 'select' | 'browse' | 'analyzed';

export default function DashboardPage() {
  // Repo selection
  const [savedRepos, setSavedRepos] = useState<SavedRepository[]>([]);
  const [selectedSlug, setSelectedSlug] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('github');

  // Browse form
  const [targetBranch, setTargetBranch] = useState('');
  const [after, setAfter] = useState('');
  const [before, setBefore] = useState('');
  const [limit, setLimit] = useState(100);
  const [useLlm, setUseLlm] = useState(false);

  // State machine
  const [step, setStep] = useState<Step>('select');
  const [browsedMrs, setBrowsedMrs] = useState<MrBrowseItem[]>([]);
  const [analysisResponse, setAnalysisResponse] = useState<AnalysisResponse | null>(null);

  // Cache detection
  const [cachedAnalysis, setCachedAnalysis] = useState<AnalysisResponse | null>(null);

  // History
  const [analysisHistory, setAnalysisHistory] = useState<AnalysisResponse[]>([]);

  // Loading & error
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingAnalyze, setLoadingAnalyze] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load repos and history on mount
  useEffect(() => {
    loadRepos();
    loadHistory();
  }, []);

  async function loadRepos() {
    try {
      const repos = await getRepos();
      setSavedRepos(repos);
    } catch {
      // silently ignore
    }
  }

  const loadHistory = useCallback(async () => {
    try {
      const analyses = await getAnalyses();
      setAnalysisHistory(analyses);
    } catch {
      // silently ignore
    }
  }, []);

  async function handleRepoSelect(slug: string, provider: string) {
    setSelectedSlug(slug);
    setSelectedProvider(provider);
    setStep('select');
    setBrowsedMrs([]);
    setAnalysisResponse(null);
    setCachedAnalysis(null);

    // Save to backend if not already saved
    const exists = savedRepos.find(
      (r) => r.projectSlug === slug && r.provider === provider
    );
    if (!exists) {
      try {
        await addRepo(slug, provider);
        await loadRepos();
      } catch {
        // silently ignore duplicates
      }
    }

    // Check for cached analysis
    try {
      const cached = await getAnalysisBySlug(slug);
      setCachedAnalysis(cached);
    } catch {
      setCachedAnalysis(null);
    }
  }

  async function handleRepoDelete(id: number) {
    try {
      await deleteRepo(id);
      await loadRepos();
      // If deleted repo was selected, clear selection
      const deleted = savedRepos.find((r) => r.id === id);
      if (deleted && deleted.projectSlug === selectedSlug) {
        setSelectedSlug('');
        setSelectedProvider('github');
        setStep('select');
        setBrowsedMrs([]);
        setAnalysisResponse(null);
        setCachedAnalysis(null);
      }
    } catch {
      setError('Nie udalo sie usunac repozytorium.');
    }
  }

  function buildRequest(): AnalysisRequest {
    const req: AnalysisRequest = {
      projectSlug: selectedSlug,
      provider: selectedProvider,
      limit,
      useLlm,
    };
    if (targetBranch) req.targetBranch = targetBranch;
    if (after) req.after = after;
    if (before) req.before = before;
    return req;
  }

  async function handleBrowse(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedSlug) return;
    setLoadingBrowse(true);
    setError(null);
    setAnalysisResponse(null);
    try {
      const items = await browseMrs(buildRequest());
      setBrowsedMrs(items);
      setStep('browse');
    } catch (err: unknown) {
      setError(extractError(err));
    } finally {
      setLoadingBrowse(false);
    }
  }

  async function handleAnalyze() {
    setLoadingAnalyze(true);
    setError(null);
    try {
      const result = await runAnalysis(buildRequest());
      setAnalysisResponse(result);
      setStep('analyzed');
      setCachedAnalysis(result);
      await loadHistory();
    } catch (err: unknown) {
      setError(extractError(err));
    } finally {
      setLoadingAnalyze(false);
    }
  }

  async function handleLoadCached() {
    if (cachedAnalysis) {
      setAnalysisResponse(cachedAnalysis);
      setStep('analyzed');
    }
  }

  async function handleDeleteCached() {
    if (!cachedAnalysis) return;
    if (!window.confirm('Usunac istniejaca analize?')) return;
    try {
      await deleteAnalysis(cachedAnalysis.reportId);
      setCachedAnalysis(null);
      await loadHistory();
    } catch {
      setError('Nie udalo sie usunac analizy.');
    }
  }

  async function handleHistoryDelete(reportId: number) {
    try {
      await deleteAnalysis(reportId);
      await loadHistory();
      // If currently viewing this analysis, reset
      if (analysisResponse?.reportId === reportId) {
        setAnalysisResponse(null);
        setStep(browsedMrs.length > 0 ? 'browse' : 'select');
      }
      // Refresh cache detection
      if (cachedAnalysis?.reportId === reportId) {
        setCachedAnalysis(null);
      }
    } catch {
      setError('Nie udalo sie usunac analizy.');
    }
  }

  function extractError(err: unknown): string {
    if (
      err &&
      typeof err === 'object' &&
      'response' in err &&
      err.response &&
      typeof err.response === 'object' &&
      'data' in err.response
    ) {
      const data = (err.response as { data?: { message?: string } }).data;
      return data?.message ?? 'Wystapil nieoczekiwany blad.';
    }
    return 'Nie udalo sie polaczyc z serwerem.';
  }

  // Compute summary stats
  const summaryProps = analysisResponse
    ? {
        totalMrs: analysisResponse.totalMrs,
        automatable: {
          count: analysisResponse.automatableCount,
          percentage:
            analysisResponse.totalMrs > 0
              ? (analysisResponse.automatableCount / analysisResponse.totalMrs) * 100
              : 0,
        },
        maybe: {
          count: analysisResponse.maybeCount,
          percentage:
            analysisResponse.totalMrs > 0
              ? (analysisResponse.maybeCount / analysisResponse.totalMrs) * 100
              : 0,
        },
        notSuitable: {
          count: analysisResponse.notSuitableCount,
          percentage:
            analysisResponse.totalMrs > 0
              ? (analysisResponse.notSuitableCount / analysisResponse.totalMrs) * 100
              : 0,
        },
      }
    : null;

  return (
    <div>
      <h2 className="mb-4">MR Analysis Dashboard</h2>

      {/* Step 1: Repo selection */}
      <RepoSelector
        savedRepos={savedRepos}
        onSelect={handleRepoSelect}
        onDelete={handleRepoDelete}
        selectedSlug={selectedSlug}
        selectedProvider={selectedProvider}
      />

      {/* Cache detection badge */}
      {cachedAnalysis && step !== 'analyzed' && (
        <Alert variant="info" className="mt-3 cache-badge d-flex align-items-center gap-2">
          <Badge bg="info">Analiza istnieje</Badge>
          <span>
            Znaleziono analize dla <strong>{cachedAnalysis.projectSlug}</strong> z{' '}
            {new Date(cachedAnalysis.analyzedAt).toLocaleString()}
          </span>
          <Button variant="outline-primary" size="sm" onClick={handleLoadCached}>
            Wczytaj
          </Button>
          <Button variant="outline-danger" size="sm" onClick={handleDeleteCached}>
            Usun i analizuj ponownie
          </Button>
        </Alert>
      )}

      {/* Browse form */}
      {selectedSlug && (
        <>
          <hr className="step-separator" />
          <Form onSubmit={handleBrowse}>
            <Row className="mb-3 g-2">
              <Col md={3}>
                <Form.Group controlId="targetBranch">
                  <Form.Label>Target Branch</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder="main"
                    value={targetBranch}
                    onChange={(e) => setTargetBranch(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={2}>
                <Form.Group controlId="after">
                  <Form.Label>Od</Form.Label>
                  <Form.Control
                    type="date"
                    value={after}
                    onChange={(e) => setAfter(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={2}>
                <Form.Group controlId="before">
                  <Form.Label>Do</Form.Label>
                  <Form.Control
                    type="date"
                    value={before}
                    onChange={(e) => setBefore(e.target.value)}
                  />
                </Form.Group>
              </Col>
              <Col md={2}>
                <Form.Group controlId="limit">
                  <Form.Label>Limit</Form.Label>
                  <Form.Control
                    type="number"
                    min={1}
                    max={500}
                    value={limit}
                    onChange={(e) => setLimit(Number(e.target.value))}
                  />
                </Form.Group>
              </Col>
              <Col md={3} className="d-flex align-items-end gap-2 pb-1">
                <Form.Check
                  type="checkbox"
                  id="useLlm"
                  label="LLM"
                  checked={useLlm}
                  onChange={(e) => setUseLlm(e.target.checked)}
                />
                <Button type="submit" variant="primary" disabled={loadingBrowse}>
                  {loadingBrowse ? (
                    <>
                      <Spinner animation="border" size="sm" className="me-1" />
                      Ladowanie...
                    </>
                  ) : (
                    'Pobierz MR'
                  )}
                </Button>
              </Col>
            </Row>
          </Form>
        </>
      )}

      {error && (
        <Alert variant="danger" className="mt-3" dismissible onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Step 2: Browse results */}
      {step === 'browse' && browsedMrs.length > 0 && (
        <div className="mt-3">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="mb-0">
              Znaleziono {browsedMrs.length} MR
            </h5>
            <Button
              variant="success"
              onClick={handleAnalyze}
              disabled={loadingAnalyze}
            >
              {loadingAnalyze ? (
                <>
                  <Spinner animation="border" size="sm" className="me-1" />
                  Analizowanie...
                </>
              ) : (
                'Analizuj'
              )}
            </Button>
          </div>
          <MrBrowseTable items={browsedMrs} />
        </div>
      )}

      {step === 'browse' && browsedMrs.length === 0 && !loadingBrowse && (
        <Alert variant="info" className="mt-3">
          Nie znaleziono MR dla podanych kryteriow.
        </Alert>
      )}

      {/* Step 3: Analysis results (replaces browse table) */}
      {step === 'analyzed' && analysisResponse && summaryProps && (
        <div className="mt-4">
          <SummaryCard
            totalMrs={summaryProps.totalMrs}
            automatable={summaryProps.automatable}
            maybe={summaryProps.maybe}
            notSuitable={summaryProps.notSuitable}
          />

          {analysisResponse.results.length === 0 ? (
            <Alert variant="info">Brak wynikow analizy.</Alert>
          ) : (
            <MrTable
              results={analysisResponse.results}
              reportId={analysisResponse.reportId}
            />
          )}
        </div>
      )}

      {/* Analysis History */}
      <hr className="step-separator mt-4" />
      <AnalysisHistory analyses={analysisHistory} onDelete={handleHistoryDelete} />
    </div>
  );
}
