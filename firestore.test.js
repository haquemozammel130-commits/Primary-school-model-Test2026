const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_1";
const ADMIN_EMAIL = "haquemozammel130@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validConfig(overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    activeModelNumber: 1,
    competitionOpen: true,
    roundNumber: 1,
    examTitle: "প্রাইমারি স্কুল টেস্ট পরীক্ষা",
    notice: "সকল পরীক্ষার্থীর জন্য শুভকামনা",
    resultsPublished: false,
    publishedModelNumber: 0,
    publishedRoundNumber: 0,
    updatedBy: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validSubmission(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    participantName: "সাদিয়া ইসলাম",
    schoolName: "ঢাকা প্রাইমারি স্কুল",
    rollNumber: "১০১",
    modelNumber: 1,
    roundNumber: 1,
    answers: Array(100).fill(0),
    score: 75,
    correctCount: 75,
    wrongCount: 20,
    unansweredCount: 5,
    durationSeconds: 1800,
    createdAt: now,
    updatedAt: now,
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("competition_config").doc("current").get());
  await assertFails(unauthDb.collection("submissions").get());
  await assertFails(unauthDb.collection("published_sheets").where("published", "==", true).get());
});

test("Spoofed admin email without email_verified cannot modify competition_config", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  await assertFails(
    spoofDb.collection("competition_config").doc("current").set(validConfig({ updatedBy: "spoof_uid" }))
  );
});

test("Regular user cannot modify competition_config, publish results, or list all submissions", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("competition_config").doc("current").set(validConfig());
    await ctx.firestore().collection("submissions").doc(`${BOB_UID}_m1_r1`).set(validSubmission(BOB_UID));
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  // Cannot update competition config
  await assertFails(
    aliceDb.collection("competition_config").doc("current").update({ competitionOpen: false })
  );
  // Cannot list all submissions without userId filter
  await assertFails(aliceDb.collection("submissions").get());
  // Cannot read Bob's submission
  await assertFails(aliceDb.collection("submissions").doc(`${BOB_UID}_m1_r1`).get());
  // Can query own submissions
  await assertSucceeds(
    aliceDb.collection("submissions").where("userId", "==", ALICE_UID).get()
  );
});

test("User can submit 100-MCQ form only when competitionOpen is true and model matches", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("competition_config").doc("current").set(validConfig());
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  // Valid submission succeeds
  await assertSucceeds(
    aliceDb.collection("submissions").doc(`${ALICE_UID}_m1_r1`).set(validSubmission(ALICE_UID))
  );

  // Wrong model fails
  await assertFails(
    aliceDb
      .collection("submissions")
      .doc(`${ALICE_UID}_m2_r1`)
      .set(validSubmission(ALICE_UID, { modelNumber: 2 }))
  );

  // Ghost field fails (Shadow Update Test)
  await assertFails(
    aliceDb
      .collection("submissions")
      .doc(`${ALICE_UID}_m1_r1_extra`)
      .set({ ...validSubmission(ALICE_UID), isAdmin: true })
  );

  // Tampered score consistency fails
  await assertFails(
    aliceDb
      .collection("submissions")
      .doc(`${ALICE_UID}_m1_r1_tamper`)
      .set(validSubmission(ALICE_UID, { score: 100 }))
  );
});

test("User cannot submit when competitionOpen is false", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("competition_config")
      .doc("current")
      .set(validConfig({ competitionOpen: false }));
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  await assertFails(
    aliceDb.collection("submissions").doc(`${ALICE_UID}_m1_r1`).set(validSubmission(ALICE_UID))
  );
});

test("Verified Admin can toggle competition, switch models, read all submissions, and publish result sheet", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  await assertSucceeds(
    adminDb.collection("competition_config").doc("current").set(validConfig())
  );
  await assertSucceeds(
    adminDb.collection("competition_config").doc("current").update({
      activeModelNumber: 5,
      competitionOpen: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Admin can list all submissions
  await assertSucceeds(adminDb.collection("submissions").get());

  // Admin can atomically publish sheet + entry
  const now = new Date(Date.now() - 500);
  const batch = adminDb.batch();
  const sheetRef = adminDb.collection("published_sheets").doc("m5_r1");
  batch.set(sheetRef, {
    modelNumber: 5,
    roundNumber: 1,
    examTitle: "প্রাইমারি স্কুল টেস্ট পরীক্ষা",
    published: true,
    totalParticipants: 1,
    highestScore: 88,
    averageScore: 88.0,
    publishedBy: ADMIN_UID,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(sheetRef.collection("entries").doc(ALICE_UID), {
    sheetId: "m5_r1",
    userId: ALICE_UID,
    rank: 1,
    participantName: "সাদিয়া ইসলাম",
    schoolName: "ঢাকা প্রাইমারি স্কুল",
    rollNumber: "১০১",
    modelNumber: 5,
    roundNumber: 1,
    score: 88,
    correctCount: 88,
    wrongCount: 10,
    unansweredCount: 2,
    durationSeconds: 1500,
    published: true,
    createdAt: now,
  });
  await assertSucceeds(batch.commit());

  // Regular user can list published sheets & entries with published == true filter
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  await assertSucceeds(
    aliceDb.collection("published_sheets").where("published", "==", true).get()
  );
  await assertSucceeds(
    aliceDb
      .collection("published_sheets")
      .doc("m5_r1")
      .collection("entries")
      .where("published", "==", true)
      .get()
  );
});
