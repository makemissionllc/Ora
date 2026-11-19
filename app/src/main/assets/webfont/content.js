// Puts the chosen font on the page. Icon fonts, code and form-less glyph classes are left alone so icons stay icons.
const STYLE_ID = "ora-web-font";
const SKIP = [
  "i", "svg", "svg *", "code", "pre", "kbd", "samp", "[class*='icon' i]", "[class*='glyph' i]",
  "[class^='fa']", "[class*=' fa-']", "[class*='material-']", "[class*='bi-']", "[data-icon]",
].map((s) => ":not(" + s + ")").join("");

function apply(key) {
  let style = document.getElementById(STYLE_ID);
  if (!key) { if (style) style.remove(); return; }
  if (!style) {
    style = document.createElement("style");
    style.id = STYLE_ID;
    (document.head || document.documentElement).appendChild(style);
  }
  const url = browser.runtime.getURL("fonts/" + key + ".ttf");
  style.textContent =
    '@font-face{font-family:"OraWeb";src:url("' + url + '") format("truetype");font-weight:100 900;font-display:swap}' +
    "html, body, body *" + SKIP + '{font-family:"OraWeb",system-ui,sans-serif !important}';
}

browser.runtime.sendMessage({ type: "get" }).then((r) => apply(r && r.key)).catch(() => {});
browser.runtime.onMessage.addListener((msg) => { if (msg && msg.type === "font") apply(msg.key); });
