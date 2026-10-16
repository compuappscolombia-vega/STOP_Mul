import { UserProfile, AuthUser, AuthSession } from '../types/game';
import { getPlayerTier } from '../game/rules';

const STORAGE_KEY = 'stop_game_user_profile_v1';
const AUTH_TOKEN_KEY = 'stop_game_auth_token_v1';
const AUTH_USER_KEY = 'stop_game_auth_user_v1';

export function getAuthToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(AUTH_TOKEN_KEY);
}

export function setAuthToken(token: string | null): void {
  if (typeof window === 'undefined') return;
  if (token) {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
  } else {
    localStorage.removeItem(AUTH_TOKEN_KEY);
  }
}

export function getAuthUser(): AuthUser | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function setAuthUser(user: AuthUser | null): void {
  if (typeof window === 'undefined') return;
  if (user) {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  } else {
    localStorage.removeItem(AUTH_USER_KEY);
  }
}

export function createDefaultProfile(): UserProfile {
  const randomSuffix = Math.floor(1000 + Math.random() * 9000);
  const avatars = ['⚡', '🦁', '🚀', '🦉', '🦊', '🔥', '👑', '🎯', '🥑', '💎'];
  const avatar = avatars[Math.floor(Math.random() * avatars.length)];
  const syncCode = `STOP-${randomSuffix}`;

  return {
    id: `usr_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
    name: `Player_${randomSuffix}`,
    avatar,
    syncCode,
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
}

export function loadLocalProfile(): UserProfile {
  if (typeof window === 'undefined') return createDefaultProfile();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && parsed.id && parsed.name) {
        return parsed;
      }
    }
  } catch (e) {
    console.warn('Failed to parse local profile:', e);
  }
  const defaultProfile = createDefaultProfile();
  saveLocalProfile(defaultProfile);
  return defaultProfile;
}

export function saveLocalProfile(profile: UserProfile): void {
  if (typeof window === 'undefined') return;
  try {
    const tier = getPlayerTier(profile.stats.totalPoints);
    if (!profile.title || profile.title === 'Rookie Rusher') {
      profile.title = tier.rankTitle;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
  } catch (e) {
    console.warn('Failed to save local profile:', e);
  }
}

/**
 * Fetch current active session from server using saved token
 */
export async function fetchCurrentSession(): Promise<{ user: AuthUser; profile: UserProfile } | null> {
  const token = getAuthToken();
  if (!token) return null;

  try {
    const res = await fetch('/api/auth/session', {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (res.ok) {
      const data = await res.json();
      setAuthUser(data.user);
      if (data.profile) {
        data.profile.isRegistered = true;
        data.profile.email = data.user.email;
        saveLocalProfile(data.profile);
      }
      return data;
    } else {
      setAuthToken(null);
      setAuthUser(null);
    }
  } catch {
    // Offline mode: keep local auth user if present
  }
  return null;
}

/**
 * Register account with email formulary
 */
export async function registerWithEmail(
  email: string,
  password: string,
  name: string,
  avatar: string,
  existingProfileId?: string
): Promise<AuthSession> {
  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, name, avatar, existingProfileId })
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || 'Failed to register account');
  }

  setAuthToken(data.token);
  setAuthUser(data.user);
  if (data.profile) {
    data.profile.isRegistered = true;
    data.profile.email = data.user.email;
    saveLocalProfile(data.profile);
  }
  return data;
}

/**
 * Sign in with email and password
 */
export async function loginWithEmail(email: string, password: string): Promise<AuthSession> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || 'Invalid email or password');
  }

  setAuthToken(data.token);
  setAuthUser(data.user);
  if (data.profile) {
    data.profile.isRegistered = true;
    data.profile.email = data.user.email;
    saveLocalProfile(data.profile);
  }
  return data;
}

/**
 * Sign in or sign up with Google account
 */
export async function signInWithGoogle(
  email: string,
  name: string,
  avatar: string,
  existingProfileId?: string
): Promise<AuthSession> {
  const res = await fetch('/api/auth/google', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, name, avatar, existingProfileId })
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || 'Failed to authenticate with Google');
  }

  setAuthToken(data.token);
  setAuthUser(data.user);
  if (data.profile) {
    data.profile.isRegistered = true;
    data.profile.email = data.user.email;
    saveLocalProfile(data.profile);
  }
  return data;
}

/**
 * Sign out and clear active session
 */
export async function logoutUser(): Promise<void> {
  const token = getAuthToken();
  if (token) {
    fetch('/api/auth/logout', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    }).catch(() => {});
  }
  setAuthToken(null);
  setAuthUser(null);
}

/**
 * Sync profile with backend server if available
 */
export async function syncProfileWithServer(profile: UserProfile): Promise<UserProfile> {
  try {
    const token = getAuthToken();
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch('/api/profile', {
      method: 'POST',
      headers,
      body: JSON.stringify(profile)
    });
    if (res.ok) {
      const serverProfile = await res.json();
      saveLocalProfile(serverProfile);
      return serverProfile;
    }
  } catch {
    // Offline fallback
  }
  return profile;
}

/**
 * Restore profile from another device using Sync Code
 */
export async function restoreProfileWithSyncCode(syncCode: string): Promise<UserProfile> {
  const res = await fetch('/api/profile/restore', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ syncCode: syncCode.trim().toUpperCase() })
  });

  if (!res.ok) {
    const err = await res.json();
    throw new Error(err.error || 'Failed to restore profile');
  }

  const restored = await res.json();
  saveLocalProfile(restored);
  return restored;
}
