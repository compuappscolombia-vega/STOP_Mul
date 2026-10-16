/**
 * STOP! Multiplayer Core Game Rules & Pure Logic Engine
 * Handles letter selection, string normalization, category validation,
 * score calculations, bot answer generation, and daily challenge generation.
 */

export interface Category {
  id: string;
  name: string;
  icon: string;
  placeholder: string;
  description: string;
}

export const DEFAULT_CATEGORIES: Category[] = [
  { id: 'name', name: 'Name', icon: '👤', placeholder: 'e.g. Alice, Bob...', description: 'First or last names of real people' },
  { id: 'animal', name: 'Animal', icon: '🦁', placeholder: 'e.g. Antelope, Bear...', description: 'Any animal, bird, insect, or fish' },
  { id: 'country_city', name: 'Country or City', icon: '🌍', placeholder: 'e.g. Argentina, Berlin...', description: 'Sovereign nations, major cities, or capitals' },
  { id: 'food', name: 'Food or Fruit', icon: '🍕', placeholder: 'e.g. Apple, Burrito...', description: 'Any fruit, vegetable, dish, or cuisine' },
  { id: 'object', name: 'Object / Thing', icon: '📦', placeholder: 'e.g. Anchor, Backpack...', description: 'Physical items, household tools, or objects' },
  { id: 'job', name: 'Profession / Job', icon: '💼', placeholder: 'e.g. Architect, Baker...', description: 'Careers, occupations, or titles' },
  { id: 'brand', name: 'Brand or Company', icon: '🏷️', placeholder: 'e.g. Apple, Boeing...', description: 'Famous brands, companies, or products' },
  { id: 'celebrity', name: 'Celebrity or Character', icon: '⭐', placeholder: 'e.g. Adele, Batman...', description: 'Famous celebrities, athletes, or fictional heroes' },
  { id: 'color', name: 'Color', icon: '🎨', placeholder: 'e.g. Amber, Blue...', description: 'Colors, shades, or hues' },
  { id: 'movie_game', name: 'Movie, Show or Game', icon: '🎬', placeholder: 'e.g. Avatar, Bioshock...', description: 'Films, TV shows, video games, or board games' },
  { id: 'sport_hobby', name: 'Sport or Hobby', icon: '⚽', placeholder: 'e.g. Archery, Baseball...', description: 'Sports, recreational activities, or hobbies' },
  { id: 'plant', name: 'Plant or Flower', icon: '🌸', placeholder: 'e.g. Aloe, Begonia...', description: 'Flowers, trees, herbs, or plants' }
];

export const VALID_LETTERS = [
  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
  'K', 'L', 'M', 'N', 'O', 'P', 'R', 'S', 'T', 'U',
  'V', 'W', 'Y', 'Z'
]; // Letters like Q, X can be opt-in or rare

/**
 * Normalizes text by trimming, converting to lower-case, stripping accents and initial articles
 */
export function normalizeAnswer(text: string): string {
  if (!text) return '';
  let cleaned = text.trim().toLowerCase();
  
  // Normalize diacritics: é -> e, ñ -> n, etc.
  cleaned = cleaned.normalize('NFD').replace(/[\u0300-\u036f]/g, '');

  // Strip leading common articles
  const articles = ['the ', 'a ', 'an ', 'el ', 'la ', 'los ', 'las ', 'le ', 'un ', 'une '];
  for (const article of articles) {
    if (cleaned.startsWith(article)) {
      cleaned = cleaned.slice(article.length).trim();
      break;
    }
  }

  // Remove trailing punctuation
  cleaned = cleaned.replace(/[.,/#!$%^&*;:{}=\-_`~()]/g, '').trim();
  return cleaned;
}

/**
 * Checks if normalized answer starts with target letter
 */
export function matchesStartingLetter(text: string, letter: string): boolean {
  const normText = normalizeAnswer(text);
  const normLetter = letter.trim().toLowerCase();
  if (!normText || !normLetter) return false;
  return normText.startsWith(normLetter);
}

/**
 * Built-in dictionary sample dataset for fast client & server validation
 */
export const CATEGORY_WORDS: Record<string, string[]> = {
  animal: [
    'aardvark', 'albatross', 'alligator', 'alpaca', 'ant', 'anteater', 'antelope', 'ape', 'armadillo', 'baboon',
    'badger', 'barracuda', 'bat', 'bear', 'beaver', 'bee', 'bison', 'boar', 'buffalo', 'butterfly',
    'camel', 'capybara', 'caribou', 'cassowary', 'cat', 'caterpillar', 'cheetah', 'chicken', 'chimpanzee', 'chinchilla',
    'clam', 'cobra', 'cockroach', 'cod', 'cormorant', 'coyote', 'crab', 'crane', 'crocodile', 'crow',
    'deer', 'dinosaur', 'dog', 'dolphin', 'donkey', 'dove', 'dragonfly', 'duck', 'eagle', 'echidna',
    'eel', 'elephant', 'elk', 'emu', 'falcon', 'ferret', 'finch', 'flamingo', 'fox', 'frog',
    'gazelle', 'gecko', 'gerbil', 'giraffe', 'goat', 'goldfish', 'goose', 'gorilla', 'grasshopper', 'grizzly',
    'hamster', 'hare', 'hawk', 'hedgehog', 'heron', 'herring', 'hippopotamus', 'hornet', 'horse', 'hummingbird',
    'hyena', 'ibex', 'iguana', 'impala', 'jackal', 'jaguar', 'jellyfish', 'kangaroo', 'kingfisher', 'koala',
    'komodo', 'kookaburra', 'lemur', 'leopard', 'lion', 'llama', 'lobster', 'lynx', 'macaw', 'magpie',
    'mallard', 'mammoth', 'manatee', 'mandrill', 'mantis', 'meerkat', 'mink', 'mole', 'mongoose', 'monkey',
    'moose', 'mosquito', 'mouse', 'mule', 'narwhal', 'newt', 'nightingale', 'ocelot', 'octopus', 'okapi',
    'opossum', 'ostrich', 'otter', 'owl', 'ox', 'oyster', 'panda', 'panther', 'parrot', 'partridge',
    'peacock', 'pelican', 'penguin', 'pheasant', 'pig', 'pigeon', 'piranha', 'platypus', 'polar bear', 'porcupine',
    'porpoise', 'quail', 'rabbit', 'raccoon', 'ram', 'rat', 'raven', 'reindeer', 'rhinoceros', 'roadrunner',
    'robin', 'salamander', 'salmon', 'sandpiper', 'sardine', 'scorpion', 'seahorse', 'seal', 'shark', 'sheep',
    'shrew', 'shrimp', 'skunk', 'sloth', 'snail', 'snake', 'sparrow', 'spider', 'squid', 'squirrel',
    'starfish', 'stingray', 'stork', 'swan', 'tapir', 'tarantula', 'termite', 'tiger', 'toad', 'toucan',
    'trout', 'turkey', 'turtle', 'viper', 'vulture', 'walrus', 'wasp', 'weasel', 'whale', 'wolf',
    'wolverine', 'wombat', 'woodpecker', 'yak', 'zebra'
  ],
  country_city: [
    'afghanistan', 'albania', 'algeria', 'amsterdam', 'andorra', 'angola', 'argentina', 'armenia', 'athens', 'auckland',
    'austria', 'australia', 'azerbaijan', 'bahamas', 'bahrain', 'baku', 'bangkok', 'bangladesh', 'barcelona', 'beijing',
    'belgium', 'belgrade', 'berlin', 'bogota', 'bolivia', 'boston', 'brazil', 'brussels', 'budapest', 'buenos aires',
    'cairo', 'california', 'cameroon', 'canada', 'cape town', 'caracas', 'chicago', 'chile', 'china', 'colombia',
    'copenhagen', 'costa rica', 'croatia', 'cuba', 'cyprus', 'czech republic', 'dallas', 'damascus', 'denmark', 'denver',
    'dhaka', 'doha', 'dominican republic', 'dubai', 'dublin', 'ecuador', 'edinburgh', 'egypt', 'el salvador', 'estonia',
    'ethiopia', 'fiji', 'finland', 'florence', 'france', 'frankfurt', 'georgia', 'germany', 'ghana', 'greece',
    'guatemala', 'hague', 'hamburg', 'hanoi', 'havana', 'helsinki', 'honduras', 'hong kong', 'hungary', 'iceland',
    'india', 'indonesia', 'iran', 'iraq', 'ireland', 'islamabad', 'israel', 'istanbul', 'italy', 'jakarta',
    'jamaica', 'japan', 'jerusalem', 'johannesburg', 'jordan', 'kazakhstan', 'kenya', 'kiev', 'kuwait', 'kyoto',
    'laos', 'latvia', 'lebanon', 'lima', 'lisbon', 'lithuania', 'london', 'los angeles', 'luxembourg', 'madrid',
    'malaysia', 'maldives', 'manila', 'melbourne', 'mexico', 'miami', 'milan', 'monaco', 'montreal', 'morocco',
    'moscow', 'mumbai', 'munich', 'nairobi', 'nepal', 'netherlands', 'new delhi', 'new york', 'new zealand', 'nigeria',
    'norway', 'oslo', 'ottawa', 'pakistan', 'panama', 'paris', 'peru', 'philippines', 'poland', 'portugal',
    'prague', 'qatar', 'reykjavik', 'rio de janeiro', 'rome', 'romania', 'russia', 'san francisco', 'santiago', 'seoul',
    'singapore', 'south africa', 'spain', 'stockholm', 'sweden', 'switzerland', 'sydney', 'taiwan', 'thailand', 'tokyo',
    'toronto', 'turkey', 'uganda', 'ukraine', 'united kingdom', 'united states', 'uruguay', 'vancouver', 'venice', 'vienna',
    'vietnam', 'warsaw', 'washington', 'yemen', 'zagreb', 'zambia', 'zimbabwe'
  ],
  food: [
    'almond', 'apple', 'apricot', 'artichoke', 'asparagus', 'avocado', 'bacon', 'bagel', 'banana', 'barbecue',
    'barley', 'basil', 'bean', 'beef', 'beer', 'beet', 'berry', 'biscuit', 'blackberry', 'blueberry',
    'bread', 'broccoli', 'brownie', 'burrito', 'butter', 'cabbage', 'cake', 'candy', 'cantaloupe', 'carrot',
    'cashew', 'cauliflower', 'celery', 'cereal', 'cheese', 'cherry', 'chicken', 'chili', 'chocolate', 'cinnamon',
    'clam', 'coconut', 'coffee', 'cookie', 'corn', 'crab', 'cranberry', 'croissant', 'cucumber', 'curry',
    'date', 'donut', 'dragonfruit', 'duck', 'dumpling', 'egg', 'eggplant', 'enchilada', 'fig', 'fish',
    'flour', 'fondue', 'fry', 'garlic', 'ginger', 'grape', 'grapefruit', 'guacamole', 'ham', 'hamburger',
    'honey', 'hot dog', 'hummus', 'ice cream', 'jalapeno', 'jam', 'jelly', 'juice', 'kale', 'kebab',
    'kiwi', 'lasagna', 'lemon', 'lentil', 'lettuce', 'lime', 'lobster', 'macaroni', 'mango', 'marshmallow',
    'meatball', 'melon', 'milk', 'muffin', 'mushroom', 'mustard', 'nachos', 'noodle', 'nut', 'oatmeal',
    'olive', 'omelet', 'onion', 'orange', 'pancake', 'papaya', 'pasta', 'peach', 'peanut', 'pear',
    'pea', 'pecan', 'pepper', 'pickle', 'pie', 'pineapple', 'pizza', 'plum', 'popcorn', 'pork',
    'potato', 'pretzel', 'pudding', 'pumpkin', 'quesadilla', 'quiche', 'radish', 'raisin', 'raspberry', 'ravioli',
    'rice', 'salad', 'salmon', 'sandwich', 'sausage', 'shrimp', 'soup', 'spaghetti', 'spinach', 'steak',
    'strawberry', 'sushi', 'taco', 'toast', 'tomato', 'tuna', 'turkey', 'vanilla', 'waffle', 'walnut',
    'watermelon', 'wheat', 'yogurt', 'zucchini'
  ]
};

/**
 * Validates whether an answer is acceptable for a given category and starting letter.
 * Returns { valid: boolean, reason?: string }
 */
export function validateAnswer(category: string, answer: string, targetLetter: string): { valid: boolean; reason?: string } {
  if (!answer || answer.trim().length === 0) {
    return { valid: false, reason: 'Empty answer' };
  }

  const trimmed = answer.trim();
  if (trimmed.length < 2) {
    return { valid: false, reason: 'Answer too short' };
  }

  if (!matchesStartingLetter(trimmed, targetLetter)) {
    return { valid: false, reason: `Must begin with letter ${targetLetter.toUpperCase()}` };
  }

  // Reject gibberish or obvious keyboard mash (e.g. "asdfghjk", repeated characters "aaaa")
  const norm = normalizeAnswer(trimmed);
  if (/^(.)\1{3,}$/.test(norm)) {
    return { valid: false, reason: 'Invalid repetitive word' };
  }

  // If we have a dictionary list for this category, check known list or plausible phonetic structure
  const categoryList = CATEGORY_WORDS[category];
  if (categoryList) {
    const directMatch = categoryList.some(item => {
      const normItem = normalizeAnswer(item);
      return normItem === norm || norm.includes(normItem) || normItem.includes(norm);
    });
    // In a flexible party game, dictionary is a soft validator:
    // If it matches known dictionary -> 100% valid.
    // If not in dictionary but valid length and letter -> still valid candidate (can be disputed in review)
    return { valid: true, reason: directMatch ? 'Dictionary match' : 'Plausible entry' };
  }

  // Default for custom or unlisted categories: valid as long as length >= 2 and correct letter
  return { valid: true, reason: 'Valid entry' };
}

export interface PlayerRoundSubmission {
  playerId: string;
  answers: Record<string, string>; // categoryId -> answer
  hasStopped: boolean;
  stopElapsedSec?: number;
}

export interface CategoryScoreResult {
  categoryId: string;
  answer: string;
  isValid: boolean;
  isDuplicate: boolean;
  score: number;
  reason?: string;
}

export interface PlayerRoundScore {
  playerId: string;
  categoryScores: Record<string, CategoryScoreResult>;
  baseScore: number;
  timeBonus: number;
  totalRoundScore: number;
  stoppedFirst: boolean;
}

/**
 * Computes official STOP scores across all players in a round:
 * - 10 pts for valid unique answer
 * - 5 pts for valid duplicate answer (shared by >= 2 players)
 * - 0 pts for empty or invalid answer
 * - Time bonus (+5) for the player who slapped STOP!
 */
export function calculateRoundScores(
  submissions: PlayerRoundSubmission[],
  categories: Category[],
  targetLetter: string
): PlayerRoundScore[] {
  // First, validate and normalize all answers
  const playerNormalized: Record<string, Record<string, { raw: string; norm: string; valid: boolean; reason?: string }>> = {};

  for (const sub of submissions) {
    playerNormalized[sub.playerId] = {};
    for (const cat of categories) {
      const raw = sub.answers[cat.id] || '';
      const validation = validateAnswer(cat.id, raw, targetLetter);
      const norm = normalizeAnswer(raw);
      playerNormalized[sub.playerId][cat.id] = {
        raw,
        norm,
        valid: validation.valid,
        reason: validation.reason
      };
    }
  }

  // Determine which answers are duplicated across players for each category
  const answerCountsByCategory: Record<string, Record<string, number>> = {};
  for (const cat of categories) {
    answerCountsByCategory[cat.id] = {};
    for (const sub of submissions) {
      const entry = playerNormalized[sub.playerId][cat.id];
      if (entry.valid && entry.norm) {
        answerCountsByCategory[cat.id][entry.norm] = (answerCountsByCategory[cat.id][entry.norm] || 0) + 1;
      }
    }
  }

  // Find who pressed STOP first
  let firstStopperId: string | null = null;
  let minElapsed = Infinity;
  for (const sub of submissions) {
    if (sub.hasStopped && sub.stopElapsedSec !== undefined && sub.stopElapsedSec < minElapsed) {
      minElapsed = sub.stopElapsedSec;
      firstStopperId = sub.playerId;
    }
  }

  // Calculate scores per player
  const results: PlayerRoundScore[] = [];

  for (const sub of submissions) {
    let baseScore = 0;
    const catScores: Record<string, CategoryScoreResult> = {};

    for (const cat of categories) {
      const entry = playerNormalized[sub.playerId][cat.id];
      if (!entry.valid || !entry.norm) {
        catScores[cat.id] = {
          categoryId: cat.id,
          answer: entry.raw,
          isValid: false,
          isDuplicate: false,
          score: 0,
          reason: entry.reason || 'Invalid'
        };
      } else {
        const count = answerCountsByCategory[cat.id][entry.norm] || 1;
        const isDuplicate = count > 1;
        const pts = isDuplicate ? 5 : 10;
        baseScore += pts;

        catScores[cat.id] = {
          categoryId: cat.id,
          answer: entry.raw,
          isValid: true,
          isDuplicate,
          score: pts,
          reason: isDuplicate ? 'Shared with another player' : 'Unique valid answer'
        };
      }
    }

    const stoppedFirst = sub.playerId === firstStopperId;
    const timeBonus = stoppedFirst ? 5 : 0;

    results.push({
      playerId: sub.playerId,
      categoryScores: catScores,
      baseScore,
      timeBonus,
      totalRoundScore: baseScore + timeBonus,
      stoppedFirst
    });
  }

  return results;
}

/**
 * Generates an answer from an offline bot based on difficulty
 */
export function generateBotAnswer(category: string, letter: string, difficulty: 'easy' | 'medium' | 'hard'): string {
  const normLetter = letter.trim().toLowerCase();
  const list = CATEGORY_WORDS[category] || [];
  const candidates = list.filter(w => normalizeAnswer(w).startsWith(normLetter));

  // If no dictionary items, generate a fallback plausible word
  if (candidates.length === 0) {
    const fallbacks: Record<string, string> = {
      name: `${letter.toUpperCase()}am`,
      animal: `${letter.toUpperCase()}ntelope`,
      country_city: `${letter.toUpperCase()}merica`,
      food: `${letter.toUpperCase()}pple`,
      object: `${letter.toUpperCase()}ctopus toy`,
      job: `${letter.toUpperCase()}ctor`,
      color: `${letter.toUpperCase()}zure`,
      brand: `${letter.toUpperCase()}pex`,
      celebrity: `${letter.toUpperCase()}riana`,
      movie_game: `${letter.toUpperCase()}vatar`,
      sport_hobby: `${letter.toUpperCase()}rchery`,
      plant: `${letter.toUpperCase()}cacia`
    };
    return fallbacks[category] || `${letter.toUpperCase()}o`;
  }

  // Difficulty determines chance of missing or picking simple answer
  const missProbability = difficulty === 'easy' ? 0.35 : difficulty === 'medium' ? 0.15 : 0.05;
  if (Math.random() < missProbability) {
    return ''; // Bot fails to think of one in time
  }

  // Pick candidate
  const chosen = candidates[Math.floor(Math.random() * candidates.length)];
  return chosen.charAt(0).toUpperCase() + chosen.slice(1);
}

/**
 * Calculates human-like thinking delay in milliseconds for a single category field.
 * @param speedSeconds User-configured speed per field (1 to 6 seconds, default 4s)
 * @param difficulty 'easy' | 'medium' | 'hard'
 * @param isFirstField whether this is the opening field of the round (requires initial letter processing)
 */
export function calculateBotFieldDelay(
  speedSeconds: number,
  difficulty: 'easy' | 'medium' | 'hard' = 'medium',
  isFirstField: boolean = false
): number {
  // Base seconds clamped between 1s and 6s
  const baseSec = Math.max(1, Math.min(6, speedSeconds));

  // Initial letter discovery overhead (humans take a few seconds to absorb the new letter)
  const introDelay = isFirstField ? 1.8 + Math.random() * 1.5 : 0;

  // Jitter representing human hesitation and word recall
  const diffMultiplier = difficulty === 'easy' ? 1.25 : difficulty === 'medium' ? 1.0 : 0.85;
  const humanJitter = (Math.random() * 0.8 - 0.4) * baseSec;

  const totalSec = (baseSec * diffMultiplier) + introDelay + humanJitter;
  return Math.max(800, Math.round(totalSec * 1000));
}

/**
 * Calculates realistic total round duration before a bot considers slapping STOP.
 */
export function estimateBotRoundDuration(
  categoryCount: number,
  speedSeconds: number,
  difficulty: 'easy' | 'medium' | 'hard' = 'medium'
): number {
  const baseSec = Math.max(1, Math.min(6, speedSeconds));
  // Total fields x speed, plus letter recognition and final STOP decision hesitation
  const total = 2.5 + (categoryCount * baseSec * (difficulty === 'easy' ? 1.3 : difficulty === 'medium' ? 1.05 : 0.9)) + 2.0;
  return Math.max(10, Math.round(total));
}

/**
 * Generates deterministic Daily Challenge for any date string (YYYY-MM-DD)
 */
export function getDailyChallenge(dateStr?: string): {
  date: string;
  letter: string;
  categories: Category[];
  targetParScore: number;
  timeLimitSec: number;
  title: string;
} {
  const today = dateStr || new Date().toISOString().split('T')[0];
  
  // Simple hash from date string
  let hash = 0;
  for (let i = 0; i < today.length; i++) {
    hash = (hash << 5) - hash + today.charCodeAt(i);
    hash |= 0;
  }
  const absHash = Math.abs(hash);

  // Deterministic letter
  const letter = VALID_LETTERS[absHash % VALID_LETTERS.length];

  // Pick 5 categories deterministically
  const shuffled = [...DEFAULT_CATEGORIES].sort((a, b) => {
    const ha = (a.id.charCodeAt(0) * 31 + absHash) % 100;
    const hb = (b.id.charCodeAt(0) * 31 + absHash) % 100;
    return ha - hb;
  });

  const selectedCategories = shuffled.slice(0, 5);

  const titles = [
    'Quickdraw Vocabulary Blitz',
    'Midnight Category Sprint',
    'The Alphabet Gauntlet',
    'Grand Slam Stop Rush',
    'Master Lexicon Sprint'
  ];
  const title = titles[absHash % titles.length];

  return {
    date: today,
    letter,
    categories: selectedCategories,
    targetParScore: 45,
    timeLimitSec: 45,
    title
  };
}

/**
 * Computes rank badge based on total points
 */
export function getPlayerTier(points: number): {
  tier: string;
  rankTitle: string;
  color: string;
  nextTierPoints: number;
} {
  if (points >= 1500) {
    return { tier: 'Grandmaster', rankTitle: 'Stop Legend', color: '#f43f5e', nextTierPoints: 2000 };
  } else if (points >= 800) {
    return { tier: 'Diamond', rankTitle: 'Speed Lexicographer', color: '#06b6d4', nextTierPoints: 1500 };
  } else if (points >= 400) {
    return { tier: 'Gold', rankTitle: 'Category Champ', color: '#f59e0b', nextTierPoints: 800 };
  } else if (points >= 150) {
    return { tier: 'Silver', rankTitle: 'Word Sprinter', color: '#94a3b8', nextTierPoints: 400 };
  }
  return { tier: 'Bronze', rankTitle: 'Rookie Rusher', color: '#b45309', nextTierPoints: 150 };
}
