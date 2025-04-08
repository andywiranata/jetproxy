import React from 'react';
import type { Config } from '../types/proxy';
import { YamlEditor } from '../components/YamlEditor';

interface YamlEditorPageProps {
  config: Config;
  onConfigUpdate: (config: Config) => void;
}

export function YamlEditorPage({ config, onConfigUpdate }: YamlEditorPageProps) {
  return <YamlEditor config={config} />;
}