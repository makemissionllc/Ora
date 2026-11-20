// Holds the tweaks Ora sends ({ "site": { dark, wide, font } }) and passes them to every page.
let tweaks = {};
let port = browser.runtime.connectNative("ora");
port.onMessage.addListener((msg) => {
  if (!msg || msg.type !== "tweaks") return;
  tweaks = msg.tweaks || {};
  browser.tabs.query({}).then((tabs) => {
    for (const tab of tabs) browser.tabs.sendMessage(tab.id, { type: "tweaks", tweaks }).catch(() => {});
  });
});
port.onDisconnect.addListener(() => { port = null; });
browser.runtime.onMessage.addListener((msg) => {
  if (msg && msg.type === "get") return Promise.resolve({ tweaks });
});
