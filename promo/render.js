// usage: node render.js <promoDir> <outDir> <fps> <timeline.json> <copy.json> [times...]
const { chromium } = require('playwright');
const fs = require('fs'), path = require('path');
(async () => {
  const [dir, out, fpsArg, tl, cp, ...only] = process.argv.slice(2);
  const fps = Number(fpsArg || 30);
  const T = JSON.parse(fs.readFileSync(tl)), C = JSON.parse(fs.readFileSync(cp));
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
  page.on('pageerror', e => { console.error('PAGE ERROR', e.message); process.exit(1); });
  await page.addInitScript(([t, c]) => { window.TIMELINE = t; window.COPY = c; }, [T, C]);
  await page.goto('file://' + path.join(dir, 'ad.html'));
  // 한글 폰트는 unicode-range 분할 파일이라 실제 사용 글자를 미리 로드
  await page.evaluate(async (c) => {
    const txt = JSON.stringify(c);
    for (const f of ['400 40px "Black Han Sans"', '400 40px "Noto Sans KR"', '600 40px "Noto Sans KR"', '700 40px "Noto Sans KR"', '800 40px "Noto Sans KR"'])
      await document.fonts.load(f, txt);
    await document.fonts.ready;
  }, C);
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
