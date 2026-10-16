import React, { useState, useEffect, useRef } from 'react';
import { GameRoom, UserProfile, ChatMessage } from '../types/game';
import { soundEngine } from '../utils/audio';
import { GameHUD } from './GameHUD';
import { GameBoard } from './GameBoard';
import { LetterWheel } from './LetterWheel';
import { RoundSummaryModal } from './RoundSummaryModal';
import { calculateRoundScores, PlayerRoundScore } from '../game/rules';
import { Copy, Check, Send, Users, ArrowLeft, Play, Shield, MessageSquare } from 'lucide-react';

interface RoomViewProps {
  room: GameRoom;
  myProfile: UserProfile;
  onLeaveRoom: () => void;
  onStartGame: () => void;
  onCallStop: (elapsedSec: number) => void;
  onSubmitAnswers: (answers: Record<string, string>) => void;
  onSendMessage: (text: string) => void;
  onNextRound: () => void;
  isMuted: boolean;
  onToggleMute: () => void;
}

export const RoomView: React.FC<RoomViewProps> = ({
  room,
  myProfile,
  onLeaveRoom,
  onStartGame,
  onCallStop,
  onSubmitAnswers,
  onSendMessage,
  onNextRound,
  isMuted,
  onToggleMute
}) => {
  const isHost = room.hostId === myProfile.id;
  const myPlayer = room.players.find(p => p.id === myProfile.id);
  const [copied, setCopied] = useState(false);
  const [chatInput, setChatInput] = useState('');
  const [localAnswers, setLocalAnswers] = useState<Record<string, string>>({});
  const [timeRemaining, setTimeRemaining] = useState(room.settings.roundDurationSec);
  const [showLetterWheel, setShowLetterWheel] = useState(false);
  const [calculatedScores, setCalculatedScores] = useState<PlayerRoundScore[]>([]);
  const chatBottomRef = useRef<HTMLDivElement | null>(null);

  // Sync answers on new round
  useEffect(() => {
    if (room.phase === 'ROUND_ACTIVE') {
      setLocalAnswers({});
      setTimeRemaining(room.settings.roundDurationSec);
    }
  }, [room.phase, room.currentRound, room.settings.roundDurationSec]);

  // Round countdown timer
  useEffect(() => {
    if (room.phase !== 'ROUND_ACTIVE') return;

    const timer = setInterval(() => {
      setTimeRemaining(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          onCallStop(room.settings.roundDurationSec);
          return 0;
        }
        if (prev <= 10) {
          soundEngine.playTimerAlert();
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [room.phase, room.settings.roundDurationSec, onCallStop]);

  // Calculate scores on STOP
  useEffect(() => {
    if (room.phase === 'STOP_CALLED' || room.phase === 'ROUND_SCORES') {
      const submissions = room.players.map(p => ({
        playerId: p.id,
        answers: p.id === myProfile.id ? localAnswers : (p.answers || {}),
        hasStopped: p.hasStopped,
        stopElapsedSec: p.stopElapsedSec
      }));
      const sc = calculateRoundScores(submissions, room.settings.categories, room.currentLetter);
      setCalculatedScores(sc);
    }
  }, [room.phase, room.players, localAnswers, room.settings.categories, room.currentLetter, myProfile.id]);

  // Auto-scroll chat
  useEffect(() => {
    chatBottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [room.chat]);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(room.code);
    setCopied(true);
    soundEngine.playKeyType();
    setTimeout(() => setCopied(false), 2000);
  };

  const handleAnswerChange = (categoryId: string, val: string) => {
    const updated = { ...localAnswers, [categoryId]: val };
    setLocalAnswers(updated);
    onSubmitAnswers(updated);
  };

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;
    soundEngine.playKeyType();
    onSendMessage(chatInput.trim());
    setChatInput('');
  };

  const sendQuickEmoji = (emoji: string) => {
    soundEngine.playKeyType();
    onSendMessage(emoji);
  };

  // Render Letter Wheel when spinning
  if (showLetterWheel) {
    return (
      <LetterWheel
        targetLetter={room.currentLetter}
        onSpinComplete={() => setShowLetterWheel(false)}
      />
    );
  }

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col">
      {/* If in active round: show GameHUD */}
      {room.phase === 'ROUND_ACTIVE' && (
        <GameHUD
          currentLetter={room.currentLetter}
          currentRound={room.currentRound}
          totalRounds={room.settings.roundCount}
          timeRemainingSec={timeRemaining}
          totalTimeSec={room.settings.roundDurationSec}
          currentScore={myPlayer?.totalScore || 0}
          isMuted={isMuted}
          onToggleMute={onToggleMute}
          onPause={onLeaveRoom}
        />
      )}

      {/* Main Container */}
      <div className="flex-1 w-full max-w-6xl mx-auto p-4 md:p-6 flex flex-col">
        {/* Lobby State */}
        {room.phase === 'LOBBY' && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 my-auto">
            {/* Left 2 Cols: Room Header, Players, Settings */}
            <div className="lg:col-span-2 space-y-6">
              {/* Room Header Card */}
              <div className="p-6 rounded-3xl bg-slate-900/90 border border-slate-800">
                <div className="flex items-center justify-between gap-4 mb-4">
                  <button
                    onClick={onLeaveRoom}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
                  >
                    <ArrowLeft className="w-4 h-4" />
                    <span>Leave</span>
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleCopyCode}
                      className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-950 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-mono font-bold transition-all"
                      title="Click to copy room code"
                    >
                      <span>{room.code}</span>
                      {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
                    </button>
                  </div>
                </div>

                <h1 className="font-display font-black text-2xl md:text-3xl text-white mb-1">
                  {room.name}
                </h1>
                <p className="text-xs text-slate-400">
                  {room.settings.roundCount} Rounds · {room.settings.roundDurationSec}s per round · {room.settings.categories.length} Categories
                </p>

                {/* Host Start CTA */}
                <div className="mt-6 pt-5 border-t border-slate-800 flex items-center justify-between">
                  <div className="text-xs text-slate-400">
                    {isHost ? (
                      <span className="text-amber-400 font-semibold">You are the host</span>
                    ) : (
                      <span>Waiting for host to start match...</span>
                    )}
                  </div>

                  {isHost && (
                    <button
                      onClick={() => {
                        soundEngine.playLetterReveal();
                        onStartGame();
                      }}
                      className="flex items-center gap-2 px-6 py-3 rounded-2xl bg-gradient-to-r from-rose-600 to-amber-500 hover:from-rose-500 hover:to-amber-400 text-white font-display font-bold text-sm shadow-md transition-all active:scale-95"
                    >
                      <Play className="w-4 h-4 fill-white" />
                      <span>Start Round 1</span>
                    </button>
                  )}
                </div>
              </div>

              {/* Players List in Lobby */}
              <div className="p-6 rounded-3xl bg-slate-900/90 border border-slate-800">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="font-display font-bold text-base text-white flex items-center gap-2">
                    <Users className="w-4 h-4 text-rose-400" />
                    <span>Connected Players ({room.players.length})</span>
                  </h2>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {room.players.map(p => (
                    <div
                      key={p.id}
                      className="p-3.5 rounded-2xl bg-slate-950/80 border border-slate-800 flex items-center justify-between"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-center text-xl">
                          {p.avatar}
                        </div>
                        <div>
                          <div className="font-bold text-sm text-white flex items-center gap-1.5">
                            <span>{p.name}</span>
                            {p.id === myProfile.id && (
                              <span className="text-[10px] text-slate-400 font-normal">(You)</span>
                            )}
                          </div>
                          <div className="text-xs text-slate-400">
                            {p.id === room.hostId ? 'Host' : 'Competitor'}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <span className="w-2 h-2 rounded-full bg-emerald-500" />
                        <span className="text-xs text-emerald-400 font-medium">Ready</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Right Column: In-Game Live Chat */}
            <div className="p-5 rounded-3xl bg-slate-900/90 border border-slate-800 flex flex-col h-[520px]">
              <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-800">
                <div className="flex items-center gap-2">
                  <MessageSquare className="w-4 h-4 text-amber-400" />
                  <span className="font-display font-bold text-sm text-white">Match Chat</span>
                </div>
                <div className="flex items-center gap-1">
                  {['🔥', '🛑', '😱', '👏'].map(em => (
                    <button
                      key={em}
                      type="button"
                      onClick={() => sendQuickEmoji(em)}
                      className="px-1.5 py-0.5 rounded hover:bg-slate-800 text-sm transition-transform active:scale-90"
                    >
                      {em}
                    </button>
                  ))}
                </div>
              </div>

              {/* Chat Stream */}
              <div className="flex-1 overflow-y-auto space-y-2.5 pr-1">
                {room.chat.map(msg => (
                  <div
                    key={msg.id}
                    className={`p-2.5 rounded-xl text-xs ${
                      msg.isSystem
                        ? 'bg-slate-950/40 text-amber-300/90 border border-amber-500/20 italic'
                        : msg.senderId === myProfile.id
                        ? 'bg-rose-950/40 border border-rose-800/30 text-rose-100 ml-4'
                        : 'bg-slate-950/80 border border-slate-800 text-slate-200 mr-4'
                    }`}
                  >
                    {!msg.isSystem && (
                      <div className="font-bold text-[11px] text-slate-400 mb-0.5 flex items-center gap-1">
                        <span>{msg.senderAvatar}</span>
                        <span>{msg.senderName}</span>
                      </div>
                    )}
                    <div className="break-words leading-relaxed">{msg.text}</div>
                  </div>
                ))}
                <div ref={chatBottomRef} />
              </div>

              {/* Chat Form */}
              <form onSubmit={handleSendChat} className="pt-3 mt-2 border-t border-slate-800 flex gap-2">
                <input
                  type="text"
                  value={chatInput}
                  onChange={e => setChatInput(e.target.value)}
                  placeholder="Type message..."
                  maxLength={160}
                  className="flex-1 px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-white placeholder-slate-600 text-xs focus:outline-none focus:border-rose-500"
                />
                <button
                  type="submit"
                  disabled={!chatInput.trim()}
                  className="p-2 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:opacity-40 text-white transition-colors"
                >
                  <Send className="w-3.5 h-3.5" />
                </button>
              </form>
            </div>
          </div>
        )}

        {/* Active Round Play State */}
        {room.phase === 'ROUND_ACTIVE' && (
          <GameBoard
            categories={room.settings.categories}
            targetLetter={room.currentLetter}
            answers={localAnswers}
            onAnswerChange={handleAnswerChange}
            onCallStop={() => onCallStop(room.settings.roundDurationSec - timeRemaining)}
            activePlayers={room.players.map(p => ({
              id: p.id,
              name: p.name,
              avatar: p.avatar,
              hasStopped: p.hasStopped
            }))}
          />
        )}

        {/* Scorecard Modal when STOP called */}
        {(room.phase === 'STOP_CALLED' || room.phase === 'ROUND_SCORES') && (
          <RoundSummaryModal
            currentRound={room.currentRound}
            totalRounds={room.settings.roundCount}
            letter={room.currentLetter}
            categories={room.settings.categories}
            playerScores={calculatedScores}
            players={room.players}
            myPlayerId={myProfile.id}
            onNextRound={onNextRound}
            isMatchFinished={room.currentRound >= room.settings.roundCount}
            onReturnToLobby={onLeaveRoom}
          />
        )}
      </div>
    </div>
  );
};
