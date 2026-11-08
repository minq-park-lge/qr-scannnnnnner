// usage: node render.js <promoDir> <outDir> [fps] [times...]
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [dir, out, fpsArg, ...only] = process.argv.slice(2);
  const fps = Number(fpsArg || 30);
  const T = JSON.parse(fs.readFileSync(path.join(dir, 'timeline.json')));
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  await page.addInitScript(t => { window.TIMELINE = t; }, T);
  await page.goto('file://' + path.join(dir, 'ad.html'));
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(300);
  const times = only.length ? only.map(Number) : Array.from({ length: Math.round(T.end * fps) }, (_, i) => i / fps);
  for (let i = 0; i < times.length; i++) {
    await page.evaluate(t => window.render(t), times[i]);
    const name = only.length ? `t_${times[i].toFixed(2)}.jpg` : `f_${String(i).padStart(5, '0')}.jpg`;
    await page.screenshot({ path: path.join(out, name), type: 'jpeg', quality: 92 });
  }
  await browser.close();
  console.log('frames', times.length);
})();
