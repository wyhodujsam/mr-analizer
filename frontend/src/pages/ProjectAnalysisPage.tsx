import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Badge, Button, Form, ListGroup, Row, Col, Spinner, ProgressBar } from 'react-bootstrap';
import RepoSelector from '../components/RepoSelector';
import AiPotentialCard from '../components/project/AiPotentialCard';
import BddSddCards from '../components/project/BddSddCards';
import ProjectPrTable from '../components/project/ProjectPrTable';
import type { SavedRepository } from '../types';
import type { ProjectAnalysisResult } from '../types/project';
import { getRepos, addRepo, deleteRepo } from '../api/analysisApi';
import { analyzeProjectWithProgress, getSavedAnalyses, deleteProjectAnalysis } from '../api/projectApi';

export default function ProjectAnalysisPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>();
  const [savedRepos, setSavedRepos] = useState<SavedRepository[]>([]);
  const [slug, setSlug] = useState(owner && repo ? `${owner}/${repo}` : '');
  const [provider, setProvider] = useState('github');
  const [useLlm, setUseLlm] = useState(false);
  const [result, setResult] = useState<ProjectAnalysisResult | null>(null);
  const [savedAnalyses, setSavedAnalyses] = useState<ProjectAnalysisResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState<{ processed: number; total: number } | null>(null);
  const [error, setError] = useState('');

  const loadRepos = useCallback(async () => {
    try { setSavedRepos(await getRepos()); } catch { /* ignore */ }
  }, []);

  useEffect(() => { loadRepos(); }, [loadRepos]);

  const loadSavedAnalyses = useCallback(async (projectSlug: string) => {
    try {
      const [o, r] = projectSlug.split('/');
      const analyses = await getSavedAnalyses(o, r);
      setSavedAnalyses(analyses);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    if (slug) loadSavedAnalyses(slug);
    else setSavedAnalyses([]);
  }, [slug, loadSavedAnalyses]);

  async function handleSelectRepo(selectedSlug: string, selectedProvider: string) {
    setSlug(selectedSlug);
    setProvider(selectedProvider);
    setResult(null);
    setError('');
    setProgress(null);

    const exists = savedRepos.some(r => r.projectSlug === selectedSlug);
    if (!exists && selectedSlug.includes('/')) {
      try { await addRepo(selectedSlug, selectedProvider); await loadRepos(); } catch { /* ignore */ }
    }
  }

  async function handleDeleteRepo(id: number) {
    try {
      await deleteRepo(id);
      await loadRepos();
      const deleted = savedRepos.find(r => r.id === id);
      if (deleted && deleted.projectSlug === slug) { setSlug(''); setResult(null); setSavedAnalyses([]); }
    } catch { /* ignore */ }
  }

  async function handleAnalyze() {
    if (!slug) return;
    setLoading(true);
    setError('');
    setResult(null);
    setProgress(null);
    try {
      const [o, r] = slug.split('/');
      const data = await analyzeProjectWithProgress(o, r, useLlm,
        (processed, total) => setProgress({ processed, total }));
      setResult(data);
      await loadSavedAnalyses(slug);
    } catch (e: any) {
      setError(e.message ?? 'Nie udało się przeanalizować projektu');
    } finally {
      setLoading(false);
      setProgress(null);
    }
  }

  async function handleLoadSaved(analysis: ProjectAnalysisResult) {
    setResult(analysis);
    setError('');
  }

  async function handleDeleteAnalysis(id: number) {
    try {
      await deleteProjectAnalysis(id);
      setSavedAnalyses(prev => prev.filter(a => a.id !== id));
      if (result?.id === id) setResult(null);
    } catch { /* ignore */ }
  }

  const progressPct = progress ? Math.round((progress.processed / Math.max(progress.total, 1)) * 100) : 0;

  return (
    <>
      <h2 className="mb-4">Analiza projektu</h2>

      <RepoSelector
        savedRepos={savedRepos}
        onSelect={handleSelectRepo}
        onDelete={handleDeleteRepo}
        selectedSlug={slug}
        selectedProvider={provider}
      />

      {slug && (
        <div className="d-flex align-items-center gap-3 mt-3 mb-3">
          <Button variant="primary" onClick={handleAnalyze} disabled={loading}>
            {loading ? <><Spinner size="sm" animation="border" className="me-1" /> Analizuję...</>
              : result ? 'Odśwież analizę' : 'Analizuj projekt'}
          </Button>
          <Form.Check
            type="checkbox"
            label="Użyj LLM (kosztowne)"
            checked={useLlm}
            onChange={e => setUseLlm(e.target.checked)}
          />
        </div>
      )}

      {error && <Alert variant="danger">{error}</Alert>}

      {loading && progress && (
        <div className="mb-4">
          <div className="d-flex justify-content-between mb-1">
            <span>Analizuję PR-y repozytorium {slug}...</span>
            <span>{progress.processed} / {progress.total}</span>
          </div>
          <ProgressBar now={progressPct} label={`${progressPct}%`} animated striped />
        </div>
      )}

      {loading && !progress && (
        <div className="text-center py-4">
          <Spinner animation="border" size="sm" className="me-2" />
          Ładowanie danych repozytorium...
        </div>
      )}

      {!loading && savedAnalyses.length > 0 && !result && (
        <div className="mb-4">
          <h5>Zapisane analizy</h5>
          <ListGroup>
            {savedAnalyses.map(a => (
              <ListGroup.Item key={a.id} className="d-flex justify-content-between align-items-center">
                <div role="button" onClick={() => handleLoadSaved(a)} className="flex-grow-1">
                  <strong>{a.projectSlug}</strong>
                  <span className="text-muted ms-2">{new Date(a.analyzedAt).toLocaleString('pl-PL')}</span>
                  <Badge bg="secondary" className="ms-2">{a.summary.totalPrs} PR-ów</Badge>
                  <Badge bg="success" className="ms-2">{a.summary.automatablePercent}% AI</Badge>
                  <Badge bg="info" className="ms-2">{a.summary.bddPercent}% BDD</Badge>
                  <Badge bg="info" className="ms-2">{a.summary.sddPercent}% SDD</Badge>
                </div>
                <Button variant="outline-danger" size="sm"
                  onClick={(e) => { e.stopPropagation(); if (a.id) handleDeleteAnalysis(a.id); }}>
                  Usuń
                </Button>
              </ListGroup.Item>
            ))}
          </ListGroup>
        </div>
      )}

      {!loading && result && (
        <>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="text-muted mb-0">
              {result.projectSlug} — {result.summary.totalPrs} PR-ów
              <span className="text-muted small ms-2">
                ({new Date(result.analyzedAt).toLocaleString('pl-PL')})
              </span>
            </h5>
            {savedAnalyses.length > 1 && (
              <Button variant="outline-secondary" size="sm" onClick={() => setResult(null)}>
                Pokaż zapisane analizy
              </Button>
            )}
          </div>

          <Row className="g-3 mb-4">
            <Col lg={6}>
              <AiPotentialCard summary={result.summary} rows={result.rows} />
            </Col>
            <Col lg={6}>
              <BddSddCards summary={result.summary} />
            </Col>
          </Row>

          <h4 className="mb-3">Szczegóły PR-ów</h4>
          <ProjectPrTable rows={result.rows} />
        </>
      )}
    </>
  );
}
