// Runs in the page. Finds the article, tells Ora whether the page can be read, and draws the
// reader over the page when Ora asks. Closing removes the overlay, so the page is still there.
(() => {
  if (window.top !== window) return;
  const send = (msg) => browser.runtime.sendMessage(Object.assign({ url: location.href }, msg)).catch(() => {});
  let overlay = null;

  const SKIP = "nav, aside, footer, header, form, script, style, noscript, [aria-hidden=true], [hidden]";
  const PAGES = {
    stone: { bg: "#E4E3DE", fg: "#24252A", meta: "#5C5B57" },
    paper: { bg: "#F6F5F2", fg: "#24252A", meta: "#5C5B57" },
    basalt: { bg: "#1A1B1E", fg: "#ECE6DC", meta: "#A29D94" },
  };

  function words(text) {
    return (text.trim().match(/\S+/g) || []).length;
  }

  function usable(el) {
    return !el.closest(SKIP);
  }

  // The container whose paragraphs hold the most text is the article.
  function findRoot() {
    const scores = new Map();
    for (const p of document.querySelectorAll("p")) {
      const len = p.textContent.trim().length;
      if (len < 60 || !usable(p)) continue;
      const parent = p.parentElement;
      if (!parent) continue;
      scores.set(parent, (scores.get(parent) || 0) + len);
      const grand = parent.parentElement;
      if (grand) scores.set(grand, (scores.get(grand) || 0) + len / 2);
    }
    let best = null;
    let top = 0;
    for (const [el, score] of scores) {
      if (score > top) { top = score; best = el; }
    }
    const article = document.querySelector("article");
    if (article && usable(article) && scores.has(article)) return article;
    // Climb while the parent holds only a little more paragraph text, so a lead that sits beside the
    // best container is not left out.
    const paragraphText = (el) => {
      let n = 0;
      for (const p of el.querySelectorAll("p")) if (usable(p)) n += p.textContent.trim().length;
      return n;
    };
    while (best && best.parentElement && best.parentElement !== document.body && best.parentElement !== document.documentElement) {
      if (paragraphText(best.parentElement) > paragraphText(best) * 2.5) break;
      best = best.parentElement;
    }
    // A page's own main region is the better root when it holds the best container and not much besides.
    const main = document.querySelector("article, main, [role=main]");
    if (best && main && main !== best && main.contains(best) && usable(main) && paragraphText(main) <= paragraphText(best) * 4) {
      return main;
    }
    return best;
  }

  function extract() {
    const root = findRoot();
    if (!root) return null;
    const blocks = [];
    let count = 0;
    for (const el of root.querySelectorAll("h2, h3, h4, p, blockquote, li, pre, img")) {
      if (!usable(el)) continue;
      if (el.tagName === "IMG") {
        const src = el.currentSrc || el.src;
        if (src && (el.naturalWidth || el.width) >= 240) blocks.push({ tag: "img", src, alt: el.alt || "" });
        continue;
      }
      if (el.tagName === "P" || el.tagName === "BLOCKQUOTE" || el.tagName === "LI") {
        if (el.tagName === "LI" ? el.closest("blockquote") : el.querySelector("p")) continue;
      }
      const text = el.textContent.replace(/\s+/g, " ").trim();
      if (!text) continue;
      if (el.tagName === "P" && text.length < 25) continue;
      if (el.tagName === "LI" && text.length < 40) continue;
      count += words(text);
      blocks.push({ tag: el.tagName.toLowerCase(), text });
    }
    if (count < 120 || blocks.filter((b) => b.tag === "p").length < 3) return null;
    const meta = (name) => {
      const m = document.querySelector('meta[property="' + name + '"], meta[name="' + name + '"]');
      return m ? m.content : "";
    };
    const h1 = document.querySelector("h1");
    const title = (h1 && h1.textContent.trim()) || meta("og:title") || document.title;
    const site = meta("og:site_name") || location.hostname.replace(/^www\./, "");
    return { title, site, minutes: Math.max(1, Math.round(count / 220)), blocks };
  }

  function style(opts) {
    const page = PAGES[opts.page] || PAGES.stone;
    const family = opts.serif
      ? "Literata, Georgia, 'Noto Serif', serif"
      : "'Instrument Sans', system-ui, sans-serif";
    return `
      :host { all: initial; }
      .wrap { position: fixed; inset: 0; z-index: 2147483647; overflow-y: auto; background: ${page.bg}; color: ${page.fg}; -webkit-text-size-adjust: none; }
      article { box-sizing: border-box; max-width: 680px; margin: 0 auto; padding: 72px 22px 160px; font-family: ${family}; font-size: ${opts.size}px; line-height: 1.7; }
      .meta { font-family: 'Instrument Sans', system-ui, sans-serif; font-size: 15px; color: ${page.meta}; margin: 0 0 10px; }
      h1 { font-size: 2em; line-height: 1.15; font-weight: 600; letter-spacing: -0.5px; margin: 0 0 22px; }
      h2, h3, h4 { line-height: 1.25; margin: 1.4em 0 .5em; }
      p, li { margin: 0 0 .9em; }
      blockquote { margin: 0 0 .9em; padding-left: 16px; border-left: 3px solid ${page.meta}; }
      pre { overflow-x: auto; font-size: .8em; }
      img { max-width: 100%; height: auto; border-radius: 12px; margin: .4em 0 .9em; }
    `;
  }

  function render(data, opts) {
    close();
    overlay = document.createElement("div");
    const root = overlay.attachShadow({ mode: "closed" });
    const css = document.createElement("style");
    css.textContent = style(opts);
    const wrap = document.createElement("div");
    wrap.className = "wrap";
    const article = document.createElement("article");
    const meta = document.createElement("p");
    meta.className = "meta";
    meta.textContent = data.site + ", " + data.minutes + " min read";
    const h1 = document.createElement("h1");
    h1.textContent = data.title;
    article.append(meta, h1);
    for (const b of data.blocks) {
      if (b.tag === "img") {
        const img = document.createElement("img");
        img.src = b.src;
        img.alt = b.alt;
        article.append(img);
      } else {
        const node = document.createElement(b.tag === "li" ? "p" : b.tag);
        node.textContent = (b.tag === "li" ? "• " : "") + b.text;
        article.append(node);
      }
    }
    wrap.append(article);
    root.append(css, wrap);
    overlay.__css = css;
    document.documentElement.append(overlay);
  }

  function close() {
    if (overlay) overlay.remove();
    overlay = null;
  }

  browser.runtime.onMessage.addListener((msg) => {
    if (!msg) return;
    if (msg.type === "open") {
      const data = extract();
      if (!data) {
        send({ type: "opened", ok: false });
        return;
      }
      render(data, msg);
      send({ type: "opened", ok: true, title: data.title, site: data.site, minutes: data.minutes });
    } else if (msg.type === "style" && overlay) {
      overlay.__css.textContent = style(msg);
    } else if (msg.type === "close") {
      close();
    }
  });

  // One report per page: whether it has an article, so the menu can say so.
  let ready = null;
  try {
    ready = extract();
  } catch (e) {
    send({ type: "error", message: String(e) });
  }
  send({
    type: "ready",
    open: !!overlay,
    readable: !!ready,
    title: ready ? ready.title : "",
    site: ready ? ready.site : "",
    minutes: ready ? ready.minutes : 0,
  });
})();
