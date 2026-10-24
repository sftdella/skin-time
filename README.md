<div align="center">

# 🌸 SkinTime

**Teman setia untuk skincare routine harianmu.**

Atur rutinitas pagi & malam, dapatkan rekomendasi perawatan kulit sesuai cuaca, dan pantau konsistensimu lewat kalender, streak, dan achievement.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

</div>

---

## ✨ Fitur

### 🏠 Home
- Sapaan sesuai waktu (pagi, siang, sore, malam) dan quote self-care yang berganti-ganti
- **Cuaca hari ini**: suhu, kelembapan, dan indeks UV berdasarkan lokasi kamu (OpenWeatherMap)
- **Skin Recommendation**: saran perawatan kulit yang menyesuaikan suhu, kelembapan, dan UV
- Progress harian & mingguan, plus achievement terbaru
- **Reminder** skincare yang bisa diatur jamnya

### 🧴 Routine
- Checklist **Morning Routine** dan **Night Routine**, step per step
- **Kelola Checklist**: tambah, edit, hapus, atur urutan, dan tandai step sebagai opsional
- **Kelola Aktivitas**: jadwalkan aktivitas tambahan (misalnya masker atau eksfoliasi) di hari tertentu

### 📅 Calendar
- Kalender bulanan yang menandai hari-hari yang punya sesi routine
- Detail sesi per tanggal: Morning, Night, dan aktivitas tambahan

### 📊 Analytics
- Grafik mingguan sesi yang selesai (7 hari terakhir)
- Step skincare yang paling sering kamu lakukan
- Statistik: total hari aktif dan **streak** saat ini
- **Achievement**: Morning Hero, Hydration Queen, Konsisten 1 Minggu, dan lainnya 🏆

---

## 🛠️ Tech Stack

| Kategori      | Teknologi                                             |
|---------------|-------------------------------------------------------|
| Bahasa        | Kotlin                                                |
| UI            | Jetpack Compose + Material 3                          |
| Navigasi      | Navigation Compose                                    |
| Database      | SQLite (`SQLiteOpenHelper`)                           |
| Cuaca         | [OpenWeatherMap API](https://openweathermap.org/api)  |
| Notifikasi    | `AlarmManager` + `BroadcastReceiver` (tetap aktif setelah reboot) |
| Min / Target SDK | 26 (Android 8.0) / 36                             |

---

## 📂 Struktur Proyek

```
app/src/main/java/com/della/skintime/
├── MainActivity.kt          # Entry point + navigasi & bottom bar
├── SkinTimeApp.kt           # Application class
├── data/db/                 # SQLite database & model data
├── receiver/                # AlarmReceiver & BootReceiver untuk reminder
├── ui/
│   ├── home/                # Halaman Home (cuaca, progress, reminder)
│   ├── routine/             # Checklist routine, kelola step & aktivitas
│   ├── calendar/            # Kalender riwayat routine
│   ├── analytics/           # Statistik, grafik & achievement
│   └── theme/               # Warna, tipografi & tema
└── utils/                   # ReminderManager, StreakManager, WeatherManager
```

---

## 🚀 Cara Menjalankan

1. **Clone repository**
   ```bash
   git clone https://github.com/sftdella/skin-time.git
   ```
2. **Buka di Android Studio** (versi terbaru disarankan).
3. **Tambahkan API key OpenWeatherMap.** Daftar gratis di [openweathermap.org](https://openweathermap.org/api), lalu tambahkan baris ini ke file `local.properties` di root proyek:
   ```properties
   OPENWEATHER_API_KEY=api_key_kamu
   ```
   > `local.properties` tidak ikut ter-upload ke Git, jadi API key kamu tetap aman.
4. **Sync Gradle**, lalu jalankan di emulator atau perangkat dengan Android 8.0 ke atas.

### Izin yang dibutuhkan
| Izin | Kegunaan |
|------|----------|
| Lokasi | Mengambil data cuaca di lokasi kamu |
| Internet | Mengakses OpenWeatherMap API |
| Notifikasi & Exact Alarm | Mengirim reminder skincare tepat waktu |
| Boot Completed | Menjadwalkan ulang reminder setelah HP dinyalakan ulang |

---

## 👩‍💻 Developer

Dibuat dengan 💕 oleh **Seftia Della**: [@sftdella](https://github.com/sftdella)

<div align="center">

*"Consistency is the key to great skin."* ✨

</div>
