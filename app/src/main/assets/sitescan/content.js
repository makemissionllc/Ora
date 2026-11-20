// Runs in the page. Collects plain facts about the page (text, forms, links, generator tags) and hands them to
// Ora, which decides what they mean. Nothing leaves the device and nothing on the page is changed.
(() => {
  if (window.top !== window) return;
  const send = (msg) => browser.runtime.sendMessage(Object.assign({ url: location.href }, msg)).catch(() => {});

  function collect() {
    const body = (document.body && document.body.innerText) || "";
    const links = Array.from(document.querySelectorAll("a[href]")).slice(0, 400);
    const linkText = links.map((a) => ((a.textContent || "") + " " + (a.getAttribute("href") || "")).toLowerCase()).join(" | ");
    const meta = (n) => {
      const m = document.querySelector('meta[name="' + n + '"]');
      return m ? (m.getAttribute("content") || "") : "";
    };
    const buttons = Array.from(document.querySelectorAll("button, a, input[type=submit]"))
      .slice(0, 300).map((b) => (b.textContent || b.value || "").toLowerCase()).join(" | ");
    return {
      type: "scan",
      title: (document.title || "").slice(0, 200),
      h1: ((document.querySelector("h1") || {}).textContent || "").trim().slice(0, 200),
      text: body.slice(0, 20000).toLowerCase(),
      words: (body.match(/\S+/g) || []).length,
      password: !!document.querySelector("input[type=password]"),
      card: !!document.querySelector('input[autocomplete="cc-number"], input[name*="card" i], input[name*="cvv" i], input[id*="card" i]'),
      cart: /add to cart|buy now|checkout|add to bag/.test(buttons),
      linkText: linkText.slice(0, 8000),
      generator: meta("generator").slice(0, 120),
      scripts: Array.from(document.scripts).map((s) => s.src || "").filter(Boolean).slice(0, 60).join(" ").toLowerCase().slice(0, 4000),
    };
  }

  // Once the page is idle, and once more a little later for pages that draw their content with scripts.
  send(collect());
  setTimeout(() => send(collect()), 3000);
})();
