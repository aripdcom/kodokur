#!/usr/bin/env python3
"""Çok dilli metin denetimi (Reyon'daki denetimin Kodokur uyarlaması).
Depo kökünden koşar, CI her itmede çağırır.

Düzen:
  res/values/strings.xml            varsayılan (İngilizce): tüm anahtarlar
  res/values-<dil>/strings.xml      çeviri; eksik anahtar bırakamaz

Denetimler:
  1. AppLocale.TAGS, locales_config.xml ve values-* klasörleri birbirini tutar
  2. Her çeviri varsayılandaki tüm <string> ve <plurals> anahtarlarını içerir
  3. Çeviride fazladan anahtar yok, aynı dilde yinelenen anahtar yok
  4. Biçim belirteçleri (%1$s, %1$d) çeviride birebir aynı; kaçırılmamış % yok.
     Çoğullarda her nicelik (one, few, other…) varsayılanın "other"ıyla karşılaştırılır
  5. Her çoğulda "other" var
  6. Kotlin'deki her R.string.X / R.plurals.X bir kaynakta var; kullanılmayan uyarı verir
  7. Kaçışsız çift tırnak yok: aapt2 tırnağı sessizce atar
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
    """({anahtar: metin}, {çoğul: {nicelik: metin}}, yinelenenler)."""
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
        sys.exit('app/src/main/res bulunamadı; depo kökünden koşturun')

    files = {}
    for d in sorted(os.listdir(RES)):
        p = os.path.join(RES, d, 'strings.xml')
        if not d.startswith('values') or not os.path.exists(p):
            continue
        strings, plurals, dupes = parse(p)
        for k in dupes:
            err(f'{d}/strings.xml: yinelenen anahtar "{k}"')
        files[locale_of(d)] = (strings, plurals)

    if '' not in files:
        sys.exit(f'{RES}/values/strings.xml yok')
    base, base_plurals = files['']
    # translatable="false" anahtarlar çeviride aranmaz.
    base_src = open(os.path.join(RES, 'values', 'strings.xml'), encoding='utf-8').read()
    fixed = set(re.findall(r'<string name="([^"]+)"[^>]*translatable="false"', base_src))
    translatable = {k: v for k, v in base.items() if k not in fixed}

    # 1. Dil listeleri
    translations = sorted(loc for loc in files if loc)
    declared, configured = declared_tags(), configured_tags()
    if declared is None:
        err(f'{APP_LOCALE}: TAGS listesi okunamadı')
    else:
        if declared != configured:
            err(f'AppLocale.TAGS ile locales_config.xml ayrışmış:\n'
                f'    Kotlin : {declared}\n    XML    : {configured}')
        for tag in declared:
            if tag != DEFAULT_LOCALE and tag not in translations:
                err(f'"{tag}" dil listesinde ama values-{tag}/strings.xml yok')
        for tag in translations:
            if tag not in declared:
                warn(f'values-{tag} var ama dil listesinde yok; seçicide çıkmaz')

    for name, items in base_plurals.items():
        if 'other' not in items:
            err(f'values: çoğul "{name}" için "other" yok')

    # 2-5. Her çeviri
    total = len(translatable) + len(base_plurals)
    print(f'varsayılan ({DEFAULT_LOCALE}): {len(translatable)} metin + {len(base_plurals)} çoğul')
    for loc in translations:
        strings, plurals = files[loc]
        missing = sorted((set(translatable) - set(strings)) | (set(base_plurals) - set(plurals)))
        extra = sorted((set(strings) - set(translatable)) | (set(plurals) - set(base_plurals)))
        problems = 0
        for k in extra:
            err(f'values-{loc}: varsayılanda olmayan (ya da çevrilmemesi gereken) anahtar "{k}"')
        for k in missing:
            err(f'values-{loc}: "{k}" çevrilmemiş')
        for k, v in strings.items():
            if k in translatable and fmts(translatable[k]) != fmts(v):
                err(f'values-{loc}: "{k}" biçim belirteçleri uyuşmuyor')
            if not v.strip():
                err(f'values-{loc}: "{k}" boş')
        for k, items in plurals.items():
            if k not in base_plurals:
                continue
            if 'other' not in items:
                err(f'values-{loc}: çoğul "{k}" için "other" yok')
            want = fmts(base_plurals[k]['other'])
            for q, v in items.items():
                if fmts(v) != want:
                    err(f'values-{loc}: çoğul "{k}" [{q}] biçim belirteçleri uyuşmuyor')
        problems = len(missing) + len(extra)
        print(f'  values-{loc:<6} {len(strings) + len(plurals):>4}/{total} {"TAM" if not problems else "EKSİK"}')

    # 4, 7. Her dosya: kaçırılmamış % ve kaçışsız tırnak
    for loc, (strings, plurals) in sorted(files.items()):
        where = f'values{"-" + loc if loc else ""}'
        texts = list(strings.items()) + [
            (f'{k}[{q}]', v) for k, items in plurals.items() for q, v in items.items()
        ]
        for k, v in texts:
            if fmts(v) and bad_percent(v):
                err(f'{where}: "{k}" kaçırılmamış % içeriyor (%% olmalı)')
            if bare_quotes(v):
                err(f'{where}: "{k}" kaçışsız çift tırnak içeriyor (\\" olmalı)')

    # 6. Kotlin referansları
    used_strings, used_plurals = set(), set()
    for root in KOTLIN_ROOTS:
        for dirpath, _, names in os.walk(root):
            for n in names:
                if n.endswith('.kt'):
                    src = open(os.path.join(dirpath, n), encoding='utf-8').read()
                    used_strings |= set(re.findall(r'R\.string\.([A-Za-z0-9_]+)', src))
                    used_plurals |= set(re.findall(r'R\.plurals\.([A-Za-z0-9_]+)', src))
    # Manifestte ve XML'de okunanlar
    for dirpath, _, names in os.walk('app/src/main'):
        for n in names:
            if n.endswith('.xml'):
                src = open(os.path.join(dirpath, n), encoding='utf-8').read()
                used_strings |= set(re.findall(r'@string/([A-Za-z0-9_]+)', src))
    for k in sorted(used_strings - set(base)):
        err(f'R.string.{k} hiçbir kaynakta yok')
    for k in sorted(used_plurals - set(base_plurals)):
        err(f'R.plurals.{k} hiçbir kaynakta yok')
    unused = sorted((set(base) - used_strings) | (set(base_plurals) - used_plurals))
    if unused:
        warn(f'{len(unused)} kullanılmayan anahtar: {", ".join(unused)}')

    print()
    for w in warnings:
        print(f'UYARI  {w}')
    for e in errors:
        print(f'HATA   {e}')
    print()
    if errors:
        print(f'✗ {len(errors)} hata, {len(warnings)} uyarı')
        return 1
    print(f'✓ hata yok, {len(warnings)} uyarı')
    return 0


if __name__ == '__main__':
    sys.exit(main())
