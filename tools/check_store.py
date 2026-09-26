#!/usr/bin/env python3
"""Checks the Play listing texts (Kodokur's adaptation of the check in Reyon).

Under store/play/<lang>/:
  title.txt   ≤ 30 characters    short.txt  ≤ 80
  full.txt    ≤ 4000             notes-<version>.txt ≤ 500 (release notes)

Checks:
  * every language has the three listing files, non-empty and within limits
  * the language folders match AppLocale.TAGS exactly
  * every language has a known Play locale (which one to pick when copying
    into Play Console is in store/README.md)
  * every release note exists in all languages
  * no invisible characters (soft hyphen, zero-width space, BOM): Play
    publishes them as is, and they show up as broken words in the listing
"""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLAY = os.path.join(ROOT, "store", "play")
LIMITS = {"title.txt": 30, "short.txt": 80, "full.txt": 4000}
NOTE_LIMIT = 500

# Repository language code → Play Console locale.
PLAY_LOCALES = {
    "en": "en-US", "tr": "tr-TR", "de": "de-DE", "fr": "fr-FR", "nl": "nl-NL",
    "es": "es-ES", "pt": "pt-BR", "it": "it-IT", "da": "da-DK", "sv": "sv-SE",
    "nb": "no-NO", "fi": "fi-FI", "ru": "ru-RU", "ar": "ar",
}
INVISIBLE = {0x00AD: "soft hyphen", 0x200B: "zero-width space", 0xFEFF: "BOM"}

errors = []


def app_languages():
    path = os.path.join(ROOT, "app", "src", "main", "kotlin", "com", "aripd", "kodokur",
                        "platform", "AppLocale.kt")
    source = open(path, encoding="utf-8").read()
    block = re.search(r"val TAGS: List<String> = listOf\((.*?)\)", source, re.S)
    return re.findall(r'"([a-z-]+)"', block.group(1)) if block else []


def check_text(where, text, limit):
    if not text:
        errors.append(f"{where} is empty")
        return
    if len(text) > limit:
        errors.append(f"{where}: {len(text)} characters, limit {limit}")
    for ch in set(text):
        if ord(ch) in INVISIBLE:
            errors.append(f"{where}: invisible character U+{ord(ch):04X} ({INVISIBLE[ord(ch)]})")


def main():
    langs = sorted(d for d in os.listdir(PLAY) if os.path.isdir(os.path.join(PLAY, d)))
    versions = sorted({os.path.basename(p)[len("notes-"):-len(".txt")]
                       for p in glob.glob(os.path.join(PLAY, "*", "notes-*.txt"))})
    for lang in langs:
        sizes = []
        for name, limit in LIMITS.items():
            path = os.path.join(PLAY, lang, name)
            if not os.path.exists(path):
                errors.append(f"{lang}/{name} is missing")
                continue
            text = open(path, encoding="utf-8").read().strip()
            check_text(f"{lang}/{name}", text, limit)
            sizes.append(f"{name[:-4]} {len(text)}/{limit}")
        for version in versions:
            path = os.path.join(PLAY, lang, f"notes-{version}.txt")
            if not os.path.exists(path):
                errors.append(f"{lang}: release notes for {version} are missing")
                continue
            text = open(path, encoding="utf-8").read().strip()
            check_text(f"{lang}/notes-{version}.txt", text, NOTE_LIMIT)
            sizes.append(f"notes {version} {len(text)}/{NOTE_LIMIT}")
        print(f"  {lang:<3} {', '.join(sizes)}")

    app = app_languages()
    for lang in app:
        if lang not in langs:
            errors.append(f"the app has {lang}, but store/play/{lang}/ does not exist")
    for lang in langs:
        if lang not in app:
            errors.append(f"store/play/{lang}/ exists, but it is not in AppLocale.TAGS")
        if lang not in PLAY_LOCALES:
            errors.append(f"no Play locale mapping for {lang}")

    print()
    for e in errors:
        print(f"ERROR  {e}")
    print(f"✗ {len(errors)} errors" if errors else f"✓ {len(langs)} languages, no errors")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
