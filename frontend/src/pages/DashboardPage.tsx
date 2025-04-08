import { useState, useEffect } from 'react';
import { 
  Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell
} from 'recharts';
import { 

  Server, RefreshCw,
} from 'lucide-react';
import type { Config } from '../types/proxy';
import type { HealthStatus } from '../services/api';
import { fetchHealthStatus } from '../services/api';
import { ServerHealthSection } from '../components/ServerHealthSection';


interface DashboardPageProps {
  config: Config;
}


export function DashboardPage({ config }: DashboardPageProps) {
  const [statusData] = useState([
    { name: 'Success', value: 85, color: '#10B981' },
    { name: 'Failed', value: 15, color: '#EF4444' },
  ]);
  const [healthStatus, setHealthStatus] = useState<HealthStatus | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [isHealthLoading, setIsHealthLoading] = useState(false);


  // Simulate real-time updates
  useEffect(() => {
    const interval = setInterval(() => {
      fetchHealth(true);
    }, 5000);

    fetchHealth();
    return () => clearInterval(interval);
  }, []);

  const fetchHealth = async (silentFetching: boolean = false) => {
    try {
      setIsHealthLoading(silentFetching ? false: true);
      setHealthError(null);
      const status = await fetchHealthStatus();
      setHealthStatus(status);
    } catch (error) {
      setHealthError('Failed to fetch health status');
    } finally {
      setIsHealthLoading(false);
    }
  };
  return (
    <div className="space-y-6">
      {/* Time Range Selector */}
     
       {/* Server Health Section */}
       <div className="bg-white rounded-lg shadow p-6">
        <ServerHealthSection
          health={healthStatus}
          isLoading={isHealthLoading}
          error={healthError}
          onRefresh={fetchHealth}
        />
      </div>


      {/* Stats Overview */}
      {/* <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
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
      </div> */}
    </div>
  );
}