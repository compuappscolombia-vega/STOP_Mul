import React, { useEffect, useState } from 'react';
import { UserProfile, AuthUser } from '../types/game';
import { soundEngine } from '../utils/audio';
import { getPlayerTier } from '../game/rules';
import { Trophy, Users, Zap, Calendar, User, Volume2, VolumeX, Shield, Play, LogIn, Gauge, Download } from 'lucide-react';

interface TitleScreenProps {
  profile: UserProfile;
  currentUser: AuthUser | null;
  onStartOffline: (difficulty: 'easy' | 'medium' | 'hard', botSpeedSec: number) => void;
  onOpenMultiplayer: () => void;
  onOpenDaily: () => void;
  onOpenLeaderboards: () => void;
  onOpenProfile: () => void;
  onOpenAuth: () => void;
  onOpenApk: () => void;
  onToggleMute: () => void;
  isMuted: boolean;
}

export const TitleScreen: React.FC<TitleScreenProps> = ({
  profile,
  currentUser,
  onStartOffline,
  onOpenMultiplayer,
  onOpenDaily,
  onOpenLeaderboards,
  onOpenProfile,
  onOpenAuth,
  onOpenApk,
  onToggleMute,
  isMuted
}) => {
  const [selectedDifficulty, setSelectedDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');
  const [botSpeedSec, setBotSpeedSec] = useState<number>(() => {
    try {
      return parseInt(localStorage.getItem('stop_game_bot_speed') || '4', 10) || 4;
    } catch {
      return 4;
    }
  }); // Default 4 seconds per field as requested (human pace)
  const [tickerLetter, setTickerLetter] = useState('S');
  const tier = getPlayerTier(profile.stats.totalPoints);

  // Animated letter carousel for title centerpiece
  useEffect(() => {
    const letters = ['S', 'T', 'O', 'P', '!', 'A', 'M', 'R', 'C', 'L', 'B'];
    let idx = 0;
    const interval = setInterval(() => {
      idx = (idx + 1) % letters.length;
      setTickerLetter(letters[idx]);
    }, 1800);
    return () => clearInterval(interval);
  }, []);

  // Keyboard shortcut listener: Enter starts offline practice, Space quick plays, M toggles mute
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const activeEl = document.activeElement;
      if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
        return;
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        soundEngine.playLetterReveal();
        onStartOffline(selectedDifficulty, botSpeedSec);
      } else if (e.key === 'm' || e.key === 'M') {
        e.preventDefault();
        onToggleMute();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onStartOffline, selectedDifficulty, onToggleMute]);

  return (
    <div className="relative min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col justify-between p-4 md:p-8 select-none overflow-hidden">
      {/* Background radial atmosphere */}
      <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(ellipse_80%_80%_at_50%_-20%,rgba(244,63,94,0.18),rgba(255,255,255,0))]" />
      <div className="absolute -top-32 -left-32 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-32 -right-32 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Top Perimeter Cluster */}
      <div className="relative z-10 flex items-center justify-between w-full max-w-6xl mx-auto flex-wrap gap-3">
        {/* Profile Quick Pill */}
        <button
          onClick={() => {
            soundEngine.playKeyType();
            onOpenProfile();
          }}
          className="flex items-center gap-3 px-3.5 py-2 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/80 transition-all text-left group"
          title="Open Profile & Cross-Device Sync"
        >
          <div className="w-9 h-9 rounded-lg bg-rose-500/20 border border-rose-500/40 flex items-center justify-center text-lg shadow-inner">
            {profile.avatar}
          </div>
          <div>
            <div className="text-sm font-bold text-white group-hover:text-rose-400 transition-colors flex items-center gap-1.5">
              <span>{profile.name}</span>
              <span className="text-[10px] px-1.5 py-0.2 rounded font-mono text-rose-300 bg-rose-950/60 border border-rose-800/40">
                {profile.syncCode}
              </span>
            </div>
            <div className="text-xs text-slate-400 flex items-center gap-2">
              <span style={{ color: tier.color }} className="font-medium">{tier.rankTitle}</span>
              <span>·</span>
              <span className="font-mono tabular-nums text-slate-300">{profile.stats.totalPoints} pts</span>
            </div>
          </div>
        </button>

        {/* Utility controls and Session Sign In */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Sign In / Register Account Button */}
          <button
            onClick={() => {
              soundEngine.playKeyType();
              onOpenAuth();
            }}
            className={`flex items-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold transition-all ${
              currentUser
                ? 'bg-slate-900/80 border border-emerald-500/30 text-emerald-300 hover:bg-emerald-500/10'
                : 'bg-rose-600/20 border border-rose-500/40 text-rose-300 hover:bg-rose-500/30'
            }`}
            title={currentUser ? `Session: ${currentUser.email}` : 'Sign In with Google or Register with Email'}
          >
            {currentUser ? <Shield className="w-4 h-4 text-emerald-400" /> : <LogIn className="w-4 h-4 text-rose-400" />}
            <span>{currentUser ? 'Session Account' : 'Sign In / Register'}</span>
          </button>

          <button
            onClick={() => {
              soundEngine.playKeyType();
              onOpenDaily();
            }}
            className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-900/80 border border-amber-500/30 text-amber-300 hover:bg-amber-500/10 transition-all text-xs font-semibold"
          >
            <Calendar className="w-4 h-4 text-amber-400" />
            <span className="hidden sm:inline">Daily Challenge</span>
          </button>

          <button
            onClick={() => {
              soundEngine.playKeyType();
              onOpenLeaderboards();
            }}
            className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300 hover:border-slate-700 hover:bg-slate-800/80 transition-all text-xs font-semibold"
            title="Global & Weekly Leaderboards"
          >
            <Trophy className="w-4 h-4 text-amber-400" />
            <span className="hidden sm:inline">Rankings</span>
          </button>

          {/* Download APK / Install PWA / PC Web Button */}
          <button
            onClick={() => {
              soundEngine.playKeyType();
              onOpenApk();
            }}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-gradient-to-r from-cyan-950/80 to-purple-950/80 border border-cyan-400/40 text-cyan-300 hover:border-cyan-400 hover:text-white transition-all text-xs font-semibold shadow-sm"
            title="Descargar APK para Android o Instalar en PC"
          >
            <Download className="w-4 h-4 text-cyan-400" />
            <span>APK / Instalar</span>
          </button>

          <button
            onClick={onToggleMute}
            className="p-2.5 rounded-xl bg-slate-900/80 border border-slate-800 text-slate-300 hover:border-slate-700 hover:bg-slate-800 transition-all"
            title={isMuted ? 'Unmute Sound [M]' : 'Mute Sound [M]'}
            aria-label={isMuted ? 'Unmute Sound' : 'Mute Sound'}
          >
            {isMuted ? <VolumeX className="w-4 h-4 text-rose-400" /> : <Volume2 className="w-4 h-4 text-emerald-400" />}
          </button>
        </div>
      </div>

      {/* Center Stage & Bespoke SVG Title Lockup */}
      <div className="relative z-10 flex flex-col items-center justify-center my-auto py-8 text-center max-w-4xl mx-auto">
        {/* Animated letter badge emblem in azul neon, cian, morado, violeta gradiente */}
        <div className="relative mb-6">
          <div className="w-24 h-24 md:w-28 md:h-28 rounded-3xl bg-gradient-to-br from-cyan-400 via-blue-600 via-purple-600 to-violet-500 p-[3.5px] shadow-[0_0_50px_rgba(6,182,212,0.6),0_0_60px_rgba(168,85,247,0.5)] transform transition-transform hover:scale-105">
            <div className="w-full h-full bg-slate-950 rounded-[22px] flex items-center justify-center border border-cyan-400/30">
              <span className="font-display font-black text-5xl md:text-6xl text-white tracking-wider animate-pulse-subtle drop-shadow-[0_0_15px_rgba(6,182,212,0.8)]">
                {tickerLetter}
              </span>
            </div>
          </div>
          <div className="absolute -bottom-2 -right-2 px-2 py-0.5 rounded-md bg-cyan-500 text-slate-950 text-[11px] font-black uppercase tracking-wider shadow">
            Online
          </div>
        </div>

        {/* Bespoke SVG Title Lockup */}
        <div className="w-full max-w-lg mb-3">
          <svg viewBox="0 0 500 110" className="w-full h-auto drop-shadow-[0_10px_20px_rgba(0,0,0,0.6)]">
            <defs>
              <linearGradient id="stopGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#ffffff" />
                <stop offset="45%" stopColor="#f43f5e" />
                <stop offset="100%" stopColor="#e11d48" />
              </linearGradient>
              <linearGradient id="goldGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#fbbf24" />
                <stop offset="100%" stopColor="#f59e0b" />
              </linearGradient>
            </defs>
            {/* Background shadow stroke */}
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
              fill="url(#stopGrad)"
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
              fill="url(#goldGrad)"
            >
              MULTIPLAYER CATEGORY RUSH
            </text>
          </svg>
        </div>

        <p className="text-slate-400 text-sm md:text-base max-w-lg mb-8 leading-relaxed">
          Spin the alphabet, brainstorm fast categories under pressure, and slap <span className="text-rose-400 font-bold">STOP!</span> before your rivals.
        </p>

        {/* Primary Start & Mode Selection */}
        <div className="w-full max-w-md space-y-3">
          {/* Main Enter Start CTA */}
          <button
            data-start="true"
            onClick={() => {
              soundEngine.playLetterReveal();
              onStartOffline(selectedDifficulty, botSpeedSec);
            }}
            className="w-full group relative flex items-center justify-center gap-3 py-4 px-6 rounded-2xl bg-gradient-to-r from-rose-600 via-rose-500 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white font-display font-extrabold text-lg shadow-[0_10px_30px_rgba(244,63,94,0.4)] active:scale-[0.98] transition-all"
          >
            <Play className="w-6 h-6 fill-white" />
            <span>START PRACTICE SPRINT</span>
            <span className="hidden sm:inline-block ml-2 text-xs font-mono bg-white/20 px-2 py-0.5 rounded text-white/90">
              [Enter]
            </span>
          </button>

          {/* Bot Processing Speed Selector (1s to 6s per field) */}
          <div className="p-3.5 rounded-2xl bg-slate-900/90 border border-slate-800 text-xs space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-slate-300 font-semibold flex items-center gap-1.5">
                <Gauge className="w-3.5 h-3.5 text-amber-400" />
                <span>Velocidad de Respuesta del Bot:</span>
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
                    setBotSpeedSec(sec);
                    try {
                      localStorage.setItem('stop_game_bot_speed', sec.toString());
                    } catch {}
                  }}
                  className={`py-1.5 rounded-lg font-mono font-bold text-xs transition-colors ${
                    botSpeedSec === sec
                      ? 'bg-amber-500 text-slate-950 shadow-sm'
                      : 'bg-slate-950 text-slate-400 hover:text-white border border-slate-800/60'
                  }`}
                  title={`${sec} segundos por campo`}
                >
                  {sec}s
                </button>
              ))}
            </div>

            <div className="text-[11px] text-slate-400 text-center font-medium pt-0.5">
              {botSpeedSec === 1 && '⚡ 1s · Máquina Flash (Extremo)'}
              {botSpeedSec === 2 && '🏎️ 2s · Rápido (Alta Presión)'}
              {botSpeedSec === 3 && '🏃 3s · Ágil (Competitivo)'}
              {botSpeedSec === 4 && '👤 4s · Humano Normal (Equilibrado - Recomendado)'}
              {botSpeedSec === 5 && '☕ 5s · Pensativo (Relajado para pensar)'}
              {botSpeedSec === 6 && '🧘 6s · Sin Prisa (Ideal para principiantes)'}
            </div>
          </div>

          {/* Difficulty selector for practice */}
          <div className="flex items-center justify-between p-1.5 rounded-xl bg-slate-900/90 border border-slate-800 text-xs">
            <span className="text-slate-400 px-3 font-medium">Nivel de Bot:</span>
            <div className="flex items-center gap-1">
              {(['easy', 'medium', 'hard'] as const).map(diff => (
                <button
                  key={diff}
                  onClick={() => {
                    soundEngine.playKeyType();
                    setSelectedDifficulty(diff);
                  }}
                  className={`px-3 py-1.5 rounded-lg capitalize font-medium transition-colors ${
                    selectedDifficulty === diff
                      ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {diff}
                </button>
              ))}
            </div>
          </div>

          {/* Online Multiplayer Hub CTA */}
          <button
            onClick={() => {
              soundEngine.playLetterTick();
              onOpenMultiplayer();
            }}
            className="w-full flex items-center justify-center gap-3 py-3.5 px-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/80 text-white font-semibold text-sm transition-all shadow-sm active:scale-[0.99]"
          >
            <Users className="w-4 h-4 text-rose-400" />
            <span>Online Multiplayer Lobby & Rooms</span>
          </button>
        </div>
      </div>

      {/* Bottom Flat Control-Hint Line & Footer Credits */}
      <div className="relative z-10 w-full max-w-4xl mx-auto pt-4 border-t border-slate-900/80 text-center">
        <p className="text-xs text-slate-500 font-mono tracking-wide">
          Enter Start · Tab Next Category · Slap STOP! to Lock · [M] Mute Audio · [Esc] Pause
        </p>
        <div className="text-xs text-slate-500 font-mono tracking-wide mt-2">
          createby CompuApps SecureDev
        </div>
      </div>
    </div>
  );
};
