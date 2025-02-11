import React, { useState } from 'react';
import { Plus } from 'lucide-react';
import type { Config, Proxy } from '../types/proxy';
import { ProxyCard } from '../components/ProxyCard';
import { YamlPopupEditor } from '../components/YamlPopupEditor';

interface ProxiesPageProps {
  config: Config;
  onConfigUpdate: (config: Config) => void;
  canEdit: boolean;
}

const proxyTemplate = {
  path: "/new-proxy",
  service: "serviceApi",
  middleware: {
    jwtAuth: { enabled: false },
    rateLimiter: { enabled: false },
    circuitBreaker: { enabled: false }
  },
  ttl: -1
};

export function ProxiesPage({ config, onConfigUpdate, canEdit }: ProxiesPageProps) {
  const [selectedProxy, setSelectedProxy] = useState<Proxy | null>(null);
  const [showProxyEditor, setShowProxyEditor] = useState(false);

  const handleEditProxy = (proxy: Proxy) => {
    setSelectedProxy(proxy);
    setShowProxyEditor(true);
  };

  const handleDeleteProxy = (path: string) => {
    onConfigUpdate({
      ...config,
      proxies: config.proxies.filter(p => p.path !== path)
    });
  };

  const handleProxySave = (proxy: Proxy) => {
    onConfigUpdate({
      ...config,
      proxies: selectedProxy
        ? config.proxies.map(p => p.path === selectedProxy.path ? proxy : p)
        : [...config.proxies, proxy]
    });
    setSelectedProxy(null);
    setShowProxyEditor(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold">Proxies</h2>
        <button
          onClick={() => setShowProxyEditor(true)}
          disabled={!canEdit}
          className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add Proxy
        </button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {config.proxies.map((proxy) => (
          <ProxyCard
            key={proxy.path}
            proxy={proxy}
            onEdit={handleEditProxy}
            onDelete={handleDeleteProxy}
          />
        ))}
      </div>

      {showProxyEditor && (
        <YamlPopupEditor
          title={selectedProxy ? "Edit Proxy" : "Add Proxy"}
          initialValue={selectedProxy}
          template={proxyTemplate}
          onSave={handleProxySave}
          onClose={() => {
            setShowProxyEditor(false);
            setSelectedProxy(null);
          }}
        />
      )}
    </div>
  );
}