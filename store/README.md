# Play mağaza paketi

Play Console'a elle aktarılan her şey burada. Metinleri `tools/check_store.py`
denetler (karakter sınırları, 14 dilin tamamı, görünmez karakter); CI her itmede
koşturur.

```
play/<dil>/title.txt          uygulama adı          ≤ 30 karakter
play/<dil>/short.txt          kısa açıklama         ≤ 80
play/<dil>/full.txt           tam açıklama          ≤ 4000
play/<dil>/notes-<sürüm>.txt  sürüm notu            ≤ 500
graphics/icon-512.png         yüksek çözünürlüklü simge (512×512)
graphics/feature-1024.png     öne çıkan görsel (1024×500); metinsiz, bütün dillerde aynı
screenshots/en/               telefon ekran görüntüleri, yalnız İngilizce (1080×2160: Play
                              uzun kenarın kısa kenarın en fazla 2 katı olmasını ister;
                              durum ve gezinme çubukları kırpılmış)
data-safety.md                Veri güvenliği formunun cevapları
content-rating.md             İçerik derecelendirme (IARC) ve hedef kitle cevapları
checklist.md                  yayın adımları
```

Görseller SVG'den üretilir: `inkscape graphics/icon-512.svg --export-type=png -w 512 -h 512 …`

## Dil klasörü → Play Console yerel ayarı

| Klasör | Play | Klasör | Play |
|---|---|---|---|
| `en` | English (United States) – en-US | `it` | Italian – it-IT |
| `tr` | Turkish – tr-TR | `da` | Danish – da-DK |
| `de` | German – de-DE | `sv` | Swedish – sv-SE |
| `fr` | French (France) – fr-FR | `nb` | Norwegian – no-NO |
| `nl` | Dutch – nl-NL | `fi` | Finnish – fi-FI |
| `es` | Spanish (Spain) – es-ES | `ru` | Russian – ru-RU |
| `pt` | Portuguese (Brazil) – pt-BR | `ar` | Arabic – ar |

Varsayılan dil **en-US**. Sürüm notları Play'e tek kutuda, dil etiketleriyle
yapıştırılır (`<tr-TR> … </tr-TR>`); her dilin metni kendi `notes-<sürüm>.txt`
dosyasında.
