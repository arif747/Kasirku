const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_kasir_123";
const BOB_UID = "bob_kasir_456";

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

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("products").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("store").doc("profile").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").set({
      userId: ALICE_UID,
      name: "Kopi Susu",
      category: "Minuman",
      price: 18000,
      stock: 20,
      trackStock: true,
      unit: "cup",
      sku: "KPS-01",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("StoreProfile: owner create, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("store").doc("profile");
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      storeName: "Kedai Kopi Nusantara",
      address: "Jl. Merdeka No. 12",
      phone: "08123456789",
      receiptFooter: "Terima kasih telah berbelanja!",
      taxPercent: 11.0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update of mutable fields succeeds
  await assertSucceeds(
    profileRef.update({
      storeName: "Kedai Kopi Nusantara Cabang 2",
      taxPercent: 10.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt change
  await assertFails(
    profileRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject taxPercent out of bounds
  await assertFails(
    profileRef.update({
      taxPercent: 50.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read/write denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("store").doc("profile").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("store").doc("profile").update({
      storeName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Products: owner CRUD, stock adjustment, @DocumentId guard, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const prodRef = aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1");
  await assertSucceeds(
    prodRef.set({
      userId: ALICE_UID,
      name: "Es Kopi Susu Gula Aren",
      category: "Minuman",
      price: 22000,
      wholesalePrice: 18000,
      wholesaleMinQty: 10,
      wholesaleUnit: "pack",
      stock: 30,
      trackStock: true,
      unit: "cup",
      sku: "MIN-001",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin repository (empty or populated)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").get()
  );

  // Edit product details including wholesale tier
  await assertSucceeds(
    prodRef.update({
      name: "Es Kopi Susu Aren",
      price: 24000,
      wholesalePrice: 19000,
      wholesaleMinQty: 6,
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Adjust stock on checkout
  await assertSucceeds(
    prodRef.update({
      stock: 28,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Negative stock rejected
  await assertFails(
    prodRef.update({
      stock: -1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' inside document body on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_2").set({
      id: "prod_2",
      userId: ALICE_UID,
      name: "Roti Bakar",
      category: "Makanan",
      price: 15000,
      stock: 10,
      trackStock: true,
      unit: "porsi",
      sku: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_3").set({
      userId: ALICE_UID,
      name: "Future Item",
      category: "Lainnya",
      price: 5000,
      stock: 5,
      trackStock: false,
      unit: "pcs",
      sku: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("products").doc("prod_1").delete()
  );
});

test("Transactions: create COMPLETED with saleMode, void once, terminal state lock, and no delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_001");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      receiptNumber: "INV-20261117-001",
      customerName: "Budi",
      saleMode: "WHOLESALE",
      items: [
        {
          productId: "prod_1",
          name: "Es Kopi Susu",
          unitPrice: 18000,
          quantity: 10,
          subtotal: 180000,
        },
      ],
      itemCount: 10,
      subtotal: 180000,
      discountAmount: 0,
      taxAmount: 0,
      totalAmount: 180000,
      paymentMethod: "CASH",
      cashReceived: 200000,
      changeAmount: 20000,
      status: "COMPLETED",
      note: "Order grosir",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Query matching TransactionRepository
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // Reject creating an already-VOID transaction
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_002").set({
      userId: ALICE_UID,
      receiptNumber: "INV-20261117-002",
      customerName: "",
      items: [{ productId: "custom", name: "Kopi", unitPrice: 10000, quantity: 1, subtotal: 10000 }],
      itemCount: 1,
      subtotal: 10000,
      discountAmount: 0,
      taxAmount: 0,
      totalAmount: 10000,
      paymentMethod: "QRIS",
      cashReceived: 10000,
      changeAmount: 0,
      status: "VOID",
      note: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Voiding COMPLETED -> VOID succeeds
  await assertSucceeds(
    txRef.update({
      status: "VOID",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: updating VOID transaction fails
  await assertFails(
    txRef.update({
      status: "COMPLETED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Tampering totals fails
  await assertFails(
    txRef.update({
      totalAmount: 0,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Delete transaction is strictly prohibited
  await assertFails(txRef.delete());

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );
});
