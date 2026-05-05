import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { TrendingUp, MousePointer2, Clock, Zap } from 'lucide-react';

interface Click {
  x: number;
  y: number;
  timestamp: number;
}

interface AnalyticsDashboardProps {
  clicks: Click[];
}

export function AnalyticsDashboard({ clicks }: AnalyticsDashboardProps) {
  const totalClicks = clicks.length;
  const avgClicksPerMinute = clicks.length > 0
    ? (clicks.length / ((Date.now() - clicks[0].timestamp) / 60000)).toFixed(1)
    : '0';

  const last10Minutes = Date.now() - 600000;
  const recentClicks = clicks.filter(c => c.timestamp > last10Minutes);

  // Group clicks by second for the time series chart
  const timeSeriesData = clicks.reduce((acc, click) => {
    const second = Math.floor((click.timestamp - (clicks[0]?.timestamp || 0)) / 1000);
    const existing = acc.find(d => d.time === second);
    if (existing) {
      existing.clicks++;
    } else {
      acc.push({ time: second, clicks: 1 });
    }
    return acc;
  }, [] as { time: number; clicks: number }[]).slice(-30);

  // Group clicks by region (divide canvas into 4 quadrants)
  const regionData = [
    { region: 'Superior Izq.', clicks: clicks.filter(c => c.x < 300 && c.y < 150).length },
    { region: 'Superior Der.', clicks: clicks.filter(c => c.x >= 300 && c.y < 150).length },
    { region: 'Inferior Izq.', clicks: clicks.filter(c => c.x < 300 && c.y >= 150).length },
    { region: 'Inferior Der.', clicks: clicks.filter(c => c.x >= 300 && c.y >= 150).length },
  ];

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-4 gap-4">
        <StatCard
          icon={MousePointer2}
          label="Total Clics"
          value={totalClicks.toString()}
          color="text-blue-400"
        />
        <StatCard
          icon={TrendingUp}
          label="Clics/Minuto"
          value={avgClicksPerMinute}
          color="text-green-400"
        />
        <StatCard
          icon={Clock}
          label="Últimos 10min"
          value={recentClicks.length.toString()}
          color="text-purple-400"
        />
        <StatCard
          icon={Zap}
          label="Más Activo"
          value={regionData.reduce((max, r) => r.clicks > max.clicks ? r : max, regionData[0]).region}
          color="text-orange-400"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-slate-900 p-4 rounded-lg border border-slate-700">
          <h3 className="text-sm mb-4 text-slate-300">Clics en el Tiempo</h3>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={timeSeriesData}>
              <defs>
                <linearGradient id="colorClicks" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.8}/>
                  <stop offset="95%" stopColor="#3b82f6" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" stroke="#64748b" fontSize={12} />
              <YAxis stroke="#64748b" fontSize={12} />
              <Tooltip
                contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155' }}
                labelStyle={{ color: '#cbd5e1' }}
              />
              <Area type="monotone" dataKey="clicks" stroke="#3b82f6" fillOpacity={1} fill="url(#colorClicks)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-slate-900 p-4 rounded-lg border border-slate-700">
          <h3 className="text-sm mb-4 text-slate-300">Clics por Región</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={regionData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="region" stroke="#64748b" fontSize={12} />
              <YAxis stroke="#64748b" fontSize={12} />
              <Tooltip
                contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155' }}
                labelStyle={{ color: '#cbd5e1' }}
              />
              <Bar dataKey="clicks" fill="#10b981" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color }: { icon: any; label: string; value: string; color: string }) {
  return (
    <div className="bg-slate-900 p-4 rounded-lg border border-slate-700">
      <div className="flex items-center gap-3">
        <div className={`${color}`}>
          <Icon className="w-5 h-5" />
        </div>
        <div>
          <p className="text-xs text-slate-400">{label}</p>
          <p className="text-xl text-white">{value}</p>
        </div>
      </div>
    </div>
  );
}
