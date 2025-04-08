import React, { createContext, useContext, useState, useEffect } from 'react';
import type { Config } from '../types/proxy';
import { fetchConfig } from '../services/api';
import { getSession, Session } from '../utils/auth';
import { clearSession } from '../utils/auth';

interface AppContextType {
  config: Config | null;
  setConfig: (config: Config) => void;
  session: Session | null;
  setSession: (session: Session | null) => void;
  isLoading: boolean;
  error: string | null;
  fetchAndSetConfig: () => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [config, setConfig] = useState<Config | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const initializeApp = async () => {
      try {
        // Check for existing session
        const existingSession = getSession();
        if (existingSession) {
          setSession(existingSession);
        }
      } catch (err) {
        setError('Failed to initialize application');
      } finally {
        setIsLoading(false);
      }
    };
    initializeApp();
  }, []);
  const fetchAndSetConfig = async () => {
    if (session) {
      try {
        let initialConfig = await fetchConfig(session.token);
        setConfig(initialConfig);
      } catch (err) {
        setError(`${err}`);
      } finally {
        setIsLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchAndSetConfig();
  }, [session]);

  const value = {
    config,
    setConfig,
    session,
    setSession,
    isLoading,
    error,
    fetchAndSetConfig
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-copper mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading application...</p>
        </div>
      </div>
    );
  } 

  if (error && !config) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full mx-auto p-6">
          <div className="bg-white rounded-lg shadow-xl p-6">
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-red-100 mb-4">
                <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">Failed to Load Application</h2>
              <p className="text-gray-600 mb-6">{error}</p>
              <div className="space-y-3">
                <button
                  onClick={()=> {
                    setIsLoading(true);
                    setTimeout(()=> {
                      fetchAndSetConfig();
                    }, 1000);
                  }}
                  className="w-full flex justify-center items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-copper hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-copper"
                >
                  <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                  </svg>
                  Try Again
                </button>
                <button
                  onClick={()=> {
                      clearSession();
                      location.href = '/';
                  }}
                  className="w-full flex justify-center items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-copper"
                >
                  <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                  </svg>
                  Sign In Again
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
  console.log('error', error)

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}