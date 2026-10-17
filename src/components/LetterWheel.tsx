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
    const maxTicks = 16;
    let delay = 40;
    const activeTimeouts: NodeJS.Timeout[] = [];

    const schedule = (fn: () => void, ms: number) => {
      const id = setTimeout(fn, ms);
      activeTimeouts.push(id);
      return id;
    };

    // 1. Spinning wheel ticks: fast, thrilling deceleration through random letters
    const runTick = () => {
      tickCount++;
      // Pick random letter; NEVER reveal targetLetter until the final lock-in tick!
      const randomIdx = Math.floor(Math.random() * VALID_LETTERS.length);
      const chosen = tickCount >= maxTicks ? targetLetter : VALID_LETTERS[randomIdx];
      setDisplayLetter(chosen);
      soundEngine.playLetterTick();

      if (tickCount < maxTicks) {
        delay += 12;
        schedule(runTick, delay);
      } else {
        // 2. Letter locked! Switch box from red/yellow/orange/fuchsia to cyan/blue/purple/violet!
        setDisplayLetter(targetLetter);
        setPhase('REVEALED');
        soundEngine.playLetterReveal();

        // Voice announcement: "¡El juego arranca en...!"
        soundEngine.speakVoice('¡El juego arranca en...!', { rate: 1.25 });

        // Show the chosen letter for 1.3s, then start 3... 2... 1... STOP! countdown
        schedule(() => {
          // 3...
          setPhase('COUNT_3');
          soundEngine.playCountdownBeep(3);
          soundEngine.speakVoice('¡Tres!', { rate: 1.3, pitch: 1.15 });

          schedule(() => {
            // 2...
            setPhase('COUNT_2');
            soundEngine.playCountdownBeep(2);
            soundEngine.speakVoice('¡Dos!', { rate: 1.3, pitch: 1.2 });

            schedule(() => {
              // 1...
              setPhase('COUNT_1');
              soundEngine.playCountdownBeep(1);
              soundEngine.speakVoice('¡Uno!', { rate: 1.3, pitch: 1.25 });

              schedule(() => {
                // STOP! in iconic TitleScreen style!
                setPhase('COUNT_STOP');
                soundEngine.playCountdownStop();
                soundEngine.speakVoice('¡STOP!', { rate: 1.2, pitch: 1.3 });

                schedule(() => {
                  if (!hasCompletedRef.current) {
                    hasCompletedRef.current = true;
                    onSpinCompleteRef.current();
                  }
                }, 900);
              }, 850);
            }, 850);
          }, 850);
        }, 1300);
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
            <span className="inline-flex items-center gap-2 px-5 py-2 rounded-full text-xs font-mono uppercase tracking-widest bg-slate-950/90 border border-orange-500/60 text-orange-300 shadow-[0_0_20px_rgba(249,115,22,0.4)] animate-pulse">
              <span className="w-2.5 h-2.5 rounded-full bg-gradient-to-r from-red-500 to-amber-400 animate-ping" />
              Girando la ruleta de letras...
            </span>
          )}

          {phase === 'REVEALED' && (
            <span className="inline-flex items-center gap-2 px-5 py-2 rounded-full text-xs font-mono uppercase tracking-widest bg-cyan-950/90 border border-cyan-400 text-cyan-300 shadow-[0_0_25px_rgba(6,182,212,0.7)] animate-bounce">
              <span className="w-2.5 h-2.5 rounded-full bg-cyan-300" />
              ¡Letra Oficial Seleccionada!
            </span>
          )}

          {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1') && (
            <span className="inline-flex items-center gap-2 px-5 py-2 rounded-full text-xs font-mono uppercase tracking-widest bg-purple-950/90 border border-purple-500 text-purple-200 shadow-[0_0_20px_rgba(168,85,247,0.6)]">
              ¡Prepárate! Arrancando en:
            </span>
          )}

          {phase === 'COUNT_STOP' && (
            <span className="inline-flex items-center gap-2 px-6 py-2 rounded-full text-xs font-mono uppercase tracking-widest bg-rose-950/90 border border-rose-500 text-rose-300 shadow-[0_0_30px_rgba(244,63,94,0.8)] animate-pulse">
              ¡A JUGAR AHORA!
            </span>
          )}
        </div>

        {/* 
          Tumbler Box:
          1. WHILE SPINNING AT RANDOM:
             Contour has a vibrant neon gradient of ROJO, AMARILLO, NARANJA, FUCSIA
             with an intense hot neon glow!
          2. ONCE LETTER IS SELECTED (LOCKED):
             Contour transforms into the cool neon gradient:
             AZUL NEÓN, CIAN, MORADO, VIOLETA GRADIENTE!
        */}
        <div className="relative my-3 transition-all duration-500 transform">
          <div
            className={`w-44 h-44 md:w-56 md:h-56 rounded-3xl p-[5px] flex items-center justify-center transition-all duration-500 ${
              isSpinning
                ? 'bg-gradient-to-tr from-red-600 via-amber-400 via-orange-500 to-fuchsia-500 shadow-[0_0_50px_rgba(239,68,68,0.75),0_0_80px_rgba(249,115,22,0.65),0_0_110px_rgba(217,70,239,0.55),inset_0_0_25px_rgba(250,204,21,0.35)] scale-100 animate-pulse'
                : 'bg-gradient-to-br from-cyan-400 via-blue-600 via-purple-600 to-violet-500 shadow-[0_0_60px_rgba(6,182,212,0.85),0_0_100px_rgba(168,85,247,0.75),inset_0_0_30px_rgba(6,182,212,0.4)] scale-105'
            }`}
          >
            {/* Inner display plate */}
            <div
              className={`w-full h-full rounded-[21px] flex items-center justify-center relative overflow-hidden transition-all duration-500 ${
                isSpinning
                  ? 'bg-slate-950 border border-orange-400/50'
                  : 'bg-slate-950/90 border border-cyan-300/50'
              }`}
            >
              {/* Hot neon radial glow while spinning */}
              {isSpinning && (
                <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(249,115,22,0.25)_0%,rgba(217,70,239,0.2)_50%,transparent_80%)] animate-pulse" />
              )}

              {/* Cyan & purple neon radial glow once letter is selected */}
              {isLetterLocked && (
                <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(6,182,212,0.3)_0%,rgba(168,85,247,0.25)_50%,transparent_80%)] animate-pulse" />
              )}

              {/* Displayed Letter */}
              <span
                className={`font-display font-black text-8xl md:text-9xl transition-all duration-200 z-10 ${
                  isSpinning
                    ? 'text-white drop-shadow-[0_0_30px_rgba(249,115,22,0.9)] scale-95'
                    : 'text-white drop-shadow-[0_0_35px_rgba(6,182,212,1)] scale-110'
                }`}
              >
                {displayLetter}
              </span>
            </div>
          </div>

          {/* Neon Pointer Arrow indicators */}
          <div
            className={`absolute -top-4 left-1/2 -translate-x-1/2 w-0 h-0 border-x-8 border-x-transparent border-t-8 transition-colors ${
              isSpinning
                ? 'border-t-orange-500 drop-shadow-[0_0_12px_#f97316]'
                : 'border-t-cyan-400 drop-shadow-[0_0_12px_#22d3ee]'
            }`}
          />
          <div
            className={`absolute -bottom-4 left-1/2 -translate-x-1/2 w-0 h-0 border-x-8 border-x-transparent border-b-8 transition-colors ${
              isSpinning
                ? 'border-b-fuchsia-500 drop-shadow-[0_0_12px_#d946ef]'
                : 'border-b-violet-400 drop-shadow-[0_0_12px_#a855f7]'
            }`}
          />
        </div>

        {/* 
          =======================================================
          COUNTDOWN OVERLAY: 3... 2... 1... STOP!
          Only shown when countdown starts; NO miniature appears!
          =======================================================
        */}
        {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1' || phase === 'COUNT_STOP') && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-950/85 backdrop-blur-md rounded-3xl p-6 z-30 animate-in fade-in zoom-in-95 duration-200">
            {/* Countdown Numbers: 3, 2, 1 */}
            {(phase === 'COUNT_3' || phase === 'COUNT_2' || phase === 'COUNT_1') && (
              <div key={phase} className="animate-in zoom-in-50 duration-300 flex flex-col items-center">
                <span className="font-display font-black text-9xl md:text-[11rem] leading-none text-transparent bg-clip-text bg-gradient-to-b from-white via-cyan-200 to-purple-400 drop-shadow-[0_0_40px_rgba(6,182,212,0.9)]">
                  {phase === 'COUNT_3' ? '3' : phase === 'COUNT_2' ? '2' : '1'}
                </span>
                <span className="text-sm font-bold tracking-widest uppercase text-slate-300 mt-3 font-mono">
                  {phase === 'COUNT_3' ? '3...' : phase === 'COUNT_2' ? '2...' : '1...'}
                </span>
              </div>
            )}

            {/* 
              Iconic STOP! TitleScreen-style logo
            */}
            {phase === 'COUNT_STOP' && (
              <div key="stop-blast" className="w-full max-w-md animate-in zoom-in-75 duration-200 flex flex-col items-center">
                <div className="w-full drop-shadow-[0_15px_30px_rgba(244,63,94,0.85)]">
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
