# Yayın kontrol listesi

## Sürümden önce

- [ ] `docs/device-test.md` protokolü telefonda, gerçek kodlarla (kitap, ilaç kutusu,
      market ürünü, Wi-Fi etiketi) koşuldu; sonuç o dosyanın sonuna yazıldı
- [ ] `python3 tools/check_strings.py`, `check_site.py`, `check_store.py` temiz
- [ ] `./gradlew :core:test :app:assembleDebug` geçti
- [ ] `store/screenshots/{en,tr}/` güncel (1080×2400, en az 2, önerilen 4–6)

## Sürüm

- [ ] `v<X.Y.Z>` etiketi (`git tag -a v1.1.0 -m "Kodokur 1.1.0"` + `git push origin v1.1.0`)
- [ ] `release.yml` yeşil: APK imzalı, tek izin `CAMERA`, AAB üretildi
- [ ] İmza parmak izi `~/keystores/aripdcom/README.md`'deki Kodokur satırıyla aynı
      (`apksigner verify --print-certs kodokur.apk`)

## Play Console (ilk yükleme)

- [ ] Uygulamayı oluştur: ad "Kodokur", varsayılan dil en-US, uygulama, ücretsiz
- [ ] **Play App Signing:** "Uygulama imzalama anahtarını dışa aktar ve yükle" (PEPK)
      ile `~/keystores/aripdcom/kodokur-release.jks` (alias `kodokur`) verilir; böylece
      Play ve GitHub APK'leri aynı imzayı taşır. Yükleme anahtarı ayrı olabilir.
- [ ] Mağaza girişi: `store/play/<dil>/` metinleri, `store/README.md`'deki yerel ayar
      tablosuna göre 14 dil; simge `graphics/icon-512.png`, öne çıkan görsel
      `graphics/feature-1024.png`, ekran görüntüleri
- [ ] Kategori: **Araçlar**; iletişim e-postası `kodokur@aripd.com`; web sitesi
      `https://kodokur.aripd.com`
- [ ] Gizlilik politikası: `https://kodokur.aripd.com/privacy.html`
- [ ] Uygulama içeriği: `data-safety.md`, `content-rating.md` (sağlık beyanı notuna bak),
      reklam yok, hedef kitle 13+
- [ ] Sürüm: `kodokur-v<X.Y.Z>-play.aab`; sürüm notları `play/<dil>/notes-<X.Y.Z>.txt`
- [ ] Önce **dahili test** kanalı; bir telefonda Play'den kurup açılışı dene, sonra üretim
