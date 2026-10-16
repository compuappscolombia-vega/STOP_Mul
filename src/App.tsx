/**
 * STOP! Multiplayer Category Rush
 * Main Application Orchestrator
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { UserProfile, GameRoom, RoomSettings, AuthUser } from './types/game';
import {
  loadLocalProfile,
  saveLocalProfile,
  syncProfileWithServer,
  getAuthUser,
  fetchCurrentSession
} from './services/storage';
import { soundEngine } from './utils/audio';
import {
  DEFAULT_CATEGORIES,
  VALID_LETTERS,
  Category,
  generateBotAnswer,
  calculateRoundScores,
  PlayerRoundScore,
  getDailyChallenge,
  calculateBotFieldDelay
} from './game/rules';

import { TitleScreen } from './components/TitleScreen';
import { GameHUD } from './components/GameHUD';
import { GameBoard } from './components/GameBoard';
import { LetterWheel } from './components/LetterWheel';
import { RoundSummaryModal } from './components/RoundSummaryModal';
import { MultiplayerLobby } from './components/MultiplayerLobby';
import { RoomView } from './components/RoomView';
import { ProfileModal } from './components/ProfileModal';
import { LeaderboardModal } from './components/LeaderboardModal';
import { DailyChallengeModal } from './components/DailyChallengeModal';
import { PauseModal } from './components/PauseModal';
import { AuthModal } from './components/AuthModal';
import { ApkModal } from './components/ApkModal';

type AppView = 'TITLE' | 'PRACTICE' | 'ONLINE_LOBBY' | 'ONLINE_ROOM' | 'DAILY_PLAY';

export default function App() {
  const [profile, setProfile] = useState<UserProfile>(loadLocalProfile());
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(getAuthUser());
  const [isMuted, setIsMuted] = useState<boolean>(soundEngine.isMuted());
  const [currentView, setCurrentView] = useState<AppView>('TITLE');

  // Modals
  const [showProfile, setShowProfile] = useState(false);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [showLeaderboards, setShowLeaderboards] = useState(false);
  const [showDaily, setShowDaily] = useState(false);
  const [showApkModal, setShowApkModal] = useState(false);
  const [isPaused, setIsPaused] = useState(false);

  // Offline Practice State
  const [practiceDifficulty, setPracticeDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');
  const [botSpeedSec, setBotSpeedSec] = useState<number>(() => {
    try {
      return parseInt(localStorage.getItem('stop_game_bot_speed') || '4', 10) || 4;
    } catch {
      return 4;
    }
  }); // Default 4s per category (human pace)
  const [practiceRound, setPracticeRound] = useState(1);
  const [totalPracticeRounds] = useState(3);
  const [practiceLetter, setPracticeLetter] = useState('S');
  const [practiceCategories, setPracticeCategories] = useState<Category[]>(DEFAULT_CATEGORIES.slice(0, 6));
  const [practiceAnswers, setPracticeAnswers] = useState<Record<string, string>>({});
  const [practiceTimeRemaining, setPracticeTimeRemaining] = useState(60);
  const [isSpinningPractice, setIsSpinningPractice] = useState(false);
  const [practicePhase, setPracticePhase] = useState<'PLAYING' | 'SUMMARY'>('PLAYING');
  const [practiceScores, setPracticeScores] = useState<PlayerRoundScore[]>([]);
  const [practiceStopper, setPracticeStopper] = useState<string | null>(null);

  // Live bot progressive thinking state
  const [botProgress, setBotProgress] = useState<Record<string, {
    filledCount: number;
    statusText: string;
    answers: Record<string, string>;
    hasStopped: boolean;
  }>>({
    bot_vicky: { filledCount: 0, statusText: 'Pensando...', answers: {}, hasStopped: false },
    bot_sam: { filledCount: 0, statusText: 'Pensando...', answers: {}, hasStopped: false }
  });

  // Internal mutable ref for high-frequency simulation timers without closure tearing
  const botSimRef = useRef<Record<string, {
    filledCount: number;
    statusText: string;
    answers: Record<string, string>;
    fieldStartTime: number;
    fieldDurationMs: number;
    hasStopped: boolean;
  }>>({
    bot_vicky: { filledCount: 0, statusText: 'Pensando...', answers: {}, fieldStartTime: 0, fieldDurationMs: 4000, hasStopped: false },
    bot_sam: { filledCount: 0, statusText: 'Pensando...', answers: {}, fieldStartTime: 0, fieldDurationMs: 4000, hasStopped: false }
  });

  // Bot competitors for practice
  const [practiceBots] = useState([
    { id: 'bot_vicky', name: 'Vocabulary Vicky', avatar: '🦉', difficulty: 'medium' as const },
    { id: 'bot_sam', name: 'Speedy Sam', avatar: '⚡', difficulty: 'easy' as const }
  ]);

  // Online Room State
  const [activeRoom, setActiveRoom] = useState<GameRoom | null>(null);
  const sseRef = useRef<EventSource | null>(null);

  // Update bot speed setting across app and persist to localStorage
  const updateBotSpeed = useCallback((newSpeed: number) => {
    const clamped = Math.max(1, Math.min(6, newSpeed));
    setBotSpeedSec(clamped);
    try {
      localStorage.setItem('stop_game_bot_speed', clamped.toString());
    } catch {}

    // Adjust in-flight delay for remaining bot categories
    Object.keys(botSimRef.current).forEach(botId => {
      const sim = botSimRef.current[botId];
      if (!sim.hasStopped && sim.filledCount < practiceCategories.length) {
        const diff = botId === 'bot_vicky' ? 'medium' : practiceDifficulty;
        sim.fieldDurationMs = calculateBotFieldDelay(clamped, diff, false);
        sim.fieldStartTime = Date.now();
      }
    });
  }, [practiceCategories.length, practiceDifficulty]);

  // Check active server session on mount
  useEffect(() => {
    fetchCurrentSession().then(sess => {
      if (sess) {
        setCurrentUser(sess.user);
        if (sess.profile) setProfile(sess.profile);
      } else {
        syncProfileWithServer(profile).then(synced => setProfile(synced));
      }
    });
  }, []);

  // Update body dataset state for Game Shell styling
  useEffect(() => {
    if (isPaused) {
      document.body.dataset.state = 'paused';
    } else if (currentView === 'TITLE') {
      document.body.dataset.state = 'title';
    } else if (practicePhase === 'SUMMARY') {
      document.body.dataset.state = 'gameover';
    } else {
      document.body.dataset.state = 'playing';
    }
  }, [currentView, isPaused, practicePhase]);

  // Handle ?autostart=1 support
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get('autostart') === '1') {
      startPractice('medium', 4);
    }
  }, []);

  // Global key listener for Mute [M] and Pause [Esc]
  useEffect(() => {
    const handleGlobalKey = (e: KeyboardEvent) => {
      const activeEl = document.activeElement;
      if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')) {
        if (e.key === 'Escape') {
          if (currentView === 'PRACTICE' || currentView === 'DAILY_PLAY') {
            setIsPaused(p => !p);
          }
        }
        return;
      }
      if (e.key === 'm' || e.key === 'M') {
        const nextMute = soundEngine.toggleMute();
        setIsMuted(nextMute);
      } else if (e.key === 'Escape') {
        if (currentView === 'PRACTICE' || currentView === 'DAILY_PLAY') {
          setIsPaused(p => !p);
        } else if (showProfile || showLeaderboards || showDaily || showAuthModal || showApkModal) {
          setShowProfile(false);
          setShowLeaderboards(false);
          setShowDaily(false);
          setShowAuthModal(false);
          setShowApkModal(false);
        }
      }
    };
    window.addEventListener('keydown', handleGlobalKey);
    return () => window.removeEventListener('keydown', handleGlobalKey);
  }, [currentView, showProfile, showLeaderboards, showDaily, showAuthModal, showApkModal]);

  // Practice round timer countdown
  useEffect(() => {
    if (currentView !== 'PRACTICE' && currentView !== 'DAILY_PLAY') return;
    if (practicePhase !== 'PLAYING' || isPaused || isSpinningPractice) return;

    const timer = setInterval(() => {
      setPracticeTimeRemaining(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          finishPracticeRound('timer');
          return 0;
        }
        if (prev <= 10) {
          soundEngine.playTimerAlert();
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [currentView, practicePhase, isPaused, isSpinningPractice]);

  // Realistic human-paced step-by-step Bot thinking engine
  // Simulates category-by-category filling according to botSpeedSec (1s to 6s, default 4s)
  useEffect(() => {
    if (currentView !== 'PRACTICE' || practicePhase !== 'PLAYING' || isPaused || isSpinningPractice) return;

    const interval = setInterval(() => {
      const now = Date.now();
      let stateChanged = false;

      for (const bot of practiceBots) {
        const sim = botSimRef.current[bot.id];
        if (!sim || sim.hasStopped) continue;

        // If bot still has categories to fill
        if (sim.filledCount < practiceCategories.length) {
          const elapsed = now - sim.fieldStartTime;
          if (elapsed >= sim.fieldDurationMs) {
            // Complete this category
            const cat = practiceCategories[sim.filledCount];
            const answer = generateBotAnswer(cat.id, practiceLetter, bot.id === 'bot_vicky' ? 'medium' : practiceDifficulty);
            sim.answers[cat.id] = answer;
            sim.filledCount += 1;

            if (sim.filledCount < practiceCategories.length) {
              const nextCat = practiceCategories[sim.filledCount];
              sim.fieldStartTime = now;
              sim.fieldDurationMs = calculateBotFieldDelay(botSpeedSec, bot.id === 'bot_vicky' ? 'medium' : practiceDifficulty, false);
              sim.statusText = `Pensando ${nextCat.name}... (${sim.filledCount + 1}/${practiceCategories.length})`;
            } else {
              // Bot completed all categories! Real human reaction time before hitting STOP (1.6s - 2.4s)
              sim.fieldStartTime = now;
              sim.fieldDurationMs = 1600 + Math.random() * 800;
              sim.statusText = `¡Completó todos los campos! Preparando STOP...`;
            }
            stateChanged = true;
          }
        } else if (sim.filledCount >= practiceCategories.length) {
          // Bot has completed all fields and reaction delay has elapsed
          const elapsed = now - sim.fieldStartTime;
          if (elapsed >= sim.fieldDurationMs) {
            sim.hasStopped = true;
            stateChanged = true;
            clearInterval(interval);
            soundEngine.playStopAlarm();
            finishPracticeRound(bot.id);
            break;
          }
        }
      }

      if (stateChanged) {
        setBotProgress({
          bot_vicky: { ...botSimRef.current.bot_vicky },
          bot_sam: { ...botSimRef.current.bot_sam }
        });
      }
    }, 200);

    return () => clearInterval(interval);
  }, [currentView, practicePhase, isPaused, isSpinningPractice, botSpeedSec, practiceCategories, practiceLetter, practiceDifficulty, practiceBots]);

  const toggleMute = () => {
    const nextMute = soundEngine.toggleMute();
    setIsMuted(nextMute);
  };

  // Helper to initialize fresh bot simulation state
  const resetBotSimulation = (categories: Category[], speed: number, difficulty: 'easy' | 'medium' | 'hard') => {
    const now = Date.now();
    const firstCatName = categories[0]?.name || 'Categoría';

    const vickyDelay = calculateBotFieldDelay(speed, 'medium', true);
    const samDelay = calculateBotFieldDelay(speed, difficulty, true);

    botSimRef.current = {
      bot_vicky: {
        filledCount: 0,
        statusText: `Pensando ${firstCatName}... (1/${categories.length})`,
        answers: {},
        fieldStartTime: now,
        fieldDurationMs: vickyDelay,
        hasStopped: false
      },
      bot_sam: {
        filledCount: 0,
        statusText: `Pensando ${firstCatName}... (1/${categories.length})`,
        answers: {},
        fieldStartTime: now,
        fieldDurationMs: samDelay,
        hasStopped: false
      }
    };

    setBotProgress({
      bot_vicky: { ...botSimRef.current.bot_vicky },
      bot_sam: { ...botSimRef.current.bot_sam }
    });
  };

  // Start Offline Practice Session
  const startPractice = (difficulty: 'easy' | 'medium' | 'hard', speedSec?: number) => {
    const speed = speedSec !== undefined ? speedSec : botSpeedSec;
    setPracticeDifficulty(difficulty);
    setBotSpeedSec(speed);
    try {
      localStorage.setItem('stop_game_bot_speed', speed.toString());
    } catch {}

    const cats = DEFAULT_CATEGORIES.slice(0, 6);
    setPracticeRound(1);
    setPracticeCategories(cats);
    setPracticeAnswers({});
    const letter = VALID_LETTERS[Math.floor(Math.random() * VALID_LETTERS.length)];
    setPracticeLetter(letter);
    setPracticeTimeRemaining(60);
    setPracticePhase('PLAYING');
    setIsSpinningPractice(true);
    setPracticeStopper(null);
    setCurrentView('PRACTICE');

    resetBotSimulation(cats, speed, difficulty);
  };

  // Start Next Round in Practice
  const nextPracticeRound = () => {
    const nextR = practiceRound + 1;
    const cats = DEFAULT_CATEGORIES.slice(0, 6);
    setPracticeRound(nextR);
    setPracticeCategories(cats);
    setPracticeAnswers({});
    const letter = VALID_LETTERS[Math.floor(Math.random() * VALID_LETTERS.length)];
    setPracticeLetter(letter);
    setPracticeTimeRemaining(60);
    setPracticePhase('PLAYING');
    setIsSpinningPractice(true);
    setPracticeStopper(null);

    resetBotSimulation(cats, botSpeedSec, practiceDifficulty);
  };

  // Player or bot slaps STOP in practice mode
  const finishPracticeRound = (callerId: string) => {
    setPracticeStopper(callerId);
    setPracticePhase('SUMMARY');

    // Bots ONLY submit what they actually completed before STOP was called!
    // If the human slapped STOP on field 3, bots only get whatever fields they had completed!
    const vickySimAnswers = botSimRef.current.bot_vicky?.answers || {};
    const samSimAnswers = botSimRef.current.bot_sam?.answers || {};

    const isPlayerStopper = callerId === profile.id || callerId === 'player';
    const elapsedSec = 60 - practiceTimeRemaining;

    const submissions = [
      {
        playerId: profile.id,
        answers: practiceAnswers,
        hasStopped: isPlayerStopper,
        stopElapsedSec: isPlayerStopper ? elapsedSec : undefined
      },
      {
        playerId: 'bot_vicky',
        answers: vickySimAnswers,
        hasStopped: callerId === 'bot_vicky',
        stopElapsedSec: callerId === 'bot_vicky' ? elapsedSec : undefined
      },
      {
        playerId: 'bot_sam',
        answers: samSimAnswers,
        hasStopped: callerId === 'bot_sam',
        stopElapsedSec: callerId === 'bot_sam' ? elapsedSec : undefined
      }
    ];

    const scores = calculateRoundScores(submissions, practiceCategories, practiceLetter);
    setPracticeScores(scores);

    const myScore = scores.find(s => s.playerId === profile.id);
    const roundPts = myScore?.totalRoundScore || 0;
    const isWin = scores.every(s => s.playerId === profile.id || s.totalRoundScore <= roundPts);

    const updatedProfile: UserProfile = {
      ...profile,
      stats: {
        ...profile.stats,
        gamesPlayed: profile.stats.gamesPlayed + (practiceRound === totalPracticeRounds ? 1 : 0),
        wins: profile.stats.wins + (practiceRound === totalPracticeRounds && isWin ? 1 : 0),
        stopsCalled: profile.stats.stopsCalled + (isPlayerStopper ? 1 : 0),
        totalPoints: profile.stats.totalPoints + roundPts,
        bestRoundScore: Math.max(profile.stats.bestRoundScore, roundPts),
        currentStreak: isWin ? profile.stats.currentStreak + 1 : profile.stats.currentStreak
      }
    };
    setProfile(updatedProfile);
    saveLocalProfile(updatedProfile);

    fetch('/api/leaderboards/record', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        playerId: profile.id,
        pointsEarned: roundPts,
        won: isWin,
        stopped: isPlayerStopper
      })
    }).catch(() => {});
  };

  // Start Daily Challenge Session
  const handleStartDailyChallenge = (challenge: ReturnType<typeof getDailyChallenge>) => {
    setShowDaily(false);
    setPracticeRound(1);
    setPracticeCategories(challenge.categories);
    setPracticeLetter(challenge.letter);
    setPracticeAnswers({});
    setPracticeTimeRemaining(challenge.timeLimitSec);
    setPracticePhase('PLAYING');
    setIsSpinningPractice(true);
    setPracticeStopper(null);
    setCurrentView('DAILY_PLAY');
  };

  // Multiplayer Room Connection via SSE
  const connectToRoomSSE = useCallback((code: string) => {
    if (sseRef.current) {
      sseRef.current.close();
    }
    const es = new EventSource(`/api/rooms/${code}/events`);
    es.onmessage = ev => {
      try {
        const data: GameRoom = JSON.parse(ev.data);
        setActiveRoom(data);
      } catch (err) {
        console.warn('Failed to parse SSE event:', err);
      }
    };
    es.onerror = () => {};
    sseRef.current = es;
  }, []);

  const handleCreateRoom = async (name: string, settings: RoomSettings) => {
    try {
      const res = await fetch('/api/rooms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          host: { id: profile.id, name: profile.name, avatar: profile.avatar },
          name,
          settings
        })
      });
      if (res.ok) {
        const data = await res.json();
        setActiveRoom(data.room);
        setCurrentView('ONLINE_ROOM');
        connectToRoomSSE(data.code);
      }
    } catch (e) {
      console.error('Error creating room:', e);
    }
  };

  const handleJoinRoom = async (code: string) => {
    try {
      const res = await fetch(`/api/rooms/${code}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          player: { id: profile.id, name: profile.name, avatar: profile.avatar }
        })
      });
      if (res.ok) {
        const data = await res.json();
        setActiveRoom(data.room);
        setCurrentView('ONLINE_ROOM');
        connectToRoomSSE(code);
      } else {
        alert('Could not join room. Check the code and try again.');
      }
    } catch (e) {
      console.error('Error joining room:', e);
    }
  };

  const handleLeaveRoom = () => {
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
    }
    setActiveRoom(null);
    setCurrentView('TITLE');
  };

  const handleStartOnlineRound = async () => {
    if (!activeRoom) return;
    await fetch(`/api/rooms/${activeRoom.code}/start`, { method: 'POST' });
  };

  const handleCallOnlineStop = async (elapsedSec: number) => {
    if (!activeRoom) return;
    soundEngine.playStopAlarm();
    await fetch(`/api/rooms/${activeRoom.code}/stop`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: profile.id, elapsedSec })
    });
  };

  const handleSubmitOnlineAnswers = async (answers: Record<string, string>) => {
    if (!activeRoom) return;
    await fetch(`/api/rooms/${activeRoom.code}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: profile.id, answers })
    });
  };

  const handleSendOnlineChat = async (text: string) => {
    if (!activeRoom) return;
    await fetch(`/api/rooms/${activeRoom.code}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        senderId: profile.id,
        senderName: profile.name,
        senderAvatar: profile.avatar,
        text
      })
    });
  };

  const handleAuthSuccess = (user: AuthUser, updatedProfile: UserProfile) => {
    setCurrentUser(user);
    setProfile(updatedProfile);
  };

  const handleLogout = () => {
    setCurrentUser(null);
    const guest = loadLocalProfile();
    setProfile(guest);
  };

  return (
    <main className="min-h-screen w-full bg-slate-950 text-slate-100 flex flex-col relative select-none">
      {/* 1. Title Screen */}
      {currentView === 'TITLE' && (
        <TitleScreen
          profile={profile}
          currentUser={currentUser}
          onStartOffline={startPractice}
          onOpenMultiplayer={() => setCurrentView('ONLINE_LOBBY')}
          onOpenDaily={() => setShowDaily(true)}
          onOpenLeaderboards={() => setShowLeaderboards(true)}
          onOpenProfile={() => setShowProfile(true)}
          onOpenAuth={() => setShowAuthModal(true)}
          onOpenApk={() => setShowApkModal(true)}
          onToggleMute={toggleMute}
          isMuted={isMuted}
        />
      )}

      {/* 2. Offline Practice & Daily Gauntlet Screen */}
      {(currentView === 'PRACTICE' || currentView === 'DAILY_PLAY') && (
        <div className="flex-1 flex flex-col">
          <GameHUD
            currentLetter={practiceLetter}
            currentRound={practiceRound}
            totalRounds={totalPracticeRounds}
            timeRemainingSec={practiceTimeRemaining}
            totalTimeSec={60}
            currentScore={profile.stats.totalPoints}
            isMuted={isMuted}
            onToggleMute={toggleMute}
            onPause={() => setIsPaused(true)}
            isPractice={true}
            botSpeedSec={botSpeedSec}
            onBotSpeedChange={updateBotSpeed}
          />

          <GameBoard
            categories={practiceCategories}
            targetLetter={practiceLetter}
            answers={practiceAnswers}
            onAnswerChange={(catId, val) =>
              setPracticeAnswers(prev => ({ ...prev, [catId]: val }))
            }
            onCallStop={() => finishPracticeRound(profile.id)}
            isStopDisabled={practicePhase !== 'PLAYING'}
            botSpeedSec={botSpeedSec}
            onBotSpeedChange={updateBotSpeed}
            activePlayers={[
              {
                id: profile.id,
                name: profile.name,
                avatar: profile.avatar,
                hasStopped: practiceStopper === profile.id,
                isHuman: true,
                filledCount: Object.values(practiceAnswers).filter(a => a.trim().length > 0).length,
                totalCategories: practiceCategories.length,
                statusText: `${Object.values(practiceAnswers).filter(a => a.trim().length > 0).length} de ${practiceCategories.length} completados`
              },
              {
                id: 'bot_vicky',
                name: 'Vocabulary Vicky',
                avatar: '🦉',
                hasStopped: practiceStopper === 'bot_vicky',
                isHuman: false,
                filledCount: botProgress.bot_vicky?.filledCount || 0,
                totalCategories: practiceCategories.length,
                statusText: botProgress.bot_vicky?.statusText || 'Pensando...'
              },
              {
                id: 'bot_sam',
                name: 'Speedy Sam',
                avatar: '⚡',
                hasStopped: practiceStopper === 'bot_sam',
                isHuman: false,
                filledCount: botProgress.bot_sam?.filledCount || 0,
                totalCategories: practiceCategories.length,
                statusText: botProgress.bot_sam?.statusText || 'Pensando...'
              }
            ]}
          />

          {/* Letter Wheel Roulette */}
          {isSpinningPractice && (
            <LetterWheel
              targetLetter={practiceLetter}
              onSpinComplete={() => setIsSpinningPractice(false)}
            />
          )}

          {/* Scorecard Modal on STOP */}
          {practicePhase === 'SUMMARY' && (
            <RoundSummaryModal
              currentRound={practiceRound}
              totalRounds={totalPracticeRounds}
              letter={practiceLetter}
              categories={practiceCategories}
              playerScores={practiceScores}
              players={[
                { id: profile.id, name: profile.name, avatar: profile.avatar },
                ...practiceBots
              ]}
              myPlayerId={profile.id}
              onNextRound={nextPracticeRound}
              isMatchFinished={practiceRound >= totalPracticeRounds}
              onReturnToLobby={() => setCurrentView('TITLE')}
            />
          )}

          {/* Bottom credit during gameplay */}
          <div className="py-2 text-center text-xs text-slate-600 font-mono">
            createby CompuApps SecureDev
          </div>
        </div>
      )}

      {/* 3. Multiplayer Lobby */}
      {currentView === 'ONLINE_LOBBY' && (
        <div className="flex-1 flex flex-col">
          <MultiplayerLobby
            profile={profile}
            onCreateRoom={handleCreateRoom}
            onJoinRoom={handleJoinRoom}
            onBack={() => setCurrentView('TITLE')}
          />
          <div className="py-3 text-center text-xs text-slate-600 font-mono">
            createby CompuApps SecureDev
          </div>
        </div>
      )}

      {/* 4. Active Online Multiplayer Room */}
      {currentView === 'ONLINE_ROOM' && activeRoom && (
        <div className="flex-1 flex flex-col">
          <RoomView
            room={activeRoom}
            myProfile={profile}
            onLeaveRoom={handleLeaveRoom}
            onStartGame={handleStartOnlineRound}
            onCallStop={handleCallOnlineStop}
            onSubmitAnswers={handleSubmitOnlineAnswers}
            onSendMessage={handleSendOnlineChat}
            onNextRound={handleStartOnlineRound}
            isMuted={isMuted}
            onToggleMute={toggleMute}
          />
          <div className="py-3 text-center text-xs text-slate-600 font-mono">
            createby CompuApps SecureDev
          </div>
        </div>
      )}

      {/* Modals */}
      {showAuthModal && (
        <AuthModal
          currentUser={currentUser}
          currentProfile={profile}
          onAuthSuccess={handleAuthSuccess}
          onLogout={handleLogout}
          onClose={() => setShowAuthModal(false)}
        />
      )}

      {showProfile && (
        <ProfileModal
          profile={profile}
          onUpdateProfile={setProfile}
          onClose={() => setShowProfile(false)}
        />
      )}

      {showLeaderboards && (
        <LeaderboardModal
          profile={profile}
          onClose={() => setShowLeaderboards(false)}
        />
      )}

      {showDaily && (
        <DailyChallengeModal
          profile={profile}
          onStartDaily={handleStartDailyChallenge}
          onClose={() => setShowDaily(false)}
        />
      )}

      {showApkModal && (
        <ApkModal onClose={() => setShowApkModal(false)} />
      )}

      {isPaused && (
        <PauseModal
          onResume={() => setIsPaused(false)}
          onQuit={() => {
            setIsPaused(false);
            setCurrentView('TITLE');
          }}
          isMuted={isMuted}
          onToggleMute={toggleMute}
          botSpeedSec={botSpeedSec}
          onBotSpeedChange={updateBotSpeed}
        />
      )}
    </main>
  );
}
