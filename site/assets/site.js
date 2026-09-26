/*
  Page language (Kodokur's adaptation of the site.js in Kerteriz).
  ---------------------------------------------------------------------------
  Order:
      1. ?lang=xx or #xx in the address (the app opens the privacy link with #tr)
      2. an earlier choice          (localStorage)
      3. navigator.languages        (the browser's, i.e. the system's, language)
      4. English

  Two kinds of page use the same script:
  - index.html: texts live in [data-i18n] elements. English is in the HTML
    itself and is snapshotted on first load; translations come from assets/i18n.js.
  - privacy.html: each language has its own <section data-lang="xx">; the selected
    one is shown and the others hidden. Without JavaScript all of them read.
*/

(function () {
  "use strict";

  var TABLE = window.KODOKUR_I18N || {};
  var DEFAULT = "en";
  var STORE_KEY = "kodokur.lang";
  var ALIAS = { no: "nb", nob: "nb" };
  var RTL = { ar: true };

  var english = null;

  function each(selector, fn) {
    var nodes = document.querySelectorAll(selector);
    for (var i = 0; i < nodes.length; i++) fn(nodes[i]);
  }

  function metaDescription() {
    return document.querySelector('meta[name="description"]');
  }

  function snapshot() {
    var shot = { _title: document.title, _desc: metaDescription() ? metaDescription().content : "" };
    each("[data-i18n]", function (node) {
      var key = node.getAttribute("data-i18n");
      if (!(key in shot)) shot[key] = node.textContent;
    });
    return shot;
  }

  function known(code) {
    if (!code) return null;
    code = String(code).toLowerCase().replace("_", "-");
    if (ALIAS[code]) code = ALIAS[code];
    if (code === DEFAULT || TABLE[code]) return code;
    var base = code.split("-")[0];
    if (ALIAS[base]) base = ALIAS[base];
    if (base === DEFAULT || TABLE[base]) return base;
    return null;
  }

  function stored() {
    try { return known(localStorage.getItem(STORE_KEY)); } catch (e) { return null; }
  }

  function remember(code) {
    try { localStorage.setItem(STORE_KEY, code); } catch (e) { /* private tab */ }
  }

  function fromUrl() {
    var match = /[?&]lang=([\w-]+)/.exec(location.search);
    if (match) return known(decodeURIComponent(match[1]));
    var hash = location.hash.replace("#", "");
    return hash ? known(hash) : null;
  }

  function fromBrowser() {
    var wanted = navigator.languages || [navigator.language];
    for (var i = 0; i < wanted.length; i++) {
      var hit = known(wanted[i]);
      if (hit) return hit;
    }
    return null;
  }

  function text(code, key) {
    var entry = code === DEFAULT ? null : TABLE[code];
    if (entry && typeof entry[key] === "string" && entry[key] !== "") return entry[key];
    return english[key] !== undefined ? english[key] : "";
  }

  function apply(code) {
    var root = document.documentElement;
    root.lang = code;
    root.dir = RTL[code] ? "rtl" : "ltr";

    // Only on the landing page do the page title and description come from the table.
    if (document.body.hasAttribute("data-i18n-page")) {
      document.title = text(code, "_title");
      var desc = metaDescription();
      if (desc) desc.content = text(code, "_desc");
    }
    each("[data-i18n]", function (node) {
      node.textContent = text(code, node.getAttribute("data-i18n"));
    });

    // Privacy page: only the selected language's section is visible, and the title is its own.
    var sections = document.querySelectorAll("[data-lang]");
    if (sections.length) {
      // With a single section left, the dividers between sections are pointless (CSS: html.one).
      root.classList.add("one");
      var shown = document.querySelector('[data-lang="' + code + '"]') ? code : DEFAULT;
      each("[data-lang]", function (node) {
        var on = node.getAttribute("data-lang") === shown;
        node.hidden = !on;
        if (on && node.getAttribute("data-title")) document.title = node.getAttribute("data-title");
      });
    }

    // Carry the same language across links between pages.
    each("a[data-keep-lang]", function (link) {
      link.href = link.getAttribute("data-keep-lang") + "?lang=" + code;
    });

    var select = document.getElementById("lang");
    if (select) select.value = code;
  }

  function name(code) {
    return (TABLE[code] && TABLE[code].name) || code;
  }

  function codes() {
    var list = [DEFAULT];
    for (var code in TABLE) {
      if (Object.prototype.hasOwnProperty.call(TABLE, code) && code !== DEFAULT) list.push(code);
    }
    // Sort by native name: whoever scans the list looks for their own language.
    return list.sort(function (a, b) { return name(a).localeCompare(name(b), "en"); });
  }

  function buildPicker(all, current) {
    var form = document.getElementById("picker");
    var select = document.getElementById("lang");
    if (!form || !select) return;
    all.forEach(function (code) {
      var option = document.createElement("option");
      option.value = code;
      option.textContent = name(code);
      option.lang = code;
      select.appendChild(option);
    });
    select.value = current;
    form.hidden = false;
    select.addEventListener("change", function () { choose(select.value); });
    form.addEventListener("submit", function (event) {
      event.preventDefault();
      choose(select.value);
    });
  }

  function choose(code) {
    code = known(code) || DEFAULT;
    remember(code);
    apply(code);
    // Reflect the choice in the address bar so the link can be shared.
    if (history.replaceState) {
      history.replaceState(null, "", location.pathname + "?lang=" + code);
    }
  }

  function start() {
    english = snapshot();
    var urlChoice = fromUrl();
    var current = urlChoice || stored() || fromBrowser() || DEFAULT;
    if (urlChoice) remember(urlChoice);
    buildPicker(codes(), current);
    apply(current);
    // An anchor like privacy.html#tr makes the browser scroll to that section at
    // the end of loading (even after scrollTo). Since the section is now shown on
    // its own, it should be read from the top: the anchor is turned into a ?lang=
    // address before the scroll happens. Without JavaScript the anchor still jumps
    // to that language as before.
    if (urlChoice && location.hash && history.replaceState) {
      history.replaceState(null, "", location.pathname + "?lang=" + urlChoice);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
