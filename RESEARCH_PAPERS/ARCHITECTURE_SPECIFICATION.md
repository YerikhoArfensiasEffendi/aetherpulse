# Spesifikasi Arsitektur Sistem Stabilisator Jaringan AetherPulse

Penulis: Divisi Rekayasa Protokol AetherPulse  
Target Sistem: Android Client (Samsung A53) dan Linux Edge Relay  
Status: Spesifikasi Standar Implementasi  

---

## 1. Topologi dan Alur Data End to End

Sistem AetherPulse dirancang khusus untuk mengatasi ketidakstabilan transmisi nirkabel dan rute interkontinental jarak jauh. Topologi sistem terdiri dari dua komponen utama:
1. AetherPulse Android Client: Berjalan sebagai layanan latar belakang VpnService pada ponsel pintar Android pengguna.
2. AetherPulse Edge Relay Server: Berjalan pada peladen virtual (VPS) berlokasi strategis di kawasan Eropa Utara (Helsinki, Finlandia) yang memiliki peering langsung ke pusat data game.

![Diagram Alur Arsitektur AetherPulse](../docs/assets/multipath_architecture_flowchart.svg)

---

## 2. Protokol Duplikasi Paket Jalur Ganda (Multipath Packet Duplication)

### 2.1 Konsep Teoretis Pengurangan Jitter Menuju Nol

Secara umum, koneksi internet Wi-Fi rumahan memiliki latensi rata-rata yang baik, namun sering mengalami lonjakan mendadak akibat gangguan gelombang radio lokal. Di sisi lain, koneksi seluler 4G LTE memiliki latensi dasar yang sedikit lebih tinggi namun tidak terpengaruh oleh kontensi router lokal rumah.

Jika latensi Wi-Fi pada waktu t dinyatakan dengan L wifi(t) dan latensi 4G dinyatakan dengan L cell(t), maka paket yang dikirimkan secara serentak ke dua jalur fisik berbeda akan tiba di peladen tujuan pada waktu:

L efektif(t) = Minimum( L wifi(t), L cell(t) )

Korelasi matematis probabilitas kehilangan paket (packet loss) juga menurun secara eksponensial. Jika probabilitas kehilangan paket pada Wi-Fi adalah P(W) = 3 persen dan pada seluler adalah P(C) = 2 persen, maka probabilitas kedua salinan paket hilang secara bersamaan adalah:

P gabungan = P(W) × P(C) = 0.03 × 0.02 = 0.0006 (0.06 persen)

Tingkat keandalan jaringan meningkat dari 97 persen menjadi 99.94 persen tanpa perlu melakukan transmisi ulang.

![Grafik Telemetri Latensi dan Kestabilan](../docs/assets/latency_stability_chart.svg)

### 2.2 Format Header Protokol AetherPulse

Setiap paket UDP dari game dienkapsulasi ke dalam header ringkas AetherPulse berukuran 10 byte untuk menjaga efisiensi transmisi:
* Tanda Pengenal Protokol (16 bit): Nilai tetap 0x4250 ("BP" / BridgePulse) untuk validasi integritas paket.
* Jenis Paket (8 bit): 0x01 untuk Paket Data, 0x02 untuk Paket Paritas FEC, 0x03 untuk Paket Heartbeat Keepalive.
* Tanda Bendera (8 bit): Menyimpan metadata jalur (Bit 0: Jalur Wi-Fi, Bit 1: Jalur Seluler).
* Panjang Data (16 bit): Ukuran muatan data asli dalam satuan byte.
* Nomor Urut Paket (32 bit): Angka urut unik inkremental untuk mendeteksi paket duplikat pada peladen penerima.

---

## 3. Algoritma Deduplikasi Cepat pada Sisi Peladen

Peladen Edge Relay di Helsinki menerima paket dari dua jalur jaringan secara acak. Untuk memastikan proses deduplikasi tidak membebani memori CPU peladen, sistem menggunakan algoritma bitmask sliding window dengan kompleksitas waktu konstan O(1).

### 3.1 Logika Operasi Sliding Window Bitmask

Peladen mempertahankan dua variabel status untuk setiap sesi aktif:
1. max_seq_received: Nomor urut tertinggi yang pernah berhasil diterima.
2. window_mask: Angka biner 64 bit (uint64) yang melacak status kedatangan 64 nomor urut terakhir sebelum max_seq_received.

Aturan penerimaan paket dengan nomor urut seq:
* Kasus A (Paket Lebih Baru): Jika seq > max_seq_received, hitung selisih jeda diff = seq - max_seq_received.
  * Geser bitmask ke kiri sebanyak diff posisi.
  * Pasang bit 0 pada posisi paling rendah (menandakan nomor urut ini baru saja diterima).
  * Perbarui max_seq_received = seq.
  * Teruskan paket langsung ke server game.
* Kasus B (Paket Duplikat atau Paket Lampau): Jika seq <= max_seq_received, hitung selisih diff = max_seq_received - seq.
  * Jika diff >= 64, paket sudah terlalu usang di luar jendela pantau, buang paket.
  * Jika diff < 64, periksa status bit ke diff pada window_mask.
    * Jika bit bernilai 1, paket ini sudah pernah diproses sebelumnya (salinan duplikat yang datang terlambat), segera buang paket.
    * Jika bit bernilai 0, ubah bit menjadi 1 dan teruskan paket (paket datang terlambat namun belum pernah diproses).

Pendekatan ini beroperasi murni pada register CPU tanpa alokasi memori dinamis dan tanpa membutuhkan penguncian mutex berat.

---

## 4. Mekanisme Forward Error Correction (FEC) Instan

Jika kedua jalur transmisi nirkabel maupun kabel bawah laut mengalami gangguan serentak pada satu frame tertentu, protokol FEC AetherPulse merekonstruksi paket yang hilang tanpa perlu meminta pengiriman ulang (retransmission request).

### 4.1 Skema Paritas Sliding Block XOR

Setiap blok transmisi terdiri dari K paket data asli dan M paket paritas redundan:
* Untuk setiap kelompok 4 paket data (K = 4), sistem menghasilkan 1 paket paritas (M = 1).
* Paket paritas dihitung melalui operasi biner eksklusif (XOR):

Paket Paritas = Paket(N) XOR Paket(N+1) XOR Paket(N+2) XOR Paket(N+3)

Jika salah satu dari empat paket tersebut hilang di tengah transmisi (misalnya Paket N+2 hilang), peladen penerima dapat memulihkan paket tersebut secara instan dalam waktu nol milidetik dengan persamaan:

Paket(N+2) = Paket(N) XOR Paket(N+1) XOR Paket(N+3) XOR Paket Paritas

Keuntungan utama:
* Mengeliminasi penundaan RTT sebesar 200 milidetik yang biasanya dibutuhkan oleh protokol TCP atau ARQ konvensional untuk meminta paket hilang.
* Menjaga kontinuitas aliran data game agar frame tidak mengalami kemacetan (zero stutter).

---

## 5. Optimalisasi Tingkat Kernel dan Hardware pada Samsung Galaxy A53

Ponsel pintar Samsung Galaxy A53 ditenagai oleh prosesor Samsung Exynos 1280 dengan modem 5G dan modul Wi-Fi terintegrasi. Untuk memaksimalkan performa latensi, aplikasi AetherPulse menerapkan serangkaian konfigurasi tingkat sistem:

### 5.1 Penguncian Mode Latensi Rendah Wi-Fi (Full Low Latency Lock)

Secara default, Android mengizinkan modul Wi-Fi masuk ke mode hemat daya setelah beberapa detik tidak ada lonjakan data unduhan masif. Aplikasi AetherPulse mengaktifkan pengunci khusus melalui Android WifiManager API:

```kotlin
val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
val wifiLock = wifiManager.createWifiLock(
    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
    "AetherPulse:LowLatencyMode"
)
wifiLock.acquire()
```

Dampak langsung penguncian ini:
* Menonaktifkan mekanisme 802.11 Power Save Polling pada antarmuka wlan0.
* Mengurangi jeda internal modul nirkabel dari rata-rata 25 milidetik menjadi kurang dari 3 milidetik.

### 5.2 Penandaan Paket DSCP untuk Prioritas Wi-Fi WMM Voice Queue

Router Wi-Fi modern mendukung standar Wi-Fi Multimedia (IEEE 802.11e WMM) yang membagi lalu lintas udara menjadi empat antrean prioritas:
1. AC_VO (Voice): Prioritas tertinggi, waktu tunggu transmisi udara paling singkat.
2. AC_VI (Video): Prioritas menengah tinggi.
3. AC_BE (Best Effort): Prioritas standar (seluruh lalu lintas web biasa).
4. AC_BK (Background): Prioritas terendah (unduhan latar belakang).

AetherPulse menandai seluruh soket pengiriman UDP dengan byte Differentiated Services Code Point (DSCP) bernilai 46 (0x2E), yang dalam format byte ToS bernilai 0xB8 (Expedited Forwarding).
* Nilai ini secara otomatis dipetakan oleh firmware chipset Wi-Fi dan router rumah ke antrean tertinggi AC_VO.
* Paket game secara instan melompati seluruh antrean perangkat lain di rumah yang sedang menonton YouTube atau mengunduh berkas, sepenuhnya melenyapkan fenomena bufferbloat lokal.

### 5.3 Pemisahan Jalur Berdasarkan Port Aplikasi (Selective Split Tunneling)

Hanya paket UDP yang berkaitan dengan lalu lintas game (rentang port UDP 5000 hingga 6500 dan port 30000 hingga 30200 yang umum digunakan oleh Mobile Legends) yang akan digandakan ke jalur seluler dan Wi-Fi. Lalu lintas aplikasi lain seperti pesan instan, unduhan berkas, dan layanan perbankan tetap dialihkan langsung melalui koneksi internet standar tanpa melewati jalur enkapsulasi untuk menghemat pemakaian kuota data seluler.
