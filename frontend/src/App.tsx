import { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useApp } from './contexts/AppContext';
import { useToast } from './contexts/ToastContext';
import { Sidebar } from './components/Sidebar';
import { LoginPage } from './components/LoginPage';
import { createSession } from './utils/auth';
import { DashboardPage } from './pages/DashboardPage';
import { ProxiesPage } from './pages/ProxiesPage';
import { ServicesPage } from './pages/ServicesPage';
import { UsersPage } from './pages/UsersPage';
import { SettingsPage } from './pages/SettingsPage';
import { YamlEditorPage } from './pages/YamlEditorPage';
import { upsertProxy, upsertService} from './services/api';
import { Proxy, Service } from './types/proxy';

function App() {
  const toast = useToast();
  const { config, session, setSession, fetchAndSetConfig } = useApp();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  const handleLogin = async (username: string, password: string) => {
    // In a real app, validate credentials with the server
    const newSession = createSession(username, password);
    setSession(newSession);
  };

  const handleUpdateProxy = async (proxy: Proxy)=> {
    try {
      await upsertProxy(proxy, session!.token);
      toast.showSuccess(`Success, Add or Update Proxy ${proxy.path}`);
      fetchAndSetConfig();
    } catch(e) {
      toast.showError(`Error, Add or Update Proxy ${proxy.path}`);
    }

  }
  const handleUpdateService = async (serivce: Service)=> {
    try {
      await upsertService(serivce, session!.token);
      toast.showSuccess(`Success, Add or Update Service ${serivce.name}`);
      fetchAndSetConfig();
    } catch(e) {
      toast.showError(`Error, Add or Update Service ${serivce.name}`);
    }
  }
  
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
              <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<DashboardPage config={config} />} />
                <Route 
                  path="/proxies" 
                  element={
                    <ProxiesPage 
                      config={config}
                      onConfigUpdate={handleUpdateProxy} />
                  } 
                />
                <Route 
                  path="/services" 
                  element={
                    <ServicesPage 
                      config={config} 
                      onConfigUpdate={handleUpdateService}
                    />
                  } 
                />
                <Route 
                  path="/users" 
                  element={
                    <UsersPage 
                      config={config} 
                      onConfigUpdate={()=>{}}
                    />
                  } 
                />
                <Route path="/settings" element={<SettingsPage config={config} />} />
                <Route 
                  path="/yaml" 
                  element={
                    <YamlEditorPage 
                      config={config} 
                      onConfigUpdate={()=>{}}
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