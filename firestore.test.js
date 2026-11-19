const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const PHONE_KEY = "8801712345678";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write users or caller_directory", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("caller_directory").doc(PHONE_KEY).get());
  await assertFails(
    unauthDb.collection("caller_directory").doc(PHONE_KEY).set({
      phoneKey: PHONE_KEY,
      displayName: "Unauthorized",
      carrierLabel: "Grameenphone",
      category: "PERSONAL",
      spamReports: 0,
      reportCount: 1,
      contributedByUid: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profile: owner create/update/read succeeds, cross-user and extra keys fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Shajamal",
      callerIdEnabled: true,
      communityShareEnabled: false,
      sharedContactCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      communityShareEnabled: true,
      sharedContactCount: 25,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId `id` injection
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      userId: BOB_UID,
      displayName: "Bob",
      callerIdEnabled: true,
      communityShareEnabled: false,
      sharedContactCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Caller directory: exact lookup, contribution, community spam increment, and query scoping", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Alice creates a Caller ID entry
  await assertSucceeds(
    aliceDb.collection("caller_directory").doc(PHONE_KEY).set({
      phoneKey: PHONE_KEY,
      displayName: "Karim Traders",
      carrierLabel: "Grameenphone • BD",
      category: "BUSINESS",
      spamReports: 0,
      reportCount: 1,
      contributedByUid: ALICE_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob can look up the exact phoneKey
  await assertSucceeds(bobDb.collection("caller_directory").doc(PHONE_KEY).get());

  // Bob CANNOT run an unfiltered list query (prevents scraping)
  await assertFails(bobDb.collection("caller_directory").get());

  // Alice CAN list her own contributed tags (mirrors Kotlin query)
  await assertSucceeds(
    aliceDb
      .collection("caller_directory")
      .where("contributedByUid", "==", ALICE_UID)
      .get()
  );

  // Bob CAN submit a community spam/tag update (+1 reportCount, +1 spamReports)
  await assertSucceeds(
    bobDb.collection("caller_directory").doc(PHONE_KEY).update({
      displayName: "Karim Traders",
      category: "SPAM",
      spamReports: 1,
      reportCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT jump spamReports by +50 or hijack contributedByUid or delete Alice's entry
  await assertFails(
    bobDb.collection("caller_directory").doc(PHONE_KEY).update({
      displayName: "Hijack",
      category: "SPAM",
      spamReports: 50,
      reportCount: 50,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobDb.collection("caller_directory").doc(PHONE_KEY).delete());

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("caller_directory").doc("8801811112222").set({
      phoneKey: "8801811112222",
      displayName: "Future",
      carrierLabel: "Robi",
      category: "PERSONAL",
      spamReports: 0,
      reportCount: 1,
      contributedByUid: ALICE_UID,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Alice can delete her own contributed entry
  await assertSucceeds(aliceDb.collection("caller_directory").doc(PHONE_KEY).delete());
});
