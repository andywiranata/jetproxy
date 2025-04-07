import { Settings, Server, Shield, Clock, Check, X } from 'lucide-react';
import type { Proxy, Middleware } from '../types/proxy';

interface ProxyCardProps {
  proxy: Proxy;
  onEdit: (proxy: Proxy) => void;
  onDelete: (path: string) => void;
}

const MiddlewareStatus = ({ enabled }: { enabled: boolean }) => (
  enabled ? 
    <Check className="w-4 h-4 text-green-500" /> : 
    <X className="w-4 h-4 text-gray-300" />
);

const getMiddlewareStatus = (middleware: Middleware, key: string): boolean => {
  const value: any = middleware[key as keyof Middleware];
  if (typeof value === 'string') {
    return !!value;
  }
  if (typeof value === 'object' && value !== null) {
    return value.enabled || false;
  }
  return false;
};

const formatMiddlewareLabel = (key: string): string => {
  // Convert camelCase to Title Case with spaces
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim();
};

export function ProxyCard({ proxy, onEdit, onDelete }: ProxyCardProps) {
  const middlewareList = Object.keys(proxy.middleware).map(key => ({
    key,
    label: formatMiddlewareLabel(key),
    enabled: getMiddlewareStatus(proxy.middleware, key)
  }));

  const enabledCount = middlewareList.filter(m => m.enabled).length;

  return (
    <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
      <div className="flex justify-between items-start mb-4">
        <div className="flex items-center space-x-2">
          <Server className="w-5 h-5 text-blue-600" />
          <h3 className="text-lg font-semibold text-gray-900">{proxy.path}</h3>
        </div>
        <div className="flex space-x-2">
          <button
            onClick={() => onEdit(proxy)}
            className="p-2 text-gray-600 hover:text-blue-600 rounded-full hover:bg-blue-50"
          >
            <Settings className="w-5 h-5" />
          </button>
          <button
            onClick={() => onDelete(proxy.path)}
            className="p-2 text-gray-600 hover:text-red-600 rounded-full hover:bg-red-50"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
      
      <div className="space-y-4">
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Server className="w-4 h-4" />
          <span>Service: {proxy.service}</span>
        </div>
        
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Shield className="w-4 h-4" />
          <span>
            Middleware: {enabledCount} enabled
          </span>
        </div>
        
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Clock className="w-4 h-4" />
          <span>TTL: {proxy.ttl === -1 ? 'Inactive' : `${proxy.ttl}ms`}</span>
        </div>

        <div className="border-t pt-4 mt-4">
          <h4 className="text-sm font-medium text-gray-900 mb-3">Middleware Status</h4>
          <div className="grid grid-cols-2 gap-2">
            {middlewareList.map(({ key, label, enabled }) => (
              <div 
                key={key}
                className={`flex items-center justify-between p-2 rounded ${
                  enabled ? 'bg-green-50' : 'bg-gray-50'
                }`}
              >
                <span className="text-sm font-medium text-gray-700">{label}</span>
                <MiddlewareStatus enabled={enabled} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}