import { useState, useEffect } from 'react';
import { ClickHeatmap } from './components/ClickHeatmap';
import { FlowDiagram } from './components/FlowDiagram';
import { AnalyticsDashboard } from './components/AnalyticsDashboard';
import { Play, Pause, RotateCcw } from 'lucide-react';

interface Click {
  x: number;
  y: number;
  timestamp: number;
}

export default function App() {
  const [clicks, setClicks] = useState<Click[]>([]);
  const [activeStep, setActiveStep] = useState(0);
  const [isCapturing, setIsCapturing] = useState(true);

  const CANVAS_WIDTH = 600;
  const CANVAS_HEIGHT = 300;

  const handleCanvasClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!isCapturing) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const newClick: Click = {
      x,
      y,
      timestamp: Date.now(),
    };

    setClicks((prev) => [...prev, newClick]);

    // Animate through the flow
    setActiveStep(0);
    const steps = [0, 1, 2, 3, 4, 5, 6, 7];
    steps.forEach((step, index) => {
      setTimeout(() => setActiveStep(step), index * 200);
    });
  };

  const resetClicks = () => {
    setClicks([]);
    setActiveStep(0);
  };

  const toggleCapture = () => {
    setIsCapturing(!isCapturing);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl mb-2">Sistema de Análisis de Clics</h1>
            <p className="text-slate-400">
              Click en el área de captura para registrar interacciones y visualizar el flujo de datos
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={toggleCapture}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                isCapturing
                  ? 'bg-green-600 hover:bg-green-700'
                  : 'bg-slate-700 hover:bg-slate-600'
              }`}
            >
              {isCapturing ? (
                <>
                  <Pause className="w-4 h-4" />
                  Pausar
                </>
              ) : (
                <>
                  <Play className="w-4 h-4" />
                  Reanudar
                </>
              )}
            </button>
            <button
              onClick={resetClicks}
              className="flex items-center gap-2 px-4 py-2 bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              Reiniciar
            </button>
          </div>
        </div>

        <FlowDiagram activeStep={activeStep} />

        <div className="grid grid-cols-2 gap-6">
          <div className="space-y-4">
            <div className="bg-slate-900 p-6 rounded-lg border border-slate-700">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-xl">Área de Captura</h2>
                <div className="flex items-center gap-2">
                  <div
                    className={`w-3 h-3 rounded-full ${
                      isCapturing ? 'bg-green-500 animate-pulse' : 'bg-slate-500'
                    }`}
                  />
                  <span className="text-sm text-slate-400">
                    {isCapturing ? 'Activo' : 'Pausado'}
                  </span>
                </div>
              </div>
              <div
                onClick={handleCanvasClick}
                className={`relative ${
                  isCapturing ? 'cursor-crosshair' : 'cursor-not-allowed'
                }`}
                style={{ width: CANVAS_WIDTH, height: CANVAS_HEIGHT }}
              >
                <ClickHeatmap clicks={clicks} width={CANVAS_WIDTH} height={CANVAS_HEIGHT} />
                {!isCapturing && (
                  <div className="absolute inset-0 bg-slate-950/60 flex items-center justify-center rounded-lg">
                    <p className="text-lg text-slate-300">Captura pausada</p>
                  </div>
                )}
              </div>
              <div className="mt-4 text-sm text-slate-400">
                <p>Total de clics registrados: {clicks.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-slate-900 p-6 rounded-lg border border-slate-700">
            <h2 className="text-xl mb-4">Dashboard Analítico</h2>
            <AnalyticsDashboard clicks={clicks} />
          </div>
        </div>

        <div className="bg-slate-900 p-6 rounded-lg border border-slate-700">
          <h2 className="text-xl mb-4">Descripción del Sistema</h2>
          <div className="grid grid-cols-4 gap-4 text-sm">
            <div className="space-y-2">
              <h3 className="text-blue-400">1. Captura</h3>
              <p className="text-slate-400">SDK local registra eventos de clic del usuario en tiempo real</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-purple-400">2. Buffer</h3>
              <p className="text-slate-400">Almacenamiento temporal optimiza el procesamiento por lotes</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-cyan-400">3. Gateway</h3>
              <p className="text-slate-400">API Gateway enruta y valida las solicitudes entrantes</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-green-400">4. Kafka</h3>
              <p className="text-slate-400">Message broker distribuye eventos a múltiples consumidores</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-yellow-400">5. OLAP</h3>
              <p className="text-slate-400">Base de datos columnar optimizada para análisis agregados</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-orange-400">6. Dashboard</h3>
              <p className="text-slate-400">Visualización de métricas y KPIs en tiempo real</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-red-400">7. Mapa de Calor</h3>
              <p className="text-slate-400">Representación visual de zonas de mayor interacción</p>
            </div>
            <div className="space-y-2">
              <h3 className="text-slate-400">Arquitectura</h3>
              <p className="text-slate-400">Sistema distribuido escalable con procesamiento asíncrono</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
