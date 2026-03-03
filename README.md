# 🛠️ ShizuTools

**ShizuTools** adalah aplikasi utilitas Android modern yang memanfaatkan **Shizuku API** untuk memberikan kontrol sistem mendalam tanpa memerlukan akses Root. Aplikasi ini memungkinkan pengguna melakukan modifikasi sistem secara aman melalui ADB/Wireless Debugging.

Dibangun dengan antarmuka **Material 3 (Material You)** yang dinamis, mendukung tema adaptif, dan memiliki fitur **Pure Amoled** untuk efisiensi baterai maksimal.

---

## ✨ Fitur Utama

- 📱 **Screen Controller**: Ubah DPI (Density) dan Resolusi layar secara real-time tanpa perlu menyentuh komputer.
- ⚡ **Refresh Rate Control**: Atur dan paksa kecepatan refresh layar (60Hz, 90Hz, 120Hz) pada perangkat yang mendukung.
- 🔋 **Battery Health**: Informasi kesehatan baterai mendalam termasuk jumlah siklus (cycle count), kapasitas desain, voltase, dan suhu.
- 🔍 **Floating Logcat**: Monitor log sistem secara melayang (overlay) untuk keperluan debugging instan saat menjalankan aplikasi lain.
- 📈 **FPS Monitor**: Tampilkan indikator frame per second (FPS) yang akurat di atas layar.
- 🔌 **Power Menu**: Akses cepat ke mode Reboot, Recovery, Bootloader, atau Shutdown melalui perintah Shizuku.
- 🌑 **Pure Amoled Mode**: Opsi tema hitam pekat (True Black) untuk menghemat daya pada layar OLED/AMOLED.
- 🌐 **Multi-language**: Dukungan penuh untuk Bahasa Indonesia & English.

---

## 🚀 Persyaratan Sistem

* **Android 11 (API 30)** atau versi yang lebih tinggi.
* Aplikasi **[Shizuku](https://shizuku.rikka.app/)** harus terinstal dan dalam status aktif.
* Izin **Display over other apps** (diperlukan untuk fitur Floating Logcat & FPS).

---

## 🛠️ Pengembangan (Build)

Proyek ini sepenuhnya dikembangkan menggunakan **AndroidIDE** langsung di perangkat Android.

1. **Clone** repositori ini:
   ```bash
   git clone [https://github.com/dedemardiyanto10/ShizuTools.git](https://github.com/dedemardiyanto10/ShizuTools.git)
