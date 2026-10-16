import React, { useState } from 'react';
import { UserProfile } from '../types/game';
import { getPlayerTier } from '../game/rules';
import { soundEngine } from '../utils/audio';
import { saveLocalProfile, restoreProfileWithSyncCode, syncProfileWithServer } from '../services/storage';
import { User, Copy, Check, RefreshCw, X, Shield, Award, Flame, Zap } from 'lucide-react';

interface ProfileModalProps {
  profile: UserProfile;
  onUpdateProfile: (updated: UserProfile) => void;
  onClose: () => void;
}

const AVATAR_OPTIONS = [
  '⚡', '🦁', '🚀', '🦉', '🦊', '🔥', '👑', '🎯',
  '🥑', '💎', '🎮', '🏎️', '🐉', '🤖', '⭐', '🌈'
];

export const ProfileModal: React.FC<ProfileModalProps> = ({
  profile,
  onUpdateProfile,
  onClose
}) => {
  const [name, setName] = useState(profile.name);
  const [avatar, setAvatar] = useState(profile.avatar);
  const [copied, setCopied] = useState(false);
  const [restoreCodeInput, setRestoreCodeInput] = useState('');
  const [restoreError, setRestoreError] = useState('');
  const [isRestoring, setIsRestoring] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const tier = getPlayerTier(profile.stats.totalPoints);

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    soundEngine.playKeyType();
    const updated: UserProfile = {
      ...profile,
      name: name.trim() || profile.name,
      avatar,
      title: tier.rankTitle
    };
    saveLocalProfile(updated);
    onUpdateProfile(updated);
    await syncProfileWithServer(updated);
    setSaveSuccess(true);
    setTimeout(() => setSaveSuccess(false), 2000);
  };

  const handleCopySyncCode = () => {
    navigator.clipboard.writeText(profile.syncCode);
    setCopied(true);
    soundEngine.playKeyType();
    setTimeout(() => setCopied(false), 2000);
  };

  const handleRestore = async (e: React.FormEvent) => {
    e.preventDefault();
    const code = restoreCodeInput.trim().toUpperCase();
    if (!code) return;

    setIsRestoring(true);
    setRestoreError('');
    try {
      soundEngine.playKeyType();
      const restored = await restoreProfileWithSyncCode(code);
      onUpdateProfile(restored);
      setName(restored.name);
      setAvatar(restored.avatar);
      setRestoreCodeInput('');
      soundEngine.playVictoryFanfare();
    } catch (err: any) {
      setRestoreError(err.message || 'Could not find profile with that code');
    } finally {
      setIsRestoring(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 overflow-y-auto">
      <div className="relative w-full max-w-xl bg-slate-900 border border-slate-800 rounded-3xl p-6 md:p-8 shadow-2xl my-auto">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="p-3 rounded-2xl bg-rose-500/20 border border-rose-500/40 text-rose-400">
            <User className="w-6 h-6" />
          </div>
          <div>
            <h2 className="font-display font-black text-xl md:text-2xl text-white">
              Player Profile & Cloud Sync
            </h2>
            <p className="text-xs text-slate-400">Keep progress saved across all your browsers and devices</p>
          </div>
        </div>

        {/* Profile Edit Form */}
        <form onSubmit={handleSaveProfile} className="space-y-4 mb-6">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-2xl bg-slate-950 border border-slate-800 flex items-center justify-center text-3xl shadow-inner shrink-0">
              {avatar}
            </div>

            <div className="flex-1">
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Player Nickname
              </label>
              <input
                type="text"
                value={name}
                onChange={e => setName(e.target.value)}
                maxLength={20}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-white text-sm focus:border-rose-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Avatar Selector */}
          <div>
            <label className="block text-xs font-semibold text-slate-400 mb-1.5">
              Choose Avatar
            </label>
            <div className="flex items-center gap-1.5 flex-wrap p-2 rounded-xl bg-slate-950 border border-slate-800">
              {AVATAR_OPTIONS.map(av => (
                <button
                  key={av}
                  type="button"
                  onClick={() => {
                    soundEngine.playKeyType();
                    setAvatar(av);
                  }}
                  className={`w-9 h-9 rounded-lg flex items-center justify-center text-lg transition-transform ${
                    avatar === av
                      ? 'bg-rose-500/30 border border-rose-500 scale-110'
                      : 'hover:bg-slate-800'
                  }`}
                >
                  {av}
                </button>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-end">
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition-colors"
            >
              {saveSuccess ? 'Saved!' : 'Save Details'}
            </button>
          </div>
        </form>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 p-4 rounded-2xl bg-slate-950 border border-slate-800 mb-6 text-center">
          <div>
            <div className="text-[11px] text-slate-400">Total Points</div>
            <div className="font-mono font-bold text-base text-amber-400 tabular-nums">
              {profile.stats.totalPoints}
            </div>
          </div>
          <div>
            <div className="text-[11px] text-slate-400">Victories</div>
            <div className="font-mono font-bold text-base text-white tabular-nums">
              {profile.stats.wins}
            </div>
          </div>
          <div>
            <div className="text-[11px] text-slate-400">STOPs Slapped</div>
            <div className="font-mono font-bold text-base text-rose-400 tabular-nums">
              {profile.stats.stopsCalled}
            </div>
          </div>
          <div>
            <div className="text-[11px] text-slate-400">Daily Streak</div>
            <div className="font-mono font-bold text-base text-emerald-400 tabular-nums">
              {profile.stats.currentStreak}d
            </div>
          </div>
        </div>

        {/* Cross-Device Sync Code Showcase */}
        <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800/80 mb-6">
          <div className="flex items-center justify-between mb-2">
            <div>
              <div className="text-xs font-bold text-white">Your Cross-Device Sync Code</div>
              <div className="text-[11px] text-slate-400">
                Enter this code on any phone, tablet, or PC to carry over all your stats!
              </div>
            </div>

            <button
              onClick={handleCopySyncCode}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-mono font-bold transition-all shrink-0"
            >
              <span>{profile.syncCode}</span>
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
            </button>
          </div>

          {/* Restore from Another Device Form */}
          <form onSubmit={handleRestore} className="pt-3 border-t border-slate-900 flex gap-2">
            <input
              type="text"
              value={restoreCodeInput}
              onChange={e => setRestoreCodeInput(e.target.value.toUpperCase())}
              placeholder="Paste Sync Code from other device..."
              className="flex-1 px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-white placeholder-slate-600 font-mono text-xs focus:outline-none focus:border-rose-500 uppercase"
            />
            <button
              type="submit"
              disabled={isRestoring || !restoreCodeInput.trim()}
              className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 disabled:opacity-40 text-white text-xs font-semibold transition-colors flex items-center gap-1.5 shrink-0"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isRestoring ? 'animate-spin' : ''}`} />
              <span>Restore</span>
            </button>
          </form>
          {restoreError && <div className="text-xs text-rose-400 mt-2">{restoreError}</div>}
        </div>
      </div>
    </div>
  );
};
