// Runs in every frame of a page. Reports the video that is full screen in this frame and carries out
// Ora's player commands (seek, play, pause, speed, mute, fit). Frames matter because many sites play
// their video inside an iframe, which has its own full-screen element.
(() => {
  let wasActive = false;
  // True when Ora draws its own controls. Otherwise the site's own controls stay, and nothing is hidden.
  let oraControls = false;
  let timer = null;
  let lastPoke = 0;

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
      ended: v.ended,
      rate: v.playbackRate,
      muted: v.muted,
      w: v.videoWidth,
      h: v.videoHeight,
      title: (window.top === window ? document.title : "") || "",
    };
  }

  // Ora draws its own player controls over a full-screen video, so the site's own controls (title bar, seek bar,
  // buttons) are hidden to stop the two sets drawing on top of each other. Only the video's ancestors and the
  // siblings that hold subtitles stay visible. Everything hidden is marked, so leaving full screen puts it back.
  const CAPTIONS = /caption-window|captions?-(?:text|container|layer)|subtitle(?:s)?-(?:container|layer|text)|text-?track|cue-?(?:container|layer)/i;
  // Only a sibling that is itself the captions layer stays; a controls bar that merely holds a captions button does not.
  function keepsCaptions(node) {
    const cls = typeof node.className === "string" ? node.className : "";
    return CAPTIONS.test(cls) || CAPTIONS.test(node.id || "");
  }
  function hideSiteControls() {
    const v = fullScreenVideo();
    const root = document.fullscreenElement;
    if (!v || !root || v === root) return;
    for (let node = v; node && node !== root; node = node.parentElement) {
      const parent = node.parentElement;
      if (!parent) break;
      for (const sibling of parent.children) {
        if (sibling === node || sibling.hasAttribute("data-ora-hid") || sibling.tagName === "STYLE" || sibling.tagName === "SCRIPT") continue;
        if (keepsCaptions(sibling)) continue;
        sibling.setAttribute("data-ora-hid", sibling.style.getPropertyValue("visibility"));
        sibling.style.setProperty("visibility", "hidden", "important");
      }
    }
  }
  function showSiteControls() {
    for (const node of document.querySelectorAll("[data-ora-hid]")) {
      const before = node.getAttribute("data-ora-hid");
      if (before) node.style.setProperty("visibility", before); else node.style.removeProperty("visibility");
      node.removeAttribute("data-ora-hid");
    }
  }

  // A touch on the page tells Ora to show its small controls handle for a moment. Capture phase, never stopped.
  function poke() {
    const now = Date.now();
    if (now - lastPoke < 400) return;
    lastPoke = now;
    browser.runtime.sendMessage({ type: "poke" }).catch(() => {});
  }

  function report() {
    const s = snapshot();
    if (s.has && oraControls) hideSiteControls(); else showSiteControls();
    if (s.has || wasActive) {
      wasActive = s.has;
      browser.runtime.sendMessage(s).then((reply) => {
        if (reply && typeof reply.ora === "boolean") oraControls = reply.ora;
      }).catch(() => {});
    }
    if (!s.has) stop();
  }

  // The report loop runs only while something in this frame is full screen, so ordinary pages pay nothing for it.
  function start() {
    if (timer !== null) return;
    document.addEventListener("touchstart", poke, true);
    timer = setInterval(report, 250);
    report();
  }
  function stop() {
    if (timer === null) return;
    clearInterval(timer);
    timer = null;
    document.removeEventListener("touchstart", poke, true);
  }
  document.addEventListener("fullscreenchange", () => {
    if (document.fullscreenElement) start(); else { report(); stop(); }
  });
  if (document.fullscreenElement) start();

  browser.runtime.onMessage.addListener((msg) => {
    const v = fullScreenVideo();
    if (!msg || !v) return;
    switch (msg.type) {
      case "seek":
        if (!Number.isFinite(msg.time)) return;
        v.currentTime = Math.max(0, Math.min(msg.time, Number.isFinite(v.duration) ? v.duration : msg.time));
        break;
      case "play":
        if (v.ended) v.currentTime = 0;
        v.play().catch(() => {});
        break;
      case "pause":
        v.pause();
        break;
      case "rate":
        if (Number.isFinite(msg.value) && msg.value > 0) v.playbackRate = msg.value;
        break;
      case "mute":
        v.muted = !!msg.value;
        break;
      case "exit":
        document.exitFullscreen().catch(() => {});
        break;
      case "fit":
        v.style.setProperty("object-fit", msg.value === "cover" ? "cover" : "contain", "important");
        break;
    }
  });
})();
