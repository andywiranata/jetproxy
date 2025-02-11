import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Shield, Server, Users, Settings, FileJson, ChevronLeft, ChevronRight } from 'lucide-react';

interface SidebarProps {
  isCollapsed: boolean;
  onToggle: () => void;
}

const menuItems = [
  { id: 'dashboard', icon: LayoutDashboard, label: 'Dashboard', path: '/dashboard' },
  { id: 'proxies', icon: Shield, label: 'Proxies', path: '/proxies' },
  { id: 'services', icon: Server, label: 'Services', path: '/services' },
  { id: 'users', icon: Users, label: 'Users', path: '/users' },
  { id: 'settings', icon: Settings, label: 'Settings', path: '/settings' },
  { id: 'yaml', icon: FileJson, label: 'YAML Editor', path: '/yaml' },
];

export function Sidebar({ isCollapsed, onToggle }: SidebarProps) {
  return (
    <div 
      className={`h-screen bg-white border-r border-gray-200 fixed left-0 top-0 transition-all duration-300 ease-in-out ${
        isCollapsed ? 'w-20' : 'w-64'
      }`}
    >
      <div className="flex items-center h-16 px-6 border-b border-gray-200">
        <img 
          src={import.meta.env.VITE_LOGO_URL}
          alt={import.meta.env.VITE_APP_NAME} 
          className="w-8 h-8" 
        />
        <span className={`ml-2 text-xl font-semibold text-copper transition-opacity duration-300 ${
          isCollapsed ? 'opacity-0 hidden' : 'opacity-100'
        }`}>
          {import.meta.env.VITE_APP_NAME}
        </span>
      </div>
      
      <button
        onClick={onToggle}
        className="absolute -right-3 top-7 bg-white border border-gray-200 rounded-full p-1 text-gray-400 hover:text-copper transition-colors duration-200"
      >
        {isCollapsed ? (
          <ChevronRight className="w-4 h-4" />
        ) : (
          <ChevronLeft className="w-4 h-4" />
        )}
      </button>

      <nav className="mt-6">
        {menuItems.map(({ id, icon: Icon, label, path }) => (
          <NavLink
            key={id}
            to={path}
            className={({ isActive }) =>
              `w-full flex items-center px-6 py-3 text-sm font-medium transition-colors relative group ${
                isActive
                  ? 'text-copper bg-primary-50'
                  : 'text-gray-600 hover:text-copper hover:bg-primary-50'
              }`
            }
          >
            <Icon className="w-5 h-5" />
            <span className={`ml-3 transition-all duration-300 ${
              isCollapsed ? 'opacity-0 absolute' : 'opacity-100'
            }`}>
              {label}
            </span>
            {isCollapsed && (
              <div className="absolute left-16 bg-gray-900 text-white px-2 py-1 rounded text-xs whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
                {label}
              </div>
            )}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}