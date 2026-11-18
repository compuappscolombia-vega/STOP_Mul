const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 2000) {
  return new Date(Date.now() - offsetMs);
}

function validUserPayload(uid, overrides = {}) {
  const ts = pastDate(5000);
  return {
    userId: uid,
    name: "PlayerOne",
    avatar: "⚡",
    syncCode: "STOP-1234",
    title: "Rookie Rusher",
    email: "player@example.com",
    gamesPlayed: 0,
    wins: 0,
    stopsCalled: 0,
    totalPoints: 0,
    weeklyPoints: 0,
    bestRoundScore: 0,
    currentStreak: 0,
    dailyChallengesCompleted: [],
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validLeaderboardPayload(uid, overrides = {}) {
  const ts = pastDate(5000);
  return {
    userId: uid,
    name: "PlayerOne",
    avatar: "⚡",
    title: "Rookie Rusher",
    totalPoints: 120,
    weeklyPoints: 80,
    wins: 2,
    stopsCalled: 4,
    streak: 1,
    tier: "Silver",
    isPublic: true,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validRoomPayload(code, hostId, isPrivate = false, overrides = {}) {
  const ts = pastDate(5000);
  return {
    code,
    name: "Blitz Room",
    hostId,
    hostName: "Alice",
    roundCount: 3,
    roundDurationSec: 60,
    categoryIds: ["nombre", "animal", "ciudad"],
    isPrivate,
    difficulty: "MEDIUM",
    phase: "LOBBY",
    currentRound: 0,
    currentLetter: "",
    stopperPlayerId: "",
    stopperName: "",
    memberUids: [hostId],
    playerCount: 1,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validRoomPlayerPayload(roomCode, playerId, memberUids, isPublic = true, overrides = {}) {
  const ts = pastDate(5000);
  return {
    roomCode,
    playerId,
    name: "Alice",
    avatar: "⚡",
    isHost: true,
    isBot: false,
    isReady: true,
    isPublic,
    memberUids,
    totalScore: 0,
    roundScore: 0,
    answers: {},
    hasStopped: false,
    stopElapsedSec: -1,
    filledCount: 0,
    statusText: "Ready",
    answeredRound: 0,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated requests are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("leaderboards").where("isPublic", "==", true).get());
  await assertFails(unauth.collection("rooms").where("isPrivate", "==", false).get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );
});

test("User profile PII isolation and partial update enforcement", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Bob cannot read or modify Alice's private profile (contains email PII)
  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({ totalPoints: 9999, updatedAt: pastDate(1000) })
  );

  // Alice can partially update allowed fields
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      name: "AlicePro",
      totalPoints: 250,
      updatedAt: pastDate(1000),
    })
  );

  // Immutable createdAt cannot change; ghost fields rejected; future timestamp rejected
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(500),
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      totalPoints: 300,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Leaderboards: filtered query succeeds, unfiltered fails, and cross-user writes fail", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("leaderboards").doc(ALICE_UID).set(validLeaderboardPayload(ALICE_UID))
  );
  await assertSucceeds(
    bob.collection("leaderboards").doc(BOB_UID).set(validLeaderboardPayload(BOB_UID, { name: "Bob", totalPoints: 300 }))
  );

  // Matches Kotlin query: .whereEqualTo("isPublic", true)
  await assertSucceeds(
    alice.collection("leaderboards").where("isPublic", "==", true).get()
  );

  // Unfiltered query rejected
  await assertFails(alice.collection("leaderboards").get());

  // Alice cannot overwrite Bob's leaderboard entry
  await assertFails(
    alice.collection("leaderboards").doc(BOB_UID).update({ totalPoints: 0, updatedAt: pastDate(1000) })
  );
});

test("Multiplayer rooms: public list, private code join, live answers, STOP, and chat", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Create public room & private room
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-PUB1").set(validRoomPayload("STOP-PUB1", ALICE_UID, false))
  );
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-PRIV1").set(validRoomPayload("STOP-PRIV1", ALICE_UID, true))
  );

  // Public lobby query succeeds and sees only public rooms; unfiltered fails
  await assertSucceeds(bob.collection("rooms").where("isPrivate", "==", false).get());
  await assertFails(bob.collection("rooms").get());

  // Bob can fetch private room by exact code and join it
  await assertSucceeds(bob.collection("rooms").doc("STOP-PRIV1").get());
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PRIV1").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastDate(1000),
    })
  );

  // Both players create their own player docs in STOP-PUB1
  await assertSucceeds(
    alice
      .collection("rooms")
      .doc("STOP-PUB1")
      .collection("players")
      .doc(ALICE_UID)
      .set(validRoomPlayerPayload("STOP-PUB1", ALICE_UID, [ALICE_UID], true))
  );
  await assertSucceeds(
    bob
      .collection("rooms")
      .doc("STOP-PUB1")
      .update({
        memberUids: [ALICE_UID, BOB_UID],
        playerCount: 2,
        updatedAt: pastDate(1000),
      })
  );
  await assertSucceeds(
    bob
      .collection("rooms")
      .doc("STOP-PUB1")
      .collection("players")
      .doc(BOB_UID)
      .set(validRoomPlayerPayload("STOP-PUB1", BOB_UID, [ALICE_UID, BOB_UID], true, { name: "Bob", isHost: false }))
  );

  // Non-host Bob cannot start round
  await assertFails(
    bob.collection("rooms").doc("STOP-PUB1").update({
      phase: "ROUND_ACTIVE",
      currentRound: 1,
      currentLetter: "M",
      updatedAt: pastDate(1000),
    })
  );

  // Host Alice starts round 1
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-PUB1").update({
      phase: "ROUND_ACTIVE",
      currentRound: 1,
      currentLetter: "M",
      stopperPlayerId: "",
      stopperName: "",
      updatedAt: pastDate(1000),
    })
  );

  // Bob updates own answers & calls STOP
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PUB1").collection("players").doc(BOB_UID).update({
      answers: { nombre: "Maria", animal: "Mono" },
      filledCount: 2,
      hasStopped: true,
      stopElapsedSec: 12,
      answeredRound: 1,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PUB1").update({
      phase: "STOP_CALLED",
      stopperPlayerId: BOB_UID,
      stopperName: "Bob",
      updatedAt: pastDate(1000),
    })
  );

  // Non-member Charlie cannot call STOP or send chat message into STOP-PUB1
  await assertFails(
    charlie.collection("rooms").doc("STOP-PUB1").collection("messages").doc("msg_bad").set({
      roomCode: "STOP-PUB1",
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      senderAvatar: "🚀",
      text: "Intruder!",
      isSystem: false,
      isPublic: true,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(1000),
    })
  );

  // Member Bob sends valid chat message; query with isPublic == true succeeds
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PUB1").collection("messages").doc("msg_1").set({
      roomCode: "STOP-PUB1",
      senderId: BOB_UID,
      senderName: "Bob",
      senderAvatar: "🔥",
      text: "Good game!",
      isSystem: false,
      isPublic: true,
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-PUB1").collection("messages").where("isPublic", "==", true).get()
  );

  // Bob cannot delete Alice's player doc
  await assertFails(
    bob.collection("rooms").doc("STOP-PUB1").collection("players").doc(ALICE_UID).delete()
  );
});

test("Non-existent document gets succeed as missing and atomic batch createRoom succeeds", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Regression: existence checks on non-existent docs must NOT throw PERMISSION_DENIED
  await assertSucceeds(alice.collection("leaderboards").doc(ALICE_UID).get());
  await assertSucceeds(alice.collection("rooms").doc("STOP-ARENA").get());
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-ARENA").collection("players").doc(ALICE_UID).get()
  );
  await assertSucceeds(
    alice.collection("rooms").doc("STOP-ARENA").collection("messages").doc("sys_1").get()
  );

  // Regression: atomic WriteBatch matching FirebaseRepository.createRoom() with bot + system message
  const arenaRef = alice.collection("rooms").doc("STOP-ARENA");
  const batch = alice.batch();
  batch.set(arenaRef, validRoomPayload("STOP-ARENA", ALICE_UID, false));
  batch.set(
    arenaRef.collection("players").doc(ALICE_UID),
    validRoomPlayerPayload("STOP-ARENA", ALICE_UID, [ALICE_UID], true)
  );
  batch.set(
    arenaRef.collection("players").doc("bot_vicky"),
    validRoomPlayerPayload("STOP-ARENA", "bot_vicky", [ALICE_UID], true, {
      name: "Vocabulary Vicky",
      avatar: "🦉",
      isHost: false,
      isBot: true,
    })
  );
  batch.set(arenaRef.collection("messages").doc("sys_welcome"), {
    roomCode: "STOP-ARENA",
    senderId: "system",
    senderName: "System",
    senderAvatar: "📢",
    text: "Room STOP-ARENA created by Alice.",
    isSystem: true,
    isPublic: true,
    memberUids: [ALICE_UID],
    createdAt: pastDate(4000),
  });
  await assertSucceeds(batch.commit());

  // Private room batch creation + member join updating host player memberUids
  const privRef = alice.collection("rooms").doc("STOP-PRIV2");
  const privBatch = alice.batch();
  privBatch.set(privRef, validRoomPayload("STOP-PRIV2", ALICE_UID, true));
  privBatch.set(
    privRef.collection("players").doc(ALICE_UID),
    validRoomPlayerPayload("STOP-PRIV2", ALICE_UID, [ALICE_UID], false)
  );
  await assertSucceeds(privBatch.commit());

  // Non-member Charlie cannot update Alice's player doc memberUids before joining the room
  await assertFails(
    charlie.collection("rooms").doc("STOP-PRIV2").collection("players").doc(ALICE_UID).update({
      memberUids: [ALICE_UID, CHARLIE_UID],
      updatedAt: pastDate(1000),
    })
  );

  // Bob joins STOP-PRIV2 room doc first, then updates Alice's player memberUids
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PRIV2").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerCount: 2,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    bob.collection("rooms").doc("STOP-PRIV2").collection("players").doc(ALICE_UID).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(1000),
    })
  );
});

test("DailyChallenge module: shared daily task and owner-only completion with bonus points", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const today = "2026-11-18";

  // Reading non-existent daily challenge returns empty cleanly
  await assertSucceeds(alice.collection("dailyChallenges").doc(today).get());
  await assertFails(unauth.collection("dailyChallenges").doc(today).get());

  // Cannot record completion before dailyChallenge exists
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("dailyCompletions").doc(today).set({
      userId: ALICE_UID,
      date: today,
      challengeTitle: "Master Lexicon Sprint",
      letter: "M",
      roundScore: 50,
      bonusPointsAwarded: 50,
      totalAwarded: 100,
      parAchieved: true,
      completedAt: pastDate(1000),
    })
  );

  // Authenticated user creates shared daily task
  await assertSucceeds(
    alice.collection("dailyChallenges").doc(today).set({
      date: today,
      title: "Master Lexicon Sprint",
      taskDescription: "Score 45+ pts with letter M across 5 categories to earn +50 Bonus XP!",
      letter: "M",
      categoryIds: ["name", "animal", "country_city", "food", "object"],
      targetParScore: 45,
      timeLimitSec: 45,
      bonusPoints: 50,
      isActive: true,
      createdBy: ALICE_UID,
      createdAt: pastDate(2000),
    })
  );

  // Bob can read and list active daily challenges, cannot update or delete them
  await assertSucceeds(bob.collection("dailyChallenges").doc(today).get());
  await assertSucceeds(bob.collection("dailyChallenges").where("isActive", "==", true).get());
  await assertFails(bob.collection("dailyChallenges").doc(today).update({ bonusPoints: 500 }));
  await assertFails(alice.collection("dailyChallenges").doc(today).delete());

  // Alice records her own daily completion & bonus points
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("dailyCompletions").doc(today).set({
      userId: ALICE_UID,
      date: today,
      challengeTitle: "Master Lexicon Sprint",
      letter: "M",
      roundScore: 50,
      bonusPointsAwarded: 50,
      totalAwarded: 100,
      parAchieved: true,
      completedAt: pastDate(1000),
    })
  );

  // Alice can read/list own completions; Bob cannot read or forge Alice's completion; updates denied (1 reward/day)
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("dailyCompletions").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("dailyCompletions").get());
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("dailyCompletions").doc("2026-11-19").set({
      userId: ALICE_UID,
      date: "2026-11-19",
      challengeTitle: "Forged",
      letter: "A",
      roundScore: 50,
      bonusPointsAwarded: 50,
      totalAwarded: 100,
      parAchieved: true,
      completedAt: pastDate(1000),
    })
  );
  await assertFails(
    alice.collection("users").doc(ALICE_UID).collection("dailyCompletions").doc(today).update({
      bonusPointsAwarded: 400,
    })
  );
});
