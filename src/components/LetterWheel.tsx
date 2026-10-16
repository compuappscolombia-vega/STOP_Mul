import React, { useEffect, useState, useRef } from 'react';
import { soundEngine } from '../utils/audio';
import { VALID_LETTERS } from '../game/rules';

interface LetterWheelProps {
  targetLetter: string;
  onSpinComplete: () => void;
}

type RevealPhase = 'SPINNING' | 'REVEALED' | 'COUNT_3' | 'COUNT_2' | 'COUNT_1' | 'COUNT_STOP';

export const LetterWheel: React.FC<LetterWheelProps> = ({ targetLetter, onSpinComplete }) => {
  const [displayLetter, setDisplayLetter] = useState('A');
  const [phase, setPhase] = useState<RevealPhase>('SPINNING');
  const onSpinCompleteRef = useRef(onSpinComplete);
  onSpinCompleteRef.current = onSpinComplete;
  const hasCompletedRef = useRef(false);

  useEffect(() => {
    let tickCount = 0;
    const maxTicks = 12;
    let delay = 40;
    const activeTimeouts: NodeJS.Timeout[] = [];

    const schedule = (fn: () => void, ms: number) => {
      const id = setTimeout(fn, ms);
      activeTimeouts.push(id);
      return id;
    };

    // 1. Spinning wheel ticks (fast, thrilling deceleration)
    const runTick = () => {
      tickCount++;
      const randomIdx = Math.floor(Math.random() * VALID_LETTERS.length);
      const chosen = tickCount >= maxTicks ? targetLetter : VALID_LETTERS[randomIdx];
      setDisplayLetter(chosen);
      soundEngine.playLetterTick();

      if (tickCount < maxTicks) {
        delay += 14;
        schedule(runTick, delay);
      } else {
        // 2. Letter locked! Switch box to Azul Neón, Cian, Morado, Violeta Gradiente!
        setDisplayLetter(targetLetter);
        setPhase('REVEALED');
        soundEngine.playLetterReveal();

        // Reveal letter clearly for 1.2s
        schedule(() => {
          // 3. Countdown: 3...
          setPhase('COUNT_3');
          soundEngine.playCountdownBeep(3);

          schedule(() => {
            // 2...
            setPhase('COUNT_2');
            soundEngine.playCountdownBeep(2);

            schedule(() => {
              // 1...
              setPhase('COUNT_1');
              soundEngine.playCountdownBeep(1);

              schedule(() => {
                // STOP! in iconic TitleScreen style!
                setPhase('COUNT_STOP');
                soundEngine.playCountdownStop();

                schedule(() => {
                  if (!hasCompletedRef.current) {
                    hasCompletedRef.current = true;
                    onSpinCompleteRef.current();
                  }
                }, 850);
              }, 750);
            }, 750);
          }, 750);
        }, 1200);
      }
    };

    schedule(runTick, 50);

    return () => {
      activeTimeouts.forEach(clearTimeout);
    };
  }, [targetLetter]);

  const isSpinning = phase === 'SPINNING';
  const isLetterLocked = phase !== 'SPINNING';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/90 backdrop-blur-md p-4 select-none">
      <div className="flex flex-col items-center max-w-lg w-full text-center relative">
        
        {/* Status header banner */}
        <div className="mb-4">
          {isSpinning && (
            <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-mono uppercase tracking-widest bg-slate-900 border border-slate-700 text-slate-300 animate-pulse">
              <span className="w-2 h-2 rounded-full bg-cyan-400 animate-ping" />
              Girando la ruleta de letras...
            </span>
          )}

          {phase === 'REVEALED' && (
            <span className="inline-flex items-center gap-2 px-5 py-2 rounded-full text-xs font-mono uppercase tracking-widest bg-cyan-950/80 border border-cyan-400 text-cyan-300 shadow-[0_0_20px_rgba(6,182,212,0.6)] animate-bounce">
              <span className="w-2 h-2 rounded-full bg-cyan-300" />
              ¡Letra Oficial Seleccionada!
            </span>
          )}

          {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1') && (
            <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-xs font-mono uppercase tracking-widest bg-purple-950/80 border border-purple-500 text-purple-200 shadow-[0_0_15px_rgba(168,85,247,0.5)]">
              ¡Prepárate! La ronda comienza en:
            </span>
          )}

          {phase === 'COUNT_STOP' && (
            <span className="inline-flex items-center gap-2 px-5 py-1.5 rounded-full text-xs font-mono uppercase tracking-widest bg-rose-950/80 border border-rose-500 text-rose-300 shadow-[0_0_25px_rgba(244,63,94,0.7)] animate-pulse">
              ¡A JUGAR AHORA!
            </span>
          )}
        </div>

        {/* 
          Tumbler Box:
          When spinning: neutral sleek border.
          When letter is given: changes to vibrant AZUL NEÓN, CIAN, MORADO, VIOLETA GRADIENTE!
        */}
        <div className="relative my-2 transition-all duration-500 transform">
          <div
            className={`w-40 h-40 md:w-52 md:h-52 rounded-3xl p-[4px] flex items-center justify-center transition-all duration-700 ${
              isLetterLocked
                ? 'bg-gradient-to-br from-cyan-400 via-blue-600 via-purple-600 to-violet-500 shadow-[0_0_60px_rgba(6,182,212,0.8),0_0_100px_rgba(168,85,247,0.7),inset_0_0_30px_rgba(6,182,212,0.4)] scale-105'
                : 'bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-[0_0_40px_rgba(0,0,0,0.5)] scale-95'
            }`}
          >
            {/* Inner display plate */}
            <div
              className={`w-full h-full rounded-[22px] flex items-center justify-center relative overflow-hidden transition-all duration-700 ${
                isLetterLocked
                  ? 'bg-slate-950/90 border border-cyan-300/40'
                  : 'bg-slate-900 border border-white/10'
              }`}
            >
              {/* Subtle ambient neon glow inside plate */}
              {isLetterLocked && (
                <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(6,182,212,0.25)_0%,rgba(168,85,247,0.2)_50%,transparent_80%)] animate-pulse" />
              )}

              {/* Displayed Letter */}
              <span
                className={`font-display font-black text-8xl md:text-9xl transition-all duration-300 z-10 ${
                  isLetterLocked
                    ? 'text-white drop-shadow-[0_0_35px_rgba(6,182,212,1)] scale-110'
                    : 'text-slate-300 scale-90 rotate-3'
                }`}
              >
                {displayLetter}
              </span>
            </div>
          </div>

          {/* Neon Pointer Arrow indicators */}
          <div
            className={`absolute -top-3.5 left-1/2 -translate-x-1/2 w-0 h-0 border-x-8 border-x-transparent border-t-8 transition-colors ${
              isLetterLocked ? 'border-t-cyan-400 drop-shadow-[0_0_10px_#22d3ee]' : 'border-t-slate-500'
            }`}
          />
          <div
            className={`absolute -bottom-3.5 left-1/2 -translate-x-1/2 w-0 h-0 border-x-8 border-x-transparent border-b-8 transition-colors ${
              isLetterLocked ? 'border-b-violet-400 drop-shadow-[0_0_10px_#a855f7]' : 'border-b-slate-500'
            }`}
          />
        </div>

        {/* Informational Subtext */}
        <p className="text-slate-300 text-sm md:text-base mt-4 font-medium">
          Todas las palabras de esta ronda deben comenzar con la letra{' '}
          <span className="font-extrabold text-white text-lg bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-400 text-cyan-200">
            "{targetLetter.toUpperCase()}"
          </span>
        </p>

        {/* 
          =======================================================
          COUNTDOWN OVERLAY: 3... 2... 1... STOP!
          Uses the EXACT SVG typography & style of the title screen!
          =======================================================
        */}
        {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1' || phase === 'COUNT_STOP') && (
          <div className="absolute inset-0 -top-8 flex flex-col items-center justify-center bg-slate-950/85 backdrop-blur-md rounded-3xl p-6 z-30 animate-in fade-in zoom-in-95 duration-200">
            {/* Target Letter Reminder in small glowing pill */}
            <div className="mb-4 inline-flex items-center gap-2 px-3 py-1 rounded-xl bg-gradient-to-r from-cyan-500/20 via-blue-500/20 to-purple-500/20 border border-cyan-400/40 text-xs font-mono text-cyan-200">
              <span>Letra activa:</span>
              <span className="font-black text-sm text-white px-1.5 py-0.2 rounded bg-cyan-500/30">
                {targetLetter.toUpperCase()}
              </span>
            </div>

            {/* Countdown Numbers: 3, 2, 1 */}
            {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1') && (
              <div key={phase} className="animate-in zoom-in-50 duration-300 flex flex-col items-center">
                <span className="font-display font-black text-9xl md:text-[11rem] leading-none text-transparent bg-clip-text bg-gradient-to-b from-white via-cyan-200 to-purple-400 drop-shadow-[0_0_40px_rgba(6,182,212,0.9)]">
                  {phase === 'COUNT_3' ? '3' : phase === 'COUNT_2' ? '2' : '1'}
                </span>
                <span className="text-sm font-bold tracking-widest uppercase text-slate-300 mt-2">
                  {phase === 'COUNT_3' ? 'Preparados...' : phase === 'COUNT_2' ? 'Listos...' : '¡Atentos!'}
                </span>
              </div>
            )}

            {/* 
              Iconic STOP! - Same style as TitleScreen 
              Bespoke SVG Lockup with #stopGrad and #goldGrad
            */}
            {phase === 'COUNT_STOP' && (
              <div key="stop-blast" className="w-full max-w-md animate-in zoom-in-75 duration-200 flex flex-col items-center">
                <div className="w-full drop-shadow-[0_15px_30px_rgba(244,63,94,0.8)]">
                  <svg viewBox="0 0 500 110" className="w-full h-auto">
                    <defs>
                      <linearGradient id="stopCountdownGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#ffffff" />
                        <stop offset="45%" stopColor="#f43f5e" />
                        <stop offset="100%" stopColor="#e11d48" />
                      </linearGradient>
                      <linearGradient id="goldCountdownGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stopColor="#fbbf24" />
                        <stop offset="100%" stopColor="#f59e0b" />
                      </linearGradient>
                    </defs>
                    {/* Dark shadow outline */}
                    <text
                      x="250"
                      y="78"
                      textAnchor="middle"
                      className="font-display"
                      fontWeight="900"
                      fontSize="84"
                      letterSpacing="6"
                      fill="#020617"
                      stroke="#0f172a"
                      strokeWidth="14"
                    >
                      STOP!
                    </text>
                    {/* Main gradient fill */}
                    <text
                      x="250"
                      y="78"
                      textAnchor="middle"
                      className="font-display"
                      fontWeight="900"
                      fontSize="84"
                      letterSpacing="6"
                      fill="url(#stopCountdownGrad)"
                    >
                      STOP!
                    </text>
                    <text
                      x="250"
                      y="102"
                      textAnchor="middle"
                      fontFamily="Plus Jakarta Sans, sans-serif"
                      fontWeight="700"
                      fontSize="14"
                      letterSpacing="8"
                      fill="url(#goldCountdownGrad)"
                    >
                      ¡A JUGAR AHORA!
                    </text>
                  </svg>
                </div>
              </div>
            )}
          </div>
        )}

      </div>
    </div>
  );
};
