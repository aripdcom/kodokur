#!/usr/bin/env python3
"""Website check. Runs from the repository root; CI calls it on every push.

  1. site/privacy.html is identical to the output of tools/gen_privacy.py
     (otherwise a hand edit to the generated file would drift silently)
  2. Every language in AppLocale.TAGS has a privacy section and a contact address
  3. site/assets/i18n.js matches the output of tools/gen_site_i18n.py; every app
     language (except English) is also on the landing page, with no missing keys
  4. The website and source links in the app match the site's addresses
  5. site/index.html links to the privacy page
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
SITE = "https://kodokur.aripd.com"
SOURCE = "https://github.com/aripdcom/kodokur"

errors = []


def generated_matches(path, text, fix):
    current = open(path, encoding="utf-8").read() if os.path.exists(path) else ""
    if current != text:
        errors.append(f"{os.path.relpath(path, ROOT)} is out of sync with its generator: run {fix}")


def main():
    tags, html = gen_privacy.render()
    generated_matches(gen_privacy.OUT, html, "python3 tools/gen_privacy.py")
    for tag in tags:
        if f'<section id="{tag}" lang="{tag}" data-lang="{tag}"' not in html:
            errors.append(f"privacy page has no {tag} section")
    if html.count(f"mailto:{gen_privacy.MAIL}") != len(tags):
        errors.append("not every language section has a contact address")

    table, site_errors, js = gen_site_i18n.render()
    errors.extend(site_errors)
    generated_matches(gen_site_i18n.OUT, js, "python3 tools/gen_site_i18n.py")
    for tag in tags:
        if tag not in table:
            errors.append(f"landing page has no {tag}: tools/site/{tag}.json is missing")
    for tag in table:
        if tag not in tags:
            errors.append(f"tools/site/{tag}.json exists but the app has no such language")

    actions = open(ACTIONS, encoding="utf-8").read()
    for name, want in (("SOURCE_URL", SOURCE), ("PRIVACY_URL", f"{SITE}/privacy.html")):
        m = re.search(rf'const val {name} = "([^"]+)"', actions)
        if not m:
            errors.append(f"Actions.{name} not found")
        elif m.group(1) != want:
            errors.append(f"Actions.{name} = {m.group(1)}, expected {want}")

    index = open(INDEX, encoding="utf-8").read()
    if 'href="privacy.html"' not in index:
        errors.append("site/index.html does not link to the privacy page")

    for e in errors:
        print(f"ERROR  {e}")
    if errors:
        print(f"✗ {len(errors)} errors")
        return 1
    print(f"✓ site consistent ({len(tags)} languages)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
