// Reads <meta name="theme-color"> and reports the colour as r, g, b, or null when the page has none.
(function () {
  let lastKey = null;

  function parse(value) {
    const probe = document.createElement("span");
    probe.style.display = "none";
    document.documentElement.appendChild(probe);
    probe.style.color = value;
    const computed = getComputedStyle(probe).color;
    probe.remove();
    const m = computed.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
    return m ? { r: +m[1], g: +m[2], b: +m[3] } : null;
  }

  function report() {
    const meta = document.querySelector('meta[name="theme-color"]');
    const color = meta && meta.content ? parse(meta.content) : null;
    const key = JSON.stringify([location.href, color]);
    if (key === lastKey) return;
    lastKey = key;
    browser.runtime.sendMessage({ url: location.href, color: color });
  }

  report();
  new MutationObserver(report).observe(document.head || document.documentElement, {
    childList: true, subtree: true, attributes: true, attributeFilter: ["content", "name"]
  });
})();
