// Hides the elements chosen for this site, and runs the picker when Ora asks for one.
const STYLE_ID = "ora-zapper-style";
const HOST = location.hostname;
let hidden = [];

function applyRules(list) {
  hidden = list || [];
  let style = document.getElementById(STYLE_ID);
  if (!hidden.length) { if (style) style.remove(); return; }
  if (!style) {
    style = document.createElement("style");
    style.id = STYLE_ID;
    (document.head || document.documentElement).appendChild(style);
  }
  style.textContent = hidden.map((s) => s + "{display:none !important}").join("\n");
}

// A selector that matches this element alone: an id if unique, else tag and classes with positions up the tree.
function selectorFor(el) {
  const parts = [];
  let node = el;
  while (node && node.nodeType === 1 && node !== document.documentElement) {
    if (node.id && document.querySelectorAll("#" + CSS.escape(node.id)).length === 1) {
      parts.unshift("#" + CSS.escape(node.id));
      break;
    }
    let part = node.tagName.toLowerCase();
    const parent = node.parentElement;
    if (parent) {
      const same = Array.from(parent.children).filter((c) => c.tagName === node.tagName);
      if (same.length > 1) part += ":nth-of-type(" + (same.indexOf(node) + 1) + ")";
    }
    parts.unshift(part);
    node = parent;
  }
  return parts.join(" > ");
}

let picker = null;
function stopPicking(report) {
  if (!picker) return;
  picker.host.remove();
  document.removeEventListener("click", picker.onClick, true);
  document.removeEventListener("keydown", picker.onKey, true);
  picker = null;
  if (report) browser.runtime.sendMessage({ type: "ended" }).catch(() => {});
}

function startPicking(persist) {
  if (document.visibilityState !== "visible" || picker) return;
  const host = document.createElement("div");
  host.style.cssText = "all:initial;position:fixed;inset:0;z-index:2147483647;pointer-events:none";
  const root = host.attachShadow({ mode: "closed" });
  root.innerHTML =
    '<style>.box{position:fixed;border:2px solid #7aa7ff;background:rgba(122,167,255,.22);border-radius:4px;display:none}' +
    '.bar{position:fixed;left:12px;right:12px;bottom:84px;display:flex;gap:8px;align-items:center;flex-wrap:wrap;background:#1c1d21;color:#f2f2f2;' +
    'font:14px system-ui,sans-serif;padding:10px 12px;border-radius:18px;pointer-events:auto}' +
    '.msg{flex:1 1 100%}button{font:600 14px system-ui;color:#f2f2f2;background:#34363c;border:0;border-radius:14px;padding:10px 14px;min-height:44px}' +
    'button.go{background:#e9e6df;color:#16171a}button[disabled]{opacity:.4}</style>' +
    '<div class="box"></div><div class="bar"><div class="msg">Tap what you want to hide.</div>' +
    '<button class="go" disabled>Hide</button><button class="up" disabled>Bigger</button><button class="no">Cancel</button></div>';
  document.documentElement.appendChild(host);
  const box = root.querySelector(".box");
  const msg = root.querySelector(".msg");
  const go = root.querySelector(".go");
  const up = root.querySelector(".up");
  const no = root.querySelector(".no");
  let target = null;

  function show(el) {
    target = el;
    const r = el.getBoundingClientRect();
    box.style.cssText = "display:block;left:" + r.left + "px;top:" + r.top + "px;width:" + r.width + "px;height:" + r.height + "px";
    msg.textContent = "Hide this " + el.tagName.toLowerCase() + "? Bigger picks the one around it.";
    go.disabled = false;
    up.disabled = !(el.parentElement && el.parentElement !== document.body && el.parentElement !== document.documentElement);
  }
  function onClick(e) {
    const path = e.composedPath ? e.composedPath() : [];
    if (path.includes(host)) return;
    e.preventDefault();
    e.stopPropagation();
    const t = e.target;
    if (t && t.nodeType === 1 && t !== document.body && t !== document.documentElement) show(t);
  }
  function onKey(e) {
    if (e.key === "Escape") { e.preventDefault(); stopPicking(true); }
  }
  go.addEventListener("click", () => {
    if (!target) return;
    const sel = selectorFor(target);
    if (sel) {
      applyRules(hidden.concat([sel]));
      browser.runtime.sendMessage({ type: "zapped", host: HOST, selector: sel, persist }).catch(() => {});
    }
    stopPicking(true);
  });
  up.addEventListener("click", () => { if (target && target.parentElement) show(target.parentElement); });
  no.addEventListener("click", () => stopPicking(true));
  picker = { host, onClick, onKey };
  document.addEventListener("click", onClick, true);
  document.addEventListener("keydown", onKey, true);
}

browser.runtime.sendMessage({ type: "get" }).then((r) => applyRules((r && r.rules && r.rules[HOST]) || [])).catch(() => {});
browser.runtime.onMessage.addListener((msg) => {
  if (!msg) return;
  if (msg.type === "rules") applyRules((msg.rules && msg.rules[HOST]) || []);
  else if (msg.type === "pick") startPicking(msg.persist);
  else if (msg.type === "cancel") stopPicking(false);
});
