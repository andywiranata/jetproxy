import React from 'react';
import type { Config } from '../types/proxy';
import { Info } from 'lucide-react';

interface SettingsPageProps {
  config: Config;
  onConfigUpdate: (config: Config) => void;
}

interface SettingItemProps {
  label: string;
  value: string;
  description: string;
}

function SettingItem({ label, value, description }: SettingItemProps) {
  return (
    <div className="border-b border-gray-200 pb-4">
      <div className="flex items-start justify-between">
        <div>
          <label className="block text-sm font-medium text-gray-900">{label}</label>
          <p className="mt-1 text-sm text-gray-500 flex items-center">
            <Info className="w-4 h-4 mr-1 inline" />
            {description}
          </p>
        </div>
        <div className="mt-1">
          <code className="px-2 py-1 text-sm font-mono bg-gray-100 rounded-md">
            {value}
          </code>
        </div>
      </div>
    </div>
  );
}

export function SettingsPage({ config }: SettingsPageProps) {
  const settings = [
    {
      label: "App Name",
      value: config.appName,
      description: "Application name with default value API-PROXY"
    },
    {
      label: "Port",
      value: config.port,
      description: "Server port with default value 8080"
    },
    {
      label: "Default Timeout",
      value: config.defaultTimeout,
      description: "Default request timeout in milliseconds"
    },
    {
      label: "Dashboard",
      value: config.dashboard,
      description: "Enable/disable dashboard interface"
    },
    {
      label: "Root Path",
      value: config.rootPath,
      description: "Base path for the application"
    },
    {
      label: "Debug Mode",
      value: config.debugMode,
      description: "Enable/disable debug logging"
    }
  ];

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-6">Application Settings</h2>
        <div className="space-y-6">
          {settings.map((setting, index) => (
            <SettingItem key={index} {...setting} />
          ))}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-xl font-semibold mb-6">CORS Settings</h2>
        <div className="space-y-4">
          <div>
            <h3 className="text-sm font-medium text-gray-900">Allow Methods</h3>
            <div className="mt-2 flex flex-wrap gap-2">
              {config.corsFilter.accessControlAllowMethods.map((method, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800"
                >
                  {method}
                </span>
              ))}
            </div>
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-900">Allow Headers</h3>
            <div className="mt-2 flex flex-wrap gap-2">
              {config.corsFilter.accessControlAllowHeaders.map((header, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800"
                >
                  {header}
                </span>
              ))}
            </div>
          </div>
          <div>
            <h3 className="text-sm font-medium text-gray-900">Allow Origins</h3>
            <div className="mt-2 flex flex-wrap gap-2">
              {config.corsFilter.accessControlAllowOriginList.map((origin, index) => (
                <span
                  key={index}
                  className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-purple-100 text-purple-800"
                >
                  {origin}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}