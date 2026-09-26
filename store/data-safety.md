# Veri güvenliği formu (Play Console → Uygulama içeriği → Veri güvenliği)

**Uygulamanız, gerekli kullanıcı veri türlerinden herhangi birini topluyor ya da
paylaşıyor mu?** → **Hayır.**

Gerekçe (form bir açıklama isterse):

- Manifestte `INTERNET` izni yok; uygulama hiçbir sunucuya veri gönderemez. Play'in
  tanımında "toplama", verinin cihazdan çıkarılmasıdır; burada hiçbir veri cihazdan
  çıkmaz.
- **Kamera:** kareler bellekte çözülür ve atılır; fotoğraf ya da video saklanmaz.
- **Galeriden okuma:** sistem fotoğraf seçicisi yalnızca seçilen tek görseli verir,
  depolama izni yok.
- **Geçmiş:** okunan içerik, biçim ve zaman uygulamanın özel alanında durur; bulut
  yedeğine ve cihazdan cihaza aktarıma girmez (`data_extraction_rules.xml`).
- **Paylaşım ve dışa aktarma** kullanıcının başlattığı, sistemin paylaşım sayfası
  üzerinden yapılan aktarımlardır. Play bunları "paylaşma" saymaz: kullanıcı başka
  bir uygulamaya kendisi gönderir.
- **Dış bağlantılar** (Open Library, Open Food Facts, web araması, bağlantı açma)
  tarayıcıya niyetle devredilir; Kodokur o isteği yapmaz.

Bu cevapla form "Veri toplanmıyor" ve "Veri paylaşılmıyor" olarak yayımlanır;
şifreleme ve silme talebi soruları sorulmaz.

## İzinler

| İzin | Neden | Beyan gerekir mi |
|---|---|---|
| `CAMERA` | kod okumak | Hayır (hassas izin beyanı listesinde değil) |

Bunu `release.yml` doğrular: imzalı APK kameradan başka
izin taşırsa sürüm yayımlanmaz.
