// Holds the font Ora chose ("" means off) and tells every page when it changes.
let current = "";
let port = browser.runtime.connectNative("ora");
port.onMessage.addListener((msg) => {
  if (!msg || msg.type !== "font") return;
  current = msg.key || "";
  browser.tabs.query({}).then((tabs) => {
    for (const tab of tabs) browser.tabs.sendMessage(tab.id, { type: "font", key: current }).catch(() => {});
  });
});
port.onDisconnect.addListener(() => { port = null; });
browser.runtime.onMessage.addListener((msg) => {
  if (msg && msg.type === "get") return Promise.resolve({ key: current });
});
