# Release checklist

## Before the release

- [ ] The `docs/device-test.md` protocol was run on a phone with real codes (book, medicine
      box, grocery product, Wi-Fi label); the result was logged at the end of that file
- [ ] `python3 tools/check_strings.py`, `check_site.py`, `check_store.py` are clean
- [ ] `./gradlew :core:test :app:assembleDebug` passed
- [ ] `store/screenshots/en/` is up to date (1080×2160, at least 2, 4–6 recommended; the
      same English screenshots are used for every language)

## Release

- [ ] `v<X.Y.Z>` tag (`git tag -a v1.1.0 -m "Kodokur 1.1.0"` + `git push origin v1.1.0`)
- [ ] `release.yml` is green: APK signed, only permission is `CAMERA`, AAB built
- [ ] Signing fingerprint matches the Kodokur line in `~/keystores/aripdcom/README.md`
      (`apksigner verify --print-certs kodokur.apk`)

## Play Console (first upload)

- [ ] Create the app: name "Kodokur", default language en-US, App, Free
- [ ] **Play App Signing:** use "Export and upload a key from Java keystore" (PEPK)
      with `~/keystores/aripdcom/kodokur-release.jks` (alias `kodokur`), so that
      Play and GitHub APKs carry the same signature. The upload key can be separate.
- [ ] Store listing: texts from `store/play/<lang>/`, 14 languages per the locale table
      in `store/README.md`; icon `graphics/icon-512.png`, feature graphic
      `graphics/feature-1024.png`, screenshots
- [ ] Category: **Tools**; contact email `kodokur@aripd.com`; website
      `https://kodokur.aripd.com`
- [ ] Privacy policy: `https://kodokur.aripd.com/privacy.html`
- [ ] App content: `data-safety.md`, `content-rating.md` (see the health apps declaration
      note), no ads, target audience 13+
- [ ] Release: `kodokur-v<X.Y.Z>-play.aab`; release notes `play/<lang>/notes-<X.Y.Z>.txt`
- [ ] **Internal testing** track first; install from Play on a phone and check that it
      launches, then Production
