import React, { useRef, useEffect } from 'react';
import { Category, matchesStartingLetter } from '../game/rules';
import { soundEngine } from '../utils/audio';
import { Check, AlertCircle, Sparkles } from 'lucide-react';

interface GameBoardProps {
  categories: Category[];
  targetLetter: string;
  answers: Record<string, string>;
  onAnswerChange: (categoryId: string, value: string) => void;
  onCallStop: () => void;
  isStopDisabled?: boolean;
  activePlayers?: Array<{
    id: string;
    name: string;
    avatar: string;
    hasStopped: boolean;
    filledCount?: number;
    totalCategories?: number;
    statusText?: string;
    isHuman?: boolean;
  }>;
  botSpeedSec?: number;
  onBotSpeedChange?: (speed: number) => void;
}

export const GameBoard: React.FC<GameBoardProps> = ({
  categories,
  targetLetter,
  answers,
  onAnswerChange,
  onCallStop,
  isStopDisabled = false,
  activePlayers = [],
  botSpeedSec = 4,
  onBotSpeedChange
}) => {
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  // Auto-focus first input when board loads
  useEffect(() => {
    if (categories.length > 0 && inputRefs.current[categories[0].id]) {
      inputRefs.current[categories[0].id]?.focus();
    }
  }, [categories]);

  const filledCount = categories.filter(c => (answers[c.id] || '').trim().length > 0).length;
  const canStop = filledCount >= 1 && !isStopDisabled;

  const handleKeyDown = (e: React.KeyboardEvent, index: number) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      soundEngine.playKeyType();
      if (index < categories.length - 1) {
        inputRefs.current[categories[index + 1].id]?.focus();
      } else {
        // Cycle back to first empty category if any, or blur
        const firstEmpty = categories.find(c => !(answers[c.id] || '').trim());
        if (firstEmpty) {
          inputRefs.current[firstEmpty.id]?.focus();
        } else {
          (e.target as HTMLElement).blur();
        }
      }
    }
  };

  return (
    <div className="w-full max-w-4xl mx-auto px-4 py-4 md:py-6 flex flex-col gap-5">
      {/* Competitors live progress strip with visual indicators */}
      {activePlayers.length > 0 && (
        <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 text-xs shadow-lg space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <span className="text-slate-400 font-semibold flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              <span>Competidores en Tiempo Real:</span>
            </span>

            {/* Live speed adjustment directly in-game */}
            {onBotSpeedChange && (
              <div className="flex items-center gap-1.5 bg-slate-950/80 px-2.5 py-1 rounded-xl border border-slate-800 text-[11px]">
                <span className="text-slate-400 font-medium">Velocidad Máquina:</span>
                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5, 6].map(sec => (
                    <button
                      key={sec}
                      type="button"
                      onClick={() => {
                        soundEngine.playKeyType();
                        onBotSpeedChange(sec);
                      }}
                      className={`px-1.5 py-0.5 rounded font-mono font-bold text-[10px] transition-all ${
                        botSpeedSec === sec
                          ? 'bg-amber-500 text-slate-950 shadow-sm'
                          : 'bg-slate-900 text-slate-400 hover:text-white'
                      }`}
                      title={`${sec} segundos por campo`}
                    >
                      {sec}s
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Player Cards Strip */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            {activePlayers.map(p => {
              const total = p.totalCategories || categories.length;
              const count = p.isHuman ? filledCount : (p.filledCount ?? 0);
              const pct = Math.min(100, Math.round((count / Math.max(1, total)) * 100));

              return (
                <div
                  key={p.id}
                  className={`p-2.5 rounded-xl border transition-all ${
                    p.hasStopped
                      ? 'bg-rose-950/40 border-rose-500/60 shadow-[0_0_15px_rgba(244,63,94,0.2)]'
                      : p.isHuman
                      ? 'bg-slate-950/80 border-slate-700'
                      : 'bg-slate-950/50 border-slate-800/80'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-2">
                      <span className="text-base">{p.avatar}</span>
                      <span className="font-bold text-white text-xs truncate max-w-[110px]">
                        {p.name}
                      </span>
                    </div>
                    <span className="font-mono font-semibold text-[11px] text-slate-300">
                      {count}/{total}
                    </span>
                  </div>

                  {/* Progress bar */}
                  <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden mb-1.5">
                    <div
                      className={`h-full transition-all duration-300 ${
                        p.hasStopped
                          ? 'bg-rose-500'
                          : p.isHuman
                          ? 'bg-amber-400'
                          : 'bg-indigo-400'
                      }`}
                      style={{ width: `${pct}%` }}
                    />
                  </div>

                  <div className="flex items-center justify-between text-[10px]">
                    <span className="text-slate-400 truncate max-w-[140px] font-mono">
                      {p.hasStopped ? '¡Gritó STOP!' : (p.statusText || `${count} listos`)}
                    </span>
                    {p.hasStopped && (
                      <span className="px-1.5 py-0.2 rounded bg-rose-500 text-white font-extrabold uppercase tracking-wide">
                        STOP
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Categories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
        {categories.map((category, idx) => {
          const val = answers[category.id] || '';
          const hasInput = val.trim().length > 0;
          const letterMatch = hasInput ? matchesStartingLetter(val, targetLetter) : false;

          return (
            <div
              key={category.id}
              className={`p-3.5 rounded-2xl border transition-all ${
                letterMatch
                  ? 'bg-slate-900/90 border-emerald-500/40 shadow-sm'
                  : hasInput
                  ? 'bg-slate-900/90 border-amber-500/40'
                  : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
              }`}
            >
              <div className="flex items-center justify-between mb-1.5">
                <div className="flex items-center gap-2">
                  <span className="text-lg" role="img" aria-label={category.name}>
                    {category.icon}
                  </span>
                  <label htmlFor={`input-${category.id}`} className="font-display font-bold text-sm text-slate-200">
                    {category.name}
                  </label>
                </div>

                <div className="flex items-center gap-1 text-xs">
                  {hasInput && letterMatch && (
                    <span className="flex items-center gap-1 text-emerald-400 font-mono text-[11px]">
                      <Check className="w-3.5 h-3.5" />
                      Starts with {targetLetter.toUpperCase()}
                    </span>
                  )}
                  {hasInput && !letterMatch && (
                    <span className="flex items-center gap-1 text-amber-400 font-mono text-[11px]">
                      <AlertCircle className="w-3.5 h-3.5" />
                      Needs '{targetLetter.toUpperCase()}'
                    </span>
                  )}
                </div>
              </div>

              <div className="relative flex items-center">
                <span className="absolute left-3.5 font-display font-black text-rose-400 text-base pointer-events-none select-none">
                  {targetLetter.toUpperCase()}
                </span>
                <input
                  id={`input-${category.id}`}
                  ref={el => {
                    inputRefs.current[category.id] = el;
                  }}
                  type="text"
                  value={val}
                  onChange={e => {
                    soundEngine.playKeyType();
                    onAnswerChange(category.id, e.target.value);
                  }}
                  onKeyDown={e => handleKeyDown(e, idx)}
                  placeholder={category.placeholder}
                  autoComplete="off"
                  autoCorrect="off"
                  spellCheck="false"
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-600 focus:outline-none focus:border-rose-500 text-sm font-medium transition-colors"
                />
              </div>
            </div>
          );
        })}
      </div>

      {/* The Iconic STOP Slap Button */}
      <div className="flex flex-col items-center justify-center pt-2 pb-4">
        <button
          onClick={() => {
            if (canStop) {
              soundEngine.playStopAlarm();
              onCallStop();
            }
          }}
          disabled={!canStop}
          className={`group relative w-full max-w-md py-5 px-8 rounded-2xl font-display font-black text-2xl md:text-3xl tracking-wider transition-all duration-150 transform ${
            canStop
              ? 'bg-gradient-to-r from-rose-600 via-rose-500 to-red-600 text-white shadow-[0_12px_40px_rgba(244,63,94,0.5)] hover:scale-[1.02] active:scale-[0.96] active:translate-y-1 cursor-pointer border-t-2 border-white/30'
              : 'bg-slate-800 text-slate-500 border border-slate-700/60 cursor-not-allowed opacity-70'
          }`}
        >
          <div className="flex items-center justify-center gap-3">
            <span className="drop-shadow-md">🛑 STOP!</span>
            <span className="text-xs font-mono font-normal tracking-normal px-2.5 py-1 rounded bg-black/20 text-white/90">
              {filledCount}/{categories.length} Ready
            </span>
          </div>
          {canStop && (
            <div className="text-[11px] font-sans font-medium text-rose-100/90 mt-1 uppercase tracking-widest">
              Slap to finish round & lock all answers!
            </div>
          )}
        </button>
      </div>
    </div>
  );
};
