// Relays between the page's content script and Ora's native app over one native port.
let port = null;
let activeTab = null;
// Whether Ora's own controls are on, as the app last said. Content scripts read it with every report.
let oraControls = false;

function nativePort() {
  if (!port) {
    port = browser.runtime.connectNative("ora");
    port.onMessage.addListener((msg) => {
      if (msg && msg.type === "ui") { oraControls = !!msg.ora; return; }
      if (msg && msg.type !== "state" && activeTab !== null) {
        browser.tabs.sendMessage(activeTab, msg).catch(() => {});
      }
    });
    port.onDisconnect.addListener(() => {
      port = null;
    });
  }
  return port;
}

browser.runtime.onMessage.addListener((msg, sender) => {
  if (!msg || !sender.tab) return;
  if (msg.type === "poke") { nativePort().postMessage(msg); return; }
  if (msg.type !== "state") return;
  activeTab = msg.has ? sender.tab.id : null;
  nativePort().postMessage(msg);
  return Promise.resolve({ ora: oraControls });
});
