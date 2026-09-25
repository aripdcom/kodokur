# Kodokur

Android için ISBN'den anlayan barkod ve QR okuyucu. Kitabın arkasındaki barkodu okur,
ISBN'i doğru yerlerden tireler, ISBN-10 karşılığını ve kayıt grubunu gösterir; dergi,
market ürünü, Wi-Fi, bağlantı, kişi kartı da okur. 14 dil; reklam yok, izleyici yok,
tek izin kamera, internet izni yok.

[![CI](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml/badge.svg)](https://github.com/aripdcom/kodokur/actions/workflows/ci.yml)

- **Site:** https://aripdcom.github.io/kodokur
- **Gizlilik:** https://aripdcom.github.io/kodokur/gizlilik.html
- **APK:** [en yeni sürüm](https://github.com/aripdcom/kodokur/releases/latest/download/kodokur.apk)

## Ne okur

| İçerik | Nasıl tanınır | Sonuç ekranında |
| --- | --- | --- |
| **Kitap** | 978/979 önekli EAN-13, ya da metin olarak yazılmış geçerli ISBN | Tireli ISBN-13, ISBN-10, kayıt grubu, 5 haneli ekten fiyat; Open Library, Google Books, web araması |
| **Süreli yayın** | 977 önekli EAN-13 | ISSN (sağlama hanesi yeniden hesaplanır), 2 haneli ekten sayı; ISSN Portal |
| **Ürün** | Öbür EAN-13/EAN-8/UPC-A/UPC-E | GTIN; Open Food Facts |
| **QR içerikleri** | `http(s)://`, `WIFI:`, `mailto:`/`MATMSG:`, `tel:`, `SMSTO:`, `geo:`, `MECARD:`/vCard | Bağlan (Android 11+ sistem penceresi), parola gizli; e-posta, arama, SMS, harita, kişi ekleme formu |
| **Metin** | Tanınmayan her şey | Kopyala, paylaş, web'de ara |

Çözme [ZXing](https://github.com/zxing/zxing) ile: çizgili (EAN, UPC, Code 128/39/93, ITF,
Codabar, GS1 DataBar) ve iki boyutlu (QR, Data Matrix, PDF417, Aztec) kodlar. Kamera
karesi dik konuma çevrilir; çizgili barkod dik tutulduysa her iki karede bir yan
çevrilmişi de denenir. Galeriden seçilen görsel daha inatçı çözülür (ters renkli kodlar dahil).

**Seri tarama:** okunan kod sonuç ekranı açılmadan geçmişe eklenir, aynı kod bir seride
ikinci kez eklenmez. Geçmiş CSV olarak paylaşılır (UTF-8 BOM'lu, `isbn13`/`isbn10`
sütunlarıyla; formül gibi başlayan metin etkisizleştirilir). Kitaplık sayımı için.

## Yapı

```
core/      saf Kotlin/JVM: ZXing çözücüsü, döndürme, ISBN/ISSN/GTIN doğrulama ve
           tireleme, içerik ayrıştırma (QR biçimleri), CSV + testler
app/       Android uygulaması: CameraX önizleme ve kare çözümleme, Compose arayüz
           (tarayıcı, sonuç, geçmiş, hakkında, dil), 14 dil
site/      proje sayfası ve 14 dilde gizlilik politikası (GitHub Pages)
tools/     ISBN aralık tablosu üreticisi, metin ve site denetimleri, gizlilik üreticisi
```

ISBN aralık tablosu ([ISBN International RangeMessage](https://www.isbn-international.org/range_file_generation))
`core/src/main/resources/.../isbn-ranges.txt` olarak pakete gömülüdür; uygulama ağa
bağlanmadığı için tireleme çevrimdışı yapılır. Yeni yayıncı aralıkları açıldıkça tazelenmeli:

```sh
python3 tools/isbn_araliklari.py
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
python3 tools/gen_privacy.py     # tools/privacy/*.json → site/gizlilik.html
```

Yayın `v*` etiketiyle başlar (`release.yml`): imzalı APK ve AAB, kaynak arşivleri ve
SHA256 özetleri GitHub Release'e eklenir; APK kameradan başka izin taşıyorsa sürüm durur.
Gerekli secret'lar: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` (alias `kodokur`).

## Gizlilik

Uygulamanın manifestinde tek izin var: `CAMERA`. Kareler bellekte çözülür ve atılır.
`INTERNET` izni yok; kitap araması gibi düğmeler kodu tarayıcıya devreder. Geçmiş
uygulamanın özel alanında durur ve Wi-Fi parolası içerebileceği için bulut yedeğine
ve cihazdan cihaza aktarıma girmez. Ayrıntı: [gizlilik politikası](https://aripdcom.github.io/kodokur/gizlilik.html).

## Lisans

Kaynak kod [GNU GPL v3](LICENSE) ile açıktır. ZXing ve AndroidX Apache Lisansı 2.0
iledir. "Kodokur" adı ve simgesi lisansa dahil değildir; çatallarsanız kendi adınızla ve
kendi simgenizle yayımlayın.
