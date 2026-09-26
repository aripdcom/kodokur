# Cihaz test protokolü

Birim testleri çözücüyü, ayrıştırıcıları ve bağlantı denetimini üretilmiş kodlarla
sınar (`./gradlew :core:test`). Gerçek dünyadaki kodlar (parlak kapak, eğri kutu,
küçük baskı) ve kamera davranışı ancak telefonda görülür. Her sürümden önce bu
liste, **imzalı release derlemesiyle** koşulur; sonuç en alttaki kütüğe yazılır.

Hazırlık: telefonda eski sürüm varsa üzerine kur (geçmiş korunmalı). Ekran
okuyucu maddeleri için TalkBack'i aç.

## 1. Kurulum ve izin

- [ ] İlk açılış: izin penceresi çıkar, arkadaki düğme "Kameraya izin ver"
- [ ] İki kez reddet → düğme "Uygulama ayarlarını aç" olur; ayarlardan izin ver,
      uygulamaya dön → kamera açılır
- [ ] İzinsizken "Fotoğraftan" çalışır

## 2. Kamera

- [ ] Kitap barkodu (EAN-13 + 5 haneli fiyat eki): yakından ve 30 cm'den; yatay ve dik
- [ ] İlaç kutusu karekodu (GS1 DataMatrix): SKT, parti, seri doğru; geçmiş bir kutuda
      kırmızı uyarı
- [ ] Market ürünü (EAN-13, EAN-8 ya da UPC)
- [ ] Parlak / kavisli ambalaj: dokunarak odak yardım ediyor mu
- [ ] Küçük barkod: yakınlaştırma düğmesi 1×→2×→4×→1×, iki parmak, çift dokunuş
- [ ] Karanlıkta fener
- [ ] Ters renkli (açık zeminde değil, koyu zeminde açık) QR — galeriden

## 3. İçerik türleri

- [ ] Wi-Fi QR: parola gizli, "Göster" çalışır, "Bağlan" sistem penceresini açar
      (kaydetmeden iptal et)
- [ ] Bağlantı QR'ı: başlıkta alan adı; `@`'li, Kiril harfli, `http://` IP adresli
      bağlantılarda uyarı kartı ve "Aç" → onay penceresi
- [ ] Kartvizit QR'ı (vCard / MECARD) → "Kişilere ekle" formu dolu açılır
- [ ] E-posta, telefon, SMS, konum QR'ları ilgili uygulamayı açar

## 4. Seri tarama ve geçmiş

- [ ] Seri kip: art arda 5 kitap; aynı kitap ikinci kez "zaten var" der, sayaç artmaz
- [ ] Geçmiş: tek kayıt sil → "Geri al" geri getirir; "Geçmişi temizle" onay ister
- [ ] CSV dışa aktar: dosya paylaşım sayfasında; tablo programında Türkçe karakterler ve
      `isbn13` sütunu doğru

## 5. Dil ve erişilebilirlik

- [ ] Hakkında → Dil: Türkçe, English ve Arapça (sağdan sola) arasında geçiş
- [ ] TalkBack: tarayıcıdaki dört düğme okunuyor (Fotoğraftan, Fener, Yakınlaştırma
      oranı, Seri); okuma yapılınca tür ve içerik duyuruluyor
- [ ] TalkBack: sonuç ekranında başlık, kopyalama düğmeleri neyi kopyaladığını söylüyor
- [ ] Yazı boyutu en büyükte: sonuç ve geçmiş ekranı taşmıyor

## 6. Hakkında

- [ ] Gizlilik politikası bağlantısı `kodokur.aripd.com/privacy.html`'i uygulamanın
      dilinde açar
- [ ] Sürüm numarası doğru

## Kütük

| Tarih | Sürüm | Cihaz | Sonuç | Not |
|---|---|---|---|---|
| 2026-09-26 | 1.1.0 (debug) | Galaxy A51, Android 13 | 3, 5 (kısmen) | Galeriden: GS1 DataMatrix (geçerli ve SKT'si geçmiş), `@` ve Kiril harfli bağlantılar, onay penceresi (IP adresli bağlantı yalnızca birim testinde); yakınlaştırma düğmesi 1→2→4→1; uiautomator ile etiket denetimi temiz. Kamera ve 1, 2, 4 kullanıcıyla koşulacak. |
