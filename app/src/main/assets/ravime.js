/* Ravime DOM adapter: presentation-only. Does not intercept inputs, navigation, account credentials, media or requests. */
(function () {
  "use strict";
  if (window.__ravimeThemeBridge) {
    const existing = window.__ravimeThemeBridge;
    if (!existing.enabled) {
      existing.enabled = true;
      if (existing.observer && document.body) {
        existing.observer.observe(document.body,{childList:true,subtree:true});
      }
    }
    existing.refresh();
    return;
  }
  const bridge = {
    route: "",
    enabled: true,
    running: false,
    refresh() {
      if (!this.enabled) return;
      const root = document.documentElement;
      if (!root || !document.body) return;
      root.classList.add("rv-theme");
      const path = location.pathname;
      if (this.route !== path) {
        root.classList.remove("rv-route-home","rv-route-search","rv-route-anime","rv-route-watch","rv-route-profile","rv-route-community","rv-route-schedule","rv-route-watch2gether");
        let route = "home";
        if (path.startsWith("/anime/")) route = "anime";
        else if (path.startsWith("/watch/")) route = "watch";
        else if (path.startsWith("/search")) route = "search";
        else if (path.startsWith("/profile") || path.startsWith("/si/")) route = "profile";
        else if (path.startsWith("/community")) route = "community";
        else if (path.startsWith("/schedule")) route = "schedule";
        else if (path.startsWith("/watch2gether")) route = "watch2gether";
        root.classList.add("rv-route-" + route);
        this.route = path;
      }
      const header = document.querySelector("body header");
      if (header && !header.classList.contains("rv-site-chrome")) {
        const sample = (header.textContent || "").slice(0,600).toLowerCase();
        if (sample.includes("sign in") || (sample.includes("en") && sample.includes("jp"))) {
          header.classList.add("rv-site-chrome");
        }
      }
      // Site's small desktop floating navigation duplicates the native bottom dock.
      document.querySelectorAll("nav,[role=navigation]").forEach((nav) => {
        if (nav.classList.contains("rv-site-chrome")) return;
        const text = (nav.textContent || "").replace(/\s+/g," ").toLowerCase();
        if (text.includes("schedule") && text.includes("community") && text.includes("settings") && text.includes("search")) {
          nav.classList.add("rv-site-chrome");
        }
      });
      // Card detection is conservative: only card roots holding anime imagery.
      document.querySelectorAll('a[href*="/anime/"]').forEach((a) => {
        if (a.classList.contains("rv-anime-tile") || !a.querySelector("img")) return;
        a.classList.add("rv-anime-tile");
        const label = [...a.querySelectorAll("h2,h3,h4,p,span")].find(el => {
          const t = (el.textContent || "").trim();
          return t.length > 3 && t.length < 160 && el.children.length === 0;
        });
        if (label) label.classList.add("rv-anime-label");
      });
      document.querySelectorAll('a[href*="/watch/"]').forEach((a) => {
        if (!a.classList.contains("rv-episode-tile")) a.classList.add("rv-episode-tile");
      });
      document.querySelectorAll("main h1").forEach((h) => h.classList.add("rv-main-title"));
    },
    teardown() {
      this.enabled = false;
      document.documentElement.classList.remove("rv-theme");
      if (this.observer) this.observer.disconnect();
      document.querySelectorAll(".rv-site-chrome").forEach(el => el.classList.remove("rv-site-chrome"));
    }
  };
  window.__ravimeThemeBridge = bridge;
  const schedule = () => {
    if (bridge.running || !bridge.enabled) return;
    bridge.running = true;
    requestAnimationFrame(() => {
      bridge.running = false;
      try { bridge.refresh(); } catch (e) { console.debug("Ravime theme refresh failed"); }
    });
  };
  const start = () => {
    bridge.refresh();
    bridge.observer = new MutationObserver(schedule);
    bridge.observer.observe(document.body,{childList:true,subtree:true});
  };
  if (document.body) start();
  else document.addEventListener("DOMContentLoaded",start,{once:true});
  window.addEventListener("popstate",schedule);
  window.addEventListener("hashchange",schedule);
  for (const key of ["pushState","replaceState"]) {
    const original = history[key];
    if (typeof original === "function") {
      history[key] = function (...args) {
        const result = original.apply(this,args);
        schedule();
        return result;
      };
    }
  }
})();
