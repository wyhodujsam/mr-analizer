import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Button, Nav, Spinner, Tab } from 'react-bootstrap';
import RepoSelector from '../components/RepoSelector';
import ContributorSelector from '../components/activity/ContributorSelector';
import StatsCards from '../components/activity/StatsCards';
import ProductivityMetricsCards from '../components/activity/ProductivityMetricsCards';
import FlagsList from '../components/activity/FlagsList';
import ActivityHeatmap from '../components/activity/ActivityHeatmap';
import DayDrillDown from '../components/activity/DayDrillDown';
import ActivityBarChart from '../components/activity/ActivityBarChart';
import type { SavedRepository } from '../types';
import type { ContributorInfo, ActivityReport, DailyActivity, Severity } from '../types/activity';
import { getRepos, addRepo, deleteRepo } from '../api/analysisApi';
import { getContributors, getActivityReport, refreshActivityCache } from '../api/activityApi';

export default function ActivityDashboardPage() {
  const { owner, repo } = useParams<{ owner: string; repo: string }>();
  const [savedRepos, setSavedRepos] = useState<SavedRepository[]>([]);
  const [slug, setSlug] = useState(owner && repo ? `${owner}/${repo}` : '');
  const [provider, setProvider] = useState('github');
  const [contributors, setContributors] = useState<ContributorInfo[]>([]);
  const [selectedAuthor, setSelectedAuthor] = useState('');
  const [report, setReport] = useState<ActivityReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingContributors, setLoadingContributors] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [selectedDay, setSelectedDay] = useState<{ date: string; activity: DailyActivity } | null>(null);
  const [severityFilter, setSeverityFilter] = useState<Severity | null>(null);
  const [typeFilter, setTypeFilter] = useState('all');
  const [activeTab, setActiveTab] = useState('wydajnosc');

  const loadRepos = useCallback(async () => {
    try {
      const repos = await getRepos();
      setSavedRepos(repos);
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    loadRepos();
  }, [loadRepos]);

  const loadContributors = useCallback(async (projectSlug: string) => {
    setLoadingContributors(true);
    setError('');
    try {
      const [ownerPart, repoPart] = projectSlug.split('/');
      const data = await getContributors(ownerPart, repoPart);
      setContributors(data);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Nie udało się pobrać kontrybutorów');
    } finally {
      setLoadingContributors(false);
    }
  }, []);

  const loadReport = useCallback(async (projectSlug: string, author: string) => {
    setLoading(true);
    setError('');
    try {
      const [ownerPart, repoPart] = projectSlug.split('/');
      const data = await getActivityReport(ownerPart, repoPart, author);
      setReport(data);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Nie udało się pobrać raportu aktywności');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (slug) {
      loadContributors(slug);
    }
  }, [slug, loadContributors]);

  useEffect(() => {
    if (slug && selectedAuthor) {
      loadReport(slug, selectedAuthor);
    } else {
      setReport(null);
      setSelectedDay(null);
    }
  }, [slug, selectedAuthor, loadReport]);

  async function handleSelectRepo(selectedSlug: string, selectedProvider: string) {
    setSlug(selectedSlug);
    setProvider(selectedProvider);
    setSelectedAuthor('');
    setReport(null);
    setSelectedDay(null);

    const exists = savedRepos.some(r => r.projectSlug === selectedSlug);
    if (!exists && selectedSlug.includes('/')) {
      try {
        await addRepo(selectedSlug, selectedProvider);
        await loadRepos();
      } catch {
        // ignore
      }
    }
  }

  async function handleDeleteRepo(id: number) {
    try {
      await deleteRepo(id);
      await loadRepos();
      const deleted = savedRepos.find(r => r.id === id);
      if (deleted && deleted.projectSlug === slug) {
        setSlug('');
        setContributors([]);
        setReport(null);
      }
    } catch {
      // ignore
    }
  }

  async function handleRefresh() {
    if (!slug) return;
    setRefreshing(true);
    try {
      const [ownerPart, repoPart] = slug.split('/');
      await refreshActivityCache(ownerPart, repoPart);
      if (selectedAuthor) {
        await loadReport(slug, selectedAuthor);
      }
      await loadContributors(slug);
    } catch (e: any) {
      setError(e.response?.data?.message ?? 'Nie udało się odświeżyć danych');
    } finally {
      setRefreshing(false);
    }
  }

  const flagCount = report?.flags.length ?? 0;

  return (
    <>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2 className="mb-0">Aktywność kontrybutora</h2>
        {slug && (
          <Button
            variant="outline-secondary"
            size="sm"
            onClick={handleRefresh}
            disabled={refreshing}
          >
            {refreshing ? (
              <><Spinner size="sm" animation="border" className="me-1" /> Odświeżanie...</>
            ) : (
              <>&#8635; Odśwież dane</>
            )}
          </Button>
        )}
      </div>

      <RepoSelector
        savedRepos={savedRepos}
        onSelect={handleSelectRepo}
        onDelete={handleDeleteRepo}
        selectedSlug={slug}
        selectedProvider={provider}
      />

      {slug && <h5 className="text-muted mt-3 mb-3">{slug}</h5>}

      {error && <Alert variant="danger" className="mt-3">{error}</Alert>}

      {loadingContributors && (
        <div className="text-center py-4">
          <Spinner animation="border" /> Ładowanie kontrybutorów...
        </div>
      )}

      {!loadingContributors && contributors.length > 0 && (
        <div className="mt-3">
          <ContributorSelector
            contributors={contributors}
            selected={selectedAuthor}
            onChange={setSelectedAuthor}
            loading={loading}
          />
        </div>
      )}

      {loading && (
        <div className="text-center py-4">
          <Spinner animation="border" /> Analizuję aktywność...
        </div>
      )}

      {!loading && report && report.stats.totalPrs === 0 && (
        <Alert variant="info">Nie znaleziono aktywności dla użytkownika {selectedAuthor}</Alert>
      )}

      {!loading && report && report.stats.totalPrs > 0 && (
        <>
          <StatsCards
            stats={report.stats}
            activeSeverity={severityFilter}
            onSeverityClick={setSeverityFilter}
          />

          <Tab.Container activeKey={activeTab} onSelect={(k) => setActiveTab(k ?? 'wydajnosc')}>
            <Nav variant="tabs" className="mt-4">
              <Nav.Item>
                <Nav.Link eventKey="wydajnosc">Wydajność</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="aktywnosc">Aktywność</Nav.Link>
              </Nav.Item>
              <Nav.Item>
                <Nav.Link eventKey="naruszenia">
                  Naruszenia
                  {flagCount > 0 && (
                    <span className="badge bg-danger ms-2">{flagCount}</span>
                  )}
                </Nav.Link>
              </Nav.Item>
            </Nav>

            <Tab.Content className="pt-3">
              <Tab.Pane eventKey="wydajnosc">
                <ProductivityMetricsCards productivity={report.productivity} />
              </Tab.Pane>

              <Tab.Pane eventKey="aktywnosc">
                <h5 className="mb-3">Heatmapa aktywności</h5>
                <ActivityHeatmap
                  dailyActivity={report.dailyActivity}
                  onDayClick={(date, activity) => {
                    if (date && activity) {
                      setSelectedDay({ date, activity });
                    } else {
                      setSelectedDay(null);
                    }
                  }}
                />
                {selectedDay && (
                  <DayDrillDown date={selectedDay.date} activity={selectedDay.activity} />
                )}

                <h5 className="mt-4 mb-3">Aktywność w czasie</h5>
                <ActivityBarChart dailyActivity={report.dailyActivity} />
              </Tab.Pane>

              <Tab.Pane eventKey="naruszenia">
                <FlagsList
                  flags={report.flags}
                  severityFilter={severityFilter}
                  typeFilter={typeFilter}
                  onTypeFilterChange={setTypeFilter}
                />
              </Tab.Pane>
            </Tab.Content>
          </Tab.Container>
        </>
      )}
    </>
  );
}
