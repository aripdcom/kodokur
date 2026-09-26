#!/usr/bin/env python3
"""Generates site/assets/i18n.js from the tools/site/<lang>.json files.

English is not here: it lives in site/index.html itself, and site.js takes a
snapshot of it on load. Each JSON has `name` (the language's own name), `_title`,
`_desc` and every data-i18n key in index.html. A missing or extra key is an error.

Usage: python3 tools/gen_site_i18n.py
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEXTS = os.path.join(ROOT, "tools", "site")
INDEX = os.path.join(ROOT, "site", "index.html")
OUT = os.path.join(ROOT, "site", "assets", "i18n.js")

HEADER = """/*
  Website translations. GENERATED FILE: do not edit by hand.
  Source: tools/site/<lang>.json → python3 tools/gen_site_i18n.py
  English is not here; it lives in index.html itself.
*/
"""


def keys():
    html = open(INDEX, encoding="utf-8").read()
    return ["name", "_title", "_desc"] + sorted(set(re.findall(r'data-i18n="([^"]+)"', html)))


def render():
    want = keys()
    table = {"en": {"name": "English"}}
    errors = []
    for name in sorted(os.listdir(TEXTS)):
        if not name.endswith(".json"):
            continue
        tag = name[:-5]
        data = json.load(open(os.path.join(TEXTS, name), encoding="utf-8"))
        missing = [k for k in want if not str(data.get(k, "")).strip()]
        extra = [k for k in data if k not in want]
        if missing:
            errors.append(f"tools/site/{name}: missing: {', '.join(missing)}")
        if extra:
            errors.append(f"tools/site/{name}: not in index.html: {', '.join(extra)}")
        table[tag] = {k: data[k] for k in want if k in data}
    body = json.dumps(table, ensure_ascii=False, indent=2)
    return table, errors, f"{HEADER}\nwindow.KODOKUR_I18N = {body};\n"


def main():
    table, errors, js = render()
    for e in errors:
        print(f"ERROR  {e}")
    if errors:
        return 1
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w", encoding="utf-8").write(js)
    print(f"{OUT}: {len(table)} languages ({' '.join(table)})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
