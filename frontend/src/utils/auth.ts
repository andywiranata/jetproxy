// TODO: Remove hardcoded credentials in production
const DUMMY_CREDENTIALS = {
  username: 'admin',
  password: 'admin123'
};

export const SESSION_KEY = 'proxy_manager_session';
export const SESSION_TTL = 30 * 60 * 1000; // 30 minutes in milliseconds

export interface Session {
  token: string;
  expiresAt: number;
}

export function encodeCredentials(username: string, password: string): string {
  return btoa(`${username}:${password}`);
}

export function validateCredentials(username: string, password: string): boolean {
  return username === DUMMY_CREDENTIALS.username && password === DUMMY_CREDENTIALS.password;
}

export function createSession(username: string, password: string): Session {
  const token = encodeCredentials(username, password);
  const expiresAt = Date.now() + SESSION_TTL;
  
  const session: Session = { token, expiresAt };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  
  return session;
}

export function getSession(): Session | null {
  const sessionData = sessionStorage.getItem(SESSION_KEY);
  if (!sessionData) return null;
  
  const session: Session = JSON.parse(sessionData);
  if (Date.now() > session.expiresAt) {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
  
  return session;
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY);
}