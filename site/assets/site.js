/*
  Sayfanın dili (Kerteriz'deki site.js'in Kodokur uyarlaması).
  ---------------------------------------------------------------------------
  Sıra:
      1. adresteki ?lang=xx ya da #xx (uygulama gizlilik bağlantısını #tr ile açar)
      2. daha önce yapılmış seçim  (localStorage)
      3. navigator.languages       (tarayıcının, yani sistemin dili)
      4. İngilizce

  İki tür sayfa aynı betiği kullanır:
  - index.html: metinler [data-i18n] öğelerinde. İngilizce HTML'in kendisinde
    durur; ilk yüklemede anlık görüntüsü alınır, çeviri assets/i18n.js'ten gelir.
  - privacy.html: her dilin kendi <section data-lang="xx"> bölümü var; seçilen
    gösterilir, öbürleri gizlenir. JavaScript kapalıysa hepsi okunur.
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
    try { localStorage.setItem(STORE_KEY, code); } catch (e) { /* özel sekme */ }
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

    // Sayfa başlığı ve özet yalnız tanıtım sayfasında tablodan gelir.
    if (document.body.hasAttribute("data-i18n-page")) {
      document.title = text(code, "_title");
      var desc = metaDescription();
      if (desc) desc.content = text(code, "_desc");
    }
    each("[data-i18n]", function (node) {
      node.textContent = text(code, node.getAttribute("data-i18n"));
    });

    // Gizlilik sayfası: yalnız seçilen dilin bölümü görünür, başlık onundur.
    var sections = document.querySelectorAll("[data-lang]");
    if (sections.length) {
      // Tek bölüm kalınca aradaki ayırıcılar anlamsız (CSS: html.one).
      root.classList.add("one");
      var shown = document.querySelector('[data-lang="' + code + '"]') ? code : DEFAULT;
      each("[data-lang]", function (node) {
        var on = node.getAttribute("data-lang") === shown;
        node.hidden = !on;
        if (on && node.getAttribute("data-title")) document.title = node.getAttribute("data-title");
      });
    }

    // Aynı dili sayfalar arası bağlantılarda da taşı.
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
    // Kendi dilindeki adına göre: listeye bakan kendi dilini arar.
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
    // Adres çubuğu seçimi göstersin ki bağlantı paylaşılabilsin.
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
    // privacy.html#tr gibi bir çapa, tarayıcıyı yüklemenin sonunda o bölüme
    // kaydırır (scrollTo'dan da sonra). Bölüm artık tek başına gösterildiği
    // için baştan okunmalı: çapa, kaydırma gerçekleşmeden ?lang= adresine
    // çevrilir. JavaScript kapalıysa çapa eskisi gibi o dile atlar.
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
