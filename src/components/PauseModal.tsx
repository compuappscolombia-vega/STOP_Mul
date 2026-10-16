import React from 'react';
import { soundEngine } from '../utils/audio';
import { Play, Volume2, VolumeX, RotateCcw, Gauge } from 'lucide-react';

interface PauseModalProps {
  onResume: () => void;
  onQuit: () => void;
  isMuted: boolean;
  onToggleMute: () => void;
  botSpeedSec?: number;
  onBotSpeedChange?: (speed: number) => void;
}

export const PauseModal: React.FC<PauseModalProps> = ({
  onResume,
  onQuit,
  isMuted,
  onToggleMute,
  botSpeedSec,
  onBotSpeedChange
}) => {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-sm p-4">
      <div className="relative w-full max-w-sm bg-slate-900 border border-slate-800 rounded-3xl p-6 text-center shadow-2xl">
        <h2 className="font-display font-black text-2xl text-white mb-2">GAME PAUSED</h2>
        <p className="text-xs text-slate-400 mb-5 font-mono">Timer is frozen · Take a breather</p>

        {/* Bot Speed Live Tuning */}
        {botSpeedSec !== undefined && onBotSpeedChange && (
          <div className="mb-5 p-3.5 rounded-2xl bg-slate-950 border border-slate-800 text-left space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-300 font-semibold flex items-center gap-1.5">
                <Gauge className="w-3.5 h-3.5 text-amber-400" />
                <span>Velocidad del Bot:</span>
              </span>
              <span className="font-mono font-bold text-amber-400 bg-amber-950/60 border border-amber-800/40 px-2 py-0.5 rounded">
                {botSpeedSec} seg / campo
              </span>
            </div>

            <div className="grid grid-cols-6 gap-1">
              {[1, 2, 3, 4, 5, 6].map(sec => (
                <button
                  key={sec}
                  type="button"
                  onClick={() => {
                    soundEngine.playKeyType();
                    onBotSpeedChange(sec);
                  }}
                  className={`py-1 rounded font-mono font-bold text-xs transition-colors ${
                    botSpeedSec === sec
                      ? 'bg-amber-500 text-slate-950 shadow-sm'
                      : 'bg-slate-900 text-slate-400 hover:text-white'
                  }`}
                >
                  {sec}s
                </button>
              ))}
            </div>
            <div className="text-[10px] text-slate-500 text-center">
              {botSpeedSec >= 4 ? '👤 Ritmo Humano Relajado' : '⚡ Ritmo Competitivo Rápido'}
            </div>
          </div>
        )}

        <div className="space-y-2.5">
          <button
            onClick={() => {
              soundEngine.playKeyType();
              onResume();
            }}
            className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-sm shadow transition-colors"
          >
            <Play className="w-4 h-4 fill-white" />
            <span>Resume Game</span>
          </button>

          <button
            onClick={onToggleMute}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold transition-colors"
          >
            {isMuted ? <VolumeX className="w-4 h-4 text-rose-400" /> : <Volume2 className="w-4 h-4 text-emerald-400" />}
            <span>{isMuted ? 'Unmute Sound' : 'Mute Sound'}</span>
          </button>

          <button
            onClick={() => {
              soundEngine.playKeyType();
              onQuit();
            }}
            className="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-400 hover:text-white text-xs font-semibold transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Quit to Title Screen</span>
          </button>
        </div>
      </div>
    </div>
  );
};
