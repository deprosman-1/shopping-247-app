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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Authenticated user: can create user profile with phone number", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      phoneNumber: "08012345678",
      displayName: "Alice Shopper",
      defaultAddress: "12 Depot Road"
    })
  );
});

test("Unauthenticated user: cannot read customer orders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("orders").get());
});

test("Authenticated user: can create their own order", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("orders").doc("order_1").set({
      orderId: "order_1",
      userId: ALICE_UID,
      customerName: "Alice Wonderland",
      customerPhone: "08012345678",
      deliveryAddress: "12 Depot Road, Lagos",
      totalAmount: 45000,
      deliveryStatus: "ORDER PLACED"
    })
  );
});

test("Authenticated user: cannot read another user's order", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("orders").doc("bob_order").set({
      orderId: "bob_order",
      userId: BOB_UID,
      customerName: "Bob Builder",
      deliveryAddress: "45 Port Harcourt Way",
      totalAmount: 25000,
      deliveryStatus: "ORDER PLACED"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("orders").doc("bob_order").get());
});

test("Authenticated user: can read products catalog", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("products").doc("prod_1").set({
      productId: "prod_1",
      title: "Designer Polo",
      brand: "Gucci",
      category: "Shirts",
      basePrice: 15000
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("products").doc("prod_1").get());
});

test("Authenticated user: cannot write or modify products catalog", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("products").doc("prod_hack").set({
      productId: "prod_hack",
      title: "Hacked Item",
      brand: "Hacker",
      category: "Shirts",
      basePrice: 0
    })
  );
});
