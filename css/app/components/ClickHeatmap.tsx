import { useEffect, useRef } from 'react';

interface Click {
  x: number;
  y: number;
  timestamp: number;
}

interface ClickHeatmapProps {
  clicks: Click[];
  width: number;
  height: number;
}

export function ClickHeatmap({ clicks, width, height }: ClickHeatmapProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, width, height);

    // Draw gradient background
    const gradient = ctx.createLinearGradient(0, 0, 0, height);
    gradient.addColorStop(0, '#0f172a');
    gradient.addColorStop(1, '#1e293b');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);

    // Draw heatmap
    clicks.forEach((click) => {
      const gradient = ctx.createRadialGradient(click.x, click.y, 0, click.x, click.y, 40);
      gradient.addColorStop(0, 'rgba(239, 68, 68, 0.6)');
      gradient.addColorStop(0.5, 'rgba(245, 158, 11, 0.3)');
      gradient.addColorStop(1, 'rgba(245, 158, 11, 0)');

      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, width, height);
    });

    // Draw click points
    clicks.forEach((click) => {
      ctx.beginPath();
      ctx.arc(click.x, click.y, 3, 0, 2 * Math.PI);
      ctx.fillStyle = '#ef4444';
      ctx.fill();

      ctx.beginPath();
      ctx.arc(click.x, click.y, 5, 0, 2 * Math.PI);
      ctx.strokeStyle = 'rgba(239, 68, 68, 0.5)';
      ctx.lineWidth = 2;
      ctx.stroke();
    });
  }, [clicks, width, height]);

  return (
    <canvas
      ref={canvasRef}
      width={width}
      height={height}
      className="rounded-lg border border-slate-700"
    />
  );
}
