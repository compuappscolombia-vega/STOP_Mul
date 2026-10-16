import React, { useEffect, useState } from 'react';
import { UserProfile } from '../types/game';
import { getDailyChallenge } from '../game/rules';
import { soundEngine } from '../utils/audio';
import { Calendar, Flame, Trophy, Play, X, CheckCircle2 } from 'lucide-react';

interface DailyChallengeModalProps {
  profile: UserProfile;
  onStartDaily: (challenge: ReturnType<typeof getDailyChallenge>) => void;
  onClose: () => void;
}

export const DailyChallengeModal: React.FC<DailyChallengeModalProps> = ({
  profile,
  onStartDaily,
  onClose
}) => {
  const [challenge, setChallenge] = useState(getDailyChallenge());
  const today = new Date().toISOString().split('T')[0];
  const alreadyCompleted = profile.stats.dailyChallengesCompleted.includes(today);

  useEffect(() => {
    // Try fetching from server
    fetch('/api/daily-challenge')
      .then(res => res.json())
      .then(data => setChallenge(data))
      .catch(() => {});
  }, []);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl my-auto">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-4">
          <div className="p-3 rounded-2xl bg-amber-500/20 border border-amber-500/40 text-amber-400">
            <Calendar className="w-6 h-6" />
          </div>
          <div>
            <div className="text-xs font-mono uppercase tracking-wider text-amber-400">
              Daily Gauntlet · {challenge.date}
            </div>
            <h2 className="font-display font-black text-xl md:text-2xl text-white">
              {challenge.title}
            </h2>
          </div>
        </div>

        {/* Challenge details */}
        <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 mb-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">Assigned Letter:</span>
            <span className="font-display font-black text-2xl text-rose-400">
              '{challenge.letter.toUpperCase()}'
            </span>
          </div>

          <div className="flex items-center justify-between border-t border-slate-900 pt-2 text-xs">
            <span className="text-slate-400">Target Par Score:</span>
            <span className="font-mono font-bold text-amber-400">{challenge.targetParScore} pts</span>
          </div>

          <div className="flex items-center justify-between border-t border-slate-900 pt-2 text-xs">
            <span className="text-slate-400">Time Limit:</span>
            <span className="font-mono font-bold text-slate-200">{challenge.timeLimitSec} seconds</span>
          </div>
        </div>

        {/* Categories to solve */}
        <div className="mb-6">
          <div className="text-xs font-semibold text-slate-400 mb-2">5 Challenge Categories:</div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {challenge.categories.map(c => (
              <div
                key={c.id}
                className="p-2.5 rounded-xl bg-slate-950 border border-slate-800/80 flex items-center gap-2 text-xs"
              >
                <span>{c.icon}</span>
                <span className="font-medium text-slate-200">{c.name}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Streak bonus banner */}
        <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-between text-xs mb-6">
          <div className="flex items-center gap-2 text-rose-300 font-semibold">
            <Flame className="w-4 h-4 text-rose-400" />
            <span>Daily Streak: {profile.stats.currentStreak} Days</span>
          </div>
          <span className="text-rose-400 font-mono font-bold">+50 Bonus XP</span>
        </div>

        {alreadyCompleted ? (
          <div className="flex items-center justify-center gap-2 p-3.5 rounded-xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 font-bold text-xs text-center">
            <CheckCircle2 className="w-4 h-4" />
            <span>You completed today's challenge! Come back tomorrow for new letter.</span>
          </div>
        ) : (
          <button
            onClick={() => {
              soundEngine.playLetterReveal();
              onStartDaily(challenge);
            }}
            className="w-full flex items-center justify-center gap-2 py-3.5 px-6 rounded-2xl bg-gradient-to-r from-amber-500 to-rose-600 hover:from-amber-400 hover:to-rose-500 text-white font-display font-bold text-sm shadow-lg transition-all active:scale-98"
          >
            <Play className="w-4 h-4 fill-white" />
            <span>Accept Today's Challenge</span>
          </button>
        )}
      </div>
    </div>
  );
};
