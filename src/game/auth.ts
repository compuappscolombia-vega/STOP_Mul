import crypto from 'node:crypto';

/**
 * Validates email format
 */
export function validateEmail(email: string): boolean {
  if (!email || typeof email !== 'string') return false;
  const trimmed = email.trim().toLowerCase();
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(trimmed);
}

/**
 * Validates password strength (minimum 6 characters)
 */
export function validatePassword(password: string): { valid: boolean; error?: string } {
  if (!password || typeof password !== 'string') {
    return { valid: false, error: 'Password is required' };
  }
  if (password.length < 6) {
    return { valid: false, error: 'Password must be at least 6 characters long' };
  }
  return { valid: true };
}

/**
 * Deterministic SHA-256 hash with salt for secure storage
 */
export function hashPassword(password: string, salt: string = 'compuapps_stop_secure_salt_2026'): string {
  return crypto.createHash('sha256').update(`${salt}:${password}`).digest('hex');
}
