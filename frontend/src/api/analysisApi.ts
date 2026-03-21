import axios from 'axios';
import type {
  AnalysisRequest,
  AnalysisResponse,
  MrDetailResponse,
  AnalysisSummary,
  SavedRepository,
  MrBrowseItem,
} from '../types';

const api = axios.create({
  baseURL: '',
});

export async function runAnalysis(req: AnalysisRequest): Promise<AnalysisResponse> {
  const { data } = await api.post<AnalysisResponse>('/api/analysis', req);
  return data;
}

export async function getAnalyses(): Promise<AnalysisResponse[]> {
  const { data } = await api.get<AnalysisResponse[]>('/api/analysis');
  return data;
}

export async function getAnalysis(reportId: number): Promise<AnalysisResponse> {
  const { data } = await api.get<AnalysisResponse>(`/api/analysis/${reportId}`);
  return data;
}

export async function getMrDetail(
  reportId: number,
  resultId: number
): Promise<MrDetailResponse> {
  const { data } = await api.get<MrDetailResponse>(
    `/api/analysis/${reportId}/mrs/${resultId}`
  );
  return data;
}

export async function getSummary(reportId: number): Promise<AnalysisSummary> {
  const { data } = await api.get<AnalysisSummary>(`/api/summary/${reportId}`);
  return data;
}

export async function getRepos(): Promise<SavedRepository[]> {
  const { data } = await api.get<SavedRepository[]>('/api/repos');
  return data;
}

export async function addRepo(projectSlug: string, provider: string): Promise<SavedRepository> {
  const { data } = await api.post<SavedRepository>('/api/repos', { projectSlug, provider });
  return data;
}

export async function deleteRepo(id: number): Promise<void> {
  await api.delete(`/api/repos/${id}`);
}

export async function browseMrs(request: AnalysisRequest, forceRefresh = false): Promise<MrBrowseItem[]> {
  const { data } = await api.post<MrBrowseItem[]>(`/api/browse?forceRefresh=${forceRefresh}`, request);
  return data;
}

export async function deleteAnalysis(reportId: number): Promise<void> {
  await api.delete(`/api/analysis/${reportId}`);
}

