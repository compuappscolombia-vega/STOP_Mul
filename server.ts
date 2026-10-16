import express, { Request, Response } from 'express';
import { createServer as createViteServer } from 'vite';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs';
import {
  DEFAULT_CATEGORIES,
  VALID_LETTERS,
  calculateRoundScores,
  generateBotAnswer,
  getDailyChallenge,
  getPlayerTier
} from './src/game/rules.js';
import { validateEmail, validatePassword, hashPassword } from './src/game/auth.js';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
app.use(express.json());

// In-Memory storage backed with simple persistent JSON file for profiles and leaderboards
const DATA_DIR = path.join(__dirname, '.data');
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}
const PROFILES_FILE = path.join(DATA_DIR, 'profiles.json');
const LEADERBOARD_FILE = path.join(DATA_DIR, 'leaderboard.json');
const USERS_FILE = path.join(DATA_DIR, 'users.json');

interface UserRecord {
  id: string;
  email: string;
  name: string;
  avatar: string;
  passwordHash?: string;
  authProvider: 'email' | 'google';
  createdAt: string;
}

// Types
interface ProfileRecord {
  id: string;
  name: string;
  avatar: string;
  syncCode: string;
  title: string;
  stats: {
    gamesPlayed: number;
    wins: number;
    stopsCalled: number;
    totalPoints: number;
    bestRoundScore: number;
    currentStreak: number;
    dailyChallengesCompleted: string[];
    averageResponseTimeSec: number;
  };
  createdAt: string;
}

interface Room {
  code: string;
  name: string;
  hostId: string;
  settings: {
    roundCount: number;
    roundDurationSec: number;
    categories: any[];
    isPrivate: boolean;
    difficulty: 'easy' | 'medium' | 'hard';
  };
  phase: 'LOBBY' | 'SPINNING_LETTER' | 'ROUND_ACTIVE' | 'STOP_CALLED' | 'ROUND_REVIEW' | 'ROUND_SCORES' | 'MATCH_OVER';
  currentRound: number;
  currentLetter: string;
  phaseTimeRemaining: number;
  phaseStartTime: number;
  stopperPlayerId: string | null;
  players: any[];
  chat: any[];
  lastUpdate: number;
  roundReviewVotes: Record<string, Record<string, string[]>>; // catId -> targetPlayerId -> voterIds
  sseClients: Set<Response>;
}

// Memory tables
const profiles = new Map<string, ProfileRecord>();
const syncCodes = new Map<string, string>(); // syncCode -> profileId
const users = new Map<string, UserRecord>();
const usersByEmail = new Map<string, string>(); // email -> userId
const sessions = new Map<string, { userId: string; createdAt: number }>(); // token -> session
const rooms = new Map<string, Room>();

// Load persistent profiles and users
try {
  if (fs.existsSync(PROFILES_FILE)) {
    const raw = fs.readFileSync(PROFILES_FILE, 'utf-8');
    const parsed: ProfileRecord[] = JSON.parse(raw);
    for (const p of parsed) {
      profiles.set(p.id, p);
      if (p.syncCode) syncCodes.set(p.syncCode.toUpperCase(), p.id);
    }
  }
} catch (e) {
  console.error('Error loading profiles:', e);
}

try {
  if (fs.existsSync(USERS_FILE)) {
    const raw = fs.readFileSync(USERS_FILE, 'utf-8');
    const parsed: UserRecord[] = JSON.parse(raw);
    for (const u of parsed) {
      users.set(u.id, u);
      usersByEmail.set(u.email.toLowerCase(), u.id);
    }
  }
} catch (e) {
  console.error('Error loading users:', e);
}

function saveProfiles() {
  try {
    const arr = Array.from(profiles.values());
    fs.writeFileSync(PROFILES_FILE, JSON.stringify(arr, null, 2), 'utf-8');
  } catch (e) {
    console.error('Error saving profiles:', e);
  }
}

function saveUsers() {
  try {
    const arr = Array.from(users.values());
    fs.writeFileSync(USERS_FILE, JSON.stringify(arr, null, 2), 'utf-8');
  } catch (e) {
    console.error('Error saving users:', e);
  }
}

// Seed default leaderboards if empty
function getLeaderboardData() {
  const allProfiles = Array.from(profiles.values());

  // Add realistic global champions if fewer than 5 profiles
  const seedChampions: ProfileRecord[] = [
    {
      id: 'bot_champ_1',
      name: 'VocabularyViper',
      avatar: '🐍',
      syncCode: 'STOP-VIPER',
      title: 'Stop Legend',
      stats: { gamesPlayed: 142, wins: 98, stopsCalled: 120, totalPoints: 3420, bestRoundScore: 65, currentStreak: 14, dailyChallengesCompleted: ['2026-10-14', '2026-10-15'], averageResponseTimeSec: 18.4 },
      createdAt: '2026-09-01T00:00:00Z'
    },
    {
      id: 'bot_champ_2',
      name: 'FlashLexicon',
      avatar: '⚡',
      syncCode: 'STOP-FLASH',
      title: 'Speed Lexicographer',
      stats: { gamesPlayed: 115, wins: 76, stopsCalled: 89, totalPoints: 2890, bestRoundScore: 60, currentStreak: 9, dailyChallengesCompleted: ['2026-10-15'], averageResponseTimeSec: 14.1 },
      createdAt: '2026-09-05T00:00:00Z'
    },
    {
      id: 'bot_champ_3',
      name: 'WordWhiz',
      avatar: '🦉',
      syncCode: 'STOP-WHIZ',
      title: 'Category Champ',
      stats: { gamesPlayed: 88, wins: 54, stopsCalled: 62, totalPoints: 2150, bestRoundScore: 55, currentStreak: 6, dailyChallengesCompleted: ['2026-10-13', '2026-10-15'], averageResponseTimeSec: 22.0 },
      createdAt: '2026-09-12T00:00:00Z'
    },
    {
      id: 'bot_champ_4',
      name: 'RapidRacer',
      avatar: '🏎️',
      syncCode: 'STOP-RACER',
      title: 'Category Champ',
      stats: { gamesPlayed: 73, wins: 41, stopsCalled: 51, totalPoints: 1780, bestRoundScore: 50, currentStreak: 4, dailyChallengesCompleted: ['2026-10-15'], averageResponseTimeSec: 16.8 },
      createdAt: '2026-09-20T00:00:00Z'
    },
    {
      id: 'bot_champ_5',
      name: 'CosmicNovelist',
      avatar: '🚀',
      syncCode: 'STOP-NOVEL',
      title: 'Word Sprinter',
      stats: { gamesPlayed: 49, wins: 28, stopsCalled: 34, totalPoints: 1240, bestRoundScore: 45, currentStreak: 3, dailyChallengesCompleted: [], averageResponseTimeSec: 25.5 },
      createdAt: '2026-10-01T00:00:00Z'
    }
  ];

  const pool = [...allProfiles];
  for (const champ of seedChampions) {
    if (!pool.some(p => p.id === champ.id)) {
      pool.push(champ);
    }
  }

  // Sort by total points
  const sorted = pool.sort((a, b) => b.stats.totalPoints - a.stats.totalPoints);
  return sorted.map((p, idx) => {
    const tier = getPlayerTier(p.stats.totalPoints);
    return {
      id: p.id,
      name: p.name,
      avatar: p.avatar,
      title: p.title || tier.rankTitle,
      totalPoints: p.stats.totalPoints,
      wins: p.stats.wins,
      stopsCalled: p.stats.stopsCalled,
      tier: tier.tier,
      rank: idx + 1,
      streak: p.stats.currentStreak
    };
  });
}

// Broadcast room state to SSE clients
function broadcastRoom(room: Room) {
  room.lastUpdate = Date.now();
  const payload = JSON.stringify({
    code: room.code,
    name: room.name,
    hostId: room.hostId,
    settings: room.settings,
    phase: room.phase,
    currentRound: room.currentRound,
    currentLetter: room.currentLetter,
    phaseTimeRemaining: room.phaseTimeRemaining,
    phaseStartTime: room.phaseStartTime,
    stopperPlayerId: room.stopperPlayerId,
    players: room.players,
    chat: room.chat.slice(-30),
    roundReviewVotes: room.roundReviewVotes
  });

  for (const client of room.sseClients) {
    try {
      client.write(`data: ${payload}\n\n`);
    } catch {
      room.sseClients.delete(client);
    }
  }
}

// Generate room code like 'STOP-4892'
function generateRoomCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let num = '';
  for (let i = 0; i < 4; i++) {
    num += chars[Math.floor(Math.random() * chars.length)];
  }
  return `STOP-${num}`;
}

// API Routes

// 1. Health check
app.get('/api/health', (_req: Request, res: Response) => {
  res.json({ status: 'ok', timestamp: Date.now() });
});

// Auth Routes: Email registration, email login, Google sign in, session management
app.post('/api/auth/register', (req: Request, res: Response) => {
  const { email, password, name, avatar, existingProfileId } = req.body;
  if (!validateEmail(email)) {
    return res.status(400).json({ error: 'Please provide a valid email address' });
  }
  const passCheck = validatePassword(password);
  if (!passCheck.valid) {
    return res.status(400).json({ error: passCheck.error });
  }

  const cleanEmail = email.trim().toLowerCase();
  if (usersByEmail.has(cleanEmail)) {
    return res.status(400).json({ error: 'An account with this email already exists. Please sign in.' });
  }

  const userId = existingProfileId && profiles.has(existingProfileId)
    ? existingProfileId
    : `usr_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

  const userRecord: UserRecord = {
    id: userId,
    email: cleanEmail,
    name: name?.trim() || cleanEmail.split('@')[0],
    avatar: avatar || '⚡',
    passwordHash: hashPassword(password),
    authProvider: 'email',
    createdAt: new Date().toISOString()
  };

  users.set(userId, userRecord);
  usersByEmail.set(cleanEmail, userId);
  saveUsers();

  let prof = profiles.get(userId);
  if (!prof) {
    prof = {
      id: userId,
      name: userRecord.name,
      avatar: userRecord.avatar,
      syncCode: `STOP-${Math.floor(1000 + Math.random() * 9000)}`,
      title: 'Rookie Rusher',
      stats: {
        gamesPlayed: 0,
        wins: 0,
        stopsCalled: 0,
        totalPoints: 0,
        bestRoundScore: 0,
        currentStreak: 0,
        dailyChallengesCompleted: [],
        averageResponseTimeSec: 0
      },
      createdAt: new Date().toISOString()
    };
  } else {
    prof.name = userRecord.name;
    prof.avatar = userRecord.avatar;
  }
  profiles.set(userId, prof);
  syncCodes.set(prof.syncCode.toUpperCase(), userId);
  saveProfiles();

  const token = `st_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`;
  sessions.set(token, { userId, createdAt: Date.now() });

  const safeUser = { id: userRecord.id, email: userRecord.email, name: userRecord.name, avatar: userRecord.avatar, authProvider: userRecord.authProvider, createdAt: userRecord.createdAt };
  res.json({ token, user: safeUser, profile: prof });
});

app.post('/api/auth/login', (req: Request, res: Response) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'Email and password are required' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const userId = usersByEmail.get(cleanEmail);
  if (!userId) {
    return res.status(401).json({ error: 'No account found with this email. Please register.' });
  }

  const user = users.get(userId);
  if (!user || user.passwordHash !== hashPassword(password)) {
    return res.status(401).json({ error: 'Invalid password. Please try again.' });
  }

  const token = `st_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`;
  sessions.set(token, { userId, createdAt: Date.now() });

  const prof = profiles.get(userId);
  const safeUser = { id: user.id, email: user.email, name: user.name, avatar: user.avatar, authProvider: user.authProvider, createdAt: user.createdAt };
  res.json({ token, user: safeUser, profile: prof });
});

app.post('/api/auth/google', (req: Request, res: Response) => {
  const { email, name, avatar, existingProfileId } = req.body;
  if (!email) {
    return res.status(400).json({ error: 'Google account email is required' });
  }

  const cleanEmail = email.trim().toLowerCase();
  const existingUserId = usersByEmail.get(cleanEmail);
  let user: UserRecord;
  let targetUserId: string;

  if (existingUserId && users.has(existingUserId)) {
    targetUserId = existingUserId;
    user = users.get(existingUserId)!;
    if (name) user.name = name;
    if (avatar) user.avatar = avatar;
  } else {
    targetUserId = (existingProfileId && profiles.has(existingProfileId))
      ? existingProfileId
      : `usr_g_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;

    user = {
      id: targetUserId,
      email: cleanEmail,
      name: name || cleanEmail.split('@')[0],
      avatar: avatar || '⚡',
      authProvider: 'google',
      createdAt: new Date().toISOString()
    };
    users.set(targetUserId, user);
    usersByEmail.set(cleanEmail, targetUserId);
  }
  saveUsers();

  let prof = profiles.get(targetUserId);
  if (!prof) {
    prof = {
      id: targetUserId,
      name: user.name,
      avatar: user.avatar,
      syncCode: `STOP-${Math.floor(1000 + Math.random() * 9000)}`,
      title: 'Rookie Rusher',
      stats: {
        gamesPlayed: 0,
        wins: 0,
        stopsCalled: 0,
        totalPoints: 0,
        bestRoundScore: 0,
        currentStreak: 0,
        dailyChallengesCompleted: [],
        averageResponseTimeSec: 0
      },
      createdAt: new Date().toISOString()
    };
  } else {
    prof.name = user.name;
    prof.avatar = user.avatar;
  }
  profiles.set(targetUserId, prof);
  syncCodes.set(prof.syncCode.toUpperCase(), targetUserId);
  saveProfiles();

  const token = `st_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`;
  sessions.set(token, { userId: targetUserId, createdAt: Date.now() });

  const safeUser = { id: user.id, email: user.email, name: user.name, avatar: user.avatar, authProvider: user.authProvider, createdAt: user.createdAt };
  res.json({ token, user: safeUser, profile: prof });
});

app.get('/api/auth/session', (req: Request, res: Response) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'No active session' });
  }

  const token = authHeader.split(' ')[1];
  const sess = sessions.get(token);
  if (!sess) {
    return res.status(401).json({ error: 'Session expired or invalid' });
  }

  const user = users.get(sess.userId);
  if (!user) {
    return res.status(401).json({ error: 'User not found' });
  }

  const profile = profiles.get(sess.userId);
  const safeUser = { id: user.id, email: user.email, name: user.name, avatar: user.avatar, authProvider: user.authProvider, createdAt: user.createdAt };
  res.json({ user: safeUser, profile });
});

app.post('/api/auth/logout', (req: Request, res: Response) => {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1];
    sessions.delete(token);
  }
  res.json({ success: true });
});

// 2. Profile endpoints
app.get('/api/profile/:id', (req: Request, res: Response) => {
  const profile = profiles.get(req.params.id);
  if (!profile) {
    return res.status(404).json({ error: 'Profile not found' });
  }
  res.json(profile);
});

app.post('/api/profile', (req: Request, res: Response) => {
  const body = req.body as Partial<ProfileRecord>;
  if (!body.id || !body.name) {
    return res.status(400).json({ error: 'Missing id or name' });
  }

  let syncCode = body.syncCode;
  if (!syncCode) {
    syncCode = `STOP-${Math.floor(1000 + Math.random() * 9000)}`;
  }

  const existing = profiles.get(body.id) || {
    id: body.id,
    name: body.name,
    avatar: body.avatar || '⚡',
    syncCode: syncCode.toUpperCase(),
    title: body.title || 'Rookie Rusher',
    stats: {
      gamesPlayed: 0,
      wins: 0,
      stopsCalled: 0,
      totalPoints: 0,
      bestRoundScore: 0,
      currentStreak: 0,
      dailyChallengesCompleted: [],
      averageResponseTimeSec: 0
    },
    createdAt: new Date().toISOString()
  };

  const updated: ProfileRecord = {
    ...existing,
    name: body.name || existing.name,
    avatar: body.avatar || existing.avatar,
    title: body.title || existing.title,
    stats: {
      ...existing.stats,
      ...(body.stats || {})
    }
  };

  profiles.set(updated.id, updated);
  syncCodes.set(updated.syncCode.toUpperCase(), updated.id);
  saveProfiles();

  res.json(updated);
});

// Restore profile across devices via Sync Code
app.post('/api/profile/restore', (req: Request, res: Response) => {
  const { syncCode } = req.body;
  if (!syncCode) {
    return res.status(400).json({ error: 'Sync code is required' });
  }

  const codeClean = String(syncCode).trim().toUpperCase();
  const profileId = syncCodes.get(codeClean);
  if (!profileId || !profiles.has(profileId)) {
    return res.status(404).json({ error: 'No profile found with that sync code' });
  }

  res.json(profiles.get(profileId));
});

// 3. Leaderboards
app.get('/api/leaderboards', (req: Request, res: Response) => {
  const period = req.query.period || 'all';
  const data = getLeaderboardData();

  // If weekly, slice or adjust ranking scores for weekly competition
  if (period === 'weekly') {
    const weekly = data.map(d => ({
      ...d,
      totalPoints: Math.round(d.totalPoints * 0.42),
      wins: Math.max(1, Math.round(d.wins * 0.4))
    })).sort((a, b) => b.totalPoints - a.totalPoints).map((item, idx) => ({ ...item, rank: idx + 1 }));
    return res.json({ leaderboards: weekly, period: 'weekly' });
  }

  res.json({ leaderboards: data, period: 'all' });
});

// Record completed match to leaderboards
app.post('/api/leaderboards/record', (req: Request, res: Response) => {
  const { playerId, pointsEarned, won, stopped } = req.body;
  if (!playerId) {
    return res.status(400).json({ error: 'Missing playerId' });
  }

  const p = profiles.get(playerId);
  if (p) {
    p.stats.gamesPlayed += 1;
    if (won) p.stats.wins += 1;
    if (stopped) p.stats.stopsCalled += 1;
    p.stats.totalPoints += (pointsEarned || 0);
    p.stats.currentStreak = won ? (p.stats.currentStreak + 1) : 0;
    saveProfiles();
  }

  res.json({ success: true, updated: p });
});

// 4. Daily Challenge
app.get('/api/daily-challenge', (_req: Request, res: Response) => {
  const challenge = getDailyChallenge();
  res.json(challenge);
});

app.post('/api/daily-challenge/submit', (req: Request, res: Response) => {
  const { playerId, date, score } = req.body;
  if (!playerId) {
    return res.status(400).json({ error: 'Missing playerId' });
  }

  const p = profiles.get(playerId);
  if (p) {
    const today = date || new Date().toISOString().split('T')[0];
    if (!p.stats.dailyChallengesCompleted.includes(today)) {
      p.stats.dailyChallengesCompleted.push(today);
      p.stats.totalPoints += (score || 30);
      saveProfiles();
    }
  }

  res.json({ success: true, profile: p });
});

// 5. Multiplayer Rooms
app.get('/api/rooms', (_req: Request, res: Response) => {
  const list = Array.from(rooms.values())
    .filter(r => !r.settings.isPrivate && r.phase === 'LOBBY')
    .map(r => ({
      code: r.code,
      name: r.name,
      playerCount: r.players.length,
      maxRounds: r.settings.roundCount,
      duration: r.settings.roundDurationSec,
      hostName: r.players.find(p => p.id === r.hostId)?.name || 'Host'
    }));
  res.json(list);
});

app.post('/api/rooms', (req: Request, res: Response) => {
  const { host, settings, name } = req.body;
  if (!host || !host.id) {
    return res.status(400).json({ error: 'Host information required' });
  }

  const code = generateRoomCode();
  const newRoom: Room = {
    code,
    name: name || `${host.name}'s Party`,
    hostId: host.id,
    settings: {
      roundCount: settings?.roundCount || 3,
      roundDurationSec: settings?.roundDurationSec || 60,
      categories: settings?.categories || DEFAULT_CATEGORIES.slice(0, 6),
      isPrivate: settings?.isPrivate ?? false,
      difficulty: settings?.difficulty || 'medium'
    },
    phase: 'LOBBY',
    currentRound: 0,
    currentLetter: '',
    phaseTimeRemaining: settings?.roundDurationSec || 60,
    phaseStartTime: Date.now(),
    stopperPlayerId: null,
    players: [
      {
        id: host.id,
        name: host.name,
        avatar: host.avatar || '👑',
        isHost: true,
        isReady: true,
        totalScore: 0,
        roundScore: 0,
        answers: {},
        hasStopped: false
      }
    ],
    chat: [
      {
        id: `sys_${Date.now()}`,
        senderId: 'system',
        senderName: 'System',
        senderAvatar: '📢',
        text: `Room ${code} created. Share the code with friends to join!`,
        timestamp: Date.now(),
        isSystem: true
      }
    ],
    lastUpdate: Date.now(),
    roundReviewVotes: {},
    sseClients: new Set()
  };

  rooms.set(code, newRoom);
  res.json({ code, room: newRoom });
});

// Join Room
app.post('/api/rooms/:code/join', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) {
    return res.status(404).json({ error: 'Room not found' });
  }

  const { player } = req.body;
  if (!player || !player.id) {
    return res.status(400).json({ error: 'Player information required' });
  }

  const existing = room.players.find(p => p.id === player.id);
  if (!existing) {
    room.players.push({
      id: player.id,
      name: player.name,
      avatar: player.avatar || '⭐',
      isHost: false,
      isReady: true,
      totalScore: 0,
      roundScore: 0,
      answers: {},
      hasStopped: false
    });

    room.chat.push({
      id: `chat_${Date.now()}`,
      senderId: 'system',
      senderName: 'System',
      senderAvatar: '👋',
      text: `${player.name} joined the room!`,
      timestamp: Date.now(),
      isSystem: true
    });
  }

  broadcastRoom(room);
  res.json({ success: true, room });
});

// Room State
app.get('/api/rooms/:code', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) {
    return res.status(404).json({ error: 'Room not found' });
  }
  res.json(room);
});

// Start round
app.post('/api/rooms/:code/start', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) return res.status(404).json({ error: 'Room not found' });

  // Pick random letter from VALID_LETTERS
  const letter = VALID_LETTERS[Math.floor(Math.random() * VALID_LETTERS.length)];
  room.currentRound += 1;
  room.currentLetter = letter;
  room.phase = 'ROUND_ACTIVE';
  room.phaseStartTime = Date.now();
  room.phaseTimeRemaining = room.settings.roundDurationSec;
  room.stopperPlayerId = null;
  room.roundReviewVotes = {};

  for (const p of room.players) {
    p.answers = {};
    p.hasStopped = false;
    p.roundScore = 0;
    delete p.stopElapsedSec;
  }

  room.chat.push({
    id: `chat_${Date.now()}`,
    senderId: 'system',
    senderName: 'System',
    senderAvatar: '🎯',
    text: `Round ${room.currentRound} started with letter '${letter}'! Fill the categories fast!`,
    timestamp: Date.now(),
    isSystem: true
  });

  broadcastRoom(room);
  res.json({ success: true, room });
});

// Slap STOP button
app.post('/api/rooms/:code/stop', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) return res.status(404).json({ error: 'Room not found' });
  if (room.phase !== 'ROUND_ACTIVE') {
    return res.status(400).json({ error: 'Not in active round' });
  }

  const { playerId, elapsedSec } = req.body;
  const player = room.players.find(p => p.id === playerId);
  if (!player) return res.status(404).json({ error: 'Player not found' });

  player.hasStopped = true;
  player.stopElapsedSec = elapsedSec || 30;
  room.stopperPlayerId = playerId;
  room.phase = 'STOP_CALLED';

  room.chat.push({
    id: `chat_${Date.now()}`,
    senderId: 'system',
    senderName: 'System',
    senderAvatar: '🛑',
    text: `🚨 STOP CALLED BY ${player.name.toUpperCase()}! Pens down!`,
    timestamp: Date.now(),
    isSystem: true
  });

  // Calculate scores immediately
  const submissions = room.players.map(p => ({
    playerId: p.id,
    answers: p.answers || {},
    hasStopped: p.hasStopped,
    stopElapsedSec: p.stopElapsedSec
  }));

  const scores = calculateRoundScores(submissions, room.settings.categories, room.currentLetter);
  for (const s of scores) {
    const pl = room.players.find(p => p.id === s.playerId);
    if (pl) {
      pl.roundScore = s.totalRoundScore;
      pl.totalScore += s.totalRoundScore;
    }
  }

  broadcastRoom(room);
  res.json({ success: true, room, scores });
});

// Submit answers during play
app.post('/api/rooms/:code/submit', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) return res.status(404).json({ error: 'Room not found' });

  const { playerId, answers } = req.body;
  const player = room.players.find(p => p.id === playerId);
  if (player && answers) {
    player.answers = answers;
  }

  res.json({ success: true });
});

// Chat message
app.post('/api/rooms/:code/chat', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) return res.status(404).json({ error: 'Room not found' });

  const { senderId, senderName, senderAvatar, text } = req.body;
  if (!text) return res.status(400).json({ error: 'Text required' });

  const msg = {
    id: `msg_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
    senderId: senderId || 'anon',
    senderName: senderName || 'Player',
    senderAvatar: senderAvatar || '💬',
    text: String(text).slice(0, 200),
    timestamp: Date.now()
  };

  room.chat.push(msg);
  broadcastRoom(room);
  res.json({ success: true, message: msg });
});

// SSE Stream for instant realtime sync
app.get('/api/rooms/:code/events', (req: Request, res: Response) => {
  const code = req.params.code.toUpperCase();
  const room = rooms.get(code);
  if (!room) {
    return res.status(404).end();
  }

  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');
  res.flushHeaders();

  room.sseClients.add(res);

  // Send initial state
  const payload = JSON.stringify({
    code: room.code,
    name: room.name,
    hostId: room.hostId,
    settings: room.settings,
    phase: room.phase,
    currentRound: room.currentRound,
    currentLetter: room.currentLetter,
    phaseTimeRemaining: room.phaseTimeRemaining,
    phaseStartTime: room.phaseStartTime,
    stopperPlayerId: room.stopperPlayerId,
    players: room.players,
    chat: room.chat.slice(-30),
    roundReviewVotes: room.roundReviewVotes
  });
  res.write(`data: ${payload}\n\n`);

  req.on('close', () => {
    room.sseClients.delete(res);
  });
});

// APK Download endpoint for Android
app.get(['/api/download/stop-game.apk', '/api/apk', '/downloads/STOP_Multiplayer_v1.0.apk'], (_req: Request, res: Response) => {
  const apkPath = path.join(__dirname, 'public', 'downloads', 'STOP_Multiplayer_v1.0.apk');
  if (fs.existsSync(apkPath)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="STOP_Multiplayer_v1.0.apk"');
    return res.sendFile(apkPath);
  }
  return res.status(404).json({ error: 'APK file not found' });
});

// Serve public static assets (manifest, sw, icons, apk)
app.use(express.static(path.join(__dirname, 'public')));

// Vite middleware / Static file handler
const isProduction = process.env.NODE_ENV === 'production';
if (!isProduction) {
  const vite = await createViteServer({
    server: { middlewareMode: true },
    appType: 'spa',
  });
  app.use(vite.middlewares);
} else {
  app.use(express.static(path.join(__dirname, 'dist')));
  app.get('*', (_req: Request, res: Response) => {
    res.sendFile(path.join(__dirname, 'dist', 'index.html'));
  });
}

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`STOP! Multiplayer server running on http://0.0.0.0:${PORT}`);
});
