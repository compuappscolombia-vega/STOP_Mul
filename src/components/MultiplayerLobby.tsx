import React, { useState, useEffect } from 'react';
import { UserProfile, RoomSettings } from '../types/game';
import { DEFAULT_CATEGORIES, Category } from '../game/rules';
import { soundEngine } from '../utils/audio';
import { Users, Lock, Unlock, Plus, RefreshCw, ArrowLeft, Play, Globe } from 'lucide-react';

interface PublicRoomItem {
  code: string;
  name: string;
  playerCount: number;
  maxRounds: number;
  duration: number;
  hostName: string;
}

interface MultiplayerLobbyProps {
  profile: UserProfile;
  onCreateRoom: (name: string, settings: RoomSettings) => void;
  onJoinRoom: (code: string) => void;
  onBack: () => void;
}

export const MultiplayerLobby: React.FC<MultiplayerLobbyProps> = ({
  profile,
  onCreateRoom,
  onJoinRoom,
  onBack
}) => {
  const [rooms, setRooms] = useState<PublicRoomItem[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [joinCodeInput, setJoinCodeInput] = useState('');
  const [joinError, setJoinError] = useState('');

  // Create room modal state
  const [isCreating, setIsCreating] = useState(false);
  const [customRoomName, setCustomRoomName] = useState(`${profile.name}'s Match`);
  const [roundCount, setRoundCount] = useState(3);
  const [roundDurationSec, setRoundDurationSec] = useState(60);
  const [isPrivate, setIsPrivate] = useState(false);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState<string[]>(
    DEFAULT_CATEGORIES.slice(0, 6).map(c => c.id)
  );

  const fetchRooms = async () => {
    setIsLoading(true);
    try {
      const res = await fetch('/api/rooms');
      if (res.ok) {
        const data = await res.json();
        setRooms(data);
      }
    } catch (e) {
      console.warn('Could not load public rooms:', e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRooms();
    const interval = setInterval(fetchRooms, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleToggleCategory = (id: string) => {
    soundEngine.playKeyType();
    if (selectedCategoryIds.includes(id)) {
      if (selectedCategoryIds.length > 3) {
        setSelectedCategoryIds(selectedCategoryIds.filter(c => c !== id));
      }
    } else {
      if (selectedCategoryIds.length < 8) {
        setSelectedCategoryIds([...selectedCategoryIds, id]);
      }
    }
  };

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    soundEngine.playLetterReveal();
    const chosenCategories = DEFAULT_CATEGORIES.filter(c => selectedCategoryIds.includes(c.id));
    const settings: RoomSettings = {
      roundCount,
      roundDurationSec,
      categories: chosenCategories,
      isPrivate,
      difficulty: 'medium'
    };
    onCreateRoom(customRoomName.trim() || `${profile.name}'s Party`, settings);
  };

  const handleDirectJoin = (e: React.FormEvent) => {
    e.preventDefault();
    const code = joinCodeInput.trim().toUpperCase();
    if (!code) return;
    soundEngine.playKeyType();
    setJoinError('');
    onJoinRoom(code);
  };

  return (
    <div className="min-h-screen w-full bg-slate-950 text-slate-100 p-4 md:p-8 flex flex-col max-w-5xl mx-auto">
      {/* Top Bar */}
      <div className="flex items-center justify-between mb-8 pb-4 border-b border-slate-900">
        <button
          onClick={() => {
            soundEngine.playKeyType();
            onBack();
          }}
          className="flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white text-xs font-semibold transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Title Screen</span>
        </button>

        <div className="text-center">
          <h1 className="font-display font-black text-xl md:text-2xl text-white">
            Online Multiplayer Rooms
          </h1>
          <p className="text-xs text-slate-400">Join a live room with friends or host custom categories</p>
        </div>

        <button
          onClick={() => {
            soundEngine.playKeyType();
            fetchRooms();
          }}
          disabled={isLoading}
          className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition-colors"
          title="Refresh Room List"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Main Grid: Join with Code / Create Room + Public List */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Join Code & Create CTA */}
        <div className="space-y-4">
          {/* Join with Room Code Card */}
          <div className="p-5 rounded-2xl bg-slate-900/90 border border-slate-800">
            <h2 className="font-display font-bold text-base text-white mb-2 flex items-center gap-2">
              <Lock className="w-4 h-4 text-rose-400" />
              <span>Join with Room Code</span>
            </h2>
            <p className="text-xs text-slate-400 mb-4 leading-relaxed">
              Enter the 4-8 character room code shared by your friend (e.g. STOP-4892)
            </p>

            <form onSubmit={handleDirectJoin} className="space-y-3">
              <input
                type="text"
                value={joinCodeInput}
                onChange={e => setJoinCodeInput(e.target.value.toUpperCase())}
                placeholder="STOP-XXXX"
                className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-slate-800 focus:border-rose-500 font-mono text-center uppercase tracking-widest text-base font-bold text-white placeholder-slate-600 focus:outline-none"
              />
              {joinError && <div className="text-xs text-rose-400">{joinError}</div>}
              <button
                type="submit"
                disabled={!joinCodeInput.trim()}
                className="w-full py-2.5 px-4 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white font-semibold text-xs transition-colors flex items-center justify-center gap-2 shadow"
              >
                <Play className="w-3.5 h-3.5 fill-white" />
                <span>Join Room</span>
              </button>
            </form>
          </div>

          {/* Host New Room Button */}
          <button
            onClick={() => {
              soundEngine.playKeyType();
              setIsCreating(true);
            }}
            className="w-full p-4 rounded-2xl bg-gradient-to-r from-amber-500/20 to-rose-500/20 border border-amber-500/40 hover:border-amber-500 text-left transition-all group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="font-display font-bold text-sm text-white group-hover:text-amber-300 transition-colors">
                Host Custom Match
              </span>
              <Plus className="w-4 h-4 text-amber-400" />
            </div>
            <p className="text-xs text-slate-400">
              Customize categories, round timer (30s-90s), and toggle private codes.
            </p>
          </button>
        </div>

        {/* Right Column: Public Open Rooms List */}
        <div className="lg:col-span-2 p-5 rounded-2xl bg-slate-900/90 border border-slate-800 flex flex-col">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-bold text-base text-white flex items-center gap-2">
              <Globe className="w-4 h-4 text-emerald-400" />
              <span>Public Matches</span>
            </h2>
            <span className="text-xs text-slate-400 font-mono">
              {rooms.length} {rooms.length === 1 ? 'room' : 'rooms'} open
            </span>
          </div>

          {rooms.length === 0 ? (
            <div className="flex flex-col items-center justify-center my-auto py-12 text-center text-slate-500">
              <Users className="w-10 h-10 mb-3 stroke-1 text-slate-600" />
              <div className="font-medium text-sm text-slate-300">No public rooms currently waiting</div>
              <p className="text-xs text-slate-500 max-w-xs mt-1">
                Be the host and create the first room, or join a friend using their private room code!
              </p>
              <button
                onClick={() => setIsCreating(true)}
                className="mt-4 px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold transition-colors"
              >
                Create Room Now
              </button>
            </div>
          ) : (
            <div className="space-y-2.5 max-h-[460px] overflow-y-auto pr-1">
              {rooms.map(room => (
                <div
                  key={room.code}
                  className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950/80 border border-slate-800/80 hover:border-slate-700 transition-all text-xs"
                >
                  <div className="min-w-0">
                    <div className="font-bold text-slate-100 text-sm flex items-center gap-2 truncate">
                      <span>{room.name}</span>
                      <span className="font-mono text-[11px] text-slate-400">({room.code})</span>
                    </div>
                    <div className="text-slate-400 text-xs flex items-center gap-2 mt-0.5">
                      <span>Host: {room.hostName}</span>
                      <span>·</span>
                      <span>{room.maxRounds} Rounds</span>
                      <span>·</span>
                      <span>{room.duration}s per round</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <div className="flex items-center gap-1 font-mono text-xs text-slate-300">
                      <Users className="w-3.5 h-3.5 text-slate-400" />
                      <span>{room.playerCount}</span>
                    </div>

                    <button
                      onClick={() => onJoinRoom(room.code)}
                      className="px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-semibold transition-colors shadow-sm"
                    >
                      Join
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create Custom Room Modal */}
      {isCreating && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 overflow-y-auto">
          <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl my-auto">
            <h2 className="font-display font-black text-xl text-white mb-1">Host Custom Match</h2>
            <p className="text-xs text-slate-400 mb-6">Configure custom categories, round time, and privacy</p>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                  Room Name
                </label>
                <input
                  type="text"
                  value={customRoomName}
                  onChange={e => setCustomRoomName(e.target.value)}
                  maxLength={30}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white text-sm focus:border-rose-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Rounds
                  </label>
                  <select
                    value={roundCount}
                    onChange={e => setRoundCount(Number(e.target.value))}
                    className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white text-sm focus:border-rose-500 focus:outline-none"
                  >
                    <option value={1}>1 Round (Blitz)</option>
                    <option value={3}>3 Rounds (Standard)</option>
                    <option value={5}>5 Rounds (Marathon)</option>
                    <option value={7}>7 Rounds (Championship)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Round Timer
                  </label>
                  <select
                    value={roundDurationSec}
                    onChange={e => setRoundDurationSec(Number(e.target.value))}
                    className="w-full px-3 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white text-sm focus:border-rose-500 focus:outline-none"
                  >
                    <option value={30}>30s (Hyper Blitz)</option>
                    <option value={45}>45s (Fast)</option>
                    <option value={60}>60s (Standard)</option>
                    <option value={90}>90s (Relaxed)</option>
                  </select>
                </div>
              </div>

              {/* Category Selector */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-semibold text-slate-300">
                    Active Categories ({selectedCategoryIds.length} selected, 3-8 allowed)
                  </label>
                </div>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-1.5 max-h-40 overflow-y-auto p-1.5 rounded-xl bg-slate-950 border border-slate-800">
                  {DEFAULT_CATEGORIES.map(cat => {
                    const isSelected = selectedCategoryIds.includes(cat.id);
                    return (
                      <button
                        key={cat.id}
                        type="button"
                        onClick={() => handleToggleCategory(cat.id)}
                        className={`p-2 rounded-lg text-left text-xs flex items-center gap-1.5 transition-colors ${
                          isSelected
                            ? 'bg-rose-500/20 border border-rose-500/50 text-white'
                            : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        <span>{cat.icon}</span>
                        <span className="truncate">{cat.name}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Private Room Toggle */}
              <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950 border border-slate-800">
                <div className="flex items-center gap-2">
                  {isPrivate ? <Lock className="w-4 h-4 text-amber-400" /> : <Unlock className="w-4 h-4 text-emerald-400" />}
                  <div>
                    <div className="text-xs font-semibold text-white">Private Match</div>
                    <div className="text-[11px] text-slate-400">Only players with code can join</div>
                  </div>
                </div>
                <input
                  type="checkbox"
                  checked={isPrivate}
                  onChange={e => setIsPrivate(e.target.checked)}
                  className="w-4 h-4 accent-rose-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsCreating(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold shadow transition-colors"
                >
                  Create & Enter Room
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
