import React from 'react';
import { Server, CheckCircle2, XCircle, AlertCircle, Database } from 'lucide-react';
import type { HealthStatus } from '../services/api';

interface ServerHealthSectionProps {
  health: HealthStatus | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

const StatusIcon = ({ status }: { status: string }) => {
  switch (status) {
    case 'Healthy':
      return <CheckCircle2 className="w-5 h-5 text-green-500" />;
    case 'Unhealthy':
      return <XCircle className="w-5 h-5 text-red-500" />;
    case 'Not Found':
      return <AlertCircle className="w-5 h-5 text-yellow-500" />;
    default:
      return <AlertCircle className="w-5 h-5 text-gray-400" />;
  }
};

const StatusBadge = ({ status }: { status: string }) => {
  const baseClasses = "px-2 py-1 text-xs font-medium rounded-full";
  switch (status) {
    case 'Healthy':
      return <span className={`${baseClasses} bg-green-100 text-green-800`}>{status}</span>;
    case 'Unhealthy':
      return <span className={`${baseClasses} bg-red-100 text-red-800`}>{status}</span>;
    case 'Not Found':
      return <span className={`${baseClasses} bg-yellow-100 text-yellow-800`}>{status}</span>;
    default:
      return <span className={`${baseClasses} bg-gray-100 text-gray-800`}>{status}</span>;
  }
};

export function ServerHealthSection({ health, isLoading, error, onRefresh }: ServerHealthSectionProps) {
  if (isLoading) {
    return (
      <div className="animate-pulse space-y-4">
        <div className="h-8 bg-gray-200 rounded w-1/4"></div>
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 bg-gray-200 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border-l-4 border-red-400 p-4">
        <div className="flex">
          <XCircle className="h-5 w-5 text-red-400" />
          <div className="ml-3">
            <p className="text-sm text-red-700">Failed to load server health status</p>
          </div>
        </div>
      </div>
    );
  }

  if (!health) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-medium text-gray-900">Server Health</h3>
        {/* <button
          onClick={onRefresh}
          className="inline-flex items-center px-3 py-1.5 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-copper"
        >
          Refresh Status
        </button> */}
      </div>

      <div className="grid grid-cols-1 gap-4">
        {/* Overall System Status */}
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Server className="h-6 w-6 text-copper" />
              <div className="ml-3">
                <h4 className="text-sm font-medium text-gray-900">System Status</h4>
                <p className="text-sm text-gray-500">Overall system health</p>
              </div>
            </div>
            <StatusBadge status={health.status === 'UP' ? 'Healthy' : 'Unhealthy'} />
          </div>
        </div>

        {/* Redis Status */}
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Database className="h-6 w-6 text-copper" />
              <div className="ml-3">
                <h4 className="text-sm font-medium text-gray-900">Redis Status</h4>
                <p className="text-sm text-gray-500">Cache and data store health</p>
              </div>
            </div>
            <StatusBadge status={health.redisStatus} />
          </div>
        </div>

        {/* Individual Server Status */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="px-4 py-5 sm:px-6">
            <h3 className="text-sm font-medium text-gray-900">Service Endpoints</h3>
          </div>
          <div className="border-t border-gray-200">
            <div className="divide-y divide-gray-200">
              {Object.entries(health.servers).map(([url, status]) => (
                <div key={url} className="px-4 py-4 flex items-center justify-between hover:bg-gray-50">
                  <div className="flex items-center min-w-0">
                    <StatusIcon status={status} />
                    <div className="ml-3 flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">{url}</p>
                    </div>
                  </div>
                  <div className="ml-4">
                    <StatusBadge status={status} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}