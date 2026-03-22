import axios from 'axios';
import type { ContributorInfo, ActivityReport } from '../types/activity';

const api = axios.create({ baseURL: '' });

export async function getContributors(owner: string, repo: string): Promise<ContributorInfo[]> {
  const { data } = await api.get<ContributorInfo[]>(`/api/activity/${owner}/${repo}/contributors`);
  return data;
}

export async function getActivityReport(owner: string, repo: string, author: string): Promise<ActivityReport> {
  const { data } = await api.get<ActivityReport>(`/api/activity/${owner}/${repo}/report`, {
    params: { author },
  });
  return data;
}
