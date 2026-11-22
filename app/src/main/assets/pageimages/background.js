// Relays between the page's content script and Ora's native app. Each page message carries its tab id,
// and Ora's commands name the tab they are for.
let port = null;

function nativePort() {
  if (!port) {
    port = browser.runtime.connectNative("ora");
    port.onMessage.addListener((msg) => {
      if (msg && Number.isInteger(msg.tab)) browser.tabs.sendMessage(msg.tab, msg).catch(() => {});
    });
    port.onDisconnect.addListener(() => {
      port = null;
    });
  }
  return port;
}

browser.runtime.onMessage.addListener((msg, sender) => {
  if (!msg || !sender.tab) return;
  nativePort().postMessage(Object.assign({ tab: sender.tab.id }, msg));
});
