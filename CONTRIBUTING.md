# Contributing to Kodokur

Thanks for your interest! Kodokur is a small, privacy-first barcode and QR scanner.
Bug reports, translations and pull requests are welcome.

## Language

**Everything on GitHub is in English:** issues, pull requests, review comments,
commit messages and release notes. This keeps the project open to everyone.
Translations of the app, the website and the store listings live in their own files
(see below) and are of course written in their languages.

## Principles

Please keep these in mind; changes that break them will not be merged.

- **One permission.** The app requests only `CAMERA`. No `INTERNET` permission, no
  network libraries. Anything online (book search, opening a link) is handed to
  another app through an intent, after the user taps a button.
- **No ads, no trackers, no analytics, no crash reporting services.**
- **Everything stays on the device.** The history is excluded from backups.
- **Open-source dependencies only**, so the app can be built from source and
  distributed outside Google Play (e.g. F-Droid). No Google Play Services / ML Kit.
- **Every language or none.** A user-visible string is added in all 14 languages in
  the same pull request (`tools/check_strings.py` enforces this).

## Getting started

```sh
./gradlew :core:test              # fast, no Android SDK needed
./gradlew :app:assembleDebug      # needs the Android SDK (compileSdk 36)
python3 tools/check_strings.py    # translations complete and consistent
python3 tools/check_site.py       # website and privacy page consistent
python3 tools/check_store.py      # Play Store texts within limits
```

Decoding, parsing and validation logic belongs in `core/` (pure Kotlin/JVM) with unit
tests. `app/` holds the Android UI and platform code.

## Commits and pull requests

- Write commit messages in English, in the imperative mood: "Add ITF-14 support",
  "Fix GS1 expiry date for day 00". Explain *why* in the body when it isn't obvious.
- Keep a pull request focused on one change. Describe what changed, why, and how you
  tested it (unit tests, a device and Android version).
- CI must be green: string/site/store checks, core tests and the debug build.
- For UI changes, add a screenshot to the pull request.

## Translations

| What | Where |
| --- | --- |
| App | `app/src/main/res/values-<lang>/strings.xml` (English: `values/`) |
| Website | `tools/site/<lang>.json` → `python3 tools/gen_site_i18n.py` |
| Privacy policy | `tools/privacy/<lang>.json` → `python3 tools/gen_privacy.py` |
| Play Store | `store/play/<lang>/` |

Fixing a translation is a great first contribution. Adding a new language means
adding it everywhere above plus `AppLocale.TAGS` and `res/xml/locales_config.xml`.

## Reporting bugs

Please include the Android version and device, what you scanned (a photo of the code
helps a lot — blur anything private), what you expected and what happened. For
security issues, e-mail kodokur@aripd.com instead of opening a public issue.

## License

By contributing you agree that your contribution is licensed under the
[GNU GPL v3](LICENSE).
