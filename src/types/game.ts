import { Category } from '../game/rules';

export interface UserStats {
  gamesPlayed: number;
  wins: number;
  stopsCalled: number;
  totalPoints: number;
  bestRoundScore: number;
  currentStreak: number;
  dailyChallengesCompleted: string[]; // YYYY-MM-DD
  averageResponseTimeSec: number;
}

export interface UserProfile {
  id: string;
  name: string;
  avatar: string;
  syncCode: string;
  title: string;
  stats: UserStats;
  createdAt: string;
  email?: string;
  isRegistered?: boolean;
}

export interface AuthUser {
  id: string;
  email: string;
  name: string;
  avatar: string;
  authProvider: 'email' | 'google';
  createdAt: string;
}

export interface AuthSession {
  token: string;
  user: AuthUser;
  profile: UserProfile;
}

export interface ChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderAvatar: string;
  text: string;
  timestamp: number;
  isSystem?: boolean;
}

export interface RoomSettings {
  roundCount: number;
  roundDurationSec: number; // e.g. 45s, 60s
  categories: Category[];
  isPrivate: boolean;
  difficulty: 'easy' | 'medium' | 'hard';
}

export type RoomPhase =
  | 'LOBBY'
  | 'SPINNING_LETTER'
  | 'ROUND_ACTIVE'
  | 'STOP_CALLED'
  | 'ROUND_REVIEW'
  | 'ROUND_SCORES'
  | 'MATCH_OVER';

export interface PlayerState {
  id: string;
  name: string;
  avatar: string;
  isHost: boolean;
  isBot?: boolean;
  isReady: boolean;
  totalScore: number;
  roundScore: number;
  answers: Record<string, string>; // categoryId -> answer
  hasStopped: boolean;
  stopElapsedSec?: number;
  disputedAnswers?: Record<string, boolean>; // categoryId -> isMarkedInvalid
}

export interface GameRoom {
  code: string;
  name: string;
  hostId: string;
  settings: RoomSettings;
  phase: RoomPhase;
  currentRound: number;
  currentLetter: string;
  phaseTimeRemaining: number;
  phaseStartTime: number;
  stopperPlayerId: string | null;
  players: PlayerState[];
  chat: ChatMessage[];
  lastUpdate: number;
  roundReviewVotes?: Record<string, Record<string, string[]>>; // catId -> targetPlayerId -> voterIds who voted invalid
}

export interface LeaderboardEntry {
  id: string;
  name: string;
  avatar: string;
  title: string;
  totalPoints: number;
  wins: number;
  stopsCalled: number;
  tier: string;
  rank: number;
  streak: number;
}
