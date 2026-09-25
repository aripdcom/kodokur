#!/usr/bin/env python3
"""site/privacy.html'i uygulamanın bütün dillerinde üretir.

Gizlilik politikası tek adreste durur (Play bir URL ister), her dil kendi
bölümünde. Metinler tools/privacy/<dil>.json dosyalarında; üretilen sayfa
depoda, `tools/check_site.py` ikisinin ayrışmadığını denetler.

JSON alanları: name, title, meta, short_label, short, sections ([[başlık, metin], …]),
contact, contact_intro. Metinler HTML'dir (<code> geçebilir), kaçışlanmaz.

Kullanım: python3 tools/gen_privacy.py
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "site", "privacy.html")
TEXTS = os.path.join(ROOT, "tools", "privacy")
APP_LOCALE = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd",
                          "kodokur", "platform", "AppLocale.kt")
MAIL = "kodokur@aripd.com"
ISSUES = "https://github.com/aripdcom/kodokur/issues/new"
RTL = {"ar"}
FIELDS = ["name", "title", "meta", "short_label", "short", "sections", "contact", "contact_intro"]

# Uygulama simgesi: koyu yeşil zemin, beyaz çizgiler, kehribar tarama çizgisi.
ICON = ("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E"
        "%3Crect width='32' height='32' rx='7' fill='%230F3D3E'/%3E"
        "%3Cpath fill='%23E8F3F1' d='M8 9h2v14H8zM11.5 9h1v14h-1zM14 9h2v14h-2zM17.5 9h1v14h-1zM20 9h2.5v14H20zM24 9h1v14h-1z'/%3E"
        "%3Crect x='6' y='15' width='20' height='2' fill='%23FFB347'/%3E%3C/svg%3E")

STYLE = """  :root {
    color-scheme: light dark;
    --bg: #F4F8F7; --surface: #FFFFFF; --surface-2: #E3EEEC; --text: #10201F; --muted: #52625F; --title: #0F5F5C;
    --card-border: rgba(16, 32, 31, 0.08);
  }
  @media (prefers-color-scheme: dark) {
    :root { --bg: #101414; --surface: #182020; --surface-2: #213030; --text: #E3EEEC; --muted: #A5B6B3; --title: #8ED4CC; --card-border: rgba(255, 255, 255, 0.06); }
  }
  * { box-sizing: border-box; margin: 0; }
  body { background: var(--bg); color: var(--text); font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; line-height: 1.6; }
  .wrap { max-width: 720px; margin: 0 auto; padding: 40px 16px 64px; }
  a { color: var(--title); }
  .top { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
  .back { text-decoration: none; font-weight: 700; }
  .picker { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--muted); }
  .picker select { font: inherit; color: var(--text); background: var(--surface); border: 1px solid var(--card-border); border-radius: 8px; padding: 6px 10px; }
  h1 { font-size: 34px; font-weight: 800; line-height: 1.15; }
  h3 { font-size: 18px; margin-top: 22px; }
  .meta { color: var(--muted); font-size: 14px; margin-top: 6px; }
  p, li { margin-top: 10px; }
  code { background: var(--surface-2); border-radius: 5px; padding: 1px 5px; font-size: 0.92em; }
  .summary {
    margin-top: 18px; background: var(--surface); border: 1px solid var(--card-border); border-radius: 16px; padding: 16px 18px;
  }
  .summary strong { color: var(--title); }
  /* JavaScript kapalıyken bölümler alt alta; açıkken yalnız biri görünür. */
  section + section { margin-top: 44px; padding-top: 12px; border-top: 1px solid var(--card-border); }
  section[hidden] { display: none; }
  html.one section { margin-top: 0; padding-top: 0; border-top: 0; }
  section[dir="rtl"] { text-align: right; }
  footer { margin-top: 48px; color: var(--muted); font-size: 14px; display: flex; flex-wrap: wrap; gap: 6px 18px; }
  footer a { text-decoration: none; }"""


def app_tags():
    """AppLocale.TAGS: uygulamanın dilleri, kaynak sırasıyla."""
    src = open(APP_LOCALE, encoding="utf-8").read()
    block = re.search(r"val TAGS: List<String> = listOf\((.*?)\)", src, re.S)
    return re.findall(r'"([^"]+)"', block.group(1))


def load():
    policy = {}
    for name in sorted(os.listdir(TEXTS)):
        if not name.endswith(".json"):
            continue
        tag = name[:-5]
        data = json.load(open(os.path.join(TEXTS, name), encoding="utf-8"))
        missing = [f for f in FIELDS if f not in data]
        if missing:
            sys.exit(f"{name}: eksik alan: {', '.join(missing)}")
        policy[tag] = data
    return policy


def section(tag, policy):
    p = policy[tag]
    rtl = ' dir="rtl"' if tag in RTL else ""
    # data-lang: site.js yalnız seçilen dilin bölümünü gösterir; id, uygulamanın
    # açtığı privacy.html#tr gibi bağlantılar için.
    out = [f'  <section id="{tag}" lang="{tag}" data-lang="{tag}" data-title="Kodokur · {p["title"]}"{rtl}>',
           f'    <h1>{p["title"]}</h1>',
           f'    <p class="meta">{p["meta"]}</p>',
           '    <div class="summary">',
           f'      <strong>{p["short_label"]}</strong> {p["short"]}',
           '    </div>']
    for heading, body in p["sections"]:
        out.append(f'    <h3>{heading}</h3>')
        out.append(f'    <p>{body}</p>')
    out.append(f'    <h3>{p["contact"]}</h3>')
    out.append(f'    <p>{p["contact_intro"]} <a href="mailto:{MAIL}">{MAIL}</a> · '
               f'<a href="{ISSUES}">github.com/aripdcom/kodokur/issues</a></p>')
    out.append('  </section>')
    return "\n".join(out)


def page(tags, policy):
    body = "\n".join(section(t, policy) for t in tags)
    return f"""<!doctype html>
<!--
  ÜRETİLMİŞ DOSYA: tools/privacy/<dil>.json → python3 tools/gen_privacy.py
  Her dil kendi bölümünde; assets/site.js seçilen dilinkini gösterir (index.html
  ile aynı seçim: ?lang=xx, #xx, önceki seçim, tarayıcının dili). JavaScript
  kapalıysa bütün bölümler alt alta okunur.
-->
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#F4F8F7" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#101414" media="(prefers-color-scheme: dark)">
<title>Kodokur · Privacy policy</title>
<meta name="description" content="Kodokur privacy policy in {len(tags)} languages: the camera is the only permission, frames never leave the phone, no internet permission, no data collected.">
<link rel="canonical" href="https://kodokur.aripd.com/privacy.html">
<link rel="icon" href="{ICON}">
<style>
{STYLE}
</style>
</head>
<body>
<div class="wrap">
  <div class="top">
    <a class="back" href="./" data-keep-lang="./">← Kodokur</a>
    <form class="picker" id="picker" hidden>
      <label for="lang" data-i18n="langLabel">Language</label>
      <select id="lang" name="lang"></select>
    </form>
  </div>

{body}

  <footer>
    <span>Kodokur · no ads, no trackers, camera only</span>
    <a href="https://github.com/aripdcom/kodokur">GitHub</a>
  </footer>
</div>
<script src="assets/i18n.js"></script>
<script src="assets/site.js"></script>
</body>
</html>
"""


def render():
    tags = app_tags()
    policy = load()
    missing = [t for t in tags if t not in policy]
    if missing:
        sys.exit(f"politika metni olmayan dil: {', '.join(missing)}")
    extra = [t for t in policy if t not in tags]
    if extra:
        sys.exit(f"AppLocale.TAGS içinde olmayan dil: {', '.join(extra)}")
    return tags, page(tags, policy)


def main():
    tags, html = render()
    open(OUT, "w", encoding="utf-8").write(html)
    print(f"{OUT}: {len(tags)} dil ({' '.join(tags)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
