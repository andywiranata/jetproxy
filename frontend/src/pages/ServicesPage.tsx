import React, { useState } from 'react';
import { Plus } from 'lucide-react';
import type { Config, Service } from '../types/proxy';
import { ServiceTable } from '../components/ServiceTable';
import { YamlPopupEditor } from '../components/YamlPopupEditor';

interface ServicesPageProps {
  config: Config;
  onConfigUpdate: (config: Config) => void;
}

const serviceTemplate = {
  name: "newService",
  url: "http://api.example.com",
  methods: ["GET"],
  healthcheck: "/health"
};

export function ServicesPage({ config, onConfigUpdate }: ServicesPageProps) {
  const [selectedService, setSelectedService] = useState<Service | null>(null);
  const [showServiceEditor, setShowServiceEditor] = useState(false);

  const handleEditService = (service: Service) => {
    setSelectedService(service);
    setShowServiceEditor(true);
  };

  const handleDeleteService = (name: string) => {
    onConfigUpdate({
      ...config,
      services: config.services.filter(s => s.name !== name)
    });
  };

  const handleServiceSave = (service: Service) => {
    onConfigUpdate({
      ...config,
      services: selectedService
        ? config.services.map(s => s.name === selectedService.name ? service : s)
        : [...config.services, service]
    });
    setSelectedService(null);
    setShowServiceEditor(false);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold">Services</h2>
        <button
          onClick={() => setShowServiceEditor(true)}
          className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add Service
        </button>
      </div>
      <div className="bg-white rounded-lg shadow">
        <ServiceTable
          services={config.services}
          onEdit={handleEditService}
          onDelete={handleDeleteService}
        />
      </div>

      {showServiceEditor && (
        <YamlPopupEditor
          title={selectedService ? "Edit Service" : "Add Service"}
          initialValue={selectedService}
          template={serviceTemplate}
          onSave={handleServiceSave}
          onClose={() => {
            setShowServiceEditor(false);
            setSelectedService(null);
          }}
        />
      )}
    </div>
  );
}