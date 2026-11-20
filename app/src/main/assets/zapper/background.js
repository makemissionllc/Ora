// Keeps the hide rules Ora sends ({ "host": ["css selector", ...] }) and passes them to every page.
// Ora asks for a pick; the page that is on screen answers, and its choice goes back to Ora to be saved.
let rules = {};
let port = browser.runtime.connectNative("ora");
function tellTabs(msg) {
  browser.tabs.query({}).then((tabs) => {
    for (const tab of tabs) browser.tabs.sendMessage(tab.id, msg).catch(() => {});
  });
}
port.onMessage.addListener((msg) => {
  if (!msg) return;
  if (msg.type === "rules") { rules = msg.rules || {}; tellTabs({ type: "rules", rules }); }
  else if (msg.type === "pick") tellTabs({ type: "pick", persist: msg.persist !== false });
  else if (msg.type === "cancel") tellTabs({ type: "cancel" });
});
port.onDisconnect.addListener(() => { port = null; });
browser.runtime.onMessage.addListener((msg) => {
  if (!msg) return;
  if (msg.type === "get") return Promise.resolve({ rules });
  if (msg.type === "zapped" && port) port.postMessage(msg);
  if (msg.type === "ended" && port) port.postMessage(msg);
});
