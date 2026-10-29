# AetherPulse: Penstabil Jaringan Seluler dan Nirkabel Jalur Ganda

Proyek riset terbuka, spesifikasi arsitektur protokol, dan implementasi penstabil koneksi game seluler real-time (Mobile Legends) untuk perangkat Android Samsung Galaxy A53 dan peladen perantara tepi di kawasan Eropa Utara (Helsinki, Finlandia).

---

## Pernyataan Ilmiah dan Realitas Teknis

Sebelum mengimplementasikan proyek ini, sangat penting memahami hukum fisika jaringan yang tidak dapat dilanggar oleh algoritma perangkat lunak apa pun:

1. Ketergantungan Mutlak pada Kabel Bawah Laut  
Data digital dari Indonesia menuju Eropa tidak dapat dipindahkan secara instan tanpa melewati media fisik. Jarak kabel serat optik bawah laut dari Jakarta ke Helsinki adalah sekitar 15.900 kilometer. Kecepatan cahaya dalam kaca silika serat optik adalah 204.190 kilometer per detik. Oleh karena itu, batas waktu bolak-balik (Round Trip Time atau RTT) fisik murni berada pada kisaran 160 milidetik hingga 210 milidetik. Tidak ada aplikasi yang bisa menurunkan angka latensi dasar ini menjadi hijau rendah (seperti 20 milidetik lokal) karena batasan hukum perambatan elektromagnetik.

![Peta Topologi Rute Kabel Bawah Laut](docs/assets/cable_route_map.svg)

2. Pengaruh Ukuran Paket Data terhadap Latensi  
Memperkecil ukuran paket game (misalnya dari 156 byte menjadi 78 byte) hanya menghemat waktu transmisi sebesar 0.03 milidetik pada koneksi broadband 20 Mbps standar. Penghematan 0.03 milidetik sama sekali tidak berpengaruh pada total latensi 190 milidetik. Pengurangan ukuran data bertujuan menghemat kuota, bukan menurunkan nilai milidetik ping.

3. Apa yang Sebenarnya Diselesaikan oleh AetherPulse  
Meskipun latensi dasar tidak dapat diturunkan di bawah 160 milidetik, AetherPulse memecahkan masalah utama yang dihadapi pemain game:
* Menghilangkan variasi jeda (jitter) sehingga ping stabil konstan pada angka terendah tanpa lonjakan mendadak ke 400 milidetik atau 900 milidetik.
* Mencegah hilangnya paket (packet loss) di udara dengan mengirimkan salinan data ganda secara paralel melalui Wi-Fi dan jaringan 4G/5G secara serentak.
* Melompati antrean kemacetan router rumahan (bufferbloat) menggunakan penandaan prioritas Wi-Fi Voice Queue (DSCP Expedited Forwarding).

![Grafik Telemetri Latensi dan Kestabilan](docs/assets/latency_stability_chart.svg)

---

## Arsitektur Sistem

![Diagram Alur Arsitektur AetherPulse](docs/assets/multipath_architecture_flowchart.svg)

Sistem AetherPulse terbagi menjadi dua komponen utama:

### 1. Klien Android (Samsung Galaxy A53)
* Berjalan sebagai layanan latar belakang Android VpnService.
* Mengikat antarmuka radio Wi-Fi dan radio Seluler (4G/5G) secara bersamaan melalui ConnectivityManager NetworkCallback.
* Melakukan duplikasi paket game UDP dan mengirimkannya secara serentak melalui kedua kartu radio.
* Mengaktifkan penguncian perangkat keras Wi-Fi Full Low Latency Lock untuk mencegah modul nirkabel masuk ke mode hemat daya (Power Save Polling).
* Menerapkan penandaan lalu lintas DSCP 46 (Expedited Forwarding) sehingga router memprioritaskan paket game di atas lalu lintas perangkat lain.

### 2. Peladen Perantara Tepi (Edge Relay Server di Helsinki, Finlandia)
* Menerima aliran paket ganda dari klien Android.
* Menggunakan algoritma penyaring deduplikasi Sliding Window Bitmask berkecepatan konstan O(1) untuk memilih paket yang tiba paling cepat dan membuang paket duplikat yang terlambat.
* Meneruskan muatan data game langsung ke peladen game tujuan melalui koneksi lokal berlatensi sangat rendah di Eropa.

---

## Integrasi Peer to Peer dan DePIN Blockchain

![Diagram Alur DePIN dan Pembayaran Mikro](docs/assets/depin_settlement_flowchart.svg)

Melalui pemanfaatan arsitektur terdesentralisasi, AetherPulse dapat dioperasikan tanpa ketergantungan pada entitas perusahaan VPN komersial terpusat:
1. Pasar Kuota Game Mikro (Pay-per-Match): Menggunakan kanal pembayaran mikro off-chain sehingga pemain hanya membayar biaya sekitar Rp 50 per pertandingan (~25 MB) tanpa biaya langganan bulanan.
2. Bukti Kualitas Latensi Kriptografis (Proof of Latency): Node relai diverifikasi secara independen. Jika server mengalami lonjakan ping di atas SLA yang disepakati, saldo jaminan (staking) pemilik server dipotong secara otomatis.
3. Tahan Sensor dan CGNAT Bypass: Kademlia DHT memfasilitasi pembukaan lubang NAT (UDP Hole Punching) tanpa memerlukan server STUN/TURN terpusat.

---

## Struktur Direktori Repositori

```
aetherpulse/
├── README.md
├── docs/
│   └── assets/
│       ├── cable_route_map.svg
│       ├── multipath_architecture_flowchart.svg
│       ├── latency_stability_chart.svg
│       └── depin_settlement_flowchart.svg
├── RESEARCH_PAPERS/
│   ├── NETWORK_PHYSICS_AND_LATENCY.md
│   ├── ARCHITECTURE_SPECIFICATION.md
│   └── BLOCKCHAIN_P2P_DEPIN_ANALYSIS.md
├── server/
│   ├── go/
│   │   ├── cmd/server/main.go
│   │   ├── internal/protocol/packet.go
│   │   ├── internal/dedup/sliding_window.go
│   │   ├── internal/fec/xor_fec.go
│   │   ├── internal/relay/udp_relay.go
│   │   ├── go.mod
│   │   └── Dockerfile
│   └── python/
│       ├── relay_server.py
│       └── requirements.txt
├── android/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── res/layout/activity_main.xml
│   │   │   └── java/com/bridgeplay/stabilizer/
│   │   │       ├── service/BridgeVpnService.kt
│   │   │       ├── network/DualNetworkBinder.kt
│   │   │       ├── protocol/PacketMultiplexer.kt
│   │   │       ├── protocol/FecEncoder.kt
│   │   │       ├── optimization/WifiLatencyOptimizer.kt
│   │   │       └── ui/MainActivity.kt
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── deploy/
    ├── docker-compose.yml
    └── setup_vps.sh
```

---

## Panduan Pemasangan dan Menjalankan Sistem

### Bagian A: Menjalankan Peladen di VPS Finlandia

Peladen dapat dijalankan pada sistem operasi Linux Ubuntu atau Debian di pusat data Helsinki (misalnya Hetzner Cloud atau penyedia lainnya).

1. Eksekusi skrip optimasi kernel:
```bash
sudo bash deploy/setup_vps.sh
```

2. Jalankan peladen perantara:

Pilihan Menggunakan Python (Instan tanpa kompilasi):
```bash
python3 server/python/relay_server.py --listen-port 8910 --target-host 127.0.0.1 --target-port 30000
```

Pilihan Menggunakan Docker:
```bash
cd deploy
docker compose up -d
```

### Bagian B: Menjalankan Aplikasi di Samsung Galaxy A53

1. Buka folder android/ pada Android Studio.
2. Hubungkan Samsung Galaxy A53 menggunakan kabel data USB dan aktifkan USB Debugging.
3. Lakukan build dan instalasi aplikasi ke ponsel.
4. Buka aplikasi AetherPulse Stabilizer:
   * Masukkan alamat IP publik VPS Finlandia tempat peladen dijalankan.
   * Masukkan port 8910.
   * Tekan tombol "Aktifkan Penstabil Jaringan".
   * Berikan persetujuan dialog izin VPN Android.
5. Pastikan koneksi Wi-Fi dan Paket Data Seluler keduanya dalam keadaan aktif di ponsel. Sistem secara otomatis menggabungkan kedua radio tersebut.
6. Buka game Mobile Legends dan nikmati koneksi yang stabil bebas dari lonjakan ping merah.

---

## Riset Ilmiah Mendalam

Laporan teknis lengkap dan analisis matematis dapat dibaca langsung pada folder RESEARCH_PAPERS:
1. RESEARCH_PAPERS/NETWORK_PHYSICS_AND_LATENCY.md: Pembuktian matematis kecepatan rambat serat optik dan dekonstruksi penundaan transmisi.
2. RESEARCH_PAPERS/ARCHITECTURE_SPECIFICATION.md: Spesifikasi detail protokol Multipath Packet Duplication, Forward Error Correction, dan mitigasi WMM.
3. RESEARCH_PAPERS/BLOCKCHAIN_P2P_DEPIN_ANALYSIS.md: Analisis arsitektur DePIN, Kademlia DHT, dan pembayaran mikro game off-chain.

---

## Lisensi

Proyek ini dirilis di bawah lisensi publik terbuka MIT License. Siapa pun bebas menggunakan, mempelajari, dan mengembangkan lebih lanjut kode sumber ini untuk keperluan riset dan stabilitas jaringan.
