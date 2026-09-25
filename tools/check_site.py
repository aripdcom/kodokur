#!/usr/bin/env python3
"""Site denetimi. Depo kökünden koşar, CI her itmede çağırır.

  1. site/privacy.html, tools/gen_privacy.py çıktısıyla birebir aynı
     (üretilen dosya elle düzenlenince sessizce ayrışırdı)
  2. AppLocale.TAGS içindeki her dilin gizlilik bölümü ve iletişim adresi var
  3. site/assets/i18n.js, tools/gen_site_i18n.py çıktısıyla aynı; uygulamanın
     her dili (İngilizce dışında) tanıtım sayfasında da var, anahtar eksiği yok
  4. Uygulamadaki site ve kaynak bağlantıları sitenin adresleriyle aynı
  5. site/index.html gizlilik sayfasına bağlanıyor
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_privacy  # noqa: E402
import gen_site_i18n  # noqa: E402

ROOT = gen_privacy.ROOT
ACTIONS = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd", "kodokur",
                       "platform", "Actions.kt")
INDEX = os.path.join(ROOT, "site", "index.html")
SITE = "https://aripdcom.github.io/kodokur"
SOURCE = "https://github.com/aripdcom/kodokur"

errors = []


def generated_matches(path, text, fix):
    current = open(path, encoding="utf-8").read() if os.path.exists(path) else ""
    if current != text:
        errors.append(f"{os.path.relpath(path, ROOT)} üreticiyle ayrışmış: {fix} koşturun")


def main():
    tags, html = gen_privacy.render()
    generated_matches(gen_privacy.OUT, html, "python3 tools/gen_privacy.py")
    for tag in tags:
        if f'<section id="{tag}" lang="{tag}" data-lang="{tag}"' not in html:
            errors.append(f"gizlilik sayfasında {tag} bölümü yok")
    if html.count(f"mailto:{gen_privacy.MAIL}") != len(tags):
        errors.append("her dil bölümünde iletişim adresi yok")

    table, site_errors, js = gen_site_i18n.render()
    errors.extend(site_errors)
    generated_matches(gen_site_i18n.OUT, js, "python3 tools/gen_site_i18n.py")
    for tag in tags:
        if tag not in table:
            errors.append(f"tanıtım sayfasında {tag} yok: tools/site/{tag}.json eksik")
    for tag in table:
        if tag not in tags:
            errors.append(f"tools/site/{tag}.json var ama uygulamada bu dil yok")

    actions = open(ACTIONS, encoding="utf-8").read()
    for name, want in (("SOURCE_URL", SOURCE), ("PRIVACY_URL", f"{SITE}/privacy.html")):
        m = re.search(rf'const val {name} = "([^"]+)"', actions)
        if not m:
            errors.append(f"Actions.{name} bulunamadı")
        elif m.group(1) != want:
            errors.append(f"Actions.{name} = {m.group(1)}, beklenen {want}")

    index = open(INDEX, encoding="utf-8").read()
    if 'href="privacy.html"' not in index:
        errors.append("site/index.html gizlilik sayfasına bağlanmıyor")

    for e in errors:
        print(f"HATA   {e}")
    if errors:
        print(f"✗ {len(errors)} hata")
        return 1
    print(f"✓ site tutarlı ({len(tags)} dil)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
