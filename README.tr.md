<div align="center">

# 📍 Waypoint Shift

**Jetpack Compose ve Material 3 ile geliştirilmiş modern, şeffaf ve root gerektirmeyen Android sahte konum (mock location) uygulaması.**

[![Dil: Türkçe](https://img.shields.io/badge/Dil-T%C3%BCrk%C3%A7e-red.svg)](README.tr.md)
[![Language: English](https://img.shields.io/badge/Language-English-blue.svg)](README.md)
[![Android](https://img.shields.io/badge/Android-API%2023--36-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Android CI](https://github.com/emi-ran/waypoint-shift/actions/workflows/ci.yml/badge.svg)](https://github.com/emi-ran/waypoint-shift/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![OpenStreetMap](https://img.shields.io/badge/Harita-OpenStreetMap%20%2F%20osmdroid-7EBC6F.svg?logo=openstreetmap&logoColor=white)](https://www.openstreetmap.org)
[![Gizlilik](https://img.shields.io/badge/Gizlilik-%25100%20Yerel%20%26%20S%C4%B1f%C4%B1r%20Takip-brightgreen.svg)](#gizlilik-ve-etik-ilkeler)

<p align="center">
  <a href="#-öne-çıkan-özellikler">Öne Çıkan Özellikler</a> •
  <a href="#-cihaz-kurulumu-ve-hızlı-başlangıç">Hızlı Başlangıç</a> •
  <a href="#-nasıl-çalışır">Nasıl Çalışır</a> •
  <a href="#-derleme-ve-geliştirme">Derleme</a> •
  <a href="#-gizlilik-ve-etik-ilkeler">Gizlilik</a> •
  <a href="#-teknoloji-yığını">Teknoloji Yığını</a>
</p>

---

</div>

## 📖 Genel Bakış

**Waypoint Shift**, Android geliştiricileri, test uzmanları ve ileri düzey kullanıcılar için tasarlanmış; gizlilik odaklı ve şeffaf bir sahte konum (mock location) simülasyon uygulamasıdır. Cihazınızı rootlamanıza veya üçüncü parti müdahaleci araçlar (hook vb.) kullanmanıza gerek kalmadan GPS koordinatlarını güvenilir bir şekilde simüle etmenizi sağlar.

Geleneksel yanıltma (spoofing) uygulamalarının aksine Waypoint Shift, tamamen **şeffaflık, modern Android standartları ve kullanıcı kontrolü** temel alınarak geliştirilmiştir:
- **Root, hook veya hile yok**: Yalnızca resmi Android ve Google Play Hizmetleri Konum API'lerini kullanır.
- **Tasarımı gereği şeffaf**: Sahte konum durumunu gizlemez, `Location.isMock()` bayrağını manipüle etmez ve sistem algılamalarını atlatmaya çalışmaz.
- **Gizlilik öncelikli**: Sıfır telemetri, sıfır analiz (analytics), sıfır reklam ve sıfır bulut sunucu. Koordinatlarınız cihazınızdan asla dışarı çıkmaz.
- **API Anahtarı gerektirmez**: OpenStreetMap (osmdroid) ve dahili Nominatim adres araması sayesinde herhangi bir harita API anahtarına ihtiyaç duymadan doğrudan çalışır.

---

## ✨ Öne Çıkan Özellikler

- 🗺️ **Etkileşimli OpenStreetMap Arayüzü**: Haritada istediğiniz bir noktaya dokunarak veya konum işaretçisini sürükleyip bırakarak sezgisel olarak koordinat belirleme.
- 🔍 **Adres ve Konum Arama**: Entegre OpenStreetMap Nominatim desteğiyle şehir, sokak veya önemli noktaları anında arama.
- 🎯 **Yüksek Hassasiyetli Manuel Giriş**: Standart 6 basamaklı ondalık doğrulama (`0.000001°` hassasiyet, ~11 cm) ile enlem ve boylam değerlerini doğrudan yazabilme.
- ⚡ **Çift Motorlu Konum Yayını**:
  - Birincil: Google Play Hizmetleri `FusedLocationProviderClient`.
  - Yedek (Fallback): Yerel Android `LocationManager` sağlayıcısı (Google servisleri bulunmayan AOSP, de-Googled ROM'lar veya emülatörler için).
- 🔔 **Sürekli Ön Plan Servisi (Foreground Service)**: Aktif koordinatları canlı gösteren ve tek dokunuşla durdurma (**Stop**) imkanı sunan kalıcı bildirim eşliğinde 1 saniyelik periyotlarla kesintisiz konum yayını.
- ⭐ **Favoriler ve Geçmiş Yönetimi**:
  - Özel etiketlerle sık kullanılan koordinatları kaydetme (60 karaktere kadar).
  - Son kullanılan konumları otomatik, tekilleştirilmiş ve sıralı biçimde hafızada tutma (20 öğeye kadar).
  - Jetpack DataStore Preferences ile yerel depolama (bulut yedeklemelerinden muaf).
- 🛡️ **Kapsamlı Sistem Tanılamaları**: Geliştirici Seçenekleri, Sahte Konum AppOp yetkisi, Google Play servisleri durumu ve Android 13+ bildirim izinleri için anında ve yol gösterici durum kontrolleri.

---

## 🚀 Cihaz Kurulumu ve Hızlı Başlangıç

Waypoint Shift'i Android cihazınızda veya emülatörünüzde kullanmak için aşağıdaki adımları izleyin:

### 1. Geliştirici Seçeneklerini Etkinleştirin
1. Cihazınızın **Ayarlar** menüsünü açın.
2. **Telefon Hakkında** (veya *Sistem > Telefon Hakkında*) bölümüne gidin.
3. *"Artık bir geliştiricisiniz!"* mesajını görene kadar **Derleme Numarası** (Build Number) seçeneğine 7 kez arka arkaya dokunun.

### 2. Sahte Konum Uygulamasını Seçin
1. **Ayarlar** > **Sistem** > **Geliştirici Seçenekleri** yolunu izleyin (veya arama kutusuna *Geliştirici Seçenekleri* yazın).
2. **Hata Ayıklama** (Debugging) başlığına kaydırın.
3. **Sahte konum uygulaması seç** (Select mock location app) seçeneğine dokunun.
4. Listeden **Waypoint Shift**'i seçin.

### 3. Bildirim İznini Verin (Android 13+)
- Android 13 (API 33) ve sonraki sürümlerde uygulama açıldığında bildirim iznini onaylayın.
- *Neden gerekli?* Sahte konumun çalıştığı süre boyunca ekranda bildirim olarak görünmesi ve kullanıcı tarafından şeffafça kontrol edilebilmesi Android standartları gereğidir.

### 4. Konumu Simüle Edin
1. Harita üzerinden dokunarak, **arama çubuğunu** kullanarak veya **Enlem / Boylam** alanlarını doldurarak hedef koordinatınızı seçin.
2. **Start** butonuna dokunarak simülasyonu başlatın.
3. Simülasyon devam ederken yeni bir nokta seçip **Update** ile güncelleyebilir veya uygulama içinden / bildirim panelinden **Stop** butonuna basarak anında sonlandırabilirsiniz.

---

## 🛠️ Teknoloji Yığını ve Mimari

Waypoint Shift, modern Android Mimari Bileşenleri ve reaktif durum yönetimi prensiplerine tam uyumludur:

| Bileşen | Teknoloji | Açıklama |
|---|---|---|
| **Programlama Dili** | Kotlin 2.0+ | Modern, tip güvenli ve performanslı |
| **Kullanıcı Arayüzü** | Jetpack Compose & Material 3 | Dinamik renk ve Gece Modu destekli bildirimsel UI |
| **Harita ve Arama** | osmdroid & Nominatim | Açık kaynak harita render motoru ve coğrafi kodlama (API key gerektirmez) |
| **Konum Altyapısı** | Google Play Services & Android LocationManager | AOSP yedekleme mekanizmalı fused konum mimarisi |
| **Yerel Depolama** | Jetpack DataStore Preferences | Asenkron, iş parçacığı güvenli yerel veri saklama |
| **Eşzamanlılık** | Kotlin Coroutines & StateFlow | Reaktif arayüz akışları ve arka plan iş parçacıkları |
| **Arka Plan Servisi** | Android `specialUse` Foreground Service | Sürekli, güvenilir ve kullanıcıya görünür konum yayını |

---

## 💻 Derleme ve Geliştirme

### Gereksinimler
- **JDK 17**
- **Android Studio Ladybug (veya daha yeni bir sürüm)** ya da Android SDK Komut Satırı Araçları
- **Android SDK Platform 36** ve **Build-Tools 36.0.0**

### Projeyi Klonlama
```bash
git clone https://github.com/emi-ran/waypoint-shift.git
cd waypoint-shift
```

### Komut Satırından Derleme (Gradle)
İşletim sisteminize göre Gradle wrapper komutunu çalıştırın:

```bash
# Linux / macOS üzerinde:
./gradlew assembleDebug

# Windows üzerinde (PowerShell veya Komut İstemi):
.\gradlew.bat assembleDebug
```

Derlenen APK dosyası şu dizinde hazır olacaktır:
`app/build/outputs/apk/debug/app-debug.apk`

### Birim Testleri ve Kod İncelemesi (Lint)
```bash
# Tüm birim testlerini çalıştır
./gradlew testDebugUnitTest

# Belirli bir test sınıfını çalıştır
./gradlew :app:testDebugUnitTest --tests '*MainViewModelTest'
./gradlew :app:testDebugUnitTest --tests '*MockLocationPublisherTest'
./gradlew :app:testDebugUnitTest --tests '*LocationRepositoryTest'

# Statik kod analizini (lint) çalıştır
./gradlew lintDebug
```

### Sürekli Entegrasyon

GitHub Actions, `main` dalına gönderilen push ve pull request'lerde veya elle tetiklendiğinde birim testleri, Android lint ve debug APK derlemesini çalıştırır. Ubuntu 24.04 üzerinde JDK 17 ile SDK Platform 36 / Build-Tools 36.0.0 kullanılır; SDK kurulumu ve lint hataları işi başarısız yapar.

Başarılı koşularda `waypoint-shift-debug-apk` çıktısı 14 gün saklanır. Test ve lint raporları, başarısız koşular dahil mevcut olduklarında `test-and-lint-reports` olarak 7 gün saklanır. Workflow sürüm yayımlamaz veya uygulamayı dağıtmaz; cihaz/enstrümantasyon testleri ayrı yürütülür.

### İmzalı sürümler

GitHub Release yayımlandığında `.github/workflows/release.yml` çalışır. Etiket `vMAJOR.MINOR.PATCH` biçiminde olmalı, `main` geçmişindeki bir commit'i göstermeli ve `app/build.gradle.kts` içindeki `versionName` ile eşleşmelidir. Her yeni sürümde `versionCode` artırılmalı, `versionName` güncellenmelidir; Android güncellemeleri daha yüksek sürüm kodu ve aynı imzalama anahtarı gerektirir.

GitHub'daki `release` environment, `v*` tag'lerine izin verir ve dört secret kullanır: `ANDROID_KEYSTORE_BASE64` (Base64 kodlu PKCS12 dosyası), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Asıl keystore'u repo dışında ve şifreli yedekleriyle saklayın. Base64 şifreleme değildir; kimlik bilgilerini Gradle veya YAML dosyalarına yazmayın.

Workflow testleri ve debug lint'i çalıştırır, küçültülmüş imzalı release derler, release lint'i çalıştırır ve APK imzasını workflow'daki beklenen sertifika SHA-256 parmak iziyle doğrular. `waypoint-shift-vX.Y.Z.apk` ile `.sha256` dosyasını yayımlanan release'e ekler. Derleme başarısızsa release bu dosyalar olmadan kalır; raporları inceleyip hatayı giderdikten sonra workflow'u yeniden çalıştırın. Geçici keystore imzalamadan sonra ve son temizlik adımında silinir. Normal CI imzalama secret'larına erişmez.

Yayımlamadan doğrulamak için mevcut sürüm tag'inde `gh workflow run release.yml --ref v0.1.1` çalıştırılabilir. Manuel koşu yalnız indirilebilir artifact üretir; GitHub Release oluşturmaz veya değiştirmez. `main` üzerinde manuel tetiklenirse release işi atlanır. Environment onayı açıksa iş başlamadan onay gerekir. Sürüm tag'i oluşturma yetkisini repo kuralları/yetkileriyle sınırlandırın; tag kalıbı tek başına yetkilendirme sağlamaz.

Yerel release derlemesi `ANDROID_KEYSTORE_PATH` ve yukarıdaki üç şifre/alias ortam değişkenini gerektirir. İmzalama ayarları eksikse release derlemesi hata verir; debug derlemesi secret gerektirmez. Farklı anahtarla imzalı debug kurulumdan release'e geçerken debug uygulamasını kaldırmak gerekir; yerel uygulama verileri silinir. Cihaz/enstrümantasyon doğrulaması CI dışında yürütülür.

---

## 🔒 Gizlilik ve Etik İlkeler

Waypoint Shift, kullanıcı gizliliği ve etik yazılım geliştirme standartlarına sıkı sıkıya bağlıdır:

- **Gerçek Konuma Erişilmez**: Uygulama sizin gerçek coğrafi konumunuzu öğrenmek için hassas (fine), yaklaşık (coarse) veya arka plan konum izinleri talep etmez.
- **Ağ Üzerinden Takip Yok**: Projede analiz (analytics), hata izleme (crash reporting), telemetri veya reklam SDK'sı yer almaz.
- **Yerel Veri İzolasyonu**: Kaydettiğiniz favoriler ve geçmiş koordinatlar cihazınızdaki özel uygulama dizininde saklanır ve Android Bulut Yedeklemesine dahil edilmez (`allowBackup="false"`).
- **Sistem Kurallarına Tam Uyum**: Cihaz ayarlarını izinsiz değiştirmez, sahte konum bayrağını gizlemeye çalışmaz ve Android işletim sisteminin yaşam döngüsü kurallarına tam riayet eder.

---

## 📋 Sistem Gereksinimleri ve Uyumluluk

- **Desteklenen Minimum Android Sürümü**: Android 6.0 Marshmallow (API düzeyi 23)
- **Hedeflenen Android Sürümü**: Android 16 (API düzeyi 36)
- **Root Gereksinimi**: Yok (Orijinal fabrika yazılımlarında ve özel ROM'larda sorunsuz çalışır)
- **Google Play Hizmetleri**: İsteğe bağlı (Play Hizmetleri bulunmayan cihazlarda otomatik olarak yerel LocationManager devreye girer)

---

## 📄 Belgeler ve Lisans

Bu proje [GNU Genel Kamu Lisansı v3.0 (GPLv3)](LICENSE) ile lisanslanmıştır.

Mimari detaylar, TDD geliştirme planları ve sürüm kriterleri için:
[`docs/plans/2026-08-24-waypoint-shift.md`](docs/plans/2026-08-24-waypoint-shift.md)

Değişiklik geçmişi ve sürüm notları için [`CHANGELOG.md`](CHANGELOG.md) dosyasını inceleyebilirsiniz.
English documentation is available in [`README.md`](README.md).
