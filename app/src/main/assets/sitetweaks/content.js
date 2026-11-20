// Applies this site's tweaks: dark (inverted page, pictures kept), wide (text fills the screen) and a font.
// The font rule carries an id in its selector so it beats the Website font in Settings when both are on.
const STYLE_ID = "ora-site-tweaks";
const SITE = location.hostname.toLowerCase().replace(/^www\./, "");
const FONTS = {
  serif: 'Georgia, "Times New Roman", serif',
  sans: 'system-ui, "Helvetica Neue", Arial, sans-serif',
  mono: 'ui-monospace, "Courier New", monospace',
};
const SKIP = [
  "i", "code", "pre", "kbd", "samp", "[class*='icon' i]", "[class*='glyph' i]",
  "[class^='fa']", "[class*=' fa-']", "[class*='material-']", "[data-icon]",
].map((s) => ":not(" + s + ")").join("");

function apply(t) {
  let style = document.getElementById(STYLE_ID);
  if (!t || (!t.dark && !t.wide && !t.font)) { if (style) style.remove(); return; }
  if (!style) {
    style = document.createElement("style");
    style.id = STYLE_ID;
    (document.head || document.documentElement).appendChild(style);
  }
  let css = "";
  if (t.dark) {
    css += "html{filter:invert(1) hue-rotate(180deg) !important;background:#fff !important}" +
      "img,video,picture,canvas,iframe,[style*='background-image']{filter:invert(1) hue-rotate(180deg) !important}";
  }
  if (t.wide) {
    css += "main,article,[role='main'],#content,#main,.content,.container,.post,.entry-content,.article{" +
      "max-width:100% !important;margin-left:0 !important;margin-right:0 !important}";
  }
  if (t.font && FONTS[t.font]) {
    css += "html body:not(#ora-tweak), html body *:not(#ora-tweak)" + SKIP + "{font-family:" + FONTS[t.font] + " !important}";
  }
  style.textContent = css;
}

browser.runtime.sendMessage({ type: "get" }).then((r) => apply(r && r.tweaks && r.tweaks[SITE])).catch(() => {});
browser.runtime.onMessage.addListener((msg) => {
  if (msg && msg.type === "tweaks") apply(msg.tweaks && msg.tweaks[SITE]);
});
