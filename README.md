# Kodokur

Android için telefonunuzda kalan barkod ve karekod okuyucu. Market ürünü, ilaç
karekodu (son kullanma tarihi, parti, seri), kitap ve dergi, Wi-Fi, bağlantı, kartvizit:
ne olduğunu ve onunla ne yapılabileceğini gösterir. Bağlantının gerçekte nereye
gittiğini denetler. 14 dil; reklam yok, izleyici yok, tek izin kamera, internet izni yok.

[![CI](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml/badge.svg)](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml)

- **Site:** https://kodokur.aripd.com
- **Gizlilik:** https://kodokur.aripd.com/privacy.html
- **APK:** [en yeni sürüm](https://github.com/aripdcom/kodokur/releases/latest/download/kodokur.apk)

## Ne okur

| İçerik | Nasıl tanınır | Sonuç ekranında |
| --- | --- | --- |
| **Kitap** | 978/979 önekli EAN-13, ya da metin olarak yazılmış geçerli ISBN | Tireli ISBN-13, ISBN-10, kayıt grubu, 5 haneli ekten fiyat; Open Library, Google Books, web araması |
| **Süreli yayın** | 977 önekli EAN-13 | ISSN (sağlama hanesi yeniden hesaplanır), 2 haneli ekten sayı; ISSN Portal |
| **Ürün** | Öbür EAN-13/EAN-8/UPC-A/UPC-E | GTIN; Open Food Facts |
| **GS1 (ilaç karekodu)** | Tarayıcının GS1 işareti (`]d2`, `]C1`, `]Q3`, `]e0`) ya da açık GS1 yazımı (`(01)…`, GS ayraçlı) | GTIN, son kullanma (geçmişse uyarı), TETT, üretim, parti, seri, adet; öbür alanlar ham |
| **Bağlantı** | `http(s)://`, `URLTO:` | Başlıkta gideceği site; `@` hilesi, Latin dışı/punycode alan adı, IP adresi ve `http` için uyarı; uyarılı bağlantı onayla açılır |
| **QR içerikleri** | `WIFI:`, `mailto:`/`MATMSG:`, `tel:`, `SMSTO:`, `geo:`, `MECARD:`/vCard | Bağlan (Android 11+ sistem penceresi), parola gizli; e-posta, arama, SMS, harita, kişi ekleme formu |
| **Metin** | Tanınmayan her şey | Kopyala, paylaş, web'de ara |

Çözme [ZXing](https://github.com/zxing/zxing) ile: çizgili (EAN, UPC, Code 128/39/93, ITF,
Codabar, GS1 DataBar) ve iki boyutlu (QR, Data Matrix, PDF417, Aztec) kodlar. Kamera
karesi dik konuma çevrilir; çizgili barkod dik tutulduysa her iki karede bir yan
çevrilmişi de denenir. Galeriden seçilen görsel daha inatçı çözülür (ters renkli kodlar dahil).
Küçük kodlar için yakınlaştırma: iki parmak, çift dokunuş (1× ↔ 2×) ve ekran okuyucuyla
da kullanılabilen oran düğmesi (1× → 2× → 4×).

**Seri tarama:** okunan kod sonuç ekranı açılmadan geçmişe eklenir, aynı kod bir seride
ikinci kez eklenmez. Geçmiş CSV olarak paylaşılır (UTF-8 BOM'lu, `isbn13`/`isbn10`
sütunlarıyla; formül gibi başlayan metin etkisizleştirilir). Raf, ecza dolabı ya da
kitaplık sayımı için.

## Yapı

```
core/      saf Kotlin/JVM: ZXing çözücüsü, döndürme, ISBN/ISSN/GTIN doğrulama ve
           tireleme, GS1 ayrıştırıcı, bağlantı denetimi, içerik ayrıştırma (QR
           biçimleri), CSV + testler
app/       Android uygulaması: CameraX önizleme ve kare çözümleme, Compose arayüz
           (tarayıcı, sonuç, geçmiş, hakkında, dil), 14 dil
site/      proje sayfası ve gizlilik politikası; ikisi de 14 dilde, dil seçicili
           (assets/site.js, assets/i18n.js; GitHub Pages)
tools/     ISBN aralık tablosu üreticisi, site çevirileri (site/, privacy/) ve
           üreticileri, metin, site ve mağaza denetimleri
store/     Play mağaza paketi: 14 dilde metinler ve sürüm notları, görseller, veri
           güvenliği ve içerik derecelendirme cevapları, yayın kontrol listesi
docs/      cihaz test protokolü ve kütüğü (device-test.md)
```

ISBN aralık tablosu ([ISBN International RangeMessage](https://www.isbn-international.org/range_file_generation))
`core/src/main/resources/.../isbn-ranges.txt` olarak pakete gömülüdür; uygulama ağa
bağlanmadığı için tireleme çevrimdışı yapılır. Yeni yayıncı aralıkları açıldıkça tazelenmeli:

```sh
python3 tools/isbn_ranges.py
```

## Derleme

```sh
./gradlew :core:test            # çekirdek birim testleri (Android SDK gerekmez)
./gradlew :app:assembleDebug    # debug APK
```

Sürüm `-PappVersion=X.Y.Z` ile verilir; `release.yml` etiketten türetir.
`versionCode = major*10000 + minor*100 + patch`.

Denetimler CI'da Gradle'dan önce koşar:

```sh
python3 tools/check_strings.py   # 14 dilde anahtar, çoğul ve biçim belirteci paritesi
python3 tools/check_site.py      # gizlilik sayfası üreticisiyle aynı mı, bağlantılar tutarlı mı
python3 tools/check_store.py     # Play metinleri: karakter sınırları, 14 dil, görünmez karakter
python3 tools/gen_privacy.py     # tools/privacy/*.json → site/privacy.html
python3 tools/gen_site_i18n.py   # tools/site/*.json → site/assets/i18n.js (İngilizce index.html içinde)
```

Yayın `v*` etiketiyle başlar (`release.yml`): imzalı APK ve AAB, kaynak arşivleri ve
SHA256 özetleri GitHub Release'e eklenir; APK kameradan başka izin taşıyorsa sürüm durur.
Gerekli secret'lar: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` (alias `kodokur`).

## Gizlilik

Uygulamanın manifestinde tek izin var: `CAMERA`. Kareler bellekte çözülür ve atılır.
`INTERNET` izni yok; kitap araması gibi düğmeler kodu tarayıcıya devreder. Geçmiş
uygulamanın özel alanında durur ve Wi-Fi parolası içerebileceği için bulut yedeğine
ve cihazdan cihaza aktarıma girmez. Ayrıntı: [gizlilik politikası](https://kodokur.aripd.com/privacy.html).

## Lisans

Kaynak kod [GNU GPL v3](LICENSE) ile açıktır. ZXing ve AndroidX Apache Lisansı 2.0
iledir. "Kodokur" adı ve simgesi lisansa dahil değildir; çatallarsanız kendi adınızla ve
kendi simgenizle yayımlayın.
