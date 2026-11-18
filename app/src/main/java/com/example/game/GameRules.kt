package com.example.game

import com.example.model.Category
import com.example.model.CategoryScoreResult
import com.example.model.DailyChallenge
import com.example.model.Difficulty
import com.example.model.PlayerRoundScore
import com.example.model.PlayerRoundSubmission
import com.example.model.PlayerTier
import com.example.ui.theme.StopColors
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object GameRules {

    val DEFAULT_CATEGORIES: List<Category> = listOf(
        Category("name", "Name", "👤", "e.g. Alice, Bob...", "First or last names of real people"),
        Category("animal", "Animal", "🦁", "e.g. Antelope, Bear...", "Any animal, bird, insect, or fish"),
        Category("country_city", "Country or City", "🌍", "e.g. Argentina, Berlin...", "Sovereign nations, major cities, or capitals"),
        Category("food", "Food or Fruit", "🍕", "e.g. Apple, Burrito...", "Any fruit, vegetable, dish, or cuisine"),
        Category("object", "Object / Thing", "📦", "e.g. Anchor, Backpack...", "Physical items, household tools, or objects"),
        Category("job", "Profession / Job", "💼", "e.g. Architect, Baker...", "Careers, occupations, or titles"),
        Category("brand", "Brand or Company", "🏷️", "e.g. Apple, Boeing...", "Famous brands, companies, or products"),
        Category("celebrity", "Celebrity or Character", "⭐", "e.g. Adele, Batman...", "Famous celebrities, athletes, or fictional heroes"),
        Category("color", "Color", "🎨", "e.g. Amber, Blue...", "Colors, shades, or hues"),
        Category("movie_game", "Movie, Show or Game", "🎬", "e.g. Avatar, Bioshock...", "Films, TV shows, video games, or board games"),
        Category("sport_hobby", "Sport or Hobby", "⚽", "e.g. Archery, Baseball...", "Sports, recreational activities, or hobbies"),
        Category("plant", "Plant or Flower", "🌸", "e.g. Aloe, Begonia...", "Flowers, trees, herbs, or plants")
    )

    val VALID_LETTERS: List<String> = listOf(
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "R", "S", "T", "U",
        "V", "W", "Y", "Z"
    )

    val AVATAR_OPTIONS: List<String> = listOf(
        "⚡", "🦁", "🚀", "🦉", "🦊", "🔥", "👑", "🎯",
        "🥑", "💎", "🎮", "🏎️", "🐉", "🤖", "⭐", "🌈"
    )

    val CATEGORY_WORDS: Map<String, List<String>> = mapOf(
        "animal" to listOf(
            "aardvark", "albatross", "alligator", "alpaca", "ant", "anteater", "antelope", "ape", "armadillo", "baboon",
            "badger", "barracuda", "bat", "bear", "beaver", "bee", "bison", "boar", "buffalo", "butterfly",
            "camel", "capybara", "caribou", "cassowary", "cat", "caterpillar", "cheetah", "chicken", "chimpanzee", "chinchilla",
            "clam", "cobra", "cockroach", "cod", "cormorant", "coyote", "crab", "crane", "crocodile", "crow",
            "deer", "dinosaur", "dog", "dolphin", "donkey", "dove", "dragonfly", "duck", "eagle", "echidna",
            "eel", "elephant", "elk", "emu", "falcon", "ferret", "finch", "flamingo", "fox", "frog",
            "gazelle", "gecko", "gerbil", "giraffe", "goat", "goldfish", "goose", "gorilla", "grasshopper", "grizzly",
            "hamster", "hare", "hawk", "hedgehog", "heron", "herring", "hippopotamus", "hornet", "horse", "hummingbird",
            "hyena", "ibex", "iguana", "impala", "jackal", "jaguar", "jellyfish", "kangaroo", "kingfisher", "koala",
            "komodo", "kookaburra", "lemur", "leopard", "lion", "llama", "lobster", "lynx", "macaw", "magpie",
            "mallard", "mammoth", "manatee", "mandrill", "mantis", "meerkat", "mink", "mole", "mongoose", "monkey",
            "moose", "mosquito", "mouse", "mule", "narwhal", "newt", "nightingale", "ocelot", "octopus", "okapi",
            "opossum", "ostrich", "otter", "owl", "ox", "oyster", "panda", "panther", "parrot", "partridge",
            "peacock", "pelican", "penguin", "pheasant", "pig", "pigeon", "piranha", "platypus", "polar bear", "porcupine",
            "porpoise", "quail", "rabbit", "raccoon", "ram", "rat", "raven", "reindeer", "rhinoceros", "roadrunner",
            "robin", "salamander", "salmon", "sandpiper", "sardine", "scorpion", "seahorse", "seal", "shark", "sheep",
            "shrew", "shrimp", "skunk", "sloth", "snail", "snake", "sparrow", "spider", "squid", "squirrel",
            "starfish", "stingray", "stork", "swan", "tapir", "tarantula", "termite", "tiger", "toad", "toucan",
            "trout", "turkey", "turtle", "viper", "vulture", "walrus", "wasp", "weasel", "whale", "wolf",
            "wolverine", "wombat", "woodpecker", "yak", "zebra"
        ),
        "country_city" to listOf(
            "afghanistan", "albania", "algeria", "amsterdam", "andorra", "angola", "argentina", "armenia", "athens", "auckland",
            "austria", "australia", "azerbaijan", "bahamas", "bahrain", "baku", "bangkok", "bangladesh", "barcelona", "beijing",
            "belgium", "belgrade", "berlin", "bogota", "bolivia", "boston", "brazil", "brussels", "budapest", "buenos aires",
            "cairo", "california", "cameroon", "canada", "cape town", "caracas", "chicago", "chile", "china", "colombia",
            "copenhagen", "costa rica", "croatia", "cuba", "cyprus", "czech republic", "dallas", "damascus", "denmark", "denver",
            "dhaka", "doha", "dominican republic", "dubai", "dublin", "ecuador", "edinburgh", "egypt", "el salvador", "estonia",
            "ethiopia", "fiji", "finland", "florence", "france", "frankfurt", "georgia", "germany", "ghana", "greece",
            "guatemala", "hague", "hamburg", "hanoi", "havana", "helsinki", "honduras", "hong kong", "hungary", "iceland",
            "india", "indonesia", "iran", "iraq", "ireland", "islamabad", "israel", "istanbul", "italy", "jakarta",
            "jamaica", "japan", "jerusalem", "johannesburg", "jordan", "kazakhstan", "kenya", "kiev", "kuwait", "kyoto",
            "laos", "latvia", "lebanon", "lima", "lisbon", "lithuania", "london", "los angeles", "luxembourg", "madrid",
            "malaysia", "maldives", "manila", "melbourne", "mexico", "miami", "milan", "monaco", "montreal", "morocco",
            "moscow", "mumbai", "munich", "nairobi", "nepal", "netherlands", "new delhi", "new york", "new zealand", "nigeria",
            "norway", "oslo", "ottawa", "pakistan", "panama", "paris", "peru", "philippines", "poland", "portugal",
            "prague", "qatar", "reykjavik", "rio de janeiro", "rome", "romania", "russia", "san francisco", "santiago", "seoul",
            "singapore", "south africa", "spain", "stockholm", "sweden", "switzerland", "sydney", "taiwan", "thailand", "tokyo",
            "toronto", "turkey", "uganda", "ukraine", "united kingdom", "united states", "uruguay", "vancouver", "venice", "vienna",
            "vietnam", "warsaw", "washington", "yemen", "zagreb", "zambia", "zimbabwe"
        ),
        "food" to listOf(
            "almond", "apple", "apricot", "artichoke", "asparagus", "avocado", "bacon", "bagel", "banana", "barbecue",
            "barley", "basil", "bean", "beef", "beer", "beet", "berry", "biscuit", "blackberry", "blueberry",
            "bread", "broccoli", "brownie", "burrito", "butter", "cabbage", "cake", "candy", "cantaloupe", "carrot",
            "cashew", "cauliflower", "celery", "cereal", "cheese", "cherry", "chicken", "chili", "chocolate", "cinnamon",
            "clam", "coconut", "coffee", "cookie", "corn", "crab", "cranberry", "croissant", "cucumber", "curry",
            "date", "donut", "dragonfruit", "duck", "dumpling", "egg", "eggplant", "enchilada", "fig", "fish",
            "flour", "fondue", "fry", "garlic", "ginger", "grape", "grapefruit", "guacamole", "ham", "hamburger",
            "honey", "hot dog", "hummus", "ice cream", "jalapeno", "jam", "jelly", "juice", "kale", "kebab",
            "kiwi", "lasagna", "lemon", "lentil", "lettuce", "lime", "lobster", "macaroni", "mango", "marshmallow",
            "meatball", "melon", "milk", "muffin", "mushroom", "mustard", "nachos", "noodle", "nut", "oatmeal",
            "olive", "omelet", "onion", "orange", "pancake", "papaya", "pasta", "peach", "peanut", "pear",
            "pea", "pecan", "pepper", "pickle", "pie", "pineapple", "pizza", "plum", "popcorn", "pork",
            "potato", "pretzel", "pudding", "pumpkin", "quesadilla", "quiche", "radish", "raisin", "raspberry", "ravioli",
            "rice", "salad", "salmon", "sandwich", "sausage", "shrimp", "soup", "spaghetti", "spinach", "steak",
            "strawberry", "sushi", "taco", "toast", "tomato", "tuna", "turkey", "vanilla", "waffle", "walnut",
            "watermelon", "wheat", "yogurt", "zucchini"
        )
    )

    private val LEADING_ARTICLES = listOf(
        "the ", "a ", "an ", "el ", "la ", "los ", "las ", "le ", "un ", "une "
    )

    private val DIACRITICS_REGEX = Regex("[\\u0300-\\u036f]")
    private val PUNCTUATION_REGEX = Regex("[.,/#!\$%\\^&*;:{}=\\-_`~()]")
    private val REPETITIVE_REGEX = Regex("^(.)\\1{3,}$")

    fun normalizeAnswer(text: String): String {
        if (text.isBlank()) return ""
        var cleaned = text.trim().lowercase()
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        cleaned = DIACRITICS_REGEX.replace(cleaned, "")
        for (article in LEADING_ARTICLES) {
            if (cleaned.startsWith(article)) {
                cleaned = cleaned.removePrefix(article).trim()
                break
            }
        }
        cleaned = PUNCTUATION_REGEX.replace(cleaned, "").trim()
        return cleaned
    }

    fun matchesStartingLetter(text: String, letter: String): Boolean {
        val normText = normalizeAnswer(text)
        val normLetter = letter.trim().lowercase()
        if (normText.isEmpty() || normLetter.isEmpty()) return false
        return normText.startsWith(normLetter)
    }

    data class ValidationResult(val valid: Boolean, val reason: String)

    fun validateAnswer(categoryId: String, answer: String, targetLetter: String): ValidationResult {
        if (answer.trim().isEmpty()) {
            return ValidationResult(false, "Empty answer")
        }
        val trimmed = answer.trim()
        if (trimmed.length < 2) {
            return ValidationResult(false, "Answer too short")
        }
        if (!matchesStartingLetter(trimmed, targetLetter)) {
            return ValidationResult(false, "Must begin with letter ${targetLetter.uppercase()}")
        }
        val norm = normalizeAnswer(trimmed)
        if (REPETITIVE_REGEX.matches(norm)) {
            return ValidationResult(false, "Invalid repetitive word")
        }
        val categoryList = CATEGORY_WORDS[categoryId]
        if (categoryList != null) {
            val directMatch = categoryList.any { item ->
                val normItem = normalizeAnswer(item)
                normItem == norm || norm.contains(normItem) || normItem.contains(norm)
            }
            return ValidationResult(true, if (directMatch) "Dictionary match" else "Plausible entry")
        }
        return ValidationResult(true, "Valid entry")
    }

    fun calculateRoundScores(
        submissions: List<PlayerRoundSubmission>,
        categories: List<Category>,
        targetLetter: String
    ): List<PlayerRoundScore> {
        val playerNormalized = mutableMapOf<String, MutableMap<String, Triple<String, String, ValidationResult>>>()

        for (sub in submissions) {
            val catMap = mutableMapOf<String, Triple<String, String, ValidationResult>>()
            for (cat in categories) {
                val raw = sub.answers[cat.id].orEmpty()
                val validation = validateAnswer(cat.id, raw, targetLetter)
                val norm = normalizeAnswer(raw)
                catMap[cat.id] = Triple(raw, norm, validation)
            }
            playerNormalized[sub.playerId] = catMap
        }

        val answerCountsByCategory = mutableMapOf<String, MutableMap<String, Int>>()
        for (cat in categories) {
            val counts = mutableMapOf<String, Int>()
            for (sub in submissions) {
                val (_, norm, validation) = playerNormalized[sub.playerId]!![cat.id]!!
                if (validation.valid && norm.isNotEmpty()) {
                    counts[norm] = (counts[norm] ?: 0) + 1
                }
            }
            answerCountsByCategory[cat.id] = counts
        }

        var firstStopperId: String? = null
        var minElapsed = Int.MAX_VALUE
        for (sub in submissions) {
            val elapsed = sub.stopElapsedSec
            if (sub.hasStopped && elapsed != null && elapsed < minElapsed) {
                minElapsed = elapsed
                firstStopperId = sub.playerId
            }
        }

        return submissions.map { sub ->
            var baseScore = 0
            val catScores = mutableMapOf<String, CategoryScoreResult>()

            for (cat in categories) {
                val (raw, norm, validation) = playerNormalized[sub.playerId]!![cat.id]!!
                if (!validation.valid || norm.isEmpty()) {
                    catScores[cat.id] = CategoryScoreResult(
                        categoryId = cat.id,
                        answer = raw,
                        isValid = false,
                        isDuplicate = false,
                        score = 0,
                        reason = validation.reason
                    )
                } else {
                    val count = answerCountsByCategory[cat.id]?.get(norm) ?: 1
                    val isDuplicate = count > 1
                    val pts = if (isDuplicate) 5 else 10
                    baseScore += pts
                    catScores[cat.id] = CategoryScoreResult(
                        categoryId = cat.id,
                        answer = raw,
                        isValid = true,
                        isDuplicate = isDuplicate,
                        score = pts,
                        reason = if (isDuplicate) "Shared with another player" else "Unique valid answer"
                    )
                }
            }

            val stoppedFirst = sub.playerId == firstStopperId
            val timeBonus = if (stoppedFirst) 5 else 0

            PlayerRoundScore(
                playerId = sub.playerId,
                categoryScores = catScores,
                baseScore = baseScore,
                timeBonus = timeBonus,
                totalRoundScore = baseScore + timeBonus,
                stoppedFirst = stoppedFirst
            )
        }
    }

    fun generateBotAnswer(categoryId: String, letter: String, difficulty: Difficulty): String {
        val normLetter = letter.trim().lowercase()
        val list = CATEGORY_WORDS[categoryId].orEmpty()
        val candidates = list.filter { normalizeAnswer(it).startsWith(normLetter) }

        if (candidates.isEmpty()) {
            val upper = letter.uppercase()
            return when (categoryId) {
                "name" -> "${upper}am"
                "animal" -> "${upper}ntelope"
                "country_city" -> "${upper}merica"
                "food" -> "${upper}pple"
                "object" -> "${upper}nchor"
                "job" -> "${upper}ctor"
                "color" -> "${upper}zure"
                "brand" -> "${upper}pex"
                "celebrity" -> "${upper}riana"
                "movie_game" -> "${upper}vatar"
                "sport_hobby" -> "${upper}rchery"
                "plant" -> "${upper}cacia"
                else -> "${upper}o"
            }
        }

        val missProbability = when (difficulty) {
            Difficulty.EASY -> 0.35
            Difficulty.MEDIUM -> 0.15
            Difficulty.HARD -> 0.05
        }
        if (Random.nextDouble() < missProbability) {
            return ""
        }

        val chosen = candidates[Random.nextInt(candidates.size)]
        return chosen.replaceFirstChar { it.uppercase() }
    }

    fun calculateBotFieldDelayMs(
        speedSeconds: Int,
        difficulty: Difficulty = Difficulty.MEDIUM,
        isFirstField: Boolean = false
    ): Long {
        val baseSec = max(1, min(6, speedSeconds))
        val introDelay = if (isFirstField) 1.8 + Random.nextDouble() * 1.5 else 0.0
        val diffMultiplier = when (difficulty) {
            Difficulty.EASY -> 1.25
            Difficulty.MEDIUM -> 1.0
            Difficulty.HARD -> 0.85
        }
        val humanJitter = (Random.nextDouble() * 0.8 - 0.4) * baseSec
        val totalSec = (baseSec * diffMultiplier) + introDelay + humanJitter
        return max(800L, (totalSec * 1000.0).roundToInt().toLong())
    }

    fun getDailyChallenge(dateStr: String? = null): DailyChallenge {
        val today = dateStr ?: LocalDate.now().toString()
        var hash = 0
        for (ch in today) {
            hash = (hash shl 5) - hash + ch.code
        }
        val absHash = abs(hash)
        val letter = VALID_LETTERS[absHash % VALID_LETTERS.size]

        val selectedCategories = DEFAULT_CATEGORIES.sortedBy { cat ->
            (cat.id.first().code * 31 + absHash) % 100
        }.take(5)

        val titles = listOf(
            "Quickdraw Vocabulary Blitz",
            "Midnight Category Sprint",
            "The Alphabet Gauntlet",
            "Grand Slam Stop Rush",
            "Master Lexicon Sprint"
        )
        val title = titles[absHash % titles.size]

        return DailyChallenge(
            date = today,
            letter = letter,
            categories = selectedCategories,
            targetParScore = 45,
            timeLimitSec = 45,
            title = title,
            taskDescription = "Score 45+ pts with letter '${letter.uppercase()}' across 5 categories in 45s to earn +50 Bonus XP!",
            bonusPoints = 50
        )
    }

    fun getPlayerTier(points: Int): PlayerTier {
        return when {
            points >= 1500 -> PlayerTier("Grandmaster", "Stop Legend", StopColors.Rose500, 2000)
            points >= 800 -> PlayerTier("Diamond", "Speed Lexicographer", StopColors.Cyan500, 1500)
            points >= 400 -> PlayerTier("Gold", "Category Champ", StopColors.Amber500, 800)
            points >= 150 -> PlayerTier("Silver", "Word Sprinter", StopColors.Slate400, 400)
            else -> PlayerTier("Bronze", "Rookie Rusher", StopColors.Amber700, 150)
        }
    }

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = buildString {
            repeat(4) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
        return "STOP-$suffix"
    }
}
