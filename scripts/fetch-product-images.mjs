#!/usr/bin/env node
/**
 * Downloads one freely licensed photo per product from Wikimedia Commons,
 * crops it to 640x480, writes a WebP plus a tiny blurred placeholder, and
 * records author and licence in CREDITS.md.
 *
 * Only CC0, public domain, CC BY and CC BY-SA are accepted, so everything that
 * lands in the repository can be redistributed with attribution.
 *
 * Usage: node scripts/fetch-product-images.mjs [--force]
 */

import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync, readFileSync, existsSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const OUT = 'src/main/resources/META-INF/resources/images/products';
const TMP = 'target/image-download';
const FORCE = process.argv.includes('--force');

const ACCEPTED = [/^cc0/i, /^public domain/i, /^cc by/i, /^cc-by/i, /^pd/i];

// Product slug to search term. The term is what a photographer would have
// called the picture, which is not always what a baker calls the product.
const searches = {
  'sourdough-loaf': 'sourdough bread loaf',
  'rye-bread': 'rye bread loaf',
  'baguette': 'baguette bread',
  'wholegrain-loaf': 'wholemeal bread loaf',
  'focaccia': 'focaccia',
  'ciabatta': 'ciabatta bread',
  'seeded-spelt-loaf': 'spelt bread',
  'country-miche': 'country bread miche',
  'butter-croissant': 'croissant',
  'almond-croissant': 'almond croissant',
  'pain-au-chocolat': 'pain au chocolat',
  'cinnamon-bun': 'cinnamon roll',
  'cardamom-knot': 'cardamom bun',
  'apple-danish': 'apple danish pastry',
  'berry-danish': 'berry danish pastry',
  'pistachio-roll': 'pistachio pastry',
  'croissant-with-ham': 'ham croissant sandwich',
  'carrot-cake': 'carrot cake',
  'chocolate-cake': 'chocolate cake',
  'cheesecake': 'cheesecake',
  'lemon-tart': 'lemon tart',
  'strawberry-tart': 'strawberry tart',
  'birthday-sponge': 'sponge cake',
  'tiramisu-cake': 'tiramisu',
  'blueberry-muffin': 'blueberry muffin',
  'chocolate-brownie': 'chocolate brownie',
  'chocolate-chip-cookie': 'chocolate chip cookie',
  'oatmeal-raisin-cookie': 'oatmeal cookie',
  'peanut-butter-cookie': 'peanut butter cookie',
  'shortbread': 'shortbread biscuit',
  'almond-biscotti': 'biscotti',
  'ginger-snap': 'gingerbread cookie',
  'sesame-cracker': 'sesame cracker',
  'spinach-quiche': 'spinach quiche',
  'cheese-and-onion-pie': 'cheese pie',
  'empanada': 'empanada',
  'tuna-roll': 'tuna sandwich',
  'vegetable-focaccia-slice': 'vegetable focaccia',
  'ham-and-cheese-sandwich': 'ham and cheese sandwich',
  'mushroom-pastry': 'mushroom pastry',
  'filter-coffee': 'filter coffee cup',
  'espresso': 'espresso cup',
  'cappuccino': 'cappuccino',
  'oat-latte': 'latte coffee',
  'orange-juice': 'orange juice glass',
  'hot-chocolate': 'hot chocolate cup',
  'iced-tea': 'iced tea glass',
  'sparkling-water': 'sparkling water glass'
};

const api = 'https://commons.wikimedia.org/w/api.php';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Commons throttles hard. One request at a time, with a pause and a retry. */
async function fetchJson(url, attempt = 0) {
  const response = await fetch(url, {
    headers: { 'User-Agent': 'bakery-25.3-demo/1.0 (https://github.com/vaadin/hackathon-25-3)' }
  });
  const text = await response.text();
  try {
    return JSON.parse(text);
  } catch (error) {
    if (attempt < 4) {
      await sleep(4000 * (attempt + 1));
      return fetchJson(url, attempt + 1);
    }
    throw new Error(`throttled: ${text.slice(0, 40)}`);
  }
}

async function search(term) {
  const url = `${api}?action=query&format=json&generator=search&gsrsearch=${
    encodeURIComponent('filetype:bitmap ' + term)}&gsrlimit=8&gsrnamespace=6&prop=imageinfo` +
    '&iiprop=url|extmetadata|size&iiurlwidth=900';
  const body = await fetchJson(url);
  const pages = Object.values(body?.query?.pages ?? {});
  for (const page of pages) {
    const info = page.imageinfo?.[0];
    if (!info) continue;
    const licence = info.extmetadata?.LicenseShortName?.value ?? '';
    const author = (info.extmetadata?.Artist?.value ?? 'Unknown')
      .replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
    if (!ACCEPTED.some((pattern) => pattern.test(licence))) continue;
    if ((info.width ?? 0) < 640) continue;
    return { title: page.title, licence, author, thumb: info.thumburl, page: info.descriptionurl };
  }
  return null;
}

mkdirSync(OUT, { recursive: true });
mkdirSync(TMP, { recursive: true });

// A manifest so credits survive across runs, and a reused file keeps its attribution.
const manifestPath = join(OUT, 'credits.json');
const credits = existsSync(manifestPath) && !FORCE
  ? JSON.parse(readFileSync(manifestPath, 'utf8'))
  : [];
let downloaded = 0;
let reused = 0;
let failed = [];

for (const [slug, term] of Object.entries(searches)) {
  const target = join(OUT, `${slug}.webp`);
  if (existsSync(target) && !FORCE) {
    reused++;
    continue;
  }
  try {
    const hit = await search(term);
    if (!hit) {
      failed.push(`${slug}: nothing acceptable for "${term}"`);
      continue;
    }
    const raw = join(TMP, `${slug}.src`);
    const response = await fetch(hit.thumb, { headers: { 'User-Agent': 'bakery-25.3-demo/1.0' } });
    writeFileSync(raw, Buffer.from(await response.arrayBuffer()));

    // Centre crop to 4:3, then a small WebP and a blur up placeholder.
    execFileSync('magick', [raw, '-resize', '640x480^', '-gravity', 'center', '-extent', '640x480',
      '-quality', '75', target]);
    execFileSync('magick', [target, '-resize', '32x24', '-blur', '0x2', '-quality', '40',
      join(OUT, `${slug}-lqip.webp`)]);
    rmSync(raw, { force: true });

    credits.push({ slug, ...hit });
    downloaded++;
    await sleep(1500);
    console.log(`${slug} <- ${hit.title} (${hit.licence})`);
  } catch (error) {
    failed.push(`${slug}: ${error.message}`);
  }
}

writeFileSync(manifestPath, JSON.stringify(credits, null, 2) + '\n');

if (credits.length > 0) {
  const lines = ['# Image credits', '',
    'Every photograph in this folder comes from Wikimedia Commons under a licence that allows',
    'redistribution with attribution. Downloaded and cropped by scripts/fetch-product-images.mjs.',
    '', '| File | Source | Author | Licence |', '| --- | --- | --- | --- |'];
  for (const credit of credits.sort((a, b) => a.slug.localeCompare(b.slug))) {
    lines.push(`| ${credit.slug}.webp | [${credit.title}](${credit.page}) | ${credit.author} | ${credit.licence} |`);
  }
  writeFileSync(join(OUT, 'CREDITS.md'), lines.join('\n') + '\n');
}

console.log(`downloaded=${downloaded} reused=${reused} failed=${failed.length}`);
failed.forEach((line) => console.log('  ' + line));
