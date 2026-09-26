#!/usr/bin/env python3
"""Play listeleme metinlerini denetler (Reyon'daki denetimin Kodokur uyarlaması).

store/play/<dil>/ altında:
  title.txt   ≤ 30 karakter      short.txt  ≤ 80
  full.txt    ≤ 4000             notes-<sürüm>.txt ≤ 500 (sürüm notu)

Denetimler:
  * her dilde üç listeleme dosyası var, boş değil, sınırların içinde
  * dil klasörleri AppLocale.TAGS ile birebir aynı
  * her dilin Play yerel ayar karşılığı biliniyor (Play Console'a kopyalarken
    hangi ayarın seçileceği store/README.md'de)
  * her sürüm notu bütün dillerde var
  * görünmez karakter yok (yumuşak tire, sıfır genişlikli boşluk, BOM): Play
    bunları olduğu gibi yayımlar, listelemede bozuk kelime olarak görünür
"""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAY = os.path.join(ROOT, "store", "play")
LIMITS = {"title.txt": 30, "short.txt": 80, "full.txt": 4000}
NOTE_LIMIT = 500

# Depodaki dil kodu → Play Console yerel ayarı.
PLAY_LOCALES = {
    "en": "en-US", "tr": "tr-TR", "de": "de-DE", "fr": "fr-FR", "nl": "nl-NL",
    "es": "es-ES", "pt": "pt-BR", "it": "it-IT", "da": "da-DK", "sv": "sv-SE",
    "nb": "no-NO", "fi": "fi-FI", "ru": "ru-RU", "ar": "ar",
}
INVISIBLE = {0x00AD: "yumuşak tire", 0x200B: "sıfır genişlikli boşluk", 0xFEFF: "BOM"}

errors = []


def app_languages():
    path = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd", "kodokur",
                        "platform", "AppLocale.kt")
    source = open(path, encoding="utf-8").read()
    block = re.search(r"val TAGS: List<String> = listOf\((.*?)\)", source, re.S)
    return re.findall(r'"([a-z-]+)"', block.group(1)) if block else []


def check_text(where, text, limit):
    if not text:
        errors.append(f"{where} boş")
        return
    if len(text) > limit:
        errors.append(f"{where}: {len(text)} karakter, sınır {limit}")
    for ch in set(text):
        if ord(ch) in INVISIBLE:
            errors.append(f"{where}: görünmez karakter U+{ord(ch):04X} ({INVISIBLE[ord(ch)]})")


def main():
    langs = sorted(d for d in os.listdir(PLAY) if os.path.isdir(os.path.join(PLAY, d)))
    versions = sorted({os.path.basename(p)[len("notes-"):-len(".txt")]
                       for p in glob.glob(os.path.join(PLAY, "*", "notes-*.txt"))})
    for lang in langs:
        sizes = []
        for name, limit in LIMITS.items():
            path = os.path.join(PLAY, lang, name)
            if not os.path.exists(path):
                errors.append(f"{lang}/{name} eksik")
                continue
            text = open(path, encoding="utf-8").read().strip()
            check_text(f"{lang}/{name}", text, limit)
            sizes.append(f"{name[:-4]} {len(text)}/{limit}")
        for version in versions:
            path = os.path.join(PLAY, lang, f"notes-{version}.txt")
            if not os.path.exists(path):
                errors.append(f"{lang}: {version} sürüm notu eksik")
                continue
            text = open(path, encoding="utf-8").read().strip()
            check_text(f"{lang}/notes-{version}.txt", text, NOTE_LIMIT)
            sizes.append(f"notlar {version} {len(text)}/{NOTE_LIMIT}")
        print(f"  {lang:<3} {', '.join(sizes)}")

    app = app_languages()
    for lang in app:
        if lang not in langs:
            errors.append(f"uygulamada {lang} var, store/play/{lang}/ yok")
    for lang in langs:
        if lang not in app:
            errors.append(f"store/play/{lang}/ var, AppLocale.TAGS'te yok")
        if lang not in PLAY_LOCALES:
            errors.append(f"{lang} için Play yerel ayar karşılığı yok")

    print()
    for e in errors:
        print(f"HATA   {e}")
    print(f"✗ {len(errors)} hata" if errors else f"✓ {len(langs)} dil, hata yok")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
