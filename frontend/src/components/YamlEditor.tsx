import React, { useEffect, useState } from 'react';
import Editor, { Monaco } from "@monaco-editor/react";
import { dump, load } from 'js-yaml';
import { AlertCircle, Save, Download } from 'lucide-react';
import type { Config } from '../types/proxy';

interface YamlEditorProps {
  config: Config;
  onSave: (config: Config) => void;
}

const yamlCompletions = {
  proxies: {
    path: '/path',
    service: 'serviceName',
    middleware: {
      jwtAuth: {
        enabled: true
      },
      forwardAuth: {
        enabled: false,
        path: '/verify',
        service: 'authApi',
        requestHeaders: 'Forward(X-Custom-*)',
        responseHeaders: 'Remove(X-Powered-By)'
      },
      rateLimiter: {
        enabled: false,
        limitRefreshPeriod: 200000,
        limitForPeriod: 5,
        maxBurstCapacity: 6
      },
      circuitBreaker: {
        enabled: false,
        failureThreshold: 30,
        slowCallThreshold: 50,
        slowCallDuration: 500,
        openStateDuration: 5,
        waitDurationInOpenState: 10000,
        permittedNumberOfCallsInHalfOpenState: 2,
        minimumNumberOfCalls: 4
      }
    },
    ttl: -1
  },
  services: {
    name: 'serviceName',
    url: 'http://example.com',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
    role: 'roleA',
    healthcheck: '/health'
  },
  users: {
    username: 'user',
    password: 'password',
    role: 'user'
  },
  corsFilter: {
    accessControlAllowMethods: ['*'],
    accessControlAllowHeaders: ['*'],
    accessControlAllowOriginList: ['*']
  }
};

export function YamlEditor({ config, onSave }: YamlEditorProps) {
  const [yamlContent, setYamlContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    try {
      const yaml = dump(config, {
        indent: 2,
        lineWidth: -1,
        noRefs: true,
      });
      setYamlContent(yaml);
      setError(null);
    } catch (err) {
      setError('Error converting configuration to YAML');
    }
  }, [config]);

  const handleSave = () => {
    try {
      const parsedConfig = load(yamlContent) as Config;
      onSave(parsedConfig);
      setError(null);
    } catch (err) {
      setError('Invalid YAML format');
    }
  };

  const handleDownload = () => {
    try {
      // Create blob from YAML content
      const blob = new Blob([yamlContent], { type: 'text/yaml' });
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      
      // Generate filename with timestamp
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
      link.download = `config-${timestamp}.yaml`;
      
      // Trigger download
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to download configuration');
    }
  };

  const handleEditorWillMount = (monaco: Monaco) => {
    // Register YAML language completion provider
    monaco.languages.registerCompletionItemProvider('yaml', {
      provideCompletionItems: (model, position) => {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn,
        };

        const line = model.getLineContent(position.lineNumber);
        const suggestions: any[] = [];

        // Helper function to create completion items
        const createCompletionItem = (label: string, insertText: string, kind: number = monaco.languages.CompletionItemKind.Property) => ({
          label,
          kind,
          insertText,
          range,
          documentation: `Add ${label} configuration`,
        });

        // Root level completions
        if (line.trim().length === 0 || line.trim().startsWith('#')) {
          suggestions.push(
            createCompletionItem('proxies', 'proxies:\n  - path: '),
            createCompletionItem('services', 'services:\n  - name: '),
            createCompletionItem('users', 'users:\n  - username: '),
            createCompletionItem('corsFilter', 'corsFilter:\n  accessControlAllowMethods:\n    - \'*\'')
          );
        }

        // Proxy completions
        if (line.includes('proxies:') || line.trim().startsWith('-')) {
          suggestions.push(
            createCompletionItem('middleware', 'middleware:\n    jwtAuth:\n      enabled: true'),
            createCompletionItem('path', 'path: /'),
            createCompletionItem('service', 'service: serviceName'),
            createCompletionItem('ttl', 'ttl: -1')
          );
        }

        // Service completions
        if (line.includes('services:')) {
          suggestions.push(
            createCompletionItem('name', 'name: serviceName'),
            createCompletionItem('url', 'url: http://'),
            createCompletionItem('methods', 'methods:\n    - GET\n    - POST'),
            createCompletionItem('role', 'role: userRole'),
            createCompletionItem('healthcheck', 'healthcheck: /health')
          );
        }

        // HTTP methods completions
        if (line.includes('methods:')) {
          ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].forEach(method => {
            suggestions.push(createCompletionItem(method, method, monaco.languages.CompletionItemKind.Enum));
          });
        }

        return { suggestions };
      }
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold">Configuration Editor</h2>
        <div className="flex space-x-3">
          <button
            onClick={handleDownload}
            className="flex items-center px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <Download className="w-4 h-4 mr-2" />
            Download YAML
          </button>
          <button
            onClick={handleSave}
            className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <Save className="w-4 h-4 mr-2" />
            Save Changes
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border-l-4 border-red-400 p-4">
          <div className="flex items-center">
            <AlertCircle className="w-5 h-5 text-red-400 mr-2" />
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      <div className="border rounded-lg overflow-hidden">
        <Editor
          height="70vh"
          defaultLanguage="yaml"
          value={yamlContent}
          onChange={(value) => setYamlContent(value || '')}
          beforeMount={handleEditorWillMount}
          options={{
            minimap: { enabled: false },
            fontSize: 14,
            lineNumbers: 'on',
            rulers: [80],
            wordWrap: 'on',
            wrappingIndent: 'indent',
            automaticLayout: true,
            tabSize: 2,
            suggest: {
              showWords: false,
            }
          }}
          theme="vs-light"
        />
      </div>
    </div>
  );
}