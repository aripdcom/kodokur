# Release checklist

The Play developer account is an **organization account** (opened September 2026), so
the 12-tester / 14-day closed testing requirement for new personal accounts does not
apply: the first release can go straight to **Production**.

Kodokur is a **paid app on Google Play: €1.99** (same as Kerteriz). The APK on GitHub
and the website stays free, as the GPL allows; the Play price pays for convenience and
supports the project, and the store description says the app is open source.

## 1. Before the release

- [x] `docs/device-test.md` was run on a phone with real codes; result logged there
- [x] `python3 tools/check_strings.py`, `check_site.py`, `check_store.py` are clean
- [x] `./gradlew :core:test :app:assembleDebug` passed
- [x] `store/screenshots/en/` is up to date (1080×2160, 2–8 images; the same English
      screenshots are used for every language)
- [x] `store/play/<lang>/notes-<X.Y.Z>.txt` exists for the version in all 14 languages

## 2. Tag and verify the build

```sh
git tag -a v1.1.1 -m "Kodokur 1.1.1" && git push origin v1.1.1
```

- [x] `release.yml` is green: APK signed, only permission `CAMERA`, AAB built
- [x] From the GitHub release download `kodokur-v1.1.1-play.aab` and
      `kodokur-v1.1.1-mapping.txt`
- [x] Signing fingerprint matches the Kodokur line in `~/keystores/aripdcom/README.md`:
      `apksigner verify --print-certs kodokur.apk | grep 'certificate SHA-256'`
      → `8e97d791…448e16`

## 3. Play Console: create the app (one time)

- [x] **Payments profile** (Settings → Payments profile) is set up with bank account and
      tax information; paid apps cannot be published without it
- [x] **Create app:** name "Kodokur", default language English (United States),
      App, **Paid**, accept the declarations. This cannot be undone later: a free app
      can never become paid (a paid app can become free)
- [x] **Pricing** (Monetize → Products → App pricing): **€1.99**. Let Play convert it to
      local prices, then review the suggestions for lower-income markets (e.g. Turkey)
      and lower them by hand where the converted price looks too high
- [x] **Automatic protection** (Test and release → App integrity): **Off** (done).
      Reason: the same app is legally free on GitHub under the GPL, so it protects
      nothing, and it injects closed-source verification code into the APK Play serves,
      which conflicts with the open-source and no-network promises
- [x] **App signing** (Test and release → Setup → App signing): choose
      *Use a different key* → *Export and upload a key from a Java keystore*. Download
      the PEPK tool and the encryption public key shown there, then run:

      ```sh
      java -jar pepk.jar \
        --keystore=$HOME/keystores/aripdcom/kodokur-release.jks \
        --alias=kodokur \
        --output=kodokur-signing-key.zip \
        --include-cert \
        --rsa-aes-encryption \
        --encryption-key-path=encryption_public_key.pem
      ```

      (it asks for the keystore password twice: `~/keystores/aripdcom/kodokur-release.password`).
      Upload `kodokur-signing-key.zip`. The same key then signs uploads too, so no separate
      upload key is needed. Result: apps installed from Play and from GitHub carry the same
      signature and update each other.
- [ ] Delete `kodokur-signing-key.zip` afterwards (it contains the encrypted private key)

## 4. Play Console: store listing and app content

- [x] **Main store listing:** app name, short and full description from `store/play/en/`;
      then *Manage translations → Add your own* for the other 13 languages using the
      locale table in `store/README.md` (`nb` → Norwegian `no-NO`, `pt` → `pt-BR`)
- [x] **Graphics:** icon `graphics/icon-512.png`, feature graphic
      `graphics/feature-1024.png`, phone screenshots `screenshots/en/1…6`
- [x] **Store settings:** category **Tools**; tags **Barcode scanner, Books & reference,
      Privacy & security, Shopping, Wi-Fi** (no health tag, to stay consistent with the
      "no health features" declaration); email `kodokur@aripd.com`, website
      `https://kodokur.aripd.com`
- [x] **External marketing:** left **on**. Google may promote the app outside Play at no
      cost; it does not change the app, its data safety answers or its privacy
- [x] **App content:**
  - Privacy policy: `https://kodokur.aripd.com/privacy.html`
  - Ads: no ads
  - App access: all functionality available without special access
  - Content rating: questionnaire answers in `content-rating.md`
  - Target audience: 13–15, 16–17, 18+ (`content-rating.md`)
  - Data safety: no data collected, no data shared (`data-safety.md`)
  - Health apps: *My app does not have any health features* (see the note in
    `content-rating.md`)
  - Government apps / financial features / news: not applicable
- [x] **Countries:** all countries and regions where Play supports paid apps (Play
      hides the rest automatically for a paid app)

## 5. Play Console: production release

- [x] Test and release → **Production → Create new release**
- [x] Upload `kodokur-v1.1.1-play.aab`
- [x] Upload the deobfuscation file `kodokur-v1.1.1-mapping.txt` (App bundle explorer →
      the version → Downloads → *ReTrace mapping file*), so crash reports are readable
- [x] Release notes: paste every language, each wrapped in its Play locale tag, e.g.
      `<en-US>` … `</en-US>`, `<tr-TR>` … `</tr-TR>` (texts: `play/<lang>/notes-1.1.1.txt`)
- [x] Review the warnings: "no native debug symbols" can be ignored (only CameraX ships a
      small native library; there is no NDK code of our own)
- [x] **Send for review**; the first review of a new app can take several days
      (sent 2026-09-26, status: *In review*)

## 6. After it is live

- [ ] Install from Play on a phone; open it, scan a code, switch language in About
- [ ] Check the Play-installed signature equals GitHub's:
      `adb shell pm path com.aripd.kodokur` → `adb pull <base.apk>` →
      `apksigner verify --print-certs base.apk`
- [ ] Add the Play link to `site/index.html` and `README.md`
- [ ] Watch the pre-launch report and Android vitals during the first week
