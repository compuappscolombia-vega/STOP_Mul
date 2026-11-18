package com.example.model

import androidx.compose.ui.graphics.Color

enum class Difficulty(val label: String) {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard")
}

enum class AuthProvider {
    EMAIL,
    GOOGLE
}

enum class RoomPhase {
    LOBBY,
    SPINNING_LETTER,
    ROUND_ACTIVE,
    STOP_CALLED,
    ROUND_REVIEW,
    ROUND_SCORES,
    MATCH_OVER
}

enum class LeaderboardPeriod {
    ALL,
    WEEKLY
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val placeholder: String,
    val description: String
)

data class UserStats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val stopsCalled: Int = 0,
    val totalPoints: Int = 0,
    val weeklyPoints: Int = 0,
    val bestRoundScore: Int = 0,
    val currentStreak: Int = 0,
    val dailyChallengesCompleted: List<String> = emptyList(),
    val averageResponseTimeSec: Double = 0.0
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatar: String,
    val syncCode: String,
    val title: String = "Rookie Rusher",
    val stats: UserStats = UserStats(),
    val createdAt: String = "",
    val email: String? = null,
    val isRegistered: Boolean = false
)

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String,
    val authProvider: AuthProvider,
    val createdAt: String
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: Long,
    val isSystem: Boolean = false
)

data class RoomSettings(
    val roundCount: Int = 3,
    val roundDurationSec: Int = 60,
    val categories: List<Category>,
    val isPrivate: Boolean = false,
    val difficulty: Difficulty = Difficulty.MEDIUM
)

data class PlayerState(
    val id: String,
    val name: String,
    val avatar: String,
    val isHost: Boolean = false,
    val isBot: Boolean = false,
    val isReady: Boolean = true,
    val totalScore: Int = 0,
    val roundScore: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val hasStopped: Boolean = false,
    val stopElapsedSec: Int? = null,
    val filledCount: Int = 0,
    val statusText: String = "",
    val answeredRound: Int = 0
)

data class GameRoom(
    val code: String,
    val name: String,
    val hostId: String,
    val settings: RoomSettings,
    val phase: RoomPhase = RoomPhase.LOBBY,
    val currentRound: Int = 0,
    val currentLetter: String = "",
    val phaseTimeRemaining: Int = 60,
    val phaseStartTime: Long = System.currentTimeMillis(),
    val stopperPlayerId: String? = null,
    val stopperName: String? = null,
    val memberUids: List<String> = emptyList(),
    val players: List<PlayerState> = emptyList(),
    val chat: List<ChatMessage> = emptyList()
)

data class PublicRoomSummary(
    val code: String,
    val name: String,
    val playerCount: Int,
    val maxRounds: Int,
    val duration: Int,
    val hostName: String
)

data class LeaderboardEntry(
    val id: String,
    val name: String,
    val avatar: String,
    val title: String,
    val totalPoints: Int,
    val wins: Int,
    val stopsCalled: Int,
    val tier: String,
    val rank: Int,
    val streak: Int
)

data class DailyChallenge(
    val date: String,
    val letter: String,
    val categories: List<Category>,
    val targetParScore: Int,
    val timeLimitSec: Int,
    val title: String,
    val taskDescription: String = "",
    val bonusPoints: Int = 50
)

data class DailyChallengeCompletion(
    val date: String,
    val challengeTitle: String,
    val letter: String,
    val roundScore: Int,
    val bonusPointsAwarded: Int,
    val totalAwarded: Int,
    val parAchieved: Boolean,
    val completedAtMillis: Long = System.currentTimeMillis()
)

data class PlayerTier(
    val tier: String,
    val rankTitle: String,
    val color: Color,
    val nextTierPoints: Int
)

data class PlayerRoundSubmission(
    val playerId: String,
    val answers: Map<String, String>,
    val hasStopped: Boolean,
    val stopElapsedSec: Int? = null
)

data class CategoryScoreResult(
    val categoryId: String,
    val answer: String,
    val isValid: Boolean,
    val isDuplicate: Boolean,
    val score: Int,
    val reason: String
)

data class PlayerRoundScore(
    val playerId: String,
    val categoryScores: Map<String, CategoryScoreResult>,
    val baseScore: Int,
    val timeBonus: Int,
    val totalRoundScore: Int,
    val stoppedFirst: Boolean
)

data class BotProgressState(
    val id: String,
    val name: String,
    val avatar: String,
    val difficulty: Difficulty,
    val filledCount: Int = 0,
    val statusText: String = "Pensando...",
    val answers: Map<String, String> = emptyMap(),
    val hasStopped: Boolean = false
)

data class ActivePlayerStripItem(
    val id: String,
    val name: String,
    val avatar: String,
    val hasStopped: Boolean,
    val filledCount: Int,
    val totalCategories: Int,
    val statusText: String,
    val isHuman: Boolean
)
