import React from 'react';
import { Volume2, VolumeX, Pause } from 'lucide-react';
import { soundEngine } from '../utils/audio';

interface GameHUDProps {
  currentLetter: string;
  currentRound: number;
  totalRounds: number;
  timeRemainingSec: number;
  totalTimeSec: number;
  currentScore: number;
  isMuted: boolean;
  onToggleMute: () => void;
  onPause: () => void;
  isPractice?: boolean;
  botSpeedSec?: number;
  onBotSpeedChange?: (speed: number) => void;
}

export const GameHUD: React.FC<GameHUDProps> = ({
  currentLetter,
  currentRound,
  totalRounds,
  timeRemainingSec,
  totalTimeSec,
  currentScore,
  isMuted,
  onToggleMute,
  onPause,
  isPractice = false,
  botSpeedSec,
  onBotSpeedChange
}) => {
  const isUrgent = timeRemainingSec <= 10;
  const progressPct = Math.max(0, Math.min(100, (timeRemainingSec / totalTimeSec) * 100));

  return (
    <header className="w-full flex items-center justify-between gap-4 py-3 px-4 md:px-8 border-b border-slate-900 bg-slate-950/80 sticky top-0 z-40">
      {/* Cluster 1: Letter & Round */}
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-400 via-blue-600 via-purple-600 to-violet-500 p-[2.5px] shadow-[0_0_20px_rgba(6,182,212,0.5),0_0_25px_rgba(168,85,247,0.4)]">
          <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center border border-cyan-400/30">
            <span className="font-display font-black text-2xl text-white drop-shadow-[0_0_10px_rgba(6,182,212,0.8)]">
              {currentLetter.toUpperCase()}
            </span>
          </div>
        </div>

        <div>
          <div className="text-xs text-slate-400 font-medium">
            Round {currentRound} of {totalRounds}
          </div>
          <div className="text-xs font-mono text-cyan-300">
            Letra <span className="font-bold text-white text-sm bg-cyan-950/60 px-1.5 py-0.2 rounded border border-cyan-400/40">'{currentLetter.toUpperCase()}'</span>
          </div>
        </div>

        {isPractice && botSpeedSec !== undefined && (
          <button
            type="button"
            onClick={() => {
              if (onBotSpeedChange) {
                // Cycle from 1 to 6
                const nextSpeed = botSpeedSec >= 6 ? 1 : botSpeedSec + 1;
                soundEngine.playKeyType();
                onBotSpeedChange(nextSpeed);
              }
            }}
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-slate-900 border border-slate-800 hover:border-amber-500/50 text-[11px] text-slate-300 transition-colors shadow-sm"
            title="Clic para cambiar velocidad del bot (1s a 6s)"
          >
            <span className="text-slate-400 font-medium">Bot:</span>
            <span className="text-amber-400 font-mono font-bold">{botSpeedSec}s/campo</span>
            <span className="text-[10px] text-slate-500 font-mono">↻</span>
          </button>
        )}
      </div>

      {/* Cluster 2: Timer center */}
      <div className="flex flex-col items-center">
        <div
          className={`font-mono font-bold text-2xl md:text-3xl tabular-nums tracking-tight transition-colors ${
            isUrgent ? 'text-rose-500 animate-pulse' : 'text-slate-100'
          }`}
        >
          {timeRemainingSec}s
        </div>
        <div className="w-32 md:w-44 h-1.5 bg-slate-800 rounded-full overflow-hidden mt-1">
          <div
            className={`h-full transition-all duration-300 ${
              isUrgent ? 'bg-rose-500' : 'bg-gradient-to-r from-amber-400 to-rose-500'
            }`}
            style={{ width: `${progressPct}%` }}
          />
        </div>
      </div>

      {/* Cluster 3: Score & Controls */}
      <div className="flex items-center gap-4">
        <div className="text-right">
          <div className="text-[11px] text-slate-400 font-medium uppercase tracking-wider">Score</div>
          <div className="font-mono font-bold text-lg md:text-xl text-amber-400 tabular-nums">
            {currentScore} pts
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Dedicated Quick Mute/Unmute Control for intense rounds */}
          <button
            type="button"
            onClick={() => {
              onToggleMute();
              if (isMuted) {
                setTimeout(() => soundEngine.playLetterReveal(), 50);
              }
            }}
            className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold border transition-all active:scale-95 cursor-pointer shadow-sm select-none ${
              isMuted
                ? 'bg-rose-950/70 border-rose-500/50 text-rose-300 hover:bg-rose-900/80 shadow-[0_0_12px_rgba(244,63,94,0.25)]'
                : 'bg-emerald-950/60 border-emerald-500/40 text-emerald-300 hover:bg-emerald-900/70 shadow-[0_0_12px_rgba(16,185,129,0.2)]'
            }`}
            title={isMuted ? 'Activar sonido [Tecla M]' : 'Silenciar sonido [Tecla M]'}
            aria-label={isMuted ? 'Activar sonido' : 'Silenciar sonido'}
            aria-pressed={isMuted}
          >
            {isMuted ? (
              <>
                <VolumeX className="w-4 h-4 text-rose-400 shrink-0" />
                <span className="hidden sm:inline font-bold">Mudo</span>
                <span className="w-2 h-2 rounded-full bg-rose-500 shrink-0" />
              </>
            ) : (
              <>
                <Volume2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span className="hidden sm:inline font-bold">Audio</span>
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse shrink-0" />
              </>
            )}
            <kbd className="hidden md:inline-block text-[10px] px-1.5 py-0.5 rounded font-mono bg-black/40 border border-white/10 text-slate-300">
              M
            </kbd>
          </button>

          <button
            onClick={onPause}
            className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white hover:border-slate-700 transition-colors active:scale-95 cursor-pointer"
            title="Pausar juego [Esc]"
            aria-label="Pausar juego"
          >
            <Pause className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
