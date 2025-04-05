import type { Config } from '../types/proxy';

const API_URL = import.meta.env.VITE_API_URL;

export interface HealthStatus {
  status: 'UP' | 'DOWN';
  redisStatus: 'Healthy' | 'Unhealthy';
  servers: Record<string, 'Healthy' | 'Unhealthy' | 'Not Found'>;
}

export async function fetchHealthStatus(): Promise<HealthStatus> {
  const response = await fetch(`${getApiUrl()}/healthcheck`);
  if (!response.ok) {
    throw new Error('Failed to fetch health status');
  }
  return response.json();
}
  
export async function fetchConfig(sessionToken: string): Promise<Config> {
  const headers = new Headers({
    'Authorization': `Basic ${sessionToken}`,
  });

  try {
    console.log(`${API_URL}/admin/config`);
    const response: any = await fetch(`${API_URL}/admin/config`, {
      method: 'GET',
      headers: headers
    });
  
    if (!response.ok) {
      throw new Error('Failed to fetch configuration');
    }
    const {data} = await response.json();
    return data;
  } catch(e) {
    console.error('Failed to fetch configuration', e);
    throw new Error('Failed to fetch configuration');
    
  }
}

export async function updateConfig(config: Config): Promise<Config> {
  const response = await fetch(`${API_URL}/config`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(config),
  });
  if (!response.ok) {
    throw new Error('Failed to update configuration');
  }
  return response.json();
}

export function getApiUrl(): string {
  return localStorage.getItem(API_URL) || import.meta.env.VITE_API_URL;
}

export function setApiUrl(url: string): void {
  localStorage.setItem(API_URL, url);
}
