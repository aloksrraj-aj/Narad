const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_citizen_101";
const BOB_UID = "bob_citizen_202";
const CHARLIE_UID = "charlie_outsider_303";

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

test("Unauthenticated users: denied across all Narad collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("chats").get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("channels").where("channelPrivacy", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("videos").where("visibility", "==", "PUBLIC").get());
});

test("UserProfile & PrivacyVault PII split: discoverability and strict owner-only vault", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates public discoverable profile (no PII)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav Sharma",
      handle: "@aarav_in",
      bio: "Building sovereign tech",
      city: "Bengaluru, KA",
      avatarIndex: 1,
      discoverable: true,
      verifiedCitizen: true,
      followersCount: 120,
      postsCount: 4,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice creates private vault (contains PII email)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("vault").set({
      userId: ALICE_UID,
      email: "aarav@example.in",
      defaultPostVisibility: "PRIVATE",
      messagePolicy: "CONTACTS",
      readReceipts: true,
      onlineStatus: false,
      disappearingHours: 24,
      screenshotGuard: true,
      dataResidencyRegion: "IN-SOU-1",
      warrantLockAccepted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob can read Alice's discoverable public profile
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(bobDb.collection("users").where("discoverable", "==", true).get());

  // Bob CANNOT read or write Alice's private vault or consent grants or audit logs
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("vault").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("consent_grants").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("audit_logs").get());

  // Alice turns discoverable = false -> Bob can no longer get Alice's profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      discoverable: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Shadow update / privilege escalation rejected (verifiedCitizen cannot be modified)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verifiedCitizen: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Consent grants and immutable audit logs: owner-only and audit logs immutable", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("consent_grants").doc("grant_1").set({
      userId: ALICE_UID,
      granteeName: "Meera Nair",
      granteeHandle: "@meera_nair",
      scope: "PRIVATE_POSTS",
      active: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("consent_grants").doc("grant_1").update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").set({
      userId: ALICE_UID,
      action: "CONSENT_REVOKED",
      detail: "Revoked PRIVATE_POSTS access for @meera_nair",
      category: "CONSENT",
      createdAt: past,
    })
  );

  // Audit logs cannot be tampered with or deleted
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").update({
      action: "TAMPERED",
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1").delete()
  );
});

test("Chats & messages: strictly participant-isolated (outsider Charlie denied)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice creates chat with Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      participantUids: [ALICE_UID, BOB_UID],
      title: "Aarav & Bob",
      chatType: "SECRET",
      lastMessage: "Namaste",
      lastSenderUid: ALICE_UID,
      disappearingHours: 24,
      encryptedFingerprint: "E2EE-9F2A-81C4",
      createdBy: ALICE_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice sends message with denormalized participantUids
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").set({
      chatId: "chat_ab",
      senderUid: ALICE_UID,
      senderName: "Aarav",
      participantUids: [ALICE_UID, BOB_UID],
      text: "Private E2EE message",
      deliveryState: "SENT",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob can list chats & messages with matching filter
  await assertSucceeds(
    bobDb.collection("chats").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages")
      .where("participantUids", "array-contains", BOB_UID).get()
  );

  // Unfiltered chat list must fail
  await assertFails(bobDb.collection("chats").get());

  // Outsider Charlie cannot read chat, read msg_1, list unfiltered messages, query Alice's messages, or inject a message
  await assertFails(charlieDb.collection("chats").doc("chat_ab").get());
  await assertFails(charlieDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").get());
  await assertFails(charlieDb.collection("chats").doc("chat_ab").collection("messages").get());
  await assertFails(
    charlieDb.collection("chats").doc("chat_ab").collection("messages")
      .where("participantUids", "array-contains", ALICE_UID).get()
  );
  const charlieSnap = await assertSucceeds(
    charlieDb.collection("chats").doc("chat_ab").collection("messages")
      .where("participantUids", "array-contains", CHARLIE_UID).get()
  );
  if (charlieSnap.size !== 0) {
    throw new Error(`Expected 0 messages leaked to Charlie, got ${charlieSnap.size}`);
  }
  await assertFails(
    charlieDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_2").set({
      chatId: "chat_ab",
      senderUid: CHARLIE_UID,
      senderName: "Charlie",
      participantUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      text: "Intrusion",
      deliveryState: "SENT",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Posts: PUBLIC posts readable by signed-in users; PRIVATE posts strictly owner-only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_pub").set({
      userId: ALICE_UID,
      authorName: "Aarav",
      authorHandle: "@aarav_in",
      caption: "Morning filter coffee in Bengaluru",
      location: "Bengaluru, KA",
      postKind: "POST",
      visibility: "PUBLIC",
      mediaPreset: "STUDIO",
      likesCount: 0,
      commentsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_priv").set({
      userId: ALICE_UID,
      authorName: "Aarav",
      authorHandle: "@aarav_in",
      caption: "Private family journal entry",
      postKind: "POST",
      visibility: "PRIVATE",
      mediaPreset: "NONE",
      likesCount: 0,
      commentsCount: 0,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob can query PUBLIC posts and like PUBLIC post
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "PUBLIC").get());
  await assertSucceeds(
    bobDb.collection("posts").doc("post_pub").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT read Alice's PRIVATE post or query Alice's posts directly
  await assertFails(bobDb.collection("posts").doc("post_priv").get());
  await assertFails(bobDb.collection("posts").where("userId", "==", ALICE_UID).get());

  // Alice CAN query all her own posts (both PUBLIC and PRIVATE)
  await assertSucceeds(aliceDb.collection("posts").where("userId", "==", ALICE_UID).get());

  // Bob CANNOT tamper with Alice's public post caption
  await assertFails(
    bobDb.collection("posts").doc("post_pub").update({
      caption: "Hacked caption",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Channels, Broadcasts, and Videos (Shorts & Long): privacy & ownership enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("channels").doc("ch_1").set({
      ownerUid: ALICE_UID,
      name: "Digital Bharat",
      handle: "@digitalbharat",
      description: "Sovereign tech & privacy updates",
      category: "Technology",
      channelPrivacy: "PUBLIC",
      subscribersCount: 1,
      verified: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("channels").doc("ch_1").collection("broadcasts").doc("bc_1").set({
      channelId: "ch_1",
      channelOwnerUid: ALICE_UID,
      channelPrivacy: "PUBLIC",
      authorUid: ALICE_UID,
      title: "Welcome to Narad Vani",
      body: "Zero-trust sovereign broadcasts are live.",
      createdAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_1").set({
      ownerUid: ALICE_UID,
      channelId: "ch_1",
      channelName: "Digital Bharat",
      title: "Why Data Sovereignty Matters",
      description: "Understanding citizen-first privacy.",
      videoFormat: "LONG",
      durationLabel: "12:40",
      category: "Technology",
      thumbnailPreset: "SOVEREIGN",
      visibility: "PUBLIC",
      viewsCount: 10,
      likesCount: 2,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob can discover public channels, broadcasts, and videos
  await assertSucceeds(bobDb.collection("channels").where("channelPrivacy", "==", "PUBLIC").get());
  await assertSucceeds(
    bobDb.collection("channels").doc("ch_1").collection("broadcasts")
      .where("channelPrivacy", "==", "PUBLIC").get()
  );
  await assertSucceeds(bobDb.collection("videos").where("visibility", "==", "PUBLIC").get());

  // Bob CANNOT broadcast into Alice's channel
  await assertFails(
    bobDb.collection("channels").doc("ch_1").collection("broadcasts").doc("bc_spoof").set({
      channelId: "ch_1",
      channelOwnerUid: ALICE_UID,
      channelPrivacy: "PUBLIC",
      authorUid: BOB_UID,
      title: "Hijack",
      body: "Unauthorized",
      createdAt: past,
    })
  );

  // Reject future tampered timestamp
  await assertFails(
    aliceDb.collection("videos").doc("vid_future").set({
      ownerUid: ALICE_UID,
      channelId: "ch_1",
      channelName: "Digital Bharat",
      title: "Future Video",
      description: "Tampered",
      videoFormat: "SHORT",
      durationLabel: "0:45",
      category: "Tech",
      thumbnailPreset: "HERITAGE",
      visibility: "PUBLIC",
      viewsCount: 0,
      likesCount: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: past,
    })
  );
});

test("Contacts, Invites, and ChatMessage reactions: strict owner isolation and participant reactions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Alice adds contact & creates invite
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("c_1").set({
      userId: ALICE_UID,
      contactUid: BOB_UID,
      displayName: "Bob",
      handle: "@bob_in",
      note: "College friend",
      verified: true,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("invites").doc("inv_1").set({
      userId: ALICE_UID,
      recipientName: "Priya",
      inviteCode: "NRD-84K2",
      channel: "SHARE",
      status: "SENT",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob cannot read or modify Alice's contacts or invites
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("contacts").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("invites").get());

  // Chat reaction by participant Bob succeeds, outsider Charlie fails
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_react").set({
      participantUids: [ALICE_UID, BOB_UID],
      title: "Aarav & Bob",
      chatType: "DIRECT",
      disappearingHours: 0,
      encryptedFingerprint: "E2EE-1122",
      createdBy: ALICE_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_react").collection("messages").doc("m_1").set({
      chatId: "chat_react",
      senderUid: ALICE_UID,
      senderName: "Aarav",
      participantUids: [ALICE_UID, BOB_UID],
      text: "Namaste!",
      deliveryState: "READ",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_react").collection("messages").doc("m_1").update({
      reaction: "🙏",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    charlieDb.collection("chats").doc("chat_react").collection("messages").doc("m_1").update({
      reaction: "🔥",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

