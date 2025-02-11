import React from 'react';
import { dump, load } from 'js-yaml';
import Editor from "@monaco-editor/react";
import { X } from 'lucide-react';

interface YamlPopupEditorProps {
  title: string;
  initialValue: any;
  template?: any;
  onSave: (value: any) => void;
  onClose: () => void;
}

export function YamlPopupEditor({ title, initialValue, template, onSave, onClose }: YamlPopupEditorProps) {
  const [content, setContent] = React.useState('');
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    try {
      const yaml = dump(initialValue || template || {}, {
        indent: 2,
        lineWidth: -1,
        noRefs: true,
      });
      setContent(yaml);
    } catch (err) {
      setError('Error converting to YAML');
    }
  }, [initialValue, template]);

  const handleSave = () => {
    try {
      const parsed = load(content);
      onSave(parsed);
      setError(null);
    } catch (err) {
      setError('Invalid YAML format');
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <h3 className="text-lg font-medium">{title}</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-500"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        
        <div className="flex-1 min-h-0">
          <Editor
            height="50vh"
            defaultLanguage="yaml"
            value={content}
            onChange={(value) => setContent(value || '')}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              lineNumbers: 'on',
              rulers: [80],
              wordWrap: 'on',
              wrappingIndent: 'indent',
              automaticLayout: true,
            }}
            theme="vs-light"
          />
        </div>

        {error && (
          <div className="px-6 py-3 bg-red-50 border-t border-red-200">
            <p className="text-sm text-red-600">{error}</p>
          </div>
        )}

        <div className="px-6 py-4 border-t flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
          >
            Save
          </button>
        </div>
      </div>
    </div>
  );
}