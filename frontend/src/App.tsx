import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useApp } from './contexts/AppContext';
import { Sidebar } from './components/Sidebar';
import { LoginPage } from './components/LoginPage';
import { createSession } from './utils/auth';
import { DashboardPage } from './pages/DashboardPage';
import { ProxiesPage } from './pages/ProxiesPage';
import { ServicesPage } from './pages/ServicesPage';
import { UsersPage } from './pages/UsersPage';
import { SettingsPage } from './pages/SettingsPage';
import { YamlEditorPage } from './pages/YamlEditorPage';
import { DataFreshnessIndicator } from './components/DataFreshnessIndicator';

function App() {
  const { config, setConfig, session, setSession } = useApp();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [canEdit, setCanEdit] = useState(true);

  const handleLogin = async (username: string, password: string) => {
    // In a real app, validate credentials with the server
    const newSession = createSession();
    setSession(newSession);
  };

  const handleConfigUpdate = async (newConfig: Config) => {
    if (!canEdit) {
      setError('Cannot update configuration while data is stale. Please refresh first.');
      return;
    }

    try {
      setIsLoading(true);
      const updatedConfig = await updateConfig(newConfig);
      setConfig(updatedConfig);
      setLastUpdated(new Date());
      setError(null);
    } catch (err) {
      setError('Failed to update configuration');
    } finally {
      setIsLoading(false);
    }
  };
  console.log('session::', session, config)
  if (!session) {
    return <LoginPage onLogin={handleLogin} />;
  }

  if (!config) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-copper mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading configuration...</p>
        </div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <div className="flex h-screen overflow-hidden bg-gray-50">
        <Sidebar 
          isCollapsed={isSidebarCollapsed} 
          onToggle={() => setIsSidebarCollapsed(!isSidebarCollapsed)} 
        />
        <div className={`flex-1 flex flex-col overflow-hidden transition-all duration-300 ${
          isSidebarCollapsed ? 'ml-20' : 'ml-64'
        }`}>
          <main className="flex-1 overflow-y-auto">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
              <div className="mb-6 bg-white shadow rounded-lg p-4">
                <DataFreshnessIndicator
                  lastUpdated={lastUpdated}
                  isLoading={isLoading}
                  error={error}
                  onRefresh={async () => {
                    try {
                      setIsLoading(true);
                      const newConfig = await fetchConfig();
                      setConfig(newConfig);
                      setLastUpdated(new Date());
                      setError(null);
                      setCanEdit(true);
                    } catch (err) {
                      setError('Failed to refresh configuration');
                      setCanEdit(false);
                    } finally {
                      setIsLoading(false);
                    }
                  }}
                />
              </div>
              <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardPage config={config} />} />
                <Route 
                  path="/proxies" 
                  element={
                    <ProxiesPage 
                      config={config} 
                      onConfigUpdate={handleConfigUpdate}
                      canEdit={canEdit}
                    />
                  } 
                />
                <Route 
                  path="/services" 
                  element={
                    <ServicesPage 
                      config={config} 
                      onConfigUpdate={handleConfigUpdate}
                    />
                  } 
                />
                <Route 
                  path="/users" 
                  element={
                    <UsersPage 
                      config={config} 
                      onConfigUpdate={handleConfigUpdate}
                    />
                  } 
                />
                <Route path="/settings" element={<SettingsPage config={config} />} />
                <Route 
                  path="/yaml" 
                  element={
                    <YamlEditorPage 
                      config={config} 
                      onConfigUpdate={handleConfigUpdate}
                      canEdit={canEdit}
                    />
                  } 
                />
              </Routes>
            </div>
          </main>
        </div>
      </div>
    </BrowserRouter>
  );
}

export default App;