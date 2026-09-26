#!/usr/bin/env python3
"""Multilingual string check (Kodokur's adaptation of the check in Reyon).
Runs from the repository root; CI calls it on every push.

Layout:
  res/values/strings.xml            default (English): all keys
  res/values-<lang>/strings.xml     translation; may not leave any key out

Checks:
  1. AppLocale.TAGS, locales_config.xml and the values-* folders agree
  2. Every translation contains all <string> and <plurals> keys of the default
  3. No extra keys in a translation, no duplicate keys within a language
  4. Format specifiers (%1$s, %1$d) are identical in the translation; no unescaped %.
     For plurals, every quantity (one, few, other…) is compared with the default's "other"
  5. Every plural has "other"
  6. Every R.string.X / R.plurals.X in Kotlin exists in a resource; unused ones warn
  7. No unescaped double quotes: aapt2 silently drops the quote
"""
import os
import re
import sys
from collections import Counter

RES = 'app/src/main/res'
LOCALES_CONFIG = 'app/src/main/res/xml/locales_config.xml'
APP_LOCALE = 'app/src/main/kotlin/com/aripd/kodokur/platform/AppLocale.kt'
DEFAULT_LOCALE = 'en'
KOTLIN_ROOTS = ['app/src/main/kotlin']

STRING_RE = re.compile(r'<string name="([^"]+)"[^>]*>(.*?)</string>', re.S)
PLURALS_RE = re.compile(r'<plurals name="([^"]+)"[^>]*>(.*?)</plurals>', re.S)
ITEM_RE = re.compile(r'<item quantity="([^"]+)"[^>]*>(.*?)</item>', re.S)
FMT_RE = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]|%%')

errors = []
warnings = []


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def parse(path):
    """({key: text}, {plural: {quantity: text}}, duplicates)."""
    src = open(path, encoding='utf-8').read()
    src = re.sub(r'<!--.*?-->', '', src, flags=re.S)
    pairs = STRING_RE.findall(src)
    plurals = [(k, dict(ITEM_RE.findall(body))) for k, body in PLURALS_RE.findall(src)]
    names = [k for k, _ in pairs] + [k for k, _ in plurals]
    dupes = [k for k, n in Counter(names).items() if n > 1]
    return dict(pairs), dict(plurals), dupes


def locale_of(dirname):
    return dirname[len('values-'):] if dirname.startswith('values-') else ''


def fmts(text):
    return Counter(f for f in FMT_RE.findall(text) if f != '%%')


def bad_percent(text):
    covered = set()
    for m in FMT_RE.finditer(text):
        covered.update(range(m.start(), m.end()))
    return [i for i, ch in enumerate(text) if ch == '%' and i not in covered]


def bare_quotes(text):
    out, i = [], 0
    while i < len(text):
        if text[i] == '\\':
            i += 2
            continue
        if text[i] == '"':
            out.append(i)
        i += 1
    return out


def declared_tags():
    src = open(APP_LOCALE, encoding='utf-8').read()
    block = re.search(r'val TAGS: List<String> = listOf\((.*?)\)', src, re.S)
    return re.findall(r'"([^"]+)"', block.group(1)) if block else None


def configured_tags():
    src = open(LOCALES_CONFIG, encoding='utf-8').read()
    return re.findall(r'<locale android:name="([^"]+)"', src)


def main():
    if not os.path.isdir(RES):
        sys.exit('app/src/main/res not found; run from the repository root')

    files = {}
    for d in sorted(os.listdir(RES)):
        p = os.path.join(RES, d, 'strings.xml')
        if not d.startswith('values') or not os.path.exists(p):
            continue
        strings, plurals, dupes = parse(p)
        for k in dupes:
            err(f'{d}/strings.xml: duplicate key "{k}"')
        files[locale_of(d)] = (strings, plurals)

    if '' not in files:
        sys.exit(f'{RES}/values/strings.xml is missing')
    base, base_plurals = files['']
    # translatable="false" keys are not expected in translations.
    base_src = open(os.path.join(RES, 'values', 'strings.xml'), encoding='utf-8').read()
    fixed = set(re.findall(r'<string name="([^"]+)"[^>]*translatable="false"', base_src))
    translatable = {k: v for k, v in base.items() if k not in fixed}

    # 1. Language lists
    translations = sorted(loc for loc in files if loc)
    declared, configured = declared_tags(), configured_tags()
    if declared is None:
        err(f'{APP_LOCALE}: could not read the TAGS list')
    else:
        if declared != configured:
            err(f'AppLocale.TAGS and locales_config.xml are out of sync:\n'
                f'    Kotlin : {declared}\n    XML    : {configured}')
        for tag in declared:
            if tag != DEFAULT_LOCALE and tag not in translations:
                err(f'"{tag}" is in the language list but values-{tag}/strings.xml is missing')
        for tag in translations:
            if tag not in declared:
                warn(f'values-{tag} exists but is not in the language list; it will not appear in the picker')

    for name, items in base_plurals.items():
        if 'other' not in items:
            err(f'values: plural "{name}" has no "other"')

    # 2-5. Each translation
    total = len(translatable) + len(base_plurals)
    print(f'default ({DEFAULT_LOCALE}): {len(translatable)} strings + {len(base_plurals)} plurals')
    for loc in translations:
        strings, plurals = files[loc]
        missing = sorted((set(translatable) - set(strings)) | (set(base_plurals) - set(plurals)))
        extra = sorted((set(strings) - set(translatable)) | (set(plurals) - set(base_plurals)))
        problems = 0
        for k in extra:
            err(f'values-{loc}: key "{k}" is not in the default (or must not be translated)')
        for k in missing:
            err(f'values-{loc}: "{k}" is not translated')
        for k, v in strings.items():
            if k in translatable and fmts(translatable[k]) != fmts(v):
                err(f'values-{loc}: "{k}" format specifiers do not match')
            if not v.strip():
                err(f'values-{loc}: "{k}" is empty')
        for k, items in plurals.items():
            if k not in base_plurals:
                continue
            if 'other' not in items:
                err(f'values-{loc}: plural "{k}" has no "other"')
            want = fmts(base_plurals[k]['other'])
            for q, v in items.items():
                if fmts(v) != want:
                    err(f'values-{loc}: plural "{k}" [{q}] format specifiers do not match')
        problems = len(missing) + len(extra)
        print(f'  values-{loc:<6} {len(strings) + len(plurals):>4}/{total} {"OK" if not problems else "INCOMPLETE"}')

    # 4, 7. Each file: unescaped % and unescaped quotes
    for loc, (strings, plurals) in sorted(files.items()):
        where = f'values{"-" + loc if loc else ""}'
        texts = list(strings.items()) + [
            (f'{k}[{q}]', v) for k, items in plurals.items() for q, v in items.items()
        ]
        for k, v in texts:
            if fmts(v) and bad_percent(v):
                err(f'{where}: "{k}" contains an unescaped % (should be %%)')
            if bare_quotes(v):
                err(f'{where}: "{k}" contains an unescaped double quote (should be \\")')

    # 6. Kotlin references
    used_strings, used_plurals = set(), set()
    for root in KOTLIN_ROOTS:
        for dirpath, _, names in os.walk(root):
            for n in names:
                if n.endswith('.kt'):
                    src = open(os.path.join(dirpath, n), encoding='utf-8').read()
                    used_strings |= set(re.findall(r'R\.string\.([A-Za-z0-9_]+)', src))
                    used_plurals |= set(re.findall(r'R\.plurals\.([A-Za-z0-9_]+)', src))
    # Those referenced from the manifest and XML
    for dirpath, _, names in os.walk('app/src/main'):
        for n in names:
            if n.endswith('.xml'):
                src = open(os.path.join(dirpath, n), encoding='utf-8').read()
                used_strings |= set(re.findall(r'@string/([A-Za-z0-9_]+)', src))
    for k in sorted(used_strings - set(base)):
        err(f'R.string.{k} is not in any resource')
    for k in sorted(used_plurals - set(base_plurals)):
        err(f'R.plurals.{k} is not in any resource')
    unused = sorted((set(base) - used_strings) | (set(base_plurals) - used_plurals))
    if unused:
        warn(f'{len(unused)} unused keys: {", ".join(unused)}')

    print()
    for w in warnings:
        print(f'WARNING  {w}')
    for e in errors:
        print(f'ERROR    {e}')
    print()
    if errors:
        print(f'✗ {len(errors)} errors, {len(warnings)} warnings')
        return 1
    print(f'✓ no errors, {len(warnings)} warnings')
    return 0


if __name__ == '__main__':
    sys.exit(main())
