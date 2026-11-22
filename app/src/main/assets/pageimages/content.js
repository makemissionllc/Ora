// Runs in the page. It does nothing until Ora asks, then lists the page's images (address, best candidate of a
// srcset, size) and sends the list back. Nothing on the page is changed and nothing leaves the device.
(() => {
  if (window.top !== window) return;
  const send = (msg) => browser.runtime.sendMessage(Object.assign({ url: location.href }, msg)).catch(() => {});

  // The address of the widest candidate in a srcset ("a.jpg 480w, b.jpg 1080w" or "a.jpg 1x, b.jpg 2x").
  function best(srcset) {
    let pick = "", top = -1;
    (srcset || "").split(",").forEach((part) => {
      const bits = part.trim().split(/\s+/);
      if (!bits[0]) return;
      const unit = bits[1] || "1x";
      const n = parseFloat(unit) || 1;
      const score = unit.endsWith("w") ? n : n * 1000;
      if (score > top) { top = score; pick = bits[0]; }
    });
    return pick;
  }

  function absolute(src) {
    try {
      const u = new URL(src, document.baseURI);
      return u.protocol === "http:" || u.protocol === "https:" ? u.href : "";
    } catch (e) { return ""; }
  }

  function collect() {
    const found = [];
    const add = (src, w, h) => { const a = absolute(src); if (a) found.push({ src: a, w: w || 0, h: h || 0 }); };
    document.querySelectorAll("img").forEach((img) => {
      const w = img.naturalWidth, h = img.naturalHeight;
      if (w && h && w < 24 && h < 24) return; // tracking pixels and spacers
      const parent = img.parentElement;
      const sourceSet = parent && parent.tagName === "PICTURE" ? best((parent.querySelector("source[srcset]") || {}).srcset) : "";
      add(best(img.getAttribute("srcset") || img.getAttribute("data-srcset")) || sourceSet || img.currentSrc || img.getAttribute("src") || img.getAttribute("data-src"), w, h);
    });
    document.querySelectorAll("picture source[srcset]").forEach((s) => add(best(s.srcset)));
    document.querySelectorAll("video[poster]").forEach((v) => add(v.getAttribute("poster")));
    document.querySelectorAll("input[type=image][src]").forEach((i) => add(i.getAttribute("src")));
    return found.slice(0, 600);
  }

  browser.runtime.onMessage.addListener((msg) => {
    if (msg && msg.type === "collect") send({ type: "images", id: msg.id, items: collect() });
  });
  // Tells Ora which tab this page is in, so a request can name it.
  send({ type: "hello" });
})();
