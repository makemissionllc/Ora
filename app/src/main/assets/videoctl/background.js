// Relays between the page's content script and Ora's native app over one native port.
let port = null;
let activeTab = null;

function nativePort() {
  if (!port) {
    port = browser.runtime.connectNative("ora");
    port.onMessage.addListener((msg) => {
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
  if (!msg || msg.type !== "state" || !sender.tab) return;
  activeTab = msg.has ? sender.tab.id : null;
  nativePort().postMessage(msg);
});
