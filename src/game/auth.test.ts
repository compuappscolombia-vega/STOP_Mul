import test from 'node:test';
import assert from 'node:assert/strict';
import { validateEmail, validatePassword, hashPassword } from './auth.ts';

test('validateEmail validates valid and invalid email addresses', () => {
  assert.equal(validateEmail('user@example.com'), true);
  assert.equal(validateEmail('gato-284@hotmail.com'), true);
  assert.equal(validateEmail('john.doe+tag@sub.domain.org'), true);
  assert.equal(validateEmail('invalid-email'), false);
  assert.equal(validateEmail('user@'), false);
  assert.equal(validateEmail('@domain.com'), false);
  assert.equal(validateEmail(''), false);
});

test('validatePassword requires at least 6 characters', () => {
  assert.equal(validatePassword('secret123').valid, true);
  assert.equal(validatePassword('12345').valid, false);
  assert.equal(validatePassword('').valid, false);
});

test('hashPassword generates consistent SHA-256 hashes', () => {
  const hash1 = hashPassword('mySecretPass!');
  const hash2 = hashPassword('mySecretPass!');
  const hashDiff = hashPassword('differentPass');

  assert.equal(hash1, hash2);
  assert.notEqual(hash1, hashDiff);
  assert.equal(hash1.length, 64);
});
