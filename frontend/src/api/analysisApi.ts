import axios from 'axios';
import type {
  AnalysisRequest,
  AnalysisResponse,
  MrDetailResponse,
  AnalysisSummary,
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
