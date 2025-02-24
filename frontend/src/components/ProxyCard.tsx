import React from 'react';
import { Settings, Server, Shield, Clock } from 'lucide-react';
import type { Proxy } from '../types/proxy';

interface ProxyCardProps {
  proxy: Proxy;
  onEdit: (proxy: Proxy) => void;
  onDelete: (path: string) => void;
}

export function ProxyCard({ proxy, onEdit, onDelete }: ProxyCardProps) {
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
      
      <div className="space-y-3">
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Server className="w-4 h-4" />
          <span>Service: {proxy.service}</span>
        </div>
        
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Shield className="w-4 h-4" />
          <span>
            Middleware: {Object.keys(proxy.middleware).length} enabled
          </span>
        </div>
        
        <div className="flex items-center space-x-2 text-sm text-gray-600">
          <Clock className="w-4 h-4" />
          <span>TTL: {proxy.ttl === -1 ? 'Infinite' : `${proxy.ttl}ms`}</span>
        </div>
      </div>
      
      {proxy.middleware.jwtAuth?.enabled && (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 mt-3 mr-2">
          JWT Auth
        </span>
      )}
      {proxy.middleware.circuitBreaker?.enabled && (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800 mt-3 mr-2">
          Circuit Breaker
        </span>
      )}
      {proxy.middleware.rateLimiter?.enabled && (
        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800 mt-3">
          Rate Limiter
        </span>
      )}
    </div>
  );
}