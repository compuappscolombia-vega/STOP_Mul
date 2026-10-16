import React, { useState, useEffect } from 'react';
import { LeaderboardEntry, UserProfile } from '../types/game';
import { soundEngine } from '../utils/audio';
import { Trophy, Flame, Award, X, Sparkles } from 'lucide-react';

interface LeaderboardModalProps {
  profile: UserProfile;
  onClose: () => void;
}

export const LeaderboardModal: React.FC<LeaderboardModalProps> = ({ profile, onClose }) => {
  const [period, setPeriod] = useState<'all' | 'weekly'>('all');
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    fetch(`/api/leaderboards?period=${period}`)
      .then(res => res.json())
      .then(data => {
        setEntries(data.leaderboards || []);
      })
      .catch(e => console.warn('Leaderboard fetch error:', e))
      .finally(() => setIsLoading(false));
  }, [period]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl my-auto">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="p-3 rounded-2xl bg-amber-500/20 border border-amber-500/40 text-amber-400">
            <Trophy className="w-6 h-6" />
          </div>
          <div>
            <h2 className="font-display font-black text-xl md:text-2xl text-white">
              STOP! Rankings & Leaderboards
            </h2>
            <p className="text-xs text-slate-400">Global Hall of Fame and Top Weekly Sprinters</p>
          </div>
        </div>

        {/* Tab Switcher (Segmented button control) */}
        <div className="flex items-center p-1 rounded-xl bg-slate-950 border border-slate-800 mb-5">
          <button
            onClick={() => {
              soundEngine.playKeyType();
              setPeriod('all');
            }}
            className={`flex-1 py-2 rounded-lg text-xs font-semibold transition-colors ${
              period === 'all'
                ? 'bg-rose-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            All-Time Legends
          </button>
          <button
            onClick={() => {
              soundEngine.playKeyType();
              setPeriod('weekly');
            }}
            className={`flex-1 py-2 rounded-lg text-xs font-semibold transition-colors ${
              period === 'weekly'
                ? 'bg-rose-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Weekly Champions
          </button>
        </div>

        {/* Leaderboard Table */}
        <div className="space-y-2 max-h-[380px] overflow-y-auto pr-1 mb-6">
          {isLoading ? (
            <div className="py-12 text-center text-xs text-slate-500">Loading standings...</div>
          ) : (
            entries.map((item, idx) => {
              const isMe = item.id === profile.id;
              const isTop3 = idx < 3;

              return (
                <div
                  key={item.id}
                  className={`flex items-center justify-between p-3 rounded-2xl border transition-all ${
                    isMe
                      ? 'bg-rose-500/10 border-rose-500/50'
                      : 'bg-slate-950/70 border-slate-800/80 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-3 min-w-0">
                    {/* Rank Number */}
                    <div
                      className={`w-7 h-7 rounded-lg flex items-center justify-center font-display font-black text-xs shrink-0 ${
                        idx === 0
                          ? 'bg-amber-400 text-slate-950'
                          : idx === 1
                          ? 'bg-slate-300 text-slate-950'
                          : idx === 2
                          ? 'bg-amber-700 text-white'
                          : 'bg-slate-800 text-slate-400 font-mono'
                      }`}
                    >
                      {item.rank}
                    </div>

                    {/* Avatar & Player Details */}
                    <div className="w-8 h-8 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center text-lg shrink-0">
                      {item.avatar}
                    </div>

                    <div className="min-w-0">
                      <div className="font-bold text-white text-xs truncate flex items-center gap-1.5">
                        <span>{item.name}</span>
                        {isMe && <span className="text-[10px] text-rose-400 font-mono">(You)</span>}
                      </div>
                      <div className="text-[11px] text-slate-400 flex items-center gap-2">
                        <span>{item.title}</span>
                        <span>·</span>
                        <span>{item.wins} Wins</span>
                        <span>·</span>
                        <span>{item.stopsCalled} Stops</span>
                      </div>
                    </div>
                  </div>

                  {/* Points Readout */}
                  <div className="text-right shrink-0">
                    <div className="font-mono font-bold text-amber-400 text-sm tabular-nums">
                      {item.totalPoints} pts
                    </div>
                    <div className="text-[10px] text-slate-500 uppercase tracking-wider font-mono">
                      {item.tier}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Your rank summary strip */}
        <div className="p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-slate-300">
            <span>Your Current Total:</span>
            <span className="font-mono font-bold text-amber-400">{profile.stats.totalPoints} pts</span>
          </div>
          <span className="text-slate-400 font-mono">{profile.stats.wins} Wins · {profile.stats.stopsCalled} Stops</span>
        </div>
      </div>
    </div>
  );
};
