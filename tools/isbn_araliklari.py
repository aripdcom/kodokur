#!/usr/bin/env python3
"""ISBN aralık tablosunu uygulamanın okuduğu sade biçime çevirir.

Kaynak, ISBN International'ın herkese açık RangeMessage.xml dosyasıdır:
    https://www.isbn-international.org/export_rangemessage.xml

Uygulama ağa bağlanmadığı için tablo pakete gömülüdür; yeni yayıncı aralıkları
açıldıkça ara ara tazelenmelidir:

    python3 tools/isbn_araliklari.py            # indirir ve yazar
    python3 tools/isbn_araliklari.py dosya.xml  # elde olan dosyadan yazar

Çıktı (core/src/main/resources/.../isbn-ranges.txt), satır başına bir grup:

    978-975<TAB>Türkiye<TAB>0000000-0199999:5,0200000-2399999:2,...

Aralıklar grup önekinden sonraki yedi haneye uygulanır; ':' sonrası yayıncı
bölümünün uzunluğudur (0 = henüz tanımlanmamış aralık).
"""
import sys
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

URL = "https://www.isbn-international.org/export_rangemessage.xml"
OUT = Path(__file__).resolve().parent.parent / "core/src/main/resources/com/aripd/kodokur/core/isbn-ranges.txt"


def main() -> None:
    if len(sys.argv) > 1:
        data = Path(sys.argv[1]).read_bytes()
    else:
        with urllib.request.urlopen(URL, timeout=60) as r:
            data = r.read()
    root = ET.fromstring(data)
    date = (root.findtext("MessageDate") or "").strip()
    serial = (root.findtext("MessageSerialNumber") or "").strip()

    lines = [f"# ISBN International RangeMessage · {date} · {serial}",
             "# tools/isbn_araliklari.py ile üretildi; elle düzenlemeyin."]
    for group in root.iter("Group"):
        prefix = group.findtext("Prefix").strip()
        agency = " ".join(group.findtext("Agency").split())
        rules = []
        for rule in group.iter("Rule"):
            rng = rule.findtext("Range").strip()
            length = rule.findtext("Length").strip()
            lo, hi = rng.split("-")
            if len(lo) != 7 or len(hi) != 7:
                sys.exit(f"Beklenmeyen aralık biçimi: {prefix} {rng}")
            rules.append(f"{rng}:{length}")
        if "\t" in agency or not rules:
            sys.exit(f"Bozuk grup: {prefix}")
        lines.append(f"{prefix}\t{agency}\t{','.join(rules)}")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{OUT.relative_to(Path.cwd()) if OUT.is_relative_to(Path.cwd()) else OUT}: "
          f"{len(lines) - 2} grup, {date}")


if __name__ == "__main__":
    main()
