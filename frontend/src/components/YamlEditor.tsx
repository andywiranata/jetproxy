import React, { useEffect, useState } from 'react';
import Editor, { Monaco } from "@monaco-editor/react";
import { dump, load } from 'js-yaml';
import { AlertCircle, Save, Download } from 'lucide-react';
import type { Config } from '../types/proxy';

interface YamlEditorProps {
  config: Config;
}
export function YamlEditor({ config }: YamlEditorProps) {
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
    monaco.languages.registerCompletionItemProvider('yaml', {
      provideCompletionItems: (model, position) => {
        const word = model.getWordUntilPosition(position);
        const range = {
          startLineNumber: position.lineNumber,
          endLineNumber: position.lineNumber,
          startColumn: word.startColumn,
          endColumn: word.endColumn,
        };
  
        const line = model.getLineContent(position.lineNumber).trim();
        const suggestions: any[] = [];
  
        const createCompletionItem = (label: string, insertText: string, kind = monaco.languages.CompletionItemKind.Property, documentation?: string) => ({
          label,
          kind,
          insertText,
          range,
          documentation: documentation || `Add ${label} configuration`,
        });
  
        // Root-level completions
        if (line === '' || line.startsWith('#')) {
          suggestions.push(
            createCompletionItem('appName', 'appName: API-PROXY'),
            createCompletionItem('port', 'port: 8080'),
            createCompletionItem('defaultTimeout', 'defaultTimeout: 10000'),
            createCompletionItem('rootPath', 'rootPath: /'),
            createCompletionItem('dashboard', 'dashboard: /'),
            createCompletionItem('accessLog', 'accessLog: true'),
            createCompletionItem('corsFilter', 'corsFilter:\n  accessControlAllowMethods:\n    - "*"\n  accessControlAllowHeaders:\n    - "*"\n  accessControlAllowOriginList:\n    - "*"'),
            createCompletionItem('storage', 'storage:\n  redis:\n    enabled: true\n    host: localhost\n    port: 6379\n    database: 0\n    maxTotal: 128\n    maxIdle: 64\n    minIdle: 8\n  inMemory:\n    enabled: true\n    maxMemory: 100\n    size: 100\n  statsd:\n    enabled: false\n    host: localhost\n    port: 8125\n    prefix: jetproxy'),
            createCompletionItem('jwtAuthSource', 'jwtAuthSource:\n  headerName: Authorization\n  tokenPrefix: Bearer\n  jwksUri: https://example.com/.well-known/jwks.json\n  jwksTtl: 60000\n  jwksType: RSA'),
            createCompletionItem('services', 'services:\n  - name: apiService\n    url: https://example.com\n    methods:\n      - GET\n      - POST'),
            createCompletionItem('proxies', 'proxies:\n  - path: /api\n    service: apiService\n    ttl: 1000'),
            createCompletionItem('users', 'users:\n  - username: user\n    password: pass\n    role: admin')
          );
        }
  
        if (line.startsWith('-') || line.includes('proxies:')) {
          suggestions.push(
            createCompletionItem('path', 'path: /example'),
            createCompletionItem('service', 'service: exampleService'),
            createCompletionItem('ttl', 'ttl: 1000'),
            createCompletionItem('middleware', 'middleware:\n    basicAuth: basicAuth:admin\n    rule: Header("Content-Type", "application/json")\n    rateLimiter:\n      enabled: true\n      limitRefreshPeriod: 2000\n      limitForPeriod: 5\n      maxBurstCapacity: 10\n    circuitBreaker:\n      enabled: true\n      failureThreshold: 50\n      slowCallThreshold: 50\n      slowCallDuration: 2000\n      openStateDuration: 10\n      waitDurationInOpenState: 10000\n      permittedNumberOfCallsInHalfOpenState: 3\n      minimumNumberOfCalls: 5\n    header:\n      requestHeaders: Remove(Authorization)\n      responseHeaders: Add(X-Powered-By, jetproxy)')
          );
        }
  
        if (line.includes('services:')) {
          suggestions.push(
            createCompletionItem('name', 'name: serviceName'),
            createCompletionItem('url', 'url: https://example.com'),
            createCompletionItem('methods', 'methods:\n  - GET\n  - POST'),
            createCompletionItem('role', 'role: roleName'),
            createCompletionItem('healthcheck', 'healthcheck: /health')
          );
        }
  
        if (line.includes('users:')) {
          suggestions.push(
            createCompletionItem('username', 'username: user'),
            createCompletionItem('password', 'password: pass'),
            createCompletionItem('role', 'role: admin')
          );
        }
  
        if (line.includes('methods:')) {
          ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'].forEach(method => {
            suggestions.push(createCompletionItem(method, `- ${method}`, monaco.languages.CompletionItemKind.Enum));
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
          {/* <button
            onClick={handleSave}
            className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            <Save className="w-4 h-4 mr-2" />
            Save Changes
          </button> */}
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