import React from 'react';
import { RefreshCw, AlertCircle } from 'lucide-react';

interface DataFreshnessIndicatorProps {
  lastUpdated: Date | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

export function DataFreshnessIndicator({
  lastUpdated,
  isLoading,
  error,
  onRefresh
}: DataFreshnessIndicatorProps) {
  const getTimeAgo = (date: Date) => {
    const seconds = Math.floor((new Date().getTime() - date.getTime()) / 1000);
    if (seconds < 60) return `${seconds} seconds ago`;
    const minutes = Math.floor(seconds / 60);
    return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
  };

  return (
    <div className="flex items-center space-x-4">
      {error ? (
        <div className="flex items-center text-red-600">
          <AlertCircle className="w-4 h-4 mr-2" />
          <span className="text-sm">{error}</span>
        </div>
      ) : (
        <div className="flex items-center text-gray-600">
          <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
          <span className="text-sm">
            {lastUpdated ? `Last updated ${getTimeAgo(lastUpdated)}` : 'Never updated'}
          </span>
        </div>
      )}
      <button
        onClick={onRefresh}
        disabled={isLoading}
        className="text-sm text-blue-600 hover:text-blue-800 disabled:opacity-50"
      >
        Refresh now
      </button>
    </div>
  );
}