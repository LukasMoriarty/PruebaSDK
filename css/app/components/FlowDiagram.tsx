import { motion } from 'motion/react';
import { ArrowRight, Database, Activity, BarChart3, MousePointer2, Server, Layers, Gauge } from 'lucide-react';

interface FlowDiagramProps {
  activeStep: number;
}

const steps = [
  { icon: MousePointer2, label: 'Clic Usuario', color: 'bg-blue-500' },
  { icon: Activity, label: 'SDK Local', color: 'bg-purple-500' },
  { icon: Layers, label: 'Buffer Temporal', color: 'bg-indigo-500' },
  { icon: Server, label: 'API Gateway', color: 'bg-cyan-500' },
  { icon: Database, label: 'Kafka', color: 'bg-green-500' },
  { icon: Database, label: 'OLAP DB', color: 'bg-yellow-500' },
  { icon: BarChart3, label: 'Dashboard', color: 'bg-orange-500' },
  { icon: Gauge, label: 'Mapa de Calor', color: 'bg-red-500' },
];

export function FlowDiagram({ activeStep }: FlowDiagramProps) {
  return (
    <div className="flex items-center justify-between gap-2 p-6 bg-slate-900 rounded-lg border border-slate-700">
      {steps.map((step, index) => (
        <div key={index} className="flex items-center gap-2">
          <motion.div
            className={`flex flex-col items-center gap-2 ${
              index <= activeStep ? 'opacity-100' : 'opacity-40'
            }`}
            animate={{
              scale: index === activeStep ? [1, 1.1, 1] : 1,
            }}
            transition={{ duration: 0.5 }}
          >
            <div
              className={`${step.color} p-3 rounded-lg shadow-lg ${
                index === activeStep ? 'ring-2 ring-white' : ''
              }`}
            >
              <step.icon className="w-5 h-5 text-white" />
            </div>
            <span className="text-xs text-slate-300 text-center max-w-[80px]">
              {step.label}
            </span>
          </motion.div>
          {index < steps.length - 1 && (
            <motion.div
              initial={{ opacity: 0.3 }}
              animate={{
                opacity: index < activeStep ? 1 : 0.3,
              }}
            >
              <ArrowRight className="w-4 h-4 text-slate-500" />
            </motion.div>
          )}
        </div>
      ))}
    </div>
  );
}
