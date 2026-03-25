import axios from 'axios';
import type { ProjectAnalysisResult } from '../types/project';

const api = axios.create({ baseURL: '' });

export async function getSavedAnalyses(owner: string, repo: string): Promise<ProjectAnalysisResult[]> {
  const { data } = await api.get<ProjectAnalysisResult[]>(`/api/project/${owner}/${repo}/analyses`);
  return data;
}

export async function deleteProjectAnalysis(id: number): Promise<void> {
  await api.delete(`/api/project/analyses/${id}`);
}

export async function analyzeProject(
  owner: string, repo: string, useLlm: boolean = false
): Promise<ProjectAnalysisResult> {
  const { data } = await api.post<ProjectAnalysisResult>(
    `/api/project/${owner}/${repo}/analyze`,
    null,
    { params: { useLlm }, timeout: 300000 }
  );
  return data;
}

export async function analyzeProjectWithProgress(
  owner: string,
  repo: string,
  useLlm: boolean,
  onProgress: (processed: number, total: number) => void,
): Promise<ProjectAnalysisResult> {
  const url = `/api/project/${owner}/${repo}/analyze-stream?useLlm=${useLlm}`;

  const response = await fetch(url, { method: 'POST' });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);

  const reader = response.body?.getReader();
  if (!reader) throw new Error('No response body');

  const decoder = new TextDecoder();
  let buffer = '';
  let result: ProjectAnalysisResult | null = null;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });

    // Process complete SSE blocks (separated by \n\n)
    let idx;
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.substring(0, idx);
      buffer = buffer.substring(idx + 2);

      let eventName = '';
      let eventData = '';
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) eventName = line.substring(6).trim();
        else if (line.startsWith('data:')) eventData = line.substring(5).trim();
      }

      if (eventName === 'progress' && eventData) {
        try {
          const p = JSON.parse(eventData);
          onProgress(p.processed, p.total);
        } catch { /* ignore */ }
      } else if (eventName === 'result' && eventData) {
        try {
          result = JSON.parse(eventData);
        } catch { /* ignore */ }
      }
    }
  }

  if (!result) throw new Error('No result received from server');
  return result;
}
