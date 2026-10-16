import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeAnswer,
  matchesStartingLetter,
  validateAnswer,
  calculateRoundScores,
  getDailyChallenge,
  getPlayerTier,
  DEFAULT_CATEGORIES,
  calculateBotFieldDelay,
  estimateBotRoundDuration
} from './rules.ts';

test('normalizeAnswer correctly strips diacritics, punctuation and common articles', () => {
  assert.equal(normalizeAnswer('  The Elephant!  '), 'elephant');
  assert.equal(normalizeAnswer('El Salvador'), 'salvador');
  assert.equal(normalizeAnswer('A Banana...'), 'banana');
  assert.equal(normalizeAnswer('Éléphant'), 'elephant');
  assert.equal(normalizeAnswer(''), '');
});

test('matchesStartingLetter checks valid first letter case-insensitively', () => {
  assert.equal(matchesStartingLetter('Elephant', 'E'), true);
  assert.equal(matchesStartingLetter('The Tiger', 'T'), true);
  assert.equal(matchesStartingLetter('Aardvark', 'A'), true);
  assert.equal(matchesStartingLetter('Zebra', 'A'), false);
  assert.equal(matchesStartingLetter('', 'E'), false);
});

test('validateAnswer verifies category criteria and rejects gibberish/repetitive inputs', () => {
  const cat = 'animal';
  const validAnimal = validateAnswer(cat, 'Lion', 'L');
  assert.equal(validAnimal.valid, true);

  const wrongLetter = validateAnswer(cat, 'Lion', 'Z');
  assert.equal(wrongLetter.valid, false);

  const empty = validateAnswer(cat, '   ', 'L');
  assert.equal(empty.valid, false);

  const repetitive = validateAnswer(cat, 'aaaaa', 'A');
  assert.equal(repetitive.valid, false);
});

test('calculateRoundScores awards 10pts for unique, 5pts for duplicates, and 5pts for first STOP', () => {
  const testCategories = DEFAULT_CATEGORIES.slice(0, 3); // name, animal, country_city
  const targetLetter = 'B';

  const submissions = [
    {
      playerId: 'p1',
      answers: {
        name: 'Bob',
        animal: 'Bear',
        country_city: 'Brazil'
      },
      hasStopped: true,
      stopElapsedSec: 15.2
    },
    {
      playerId: 'p2',
      answers: {
        name: 'Bella',
        animal: 'Bear', // Duplicate with p1!
        country_city: 'Belgium'
      },
      hasStopped: false
    }
  ];

  const scores = calculateRoundScores(submissions, testCategories, targetLetter);
  assert.equal(scores.length, 2);

  const p1Score = scores.find(s => s.playerId === 'p1')!;
  const p2Score = scores.find(s => s.playerId === 'p2')!;

  // p1: Name Bob (unique 10) + Animal Bear (duplicate 5) + Country Brazil (unique 10) = 25 + Time bonus 5 = 30
  assert.equal(p1Score.categoryScores.name.score, 10);
  assert.equal(p1Score.categoryScores.animal.score, 5);
  assert.equal(p1Score.categoryScores.country_city.score, 10);
  assert.equal(p1Score.timeBonus, 5);
  assert.equal(p1Score.totalRoundScore, 30);
  assert.equal(p1Score.stoppedFirst, true);

  // p2: Name Bella (unique 10) + Animal Bear (duplicate 5) + Country Belgium (unique 10) = 25 + 0 bonus = 25
  assert.equal(p2Score.categoryScores.name.score, 10);
  assert.equal(p2Score.categoryScores.animal.score, 5);
  assert.equal(p2Score.categoryScores.country_city.score, 10);
  assert.equal(p2Score.timeBonus, 0);
  assert.equal(p2Score.totalRoundScore, 25);
  assert.equal(p2Score.stoppedFirst, false);
});

test('getDailyChallenge produces consistent deterministic output for the same date', () => {
  const challenge1 = getDailyChallenge('2026-10-16');
  const challenge2 = getDailyChallenge('2026-10-16');

  assert.equal(challenge1.letter, challenge2.letter);
  assert.equal(challenge1.title, challenge2.title);
  assert.equal(challenge1.categories.length, 5);
  assert.equal(challenge1.categories[0].id, challenge2.categories[0].id);
});

test('calculateBotFieldDelay respects speedSeconds range and difficulty scaling', () => {
  const delay1s = calculateBotFieldDelay(1, 'hard', false);
  const delay6s = calculateBotFieldDelay(6, 'easy', false);
  const delayFirstField = calculateBotFieldDelay(4, 'medium', true);

  assert.ok(delay1s < delay6s, '1s speed should be significantly faster than 6s speed');
  assert.ok(delayFirstField >= 4000, 'First field should account for initial letter absorption delay');
});

test('estimateBotRoundDuration ensures human-paced breathing room for categories', () => {
  const est4s = estimateBotRoundDuration(6, 4, 'medium');
  const est6s = estimateBotRoundDuration(6, 6, 'easy');

  // 6 categories at 4s should take around 28-35s
  assert.ok(est4s >= 25, '4s per category should give at least 25s before stop');
  // 6 categories at 6s should take around 45-55s
  assert.ok(est6s >= 40, '6s per category should give at least 40s before stop');
});

test('getPlayerTier returns proper rank thresholds', () => {
  assert.equal(getPlayerTier(50).tier, 'Bronze');
  assert.equal(getPlayerTier(200).tier, 'Silver');
  assert.equal(getPlayerTier(500).tier, 'Gold');
  assert.equal(getPlayerTier(1000).tier, 'Diamond');
  assert.equal(getPlayerTier(2000).tier, 'Grandmaster');
});
