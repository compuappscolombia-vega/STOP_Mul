import React, { useEffect } from 'react';
import confetti from 'canvas-confetti';
import { Category, PlayerRoundScore } from '../game/rules';
import { soundEngine } from '../utils/audio';
import { Check, X, Award, Flame, ArrowRight, RotateCcw } from 'lucide-react';

interface RoundSummaryModalProps {
  currentRound: number;
  totalRounds: number;
  letter: string;
  categories: Category[];
  playerScores: PlayerRoundScore[];
  players: Array<{ id: string; name: string; avatar: string }>;
  myPlayerId: string;
  onNextRound: () => void;
  isMatchFinished: boolean;
  onReturnToLobby: () => void;
}

export const RoundSummaryModal: React.FC<RoundSummaryModalProps> = ({
  currentRound,
  totalRounds,
  letter,
  categories,
  playerScores,
  players,
  myPlayerId,
  onNextRound,
  isMatchFinished,
  onReturnToLobby
}) => {
  const myScore = playerScores.find(s => s.playerId === myPlayerId);
  const winner = [...playerScores].sort((a, b) => b.totalRoundScore - a.totalRoundScore)[0];
  const isMeWinner = winner?.playerId === myPlayerId;

  useEffect(() => {
    soundEngine.playScoreChime(myScore?.totalRoundScore || 20);
    if (isMatchFinished || (myScore && myScore.totalRoundScore >= 40)) {
      confetti({
        particleCount: 70,
        spread: 60,
        origin: { y: 0.6 }
      });
      soundEngine.playVictoryFanfare();
    }
  }, [isMatchFinished, myScore]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-2xl bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl my-auto">
        {/* Header */}
        <div className="text-center mb-6">
          <div className="text-xs font-mono uppercase tracking-widest text-rose-400 mb-1">
            {isMatchFinished ? 'MATCH CONCLUDED' : `ROUND ${currentRound} OF ${totalRounds} RESULTS`}
          </div>
          <h2 className="font-display font-black text-2xl md:text-3xl text-white">
            {isMatchFinished
              ? isMeWinner
                ? '🏆 VICTORY! YOU DOMINATED!'
                : 'MATCH SUMMARY'
              : 'ROUND SCORECARD'}
          </h2>
          <p className="text-slate-400 text-xs mt-1">
            Letter <span className="font-bold text-rose-400">'{letter.toUpperCase()}'</span> · 10 pts Unique · 5 pts Duplicate · +5 First STOP Bonus
          </p>
        </div>

        {/* First STOP Caller Shoutout */}
        {myScore?.stoppedFirst && (
          <div className="mb-4 p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-amber-300 font-semibold">
              <Flame className="w-4 h-4 text-amber-400" />
              <span>You called STOP first!</span>
            </div>
            <span className="font-mono font-bold text-amber-400">+5 Time Bonus</span>
          </div>
        )}

        {/* Category Breakdown Table */}
        <div className="space-y-2.5 max-h-72 overflow-y-auto pr-1 mb-6">
          {categories.map(cat => {
            const catScore = myScore?.categoryScores[cat.id];
            const answer = catScore?.answer || '—';
            const isValid = catScore?.isValid;
            const isDup = catScore?.isDuplicate;
            const pts = catScore?.score || 0;

            return (
              <div
                key={cat.id}
                className="flex items-center justify-between p-3 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <span className="text-base">{cat.icon}</span>
                  <div className="min-w-0">
                    <div className="text-slate-400 font-medium truncate">{cat.name}</div>
                    <div className="font-bold text-slate-100 text-sm truncate">
                      {answer || <span className="text-slate-600 italic">No entry</span>}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  {isValid ? (
                    <span
                      className={`px-2 py-0.5 rounded font-mono font-semibold ${
                        isDup
                          ? 'bg-amber-950/60 text-amber-300 border border-amber-800/40'
                          : 'bg-emerald-950/60 text-emerald-300 border border-emerald-800/40'
                      }`}
                    >
                      {isDup ? 'Shared (+5)' : 'Unique (+10)'}
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded font-mono font-semibold bg-rose-950/60 text-rose-300 border border-rose-800/40">
                      0 pts
                    </span>
                  )}
                  <div className="w-8 text-right font-mono font-bold text-white tabular-nums">
                    +{pts}
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Players Standings Bar */}
        <div className="p-3.5 rounded-2xl bg-slate-950/80 border border-slate-800 mb-6">
          <div className="text-[11px] font-mono uppercase tracking-wider text-slate-400 mb-2">
            Match Standings
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {players.map(p => {
              const sc = playerScores.find(s => s.playerId === p.id);
              const isLead = p.id === winner?.playerId;
              return (
                <div
                  key={p.id}
                  className={`p-2.5 rounded-xl border flex items-center justify-between ${
                    isLead
                      ? 'bg-rose-500/10 border-rose-500/40'
                      : 'bg-slate-900 border-slate-800'
                  }`}
                >
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-base">{p.avatar}</span>
                    <span className="font-semibold text-slate-200 text-xs truncate">{p.name}</span>
                  </div>
                  <div className="font-mono font-bold text-amber-400 text-xs shrink-0 tabular-nums">
                    {sc?.totalRoundScore || 0} pts
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Modal Actions */}
        <div className="flex items-center justify-end gap-3">
          <button
            onClick={onReturnToLobby}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white text-xs font-semibold transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
            <span>Title Menu</span>
          </button>

          {!isMatchFinished ? (
            <button
              onClick={onNextRound}
              className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-gradient-to-r from-rose-600 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white text-xs font-bold shadow-md transition-all active:scale-95"
            >
              <span>Next Round</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={onReturnToLobby}
              className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md transition-all active:scale-95"
            >
              <Award className="w-4 h-4" />
              <span>Back to Lobby</span>
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
