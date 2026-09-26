# Kodokur

A barcode and QR scanner for Android that stays on your phone. Groceries, medicine
codes (expiry date, batch, serial number), books and magazines, Wi-Fi, links, business
card QR codes: Kodokur shows what a code is and what you can do with it, and checks
where a link really goes. 14 languages; no ads, no trackers, the camera is the only
permission and there is no internet permission.

[![CI](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml/badge.svg)](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml)

- **Website:** https://kodokur.aripd.com
- **Privacy policy:** https://kodokur.aripd.com/privacy.html
- **APK:** [latest release](https://github.com/aripdcom/kodokur/releases/latest/download/kodokur.apk)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)

## What it reads

| Content | Recognised by | Result screen |
| --- | --- | --- |
| **Book** | EAN-13 with prefix 978/979, or a valid ISBN written as text | Hyphenated ISBN-13, ISBN-10, registration group, price from the 5-digit add-on; Open Library, Google Books, web search |
| **Periodical** | EAN-13 with prefix 977 | ISSN (check digit recomputed), issue from the 2-digit add-on; ISSN Portal |
| **Product** | Other EAN-13/EAN-8/UPC-A/UPC-E | GTIN; Open Food Facts |
| **GS1 (medicine codes)** | The scanner's GS1 flag (`]d2`, `]C1`, `]Q3`, `]e0`) or explicit GS1 notation (`(01)…`, GS-separated) | GTIN, expiry date (warning if passed), best before, production date, batch, serial, quantity; other fields raw |
| **Link** | `http(s)://`, `URLTO:` | The real destination as the headline; warnings for the `@` trick, non-Latin/punycode hosts, IP addresses and `http`; risky links open only after confirmation |
| **Other QR content** | `WIFI:`, `mailto:`/`MATMSG:`, `tel:`, `SMSTO:`, `geo:`, `MECARD:`/vCard | Connect (Android 11+ system dialog), hidden password; e-mail, dial, SMS, map, a pre-filled "add contact" form (name, title, organisation, phones, e-mails, website, address, note; vCard 2.1 quoted-printable included) |
| **Text** | Anything else | Copy, share, web search |

Decoding is done by [ZXing](https://github.com/zxing/zxing): linear codes (EAN, UPC,
Code 128/39/93, ITF, Codabar, GS1 DataBar) and 2D codes (QR, Data Matrix, PDF417,
Aztec). Camera frames are rotated upright; a linear barcode held sideways is also tried
rotated every other frame. A picture picked from the gallery is decoded harder
(including inverted codes). Zoom for small codes: pinch, double-tap (1× ↔ 2×) and a
ratio button that also works with a screen reader (1× → 2× → 4×).

**Batch mode:** each code goes straight to the history without opening the result
screen, and the same code is never added twice in one batch. The history is shared as
CSV (UTF-8 with BOM, with `isbn13`/`isbn10` columns; text that looks like a formula is
neutralised). For counting a shop shelf, a pharmacy cabinet or a home library.

## Layout

```
core/      pure Kotlin/JVM: ZXing decoder, rotation, ISBN/ISSN/GTIN validation and
           hyphenation, GS1 parser, link check, content parsing (QR formats), CSV + tests
app/       Android app: CameraX preview and frame analysis, Compose UI (scanner,
           result, history, about, language), 14 languages
site/      website and privacy policy, both in 14 languages with a language picker
           (assets/site.js, assets/i18n.js; GitHub Pages)
tools/     ISBN range table generator, site translations (site/, privacy/) and their
           generators, string, site and store checks
store/     Play Store package: listings and release notes in 14 languages, graphics,
           screenshots, data safety and content rating answers, release checklist
docs/      on-device test protocol and log (device-test.md)
```

The ISBN range table ([ISBN International RangeMessage](https://www.isbn-international.org/range_file_generation))
ships inside the app as `core/src/main/resources/.../isbn-ranges.txt`; the app never
goes online, so hyphenation works offline. Refresh it as new publisher ranges open:

```sh
python3 tools/isbn_ranges.py
```

## Building

```sh
./gradlew :core:test            # core unit tests (no Android SDK needed)
./gradlew :app:assembleDebug    # debug APK
```

The version is passed as `-PappVersion=X.Y.Z`; `release.yml` derives it from the tag.
`versionCode = major*10000 + minor*100 + patch`.

These checks run in CI before Gradle:

```sh
python3 tools/check_strings.py   # key, plural and format-specifier parity across 14 languages
python3 tools/check_site.py      # privacy page matches its generator, links are consistent
python3 tools/check_store.py     # Play texts: length limits, 14 languages, invisible characters
python3 tools/gen_privacy.py     # tools/privacy/*.json → site/privacy.html
python3 tools/gen_site_i18n.py   # tools/site/*.json → site/assets/i18n.js (English lives in index.html)
```

A release starts with a `v*` tag (`release.yml`): the signed APK and AAB, source
archives and SHA-256 sums are attached to the GitHub Release; the release stops if the
APK carries any permission other than the camera. Required secrets:
`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` (alias `kodokur`).

## Privacy

The manifest declares a single permission: `CAMERA`. Frames are decoded in memory and
discarded. There is no `INTERNET` permission; buttons such as book search hand the code
to the browser. The history lives in the app's private storage and, because it can
contain Wi-Fi passwords, is excluded from cloud backup and device-to-device transfer.
Details: [privacy policy](https://kodokur.aripd.com/privacy.html).

## License

The source code is available under the [GNU GPL v3](LICENSE). ZXing and AndroidX are
under the Apache License 2.0. The name "Kodokur" and its icon are not covered by the
license; if you fork, publish under your own name and icon.
