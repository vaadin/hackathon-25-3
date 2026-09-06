#!/usr/bin/env node
/**
 * Emits the demo dataset as SQL. Seeded, so running it twice produces the same
 * bytes and the result is reviewable in a diff.
 *
 * Every date is literal and relative to the anchor, which is deliberately a
 * Monday. At boot DemoDateShifter adds a whole number of weeks, which keeps the
 * weekday alignment intact: shifting by a raw day count would turn a seeded
 * Tuesday into a Friday and silently break "closed on Sundays".
 *
 * Usage: node scripts/generate-dataset.mjs [--out src/main/resources]
 */

import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';

const ANCHOR = '2026-01-05'; // a Monday
const DAYS_BACK = 90;
const DAYS_FORWARD = 21;
const ORDER_COUNT = 1400;
const CUSTOMER_COUNT = 220;

// ---------------------------------------------------------------- seeded rng
let seed = 20260904;
function rnd() {
  seed = (seed * 1664525 + 1013904223) % 4294967296;
  return seed / 4294967296;
}
const pick = (list) => list[Math.floor(rnd() * list.length)];
const between = (min, max) => min + Math.floor(rnd() * (max - min + 1));
const chance = (p) => rnd() < p;

// ---------------------------------------------------------------- date tools
const day = 86400000;
const anchorMs = Date.parse(ANCHOR + 'T00:00:00Z');
const iso = (ms) => new Date(ms).toISOString().slice(0, 10);
const dateAt = (offset) => iso(anchorMs + offset * day);
const weekdayAt = (offset) => new Date(anchorMs + offset * day).getUTCDay(); // 0 sunday
const stamp = (offset, hour, minute) =>
  new Date(anchorMs + offset * day + hour * 3600000 + minute * 60000).toISOString().replace('T', ' ').slice(0, 19);

const q = (value) => (value === null || value === undefined ? 'null' : `'${String(value).replace(/'/g, "''")}'`);
const bool = (value) => (value ? 'true' : 'false');

// ---------------------------------------------------------------- reference data
const categories = [
  { id: 1, name: 'Breads', slug: 'breads', order: 1, icon: 'vaadin:cutlery' },
  { id: 2, name: 'Pastries', slug: 'pastries', order: 2, icon: 'vaadin:coffee' },
  { id: 3, name: 'Cakes', slug: 'cakes', order: 3, icon: 'vaadin:gift' },
  { id: 4, name: 'Cookies', slug: 'cookies', order: 4, icon: 'vaadin:circle' },
  { id: 5, name: 'Savoury', slug: 'savoury', order: 5, icon: 'vaadin:cutlery' },
  { id: 6, name: 'Drinks', slug: 'drinks', order: 6, icon: 'vaadin:glass' }
];

const allergens = [
  'GLUTEN', 'LACTOSE', 'EGG', 'NUTS', 'PEANUT', 'SOY', 'SESAME', 'SULPHITES', 'CELERY', 'MUSTARD', 'FISH', 'MOLLUSC'
].map((code, index) => ({ id: index + 1, code, name: code.charAt(0) + code.slice(1).toLowerCase() }));
const allergenByCode = Object.fromEntries(allergens.map((a) => [a.code, a.id]));

// name, category, price cents, vat, lead days, allergens, weekdays (empty = every day)
const productSpecs = [
  ['Sourdough loaf', 1, 450, 'REDUCED', 0, ['GLUTEN'], []],
  ['Rye bread', 1, 420, 'REDUCED', 0, ['GLUTEN'], []],
  ['Baguette', 1, 260, 'REDUCED', 0, ['GLUTEN'], []],
  ['Wholegrain loaf', 1, 470, 'REDUCED', 0, ['GLUTEN', 'SESAME'], []],
  ['Focaccia', 1, 520, 'REDUCED', 0, ['GLUTEN'], [5, 6]],
  ['Ciabatta', 1, 300, 'REDUCED', 0, ['GLUTEN'], []],
  ['Seeded spelt loaf', 1, 540, 'REDUCED', 1, ['GLUTEN', 'SESAME', 'SOY'], []],
  ['Country miche', 1, 690, 'REDUCED', 1, ['GLUTEN'], [4, 5, 6]],
  ['Butter croissant', 2, 220, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Almond croissant', 2, 290, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'NUTS'], []],
  ['Pain au chocolat', 2, 250, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'SOY'], []],
  ['Cinnamon bun', 2, 240, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Cardamom knot', 2, 260, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Apple danish', 2, 280, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'SULPHITES'], []],
  ['Berry danish', 2, 290, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Pistachio roll', 2, 340, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'NUTS'], []],
  ['Croissant with ham', 2, 350, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'MUSTARD'], []],
  ['Carrot cake', 3, 2400, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG', 'NUTS'], []],
  ['Chocolate cake', 3, 2600, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG', 'SOY'], []],
  ['Cheesecake', 3, 2800, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Lemon tart', 3, 2200, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Strawberry tart', 3, 2500, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG'], [3, 4, 5, 6]],
  ['Birthday sponge', 3, 3200, 'REDUCED', 3, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Tiramisu cake', 3, 2900, 'REDUCED', 2, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Blueberry muffin', 3, 320, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Chocolate brownie', 3, 300, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'NUTS'], []],
  ['Chocolate chip cookie', 4, 180, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'SOY'], []],
  ['Oatmeal raisin cookie', 4, 180, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Peanut butter cookie', 4, 190, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'EGG', 'PEANUT'], []],
  ['Shortbread', 4, 170, 'REDUCED', 0, ['GLUTEN', 'LACTOSE'], []],
  ['Almond biscotti', 4, 200, 'REDUCED', 0, ['GLUTEN', 'EGG', 'NUTS'], []],
  ['Ginger snap', 4, 160, 'REDUCED', 0, ['GLUTEN', 'EGG'], []],
  ['Sesame cracker', 4, 210, 'REDUCED', 0, ['GLUTEN', 'SESAME'], []],
  ['Spinach quiche', 5, 480, 'REDUCED', 1, ['GLUTEN', 'LACTOSE', 'EGG'], []],
  ['Cheese and onion pie', 5, 520, 'REDUCED', 1, ['GLUTEN', 'LACTOSE', 'EGG', 'MUSTARD'], []],
  ['Empanada', 5, 400, 'REDUCED', 0, ['GLUTEN', 'EGG'], [2, 4, 6]],
  ['Tuna roll', 5, 430, 'REDUCED', 0, ['GLUTEN', 'FISH', 'EGG'], []],
  ['Vegetable focaccia slice', 5, 350, 'REDUCED', 0, ['GLUTEN', 'CELERY'], []],
  ['Ham and cheese sandwich', 5, 560, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'MUSTARD'], []],
  ['Mushroom pastry', 5, 460, 'REDUCED', 0, ['GLUTEN', 'LACTOSE', 'CELERY'], []],
  ['Filter coffee', 6, 190, 'STANDARD', 0, [], []],
  ['Espresso', 6, 160, 'STANDARD', 0, [], []],
  ['Cappuccino', 6, 240, 'STANDARD', 0, ['LACTOSE'], []],
  ['Oat latte', 6, 260, 'STANDARD', 0, ['GLUTEN', 'SOY'], []],
  ['Orange juice', 6, 280, 'STANDARD', 0, [], []],
  ['Hot chocolate', 6, 270, 'STANDARD', 0, ['LACTOSE', 'SOY'], []],
  ['Iced tea', 6, 230, 'STANDARD', 0, ['SULPHITES'], []],
  ['Sparkling water', 6, 180, 'STANDARD', 0, [], []]
];

const slug = (name) => name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

const products = productSpecs.map(([name, category, price, vat, lead, allergenCodes, weekdays], index) => ({
  id: index + 1,
  name,
  slug: slug(name),
  categoryId: category,
  priceCents: price,
  vatRate: vat,
  leadTimeDays: lead,
  allergens: allergenCodes,
  weekdays,
  imagePath: `${slug(name)}.webp`,
  featured: index % 9 === 0,
  stockToday: between(0, 40),
  dailyCapacity: lead > 0 ? between(4, 12) : null,
  sortOrder: index,
  // Ten products carry half the volume, so the top products chart says something.
  weight: index % 5 === 0 ? 8 : index % 3 === 0 ? 3 : 1
}));

const description = (product) => {
  const category = categories.find((c) => c.id === product.categoryId).name.toLowerCase();
  return `## ${product.name}\n\nBaked this morning in our own ovens. Part of our ${category} range.\n\n` +
    `- Made with ingredients we can name\n- No improvers, no shortcuts\n` +
    (product.leadTimeDays > 0 ? `- Order ${product.leadTimeDays} day(s) ahead\n` : '- Available every morning\n');
};

const locations = [
  { id: 1, name: 'Bakery', street: 'Calle del Horno 12', postal: '28013', city: 'Madrid', opens: '07:30', closes: '20:00', minutes: 30, capacity: 12, closedWeekdays: ['SUNDAY'] },
  { id: 2, name: 'Market stall', street: 'Mercado de San Anton 3', postal: '28004', city: 'Madrid', opens: '09:00', closes: '15:00', minutes: 30, capacity: 8, closedWeekdays: ['SUNDAY', 'MONDAY'] },
  { id: 3, name: 'Station kiosk', street: 'Estacion de Atocha', postal: '28045', city: 'Madrid', opens: '06:30', closes: '21:00', minutes: 30, capacity: 6, closedWeekdays: [] }
];

const closureSpecs = [
  [-83, null, 'Public holiday', 'HOLIDAY'],
  [-62, null, 'Public holiday', 'HOLIDAY'],
  [-41, 2, 'Market closed for cleaning', 'MAINTENANCE'],
  [-27, null, 'Public holiday', 'HOLIDAY'],
  [-14, 1, 'Oven maintenance', 'MAINTENANCE'],
  [-6, null, 'Public holiday', 'HOLIDAY'],
  [3, null, 'Public holiday', 'HOLIDAY'],
  [8, 3, 'Station works', 'MAINTENANCE'],
  [12, null, 'Public holiday', 'HOLIDAY'],
  [17, 2, 'Market closed', 'MAINTENANCE'],
  [24, null, 'Public holiday', 'HOLIDAY'],
  [31, null, 'Public holiday', 'HOLIDAY'],
  [38, 1, 'Annual deep clean', 'MAINTENANCE'],
  [45, null, 'Public holiday', 'HOLIDAY']
];

const firstNames = ['Ana', 'Luis', 'Marta', 'Carlos', 'Elena', 'Javier', 'Sofia', 'Miguel', 'Laura', 'Pablo',
  'Nuria', 'Diego', 'Carmen', 'Sergio', 'Lucia', 'Alberto', 'Irene', 'Andres', 'Paula', 'Ruben',
  'Heidi', 'Malin', 'Olli', 'Tuomas', 'Anneli', 'Petter', 'Kirsi', 'Jonas', 'Freja', 'Emil'];
const lastNames = ['Ruiz', 'Garcia', 'Lopez', 'Martinez', 'Sanchez', 'Perez', 'Gomez', 'Fernandez', 'Diaz', 'Moreno',
  'Alvarez', 'Romero', 'Navarro', 'Torres', 'Ramos', 'Gil', 'Serrano', 'Blanco', 'Molina', 'Castro',
  'Virtanen', 'Korhonen', 'Makinen', 'Nieminen', 'Heikkinen', 'Laine', 'Salminen', 'Aalto'];

const users = [
  { id: 1, email: 'admin@bakery.test', password: 'admin', first: 'Goran', last: 'Rich', role: 'ADMIN', locale: 'en', locked: false },
  { id: 2, email: 'baker@bakery.test', password: 'baker', first: 'Heidi', last: 'Carter', role: 'BAKER', locale: 'en', locked: false },
  { id: 3, email: 'barista@bakery.test', password: 'barista', first: 'Malin', last: 'Castro', role: 'BARISTA', locale: 'es', locked: false },
  { id: 4, email: 'locked@bakery.test', password: 'locked', first: 'Peter', last: 'Nord', role: 'BARISTA', locale: 'en', locked: true },
  { id: 5, email: 'ana@bakery.test', password: 'ana', first: 'Ana', last: 'Serrano', role: 'BAKER', locale: 'es', locked: false },
  { id: 6, email: 'luis@bakery.test', password: 'luis', first: 'Luis', last: 'Gil', role: 'BAKER', locale: 'es', locked: false },
  { id: 7, email: 'irene@bakery.test', password: 'irene', first: 'Irene', last: 'Ramos', role: 'BARISTA', locale: 'es', locked: false },
  { id: 8, email: 'jonas@bakery.test', password: 'jonas', first: 'Jonas', last: 'Laine', role: 'BARISTA', locale: 'en', locked: false }
];

// BCrypt hashes, generated once with htpasswd -bnBC 10 and pinned here so the
// dataset stays reproducible without hashing at generation time.
const passwordHashes = {
  admin: '$2a$10$HZ7/W22b2gvWUnagmUh04.AqbFhAKcp.z2BVfdRfxAV49kKxjUMo.',
  baker: '$2a$10$Kl5lEEGCEehkRzQhv73go.deB9UAiFK.vzt6A57V4rMllrTjc71vm',
  barista: '$2a$10$k19y/ryecGNCxcevsYQCCuSTRQ5joAd2Gjx/1Bw6SzCBEc3S/E8Z2',
  locked: '$2a$10$HI2pC/T9vWalKBFQNwUBquy4Jv5GVN8P4Xhns4yr/AXAHDLphPzdm',
  ana: '$2a$10$HZ7/W22b2gvWUnagmUh04.AqbFhAKcp.z2BVfdRfxAV49kKxjUMo.',
  luis: '$2a$10$HZ7/W22b2gvWUnagmUh04.AqbFhAKcp.z2BVfdRfxAV49kKxjUMo.',
  irene: '$2a$10$k19y/ryecGNCxcevsYQCCuSTRQ5joAd2Gjx/1Bw6SzCBEc3S/E8Z2',
  jonas: '$2a$10$k19y/ryecGNCxcevsYQCCuSTRQ5joAd2Gjx/1Bw6SzCBEc3S/E8Z2'
};

// ---------------------------------------------------------------- customers
const customers = [];
for (let i = 1; i <= CUSTOMER_COUNT; i++) {
  const first = pick(firstNames);
  const last = pick(lastNames);
  const business = i <= 12;
  customers.push({
    id: i,
    first,
    last,
    email: `${first}.${last}${i}@example.com`.toLowerCase(),
    phone: `+34 6${between(10, 99)} ${between(100, 999)} ${between(100, 999)}`,
    street: business ? `Calle ${pick(lastNames)} ${between(1, 90)}` : null,
    postal: business ? `28${between(100, 999)}` : null,
    city: business ? 'Madrid' : null,
    vatId: business ? `B${between(10000000, 99999999)}` : null,
    marketing: chance(0.35),
    createdAt: stamp(-between(90, 400), between(8, 19), between(0, 59)),
    notes: chance(0.08) ? 'Regular customer, knows the staff by name.' : null
  });
}

// ---------------------------------------------------------------- orders
const weightedProducts = [];
products.forEach((product) => {
  for (let i = 0; i < product.weight; i++) {
    weightedProducts.push(product);
  }
});

const closureDates = new Set(closureSpecs.filter(([, loc]) => loc === null).map(([offset]) => offset));
const closureByLocation = new Map();
closureSpecs.filter(([, loc]) => loc !== null).forEach(([offset, loc]) => {
  if (!closureByLocation.has(loc)) {
    closureByLocation.set(loc, new Set());
  }
  closureByLocation.get(loc).add(offset);
});

function locationOpen(location, offset) {
  const weekday = weekdayAt(offset);
  const names = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  if (location.closedWeekdays.includes(names[weekday])) {
    return false;
  }
  if (closureDates.has(offset)) {
    return false;
  }
  const own = closureByLocation.get(location.id);
  return !(own && own.has(offset));
}

function slotTimes(location) {
  const times = [];
  const [openH, openM] = location.opens.split(':').map(Number);
  const [closeH, closeM] = location.closes.split(':').map(Number);
  for (let minutes = openH * 60 + openM; minutes < closeH * 60 + closeM; minutes += location.minutes) {
    times.push(`${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}:00`);
  }
  return times;
}

/** Busy at opening and at lunch, quiet mid afternoon, so utilisation has a shape. */
function pickTime(location) {
  const times = slotTimes(location);
  const weights = times.map((time) => {
    const hour = Number(time.slice(0, 2));
    if (hour <= 9) return 6;
    if (hour <= 11) return 3;
    if (hour <= 14) return 7;
    if (hour <= 17) return 2;
    return 4;
  });
  const total = weights.reduce((a, b) => a + b, 0);
  let target = rnd() * total;
  for (let i = 0; i < times.length; i++) {
    target -= weights[i];
    if (target <= 0) return times[i];
  }
  return times[times.length - 1];
}

function stateFor(offset) {
  if (offset < -1) {
    return chance(0.93) ? 'PICKED_UP' : 'CANCELLED';
  }
  if (offset === -1 || offset === 0) {
    const roll = rnd();
    if (roll < 0.25) return 'READY';
    if (roll < 0.45) return 'IN_PREPARATION';
    if (roll < 0.65) return 'CONFIRMED';
    if (roll < 0.8) return 'PICKED_UP';
    if (roll < 0.88) return 'NEW';
    if (roll < 0.95) return 'PROBLEM';
    return 'CANCELLED';
  }
  if (offset <= 2) {
    return chance(0.7) ? 'CONFIRMED' : 'NEW';
  }
  return chance(0.85) ? 'NEW' : 'CONFIRMED';
}

const orders = [];
const orderItems = [];
const historyItems = [];
const messages = [];
const invoices = [];
const invoiceLines = [];
const slotUsage = new Map();

let orderId = 0;
let itemId = 0;
let historyId = 0;
let messageId = 0;
let invoiceId = 0;
let invoiceLineId = 0;
let referenceCounter = 0;

// Volume trends upward across the range and jumps at the weekend.
function ordersForDay(offset) {
  const trend = 1 + (offset + DAYS_BACK) / (DAYS_BACK + DAYS_FORWARD) * 0.6;
  const weekday = weekdayAt(offset);
  const weekendBoost = weekday === 5 || weekday === 6 ? 1.7 : 1;
  return Math.max(1, Math.round(9 * trend * weekendBoost * (0.7 + rnd() * 0.6)));
}

outer: for (let offset = -DAYS_BACK; offset <= DAYS_FORWARD; offset++) {
  const count = ordersForDay(offset);
  for (let i = 0; i < count; i++) {
    if (orders.length >= ORDER_COUNT) {
      break outer;
    }
    const openLocations = locations.filter((location) => locationOpen(location, offset));
    if (openLocations.length === 0) {
      continue;
    }
    const location = pick(openLocations);
    const time = pickTime(location);
    const slotKey = `${location.id}|${offset}|${time}`;
    const used = slotUsage.get(slotKey) ?? 0;
    if (used >= location.capacity) {
      continue;
    }
    slotUsage.set(slotKey, used + 1);

    const state = stateFor(offset);
    const customer = customers[between(0, customers.length - 1)];
    orderId++;
    referenceCounter++;
    const lineCount = between(1, 5);
    let net = 0;
    let vat = 0;
    const usedProducts = new Set();
    for (let line = 0; line < lineCount; line++) {
      const product = pick(weightedProducts);
      if (usedProducts.has(product.id)) {
        continue;
      }
      if (product.weekdays.length > 0 && !product.weekdays.includes(weekdayAt(offset))) {
        continue;
      }
      usedProducts.add(product.id);
      itemId++;
      const quantity = product.priceCents > 1500 ? between(1, 2) : between(1, 6);
      const lineNet = product.priceCents * quantity;
      const vatPercent = product.vatRate === 'STANDARD' ? 21 : 10;
      const lineVat = Math.round(lineNet * vatPercent / 100);
      net += lineNet;
      vat += lineVat;
      orderItems.push({
        id: itemId,
        orderId,
        position: usedProducts.size - 1,
        productId: product.id,
        quantity,
        unitPriceCents: product.priceCents,
        vatRate: product.vatRate,
        comment: chance(0.12) ? pick(['Gluten free please', 'Happy birthday Ana', 'Sliced, please', 'Extra crusty', 'No nuts on top']) : null
      });
    }
    if (usedProducts.size === 0) {
      orderId--;
      referenceCounter--;
      slotUsage.set(slotKey, used);
      continue;
    }

    const placedOffset = offset - between(1, 6);
    const order = {
      id: orderId,
      reference: `ORD-2026-${String(referenceCounter).padStart(6, '0')}`,
      customerId: customer.id,
      locationId: location.id,
      pickupDate: dateAt(offset),
      pickupTime: time,
      state,
      channel: chance(0.62) ? 'ONLINE' : chance(0.5) ? 'PHONE' : 'COUNTER',
      assignedBakerId: ['IN_PREPARATION', 'READY', 'PICKED_UP'].includes(state) ? pick([2, 5, 6]) : null,
      customerNote: chance(0.09) ? pick(['Please keep it cold until pickup.', 'I will come with the car, is there parking?', 'It is for a birthday, thank you!']) : null,
      internalNote: chance(0.05) ? 'Customer called twice about this one.' : null,
      totalNetCents: net,
      totalVatCents: vat,
      totalGrossCents: net + vat,
      placedAt: stamp(placedOffset, between(8, 20), between(0, 59)),
      createdById: null,
      trackingToken: `tk${String(orderId).padStart(6, '0')}${String(between(100000, 999999))}`
    };
    if (order.channel !== 'ONLINE') {
      order.createdById = pick([3, 7, 8]);
    }
    orders.push(order);

    // History that matches the state it ended in.
    const path = { NEW: ['NEW'], CONFIRMED: ['NEW', 'CONFIRMED'], IN_PREPARATION: ['NEW', 'CONFIRMED', 'IN_PREPARATION'],
      READY: ['NEW', 'CONFIRMED', 'IN_PREPARATION', 'READY'],
      PICKED_UP: ['NEW', 'CONFIRMED', 'IN_PREPARATION', 'READY', 'PICKED_UP'],
      PROBLEM: ['NEW', 'CONFIRMED', 'PROBLEM'], CANCELLED: ['NEW', 'CANCELLED'] }[state];
    path.forEach((step, index) => {
      historyId++;
      historyItems.push({
        id: historyId,
        orderId,
        position: index,
        newState: step,
        message: `ordering.history.${step.toLowerCase()}`,
        timestamp: stamp(placedOffset + index * 0, 8 + index, between(0, 59)),
        createdById: step === 'NEW' ? order.createdById : pick([2, 3, 5, 7])
      });
    });

    if (chance(0.13)) {
      messageId++;
      messages.push({
        id: messageId,
        orderId,
        position: 0,
        authorName: `${customer.first} ${customer.last}`,
        fromStaff: false,
        authorId: null,
        text: pick(['Can I add a candle to the cake?', 'Is the bread still warm at that time?',
          'Could I pick it up half an hour later?', 'Do you have a gluten free option for this?']),
        sentAt: stamp(placedOffset, between(9, 18), between(0, 59)),
        readByStaff: chance(0.6)
      });
      if (chance(0.7)) {
        messageId++;
        messages.push({
          id: messageId,
          orderId,
          position: 1,
          authorName: 'Bakery',
          fromStaff: true,
          authorId: pick([3, 7, 8]),
          text: pick(['Of course, we will take care of it.', 'Yes, out of the oven at eight.',
            'No problem, we will keep it for you.', 'We can do that, see you then.']),
          sentAt: stamp(placedOffset, between(9, 19), between(0, 59)),
          readByStaff: true
        });
      }
    }

    if (state === 'PICKED_UP') {
      invoiceId++;
      const invoiceNumber = `2026-${String(invoiceId).padStart(6, '0')}`;
      const lines = orderItems.filter((item) => item.orderId === orderId);
      let invoiceNet = 0;
      let invoiceVat = 0;
      lines.forEach((item, index) => {
        invoiceLineId++;
        const product = products.find((p) => p.id === item.productId);
        const lineNet = item.unitPriceCents * item.quantity;
        const percent = item.vatRate === 'STANDARD' ? 21 : 10;
        const lineVat = Math.round(lineNet * percent / 100);
        invoiceNet += lineNet;
        invoiceVat += lineVat;
        invoiceLines.push({
          id: invoiceLineId,
          invoiceId,
          position: index,
          description: product.name,
          quantity: item.quantity,
          unitPriceCents: item.unitPriceCents,
          vatRatePercent: percent,
          netCents: lineNet,
          vatCents: lineVat,
          grossCents: lineNet + lineVat
        });
      });
      const paid = chance(0.82);
      invoices.push({
        id: invoiceId,
        number: invoiceNumber,
        orderId,
        status: paid ? 'PAID' : 'ISSUED',
        issuedAt: dateAt(offset),
        dueAt: dateAt(offset + 14),
        billingName: `${customer.first} ${customer.last}`,
        billingEmail: customer.email,
        street: customer.street,
        postal: customer.postal,
        city: customer.city,
        country: 'ES',
        vatId: customer.vatId,
        netCents: invoiceNet,
        vatCents: invoiceVat,
        grossCents: invoiceNet + invoiceVat,
        paid,
        paidAt: paid ? stamp(offset, between(9, 20), between(0, 59)) : null
      });
    }
  }
}

// ---------------------------------------------------------------- emit
function emit(dialect) {
  const out = [];
  const push = (line) => out.push(line);
  const insert = (table, columns, rows) => {
    if (rows.length === 0) {
      return;
    }
    push(`-- ${table}: ${rows.length} rows`);
    for (const row of rows) {
      push(`insert into ${table} (${columns.join(', ')}) values (${row.join(', ')});`);
    }
    push('');
  };

  push('-- Generated by scripts/generate-dataset.mjs. Do not edit by hand.');
  push(`-- Anchor ${ANCHOR} (a Monday). DemoDateShifter moves every date by whole`);
  push('-- weeks at boot, which keeps weekday alignment intact.');
  push('');

  insert('app_user', ['id', 'version', 'email', 'password_hash', 'first_name', 'last_name', 'role', 'locale', 'locked'],
    users.map((u) => [u.id, 0, q(u.email), q(passwordHashes[u.password]), q(u.first), q(u.last), q(u.role), q(u.locale), bool(u.locked)]));

  insert('category', ['id', 'version', 'name', 'slug', 'display_order', 'icon_name'],
    categories.map((c) => [c.id, 0, q(c.name), q(c.slug), c.order, q(c.icon)]));

  insert('allergen', ['id', 'version', 'code', 'name'],
    allergens.map((a) => [a.id, 0, q(a.code), q(a.name)]));

  insert('product', ['id', 'version', 'name', 'slug', 'category_id', 'description_markdown', 'price_cents',
    'vat_rate', 'image_path', 'lead_time_days', 'daily_capacity', 'stock_today', 'available', 'featured', 'sort_order'],
    products.map((p) => [p.id, 0, q(p.name), q(p.slug), p.categoryId, q(description(p)), p.priceCents,
      q(p.vatRate), q(p.imagePath), p.leadTimeDays, p.dailyCapacity ?? 'null', p.stockToday,
      bool(true), bool(p.featured), p.sortOrder]));

  insert('product_allergen', ['product_id', 'allergen_id'],
    products.flatMap((p) => p.allergens.map((code) => [p.id, allergenByCode[code]])));

  insert('product_weekday', ['product_id', 'day_of_week'],
    products.flatMap((p) => p.weekdays.map((weekday) =>
      [p.id, q(['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][weekday])])));

  insert('pickup_location', ['id', 'version', 'name', 'street', 'postal_code', 'city', 'country',
    'opens_at', 'closes_at', 'slot_minutes', 'slot_capacity', 'active'],
    locations.map((l) => [l.id, 0, q(l.name), q(l.street), q(l.postal), q(l.city), q('ES'),
      q(l.opens + ':00'), q(l.closes + ':00'), l.minutes, l.capacity, bool(true)]));

  insert('location_closed_weekday', ['location_id', 'day_of_week'],
    locations.flatMap((l) => l.closedWeekdays.map((weekday) => [l.id, q(weekday)])));

  insert('pickup_closure', ['id', 'version', 'location_id', 'date', 'whole_day', 'from_time', 'to_time', 'reason', 'kind'],
    closureSpecs.map(([offset, location, reason, kind], index) =>
      [index + 1, 0, location ?? 'null', q(dateAt(offset)), bool(true), 'null', 'null', q(reason), q(kind)]));

  insert('customer', ['id', 'version', 'first_name', 'last_name', 'email', 'phone', 'street', 'postal_code',
    'city', 'country', 'vat_id', 'notes', 'marketing_opt_in', 'created_at'],
    customers.map((c) => [c.id, 0, q(c.first), q(c.last), q(c.email), q(c.phone), q(c.street), q(c.postal),
      q(c.city), q('ES'), q(c.vatId), q(c.notes), bool(c.marketing), q(c.createdAt)]));

  insert('orders', ['id', 'version', 'reference', 'customer_id', 'pickup_location_id', 'pickup_date', 'pickup_time',
    'state', 'channel', 'assigned_baker_id', 'customer_note', 'internal_note', 'total_net_cents', 'total_vat_cents',
    'total_gross_cents', 'placed_at', 'created_by_id', 'tracking_token'],
    orders.map((o) => [o.id, 0, q(o.reference), o.customerId, o.locationId, q(o.pickupDate), q(o.pickupTime),
      q(o.state), q(o.channel), o.assignedBakerId ?? 'null', q(o.customerNote), q(o.internalNote),
      o.totalNetCents, o.totalVatCents, o.totalGrossCents, q(o.placedAt), o.createdById ?? 'null', q(o.trackingToken)]));

  insert('order_item', ['id', 'version', 'order_id', 'position', 'product_id', 'quantity', 'unit_price_cents',
    'vat_rate', 'comment'],
    orderItems.map((i) => [i.id, 0, i.orderId, i.position, i.productId, i.quantity, i.unitPriceCents,
      q(i.vatRate), q(i.comment)]));

  insert('order_history_item', ['id', 'version', 'order_id', 'position', 'new_state', 'message', 'timestamp', 'created_by_id'],
    historyItems.map((h) => [h.id, 0, h.orderId, h.position, q(h.newState), q(h.message), q(h.timestamp),
      h.createdById ?? 'null']));

  insert('order_message', ['id', 'version', 'order_id', 'position', 'author_name', 'from_staff', 'author_id',
    'text', 'sent_at', 'read_by_staff'],
    messages.map((m) => [m.id, 0, m.orderId, m.position, q(m.authorName), bool(m.fromStaff), m.authorId ?? 'null',
      q(m.text), q(m.sentAt), bool(m.readByStaff)]));

  insert('invoice', ['id', 'version', 'number', 'order_id', 'status', 'issued_at', 'due_at', 'billing_name',
    'billing_email', 'street', 'postal_code', 'city', 'country', 'vat_id', 'net_cents', 'vat_cents', 'gross_cents',
    'paid', 'paid_at'],
    invoices.map((i) => [i.id, 0, q(i.number), i.orderId, q(i.status), q(i.issuedAt), q(i.dueAt), q(i.billingName),
      q(i.billingEmail), q(i.street), q(i.postal), q(i.city), q(i.country), q(i.vatId), i.netCents, i.vatCents,
      i.grossCents, bool(i.paid), q(i.paidAt)]));

  insert('invoice_line', ['id', 'version', 'invoice_id', 'position', 'description', 'quantity', 'unit_price_cents',
    'vat_rate_percent', 'net_cents', 'vat_cents', 'gross_cents'],
    invoiceLines.map((l) => [l.id, 0, l.invoiceId, l.position, q(l.description), l.quantity, l.unitPriceCents,
      l.vatRatePercent, l.netCents, l.vatCents, l.grossCents]));

  // The sequence has to clear the explicit identifiers the dataset uses.
  const highest = Math.max(orderItems.length, orders.length, customers.length, invoiceLines.length) + 1000;
  if (dialect === 'postgresql') {
    push(`alter sequence entity_seq restart with ${Math.max(10000, highest)};`);
  } else {
    push(`alter sequence entity_seq restart with ${Math.max(10000, highest)};`);
  }
  return out.join('\n') + '\n';
}

const outDir = process.argv.includes('--out')
  ? process.argv[process.argv.indexOf('--out') + 1]
  : 'src/main/resources';
mkdirSync(outDir, { recursive: true });
for (const dialect of ['h2', 'postgresql']) {
  const file = join(outDir, `data-${dialect}.sql`);
  writeFileSync(file, emit(dialect));
  console.log(`${file}: ${emit(dialect).split('\n').length} lines`);
}
console.log(`orders=${orders.length} items=${orderItems.length} history=${historyItems.length} ` +
  `messages=${messages.length} invoices=${invoices.length} lines=${invoiceLines.length} customers=${customers.length}`);
