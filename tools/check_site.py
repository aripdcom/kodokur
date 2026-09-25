#!/usr/bin/env python3
"""Site denetimi. Depo kökünden koşar, CI her itmede çağırır.

  1. site/gizlilik.html, tools/gen_privacy.py çıktısıyla birebir aynı
     (üretilen dosya elle düzenlenince sessizce ayrışırdı)
  2. AppLocale.TAGS içindeki her dilin kendi bölümü ve iletişim adresi var
  3. Uygulamadaki site ve kaynak bağlantıları sitenin adresleriyle aynı
  4. site/index.html gizlilik sayfasına bağlanıyor
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_privacy  # noqa: E402

ROOT = gen_privacy.ROOT
ACTIONS = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd", "kodokur",
                       "platform", "Actions.kt")
INDEX = os.path.join(ROOT, "site", "index.html")
SITE = "https://aripdcom.github.io/kodokur"
SOURCE = "https://github.com/aripdcom/kodokur"

errors = []


def main():
    tags, html = gen_privacy.render()

    current = open(gen_privacy.OUT, encoding="utf-8").read() if os.path.exists(gen_privacy.OUT) else ""
    if current != html:
        errors.append("site/gizlilik.html üreticiyle ayrışmış: python3 tools/gen_privacy.py koşturun")

    for tag in tags:
        if f'<section id="{tag}" lang="{tag}"' not in html:
            errors.append(f"gizlilik sayfasında {tag} bölümü yok")
    if html.count(f"mailto:{gen_privacy.MAIL}") != len(tags):
        errors.append("her dil bölümünde iletişim adresi yok")

    actions = open(ACTIONS, encoding="utf-8").read()
    for name, want in (("SOURCE_URL", SOURCE), ("PRIVACY_URL", f"{SITE}/gizlilik.html")):
        m = re.search(rf'const val {name} = "([^"]+)"', actions)
        if not m:
            errors.append(f"Actions.{name} bulunamadı")
        elif m.group(1) != want:
            errors.append(f"Actions.{name} = {m.group(1)}, beklenen {want}")

    index = open(INDEX, encoding="utf-8").read()
    if 'href="gizlilik.html"' not in index:
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
