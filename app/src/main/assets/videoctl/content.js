// Runs in the page. Reports the time of the video that is full screen on this page
// and seeks it when Ora's seek bar asks. Only the page's own full-screen video counts.
(() => {
  let wasActive = false;

  function fullScreenVideo() {
    const el = document.fullscreenElement;
    if (!el) return null;
    return el.tagName === "VIDEO" ? el : el.querySelector("video");
  }

  function snapshot() {
    const v = fullScreenVideo();
    if (!v) return { type: "state", has: false };
    return {
      type: "state",
      has: true,
      t: v.currentTime,
      d: Number.isFinite(v.duration) ? v.duration : 0,
      paused: v.paused,
    };
  }

  // Send while a full-screen video exists, and once more when it leaves full screen.
  setInterval(() => {
    const s = snapshot();
    if (s.has || wasActive) {
      wasActive = s.has;
      browser.runtime.sendMessage(s).catch(() => {});
    }
  }, 250);

  browser.runtime.onMessage.addListener((msg) => {
    if (!msg || msg.type !== "seek" || !Number.isFinite(msg.time)) return;
    const v = fullScreenVideo();
    if (!v) return;
    const end = Number.isFinite(v.duration) ? v.duration : msg.time;
    v.currentTime = Math.max(0, Math.min(msg.time, end));
  });
})();
