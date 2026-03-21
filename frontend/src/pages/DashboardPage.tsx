import { useState, useEffect, useCallback } from 'react';
import { Alert, Button, Form, Row, Col, Spinner } from 'react-bootstrap';
import RepoSelector from '../components/RepoSelector';
import MrBrowseTable from '../components/MrBrowseTable';
import SummaryCard from '../components/SummaryCard';
import VerdictPieChart from '../components/VerdictPieChart';
import MrTable from '../components/MrTable';
import AnalysisHistory from '../components/AnalysisHistory';
import {
  getRepos,
  addRepo,
  deleteRepo,
  browseMrs,
  runAnalysis,
  getAnalyses,
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
  const [savedRepos, setSavedRepos] = useState<SavedRepository[]>([]);
  const [selectedSlug, setSelectedSlug] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('github');

  const [targetBranch, setTargetBranch] = useState('');
  const [after, setAfter] = useState('');
  const [before, setBefore] = useState('');
  const [limit, setLimit] = useState(100);
  const [useLlm, setUseLlm] = useState(false);

  const [selectedMrIds, setSelectedMrIds] = useState<Set<string>>(new Set());

  const [step, setStep] = useState<Step>('select');
  const [browsedMrs, setBrowsedMrs] = useState<MrBrowseItem[]>([]);
  const [analysisResponse, setAnalysisResponse] = useState<AnalysisResponse | null>(null);

  const [analysisHistory, setAnalysisHistory] = useState<AnalysisResponse[]>([]);

  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingAnalyze, setLoadingAnalyze] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
  }

  async function handleRepoDelete(id: number) {
    try {
      await deleteRepo(id);
      await loadRepos();
      const deleted = savedRepos.find((r) => r.id === id);
      if (deleted && deleted.projectSlug === selectedSlug) {
        setSelectedSlug('');
        setSelectedProvider('github');
        setStep('select');
        setBrowsedMrs([]);
        setAnalysisResponse(null);
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

  async function handleBrowse(e: React.FormEvent, forceRefresh = false) {
    e.preventDefault();
    if (!selectedSlug) return;
    setLoadingBrowse(true);
    setError(null);
    setAnalysisResponse(null);
    try {
      const items = await browseMrs(buildRequest(), forceRefresh);
      setBrowsedMrs(items);
      setSelectedMrIds(new Set(items.map(i => i.externalId)));
      setStep('browse');
    } catch (err: unknown) {
      setError(extractError(err));
    } finally {
      setLoadingBrowse(false);
    }
  }

  async function handleRefresh(e: React.MouseEvent) {
    e.preventDefault();
    await handleBrowse(e as unknown as React.FormEvent, true);
  }

  async function handleAnalyze() {
    setLoadingAnalyze(true);
    setError(null);
    try {
      const req = buildRequest();
      req.selectedMrIds = [...selectedMrIds];
      const result = await runAnalysis(req);
      setAnalysisResponse(result);
      setStep('analyzed');
      await loadHistory();
    } catch (err: unknown) {
      setError(extractError(err));
    } finally {
      setLoadingAnalyze(false);
    }
  }

  async function handleHistoryDelete(reportId: number) {
    try {
      await deleteAnalysis(reportId);
      await loadHistory();
      if (analysisResponse?.reportId === reportId) {
        setAnalysisResponse(null);
        setStep(browsedMrs.length > 0 ? 'browse' : 'select');
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
      <h2 className="mb-4">Analiza MR</h2>

      <RepoSelector
        savedRepos={savedRepos}
        onSelect={handleRepoSelect}
        onDelete={handleRepoDelete}
        selectedSlug={selectedSlug}
        selectedProvider={selectedProvider}
      />

      {selectedSlug && (
        <>
          <hr className="step-separator" />
          <Form onSubmit={handleBrowse}>
            <Row className="mb-3 g-2">
              <Col md={3}>
                <Form.Group controlId="targetBranch">
                  <Form.Label>Branch docelowy</Form.Label>
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
                {step !== 'select' && (
                  <Button
                    variant="outline-secondary"
                    onClick={handleRefresh}
                    disabled={loadingBrowse}
                    title="Odswiez z GitHub API"
                  >
                    Odswiez
                  </Button>
                )}
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

      {step === 'browse' && browsedMrs.length > 0 && (
        <div className="mt-3">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="mb-0">
              Znaleziono {browsedMrs.length} MR
            </h5>
            <Button
              variant="success"
              onClick={handleAnalyze}
              disabled={loadingAnalyze || selectedMrIds.size === 0}
            >
              {loadingAnalyze ? (
                <>
                  <Spinner animation="border" size="sm" className="me-1" />
                  Analizowanie...
                </>
              ) : (
                `Analizuj (${selectedMrIds.size} z ${browsedMrs.length})`
              )}
            </Button>
          </div>
          <MrBrowseTable
            items={browsedMrs}
            selectedIds={selectedMrIds}
            onSelectionChange={setSelectedMrIds}
          />
        </div>
      )}

      {step === 'browse' && browsedMrs.length === 0 && !loadingBrowse && (
        <Alert variant="info" className="mt-3">
          Nie znaleziono MR dla podanych kryteriow.
        </Alert>
      )}

      {step === 'analyzed' && analysisResponse && summaryProps && (
        <div className="mt-4">
          <Row>
            <Col md={8}>
              <SummaryCard
                totalMrs={summaryProps.totalMrs}
                automatable={summaryProps.automatable}
                maybe={summaryProps.maybe}
                notSuitable={summaryProps.notSuitable}
              />
            </Col>
            <Col md={4} className="d-flex align-items-center">
              <VerdictPieChart
                automatable={summaryProps.automatable.count}
                maybe={summaryProps.maybe.count}
                notSuitable={summaryProps.notSuitable.count}
              />
            </Col>
          </Row>

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

      <hr className="step-separator mt-4" />
      <AnalysisHistory analyses={analysisHistory} onDelete={handleHistoryDelete} />
    </div>
  );
}
