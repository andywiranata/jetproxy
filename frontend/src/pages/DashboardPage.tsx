import React, { useState, useEffect } from 'react';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar, PieChart, Pie, Cell, Legend, ComposedChart
} from 'recharts';
import { 
  Activity, Clock, AlertTriangle, CheckCircle2, 
  ArrowUpRight, ArrowDownRight, Users, Shield,
  Server, RefreshCw, Calendar, ChevronDown, ChevronUp
} from 'lucide-react';
import type { Config } from '../types/proxy';

interface DashboardPageProps {
  config: Config;
}

// Mock data generator for real-time simulation
const generatePerformanceData = () => {
  const now = new Date();
  return Array.from({ length: 24 }, (_, i) => {
    const time = new Date(now.getTime() - (23 - i) * 3600000);
    return {
      time: time.getHours().toString().padStart(2, '0') + ':00',
      latency: Math.floor(Math.random() * 400) + 100,
      requests: Math.floor(Math.random() * 300) + 50,
      errors: Math.floor(Math.random() * 10),
      successRate: Math.floor(Math.random() * 20) + 80,
    };
  });
};

const timeRanges = [
  { label: '24h', value: '24h' },
  { label: '7d', value: '7d' },
  { label: '30d', value: '30d' },
];

const StatCard = ({ title, value, trend, icon: Icon, trendValue, loading = false }: any) => (
  <div className="bg-white rounded-lg shadow p-6 relative overflow-hidden">
    {loading && (
      <div className="absolute inset-0 bg-gray-50/50 flex items-center justify-center">
        <RefreshCw className="w-6 h-6 text-copper animate-spin" />
      </div>
    )}
    <div className="flex items-start justify-between">
      <div>
        <p className="text-sm font-medium text-gray-600">{title}</p>
        <p className="mt-2 text-3xl font-semibold text-gray-900">{value}</p>
      </div>
      <div className={`p-3 rounded-full ${trend === 'up' ? 'bg-green-50' : 'bg-red-50'}`}>
        <Icon className={`w-6 h-6 ${trend === 'up' ? 'text-green-500' : 'text-red-500'}`} />
      </div>
    </div>
    <div className="mt-4 flex items-center">
      {trend === 'up' ? (
        <ArrowUpRight className="w-4 h-4 text-green-500 mr-1" />
      ) : (
        <ArrowDownRight className="w-4 h-4 text-red-500 mr-1" />
      )}
      <span className={`text-sm ${trend === 'up' ? 'text-green-600' : 'text-red-600'}`}>
        {trendValue}
      </span>
    </div>
  </div>
);

// Add new mock data generator for service performance
const generateServicePerformanceData = (services: Config['services']) => {
  return services.map(service => ({
    name: service.name,
    responseTime: Math.floor(Math.random() * 300) + 50,
    requests: Math.floor(Math.random() * 1000) + 100,
    errors: Math.floor(Math.random() * 50),
    successRate: Math.floor(Math.random() * 10) + 90,
    trend: Math.random() > 0.5 ? 'up' : 'down',
    trendValue: Math.floor(Math.random() * 10) + 1,
  }));
};

export function DashboardPage({ config }: DashboardPageProps) {
  const [timeRange, setTimeRange] = useState('24h');
  const [performanceData, setPerformanceData] = useState(generatePerformanceData());
  const [loading, setLoading] = useState(false);
  const [statusData, setStatusData] = useState([
    { name: 'Success', value: 85, color: '#10B981' },
    { name: 'Failed', value: 15, color: '#EF4444' },
  ]);
  const [servicePerformance, setServicePerformance] = useState(generateServicePerformanceData(config.services));
  const [sortConfig, setSortConfig] = useState({ key: 'responseTime', direction: 'asc' });
  const [expandedService, setExpandedService] = useState<string | null>(null);

  // Simulate real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      setPerformanceData(prev => {
        const newData = [...prev.slice(1), {
          time: new Date().getHours().toString().padStart(2, '0') + ':00',
          latency: Math.floor(Math.random() * 400) + 100,
          requests: Math.floor(Math.random() * 300) + 50,
          errors: Math.floor(Math.random() * 10),
          successRate: Math.floor(Math.random() * 20) + 80,
        }];
        return newData;
      });
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const handleTimeRangeChange = async (range: string) => {
    setLoading(true);
    setTimeRange(range);
    // Simulate API call
    setTimeout(() => {
      setPerformanceData(generatePerformanceData());
      setLoading(false);
    }, 1000);
  };

  const handleSort = (key: string) => {
    setSortConfig(prev => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc'
    }));
  };

  const sortedServicePerformance = [...servicePerformance].sort((a, b) => {
    if (sortConfig.direction === 'asc') {
      return a[sortConfig.key] - b[sortConfig.key];
    }
    return b[sortConfig.key] - a[sortConfig.key];
  });

  return (
    <div className="space-y-6">
      {/* Time Range Selector */}
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-gray-900">Dashboard</h2>
        <div className="flex items-center space-x-2 bg-white rounded-lg shadow px-2">
          <Calendar className="w-4 h-4 text-gray-400" />
          {timeRanges.map(range => (
            <button
              key={range.value}
              onClick={() => handleTimeRangeChange(range.value)}
              className={`px-3 py-2 text-sm font-medium rounded-md ${
                timeRange === range.value
                  ? 'text-copper bg-primary-50'
                  : 'text-gray-600 hover:text-copper hover:bg-primary-50'
              }`}
            >
              {range.label}
            </button>
          ))}
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Active Users"
          value="1,234"
          trend="up"
          icon={Users}
          trendValue="12% vs last hour"
          loading={loading}
        />
        <StatCard
          title="Avg Response Time"
          value="235ms"
          trend="down"
          icon={Clock}
          trendValue="8% vs last hour"
          loading={loading}
        />
        <StatCard
          title="Error Rate"
          value="0.8%"
          trend="up"
          icon={AlertTriangle}
          trendValue="3% vs last hour"
          loading={loading}
        />
        <StatCard
          title="Success Rate"
          value="99.2%"
          trend="up"
          icon={CheckCircle2}
          trendValue="1% vs last hour"
          loading={loading}
        />
      </div>

      {/* Performance Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Response Time Chart */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Response Time</h3>
            <div className="flex items-center space-x-2 text-sm text-gray-500">
              <span className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-copper mr-1" />
                Latency
              </span>
            </div>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={performanceData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis 
                  dataKey="time" 
                  stroke="#6B7280"
                  tick={{ fontSize: 12 }}
                />
                <YAxis 
                  stroke="#6B7280"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => `${value}ms`}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#FFFFFF',
                    border: '1px solid #E5E7EB',
                    borderRadius: '6px',
                  }}
                />
                <Line 
                  type="monotone" 
                  dataKey="latency" 
                  stroke="#B36952" 
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4, fill: '#B36952' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Request Volume Chart */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Request Volume</h3>
            <div className="flex items-center space-x-4 text-sm text-gray-500">
              <span className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-copper mr-1" />
                Requests
              </span>
              <span className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-red-500 mr-1" />
                Errors
              </span>
            </div>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={performanceData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                <XAxis 
                  dataKey="time" 
                  stroke="#6B7280"
                  tick={{ fontSize: 12 }}
                />
                <YAxis 
                  stroke="#6B7280"
                  tick={{ fontSize: 12 }}
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#FFFFFF',
                    border: '1px solid #E5E7EB',
                    borderRadius: '6px',
                  }}
                />
                <Bar dataKey="requests" fill="#B36952" />
                <Bar dataKey="errors" fill="#EF4444" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Service Performance Table */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Service Performance</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Service
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
                  onClick={() => handleSort('responseTime')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Response Time</span>
                    {sortConfig.key === 'responseTime' && (
                      sortConfig.direction === 'asc' ? 
                        <ChevronUp className="w-4 h-4" /> : 
                        <ChevronDown className="w-4 h-4" />
                    )}
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
                  onClick={() => handleSort('requests')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Requests</span>
                    {sortConfig.key === 'requests' && (
                      sortConfig.direction === 'asc' ? 
                        <ChevronUp className="w-4 h-4" /> : 
                        <ChevronDown className="w-4 h-4" />
                    )}
                  </div>
                </th>
                <th 
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer"
                  onClick={() => handleSort('successRate')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Success Rate</span>
                    {sortConfig.key === 'successRate' && (
                      sortConfig.direction === 'asc' ? 
                        <ChevronUp className="w-4 h-4" /> : 
                        <ChevronDown className="w-4 h-4" />
                    )}
                  </div>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Trend
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {sortedServicePerformance.map((service) => (
                <React.Fragment key={service.name}>
                  <tr 
                    className="hover:bg-gray-50 cursor-pointer"
                    onClick={() => setExpandedService(
                      expandedService === service.name ? null : service.name
                    )}
                  >
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <Server className="w-5 h-5 text-gray-400 mr-3" />
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {service.name}
                          </div>
                          <div className="text-sm text-gray-500">
                            {config.services.find(s => s.name === service.name)?.url}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm text-gray-900">{service.responseTime}ms</span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="text-sm text-gray-900">{service.requests}</span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="w-16 bg-gray-200 rounded-full h-2 mr-2">
                          <div 
                            className="bg-copper rounded-full h-2"
                            style={{ width: `${service.successRate}%` }}
                          />
                        </div>
                        <span className="text-sm text-gray-900">{service.successRate}%</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        {service.trend === 'up' ? (
                          <ArrowUpRight className="w-4 h-4 text-green-500 mr-1" />
                        ) : (
                          <ArrowDownRight className="w-4 h-4 text-red-500 mr-1" />
                        )}
                        <span className={`text-sm ${
                          service.trend === 'up' ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {service.trendValue}%
                        </span>
                      </div>
                    </td>
                  </tr>
                  {expandedService === service.name && (
                    <tr>
                      <td colSpan={5} className="px-6 py-4 bg-gray-50">
                        <div className="h-64">
                          <ResponsiveContainer width="100%" height="100%">
                            <ComposedChart data={performanceData}>
                              <CartesianGrid strokeDasharray="3 3" stroke="#E5E7EB" />
                              <XAxis 
                                dataKey="time" 
                                stroke="#6B7280"
                                tick={{ fontSize: 12 }}
                              />
                              <YAxis 
                                yAxisId="left"
                                stroke="#6B7280"
                                tick={{ fontSize: 12 }}
                              />
                              <YAxis 
                                yAxisId="right"
                                orientation="right"
                                stroke="#6B7280"
                                tick={{ fontSize: 12 }}
                              />
                              <Tooltip
                                contentStyle={{
                                  backgroundColor: '#FFFFFF',
                                  border: '1px solid #E5E7EB',
                                  borderRadius: '6px',
                                }}
                              />
                              <Legend />
                              <Bar 
                                yAxisId="left"
                                dataKey="requests" 
                                fill="#B36952" 
                                name="Requests"
                              />
                              <Line
                                yAxisId="right"
                                type="monotone"
                                dataKey="latency"
                                stroke="#10B981"
                                name="Response Time"
                                strokeWidth={2}
                                dot={false}
                              />
                            </ComposedChart>
                          </ResponsiveContainer>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Service Health and Status */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Service Status */}
        <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-900">Service Health</h3>
            <button 
              onClick={() => {}} 
              className="p-2 text-gray-400 hover:text-copper rounded-full hover:bg-gray-100"
            >
              <RefreshCw className="w-5 h-5" />
            </button>
          </div>
          <div className="space-y-4">
            {config.services.map((service) => (
              <div key={service.name} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                <div className="flex items-center">
                  <Server className="w-5 h-5 text-gray-400 mr-3" />
                  <div>
                    <p className="font-medium text-gray-900">{service.name}</p>
                    <p className="text-sm text-gray-500">{service.url}</p>
                  </div>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="text-right text-sm">
                    <p className="text-gray-900">99.9%</p>
                    <p className="text-gray-500">Uptime</p>
                  </div>
                  <span className="px-2 py-1 text-sm rounded-full bg-green-100 text-green-800">
                    Healthy
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Status Distribution */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Status Distribution</h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={statusData}
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {statusData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#FFFFFF',
                    border: '1px solid #E5E7EB',
                    borderRadius: '6px',
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-4 space-y-2">
            {statusData.map((entry) => (
              <div key={entry.name} className="flex items-center justify-between">
                <div className="flex items-center">
                  <div 
                    className="w-3 h-3 rounded-full mr-2"
                    style={{ backgroundColor: entry.color }}
                  />
                  <span className="text-sm text-gray-600">{entry.name}</span>
                </div>
                <span className="text-sm font-medium text-gray-900">{entry.value}%</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}