import React, { createContext, useContext, useState, useEffect } from 'react';
import type { Config } from '../types/proxy';
import { fetchConfig } from '../services/api';
import { getSession, Session } from '../utils/auth';

interface AppContextType {
  config: Config | null;
  setConfig: (config: Config) => void;
  session: Session | null;
  setSession: (session: Session | null) => void;
  isLoading: boolean;
  error: string | null;
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

  useEffect(() => {
    const fetchAndSetConfig = async () => {
      if (session) {
        const initialConfig = await fetchConfig(session.token);
        console.log('initial config', initialConfig);
        setConfig(initialConfig);
      }
    };

    fetchAndSetConfig();
  }, [session]);

  const value = {
    config,
    setConfig,
    session,
    setSession,
    isLoading,
    error
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

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}