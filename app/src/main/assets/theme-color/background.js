// Passes each page's theme colour from the content script to the app.
browser.runtime.onMessage.addListener((message) => {
  browser.runtime.sendNativeMessage("browser", message).catch(() => {});
});
