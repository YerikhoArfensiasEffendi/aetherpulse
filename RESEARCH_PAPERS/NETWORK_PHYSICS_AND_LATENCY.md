# Analisis Fisika Jaringan dan Batas Teoretis Latensi Antarbenua

Penulis: Divisi Riset Sistem Jaringan AetherPulse  
Target Sistem: Android OS dan Jaringan Seluler serta Nirkabel  
Klasifikasi Dokumen: Dokumen Teknis Terbuka  

---

## Ringkasan Eksekutif

Terdapat kesalahpahaman umum dalam rekayasa jaringan seluler bahwa latensi tinggi antarbenua, seperti koneksi dari Indonesia ke server di kawasan Eropa (Finlandia), dapat dipangkas secara drastis melalui kompresi data tingkat aplikasi atau pengalihan rute perangkat lunak murni tanpa perantara fisik. Dokumen ini menyajikan pembuktian matematis dan empiris mengenai batas fundamental fisika jaringan, mengevaluasi ketergantungan mutlak data terhadap kabel serat optik bawah laut, serta membedah struktur penundaan transmisi jaringan nirkabel.

Dokumen ini membuktikan secara ilmiah dua poin utama:
1. Tidak ada aplikasi Android atau manipulasi algoritma perangkat lunak yang dapat menghilangkan ketergantungan fisik pada kabel serat optik bawah laut maupun melampaui batas kecepatan propagasi cahaya.
2. Memperkecil ukuran paket data game tidak menurunkan latensi dasar (ping) secara signifikan karena komponen penundaan transmisi (transmission delay) pada paket game UDP hanya mencakup kurang dari 0.1 persen dari total latensi interkontinental.

Solusi ilmiah yang valid untuk mencapai kestabilan koneksi bukan terletak pada kompresi data semu, melainkan pada eliminasi variasi jeda (jitter), penghapusan packet loss melalui Forward Error Correction (FEC), mitigasi antrean penyangga (bufferbloat), dan duplikasi paket jalur ganda (multipath packet replication) melalui agregasi Wi-Fi dan jaringan seluler.

---

## 1. Hukum Fisika Transmisi Data dan Kabel Bawah Laut

### 1.1 Kecepatan Perambatan Cahaya dalam Serat Optik Fused Silica

Informasi digital yang dikirimkan melalui internet antarbenua dikonversi menjadi pulsa foton cahaya di dalam kabel serat optik bawah laut. Kecepatan cahaya di ruang hampa udara dinyatakan dengan konstanta fundamental:

c = 299.792.458 meter per detik (sekitar 300.000 km per detik)

Namun, kabel serat optik bawah laut menggunakan kaca silika murni (fused silica glass). Silika memiliki indeks bias (refractive index) terukur pada panjang gelombang transmisi standar telekomunikasi 1550 nanometer sebesar:

n = 1.4682

Kecepatan efektif transmisi foton di dalam inti serat optik (v) dihitung dengan rumus:

v = c / n  
v = 299.792.458 / 1.4682 = 204.190.476 meter per detik (sekitar 204.190 km per detik)

Artinya, kecepatan sinyal di dalam kabel internet hanyalah sekitar 68 persen dari kecepatan cahaya di ruang hampa. Untuk menempuh jarak 1.000 kilometer di dalam kabel serat optik, foton membutuhkan waktu perambatan satu arah sebesar:

t = 1.000 km / 204.190 km per detik = 4.897 milidetik per 1.000 km

Untuk transmisi bolak-balik atau Round Trip Time (RTT), setiap jarak 1.000 kilometer membutuhkan waktu minimal:

RTT murni = 4.897 milidetik × 2 = 9.794 milidetik (dibulatkan menjadi 9.8 milidetik per 1.000 km jarak fisik kabel)

### 1.2 Geodesi vs Topologi Riil Rute Kabel Bawah Laut Jakarta ke Helsinki

Jarak garis lurus (lingkaran besar atau geodesic distance) antara Jakarta, Indonesia (koordinat 6.2088 LS, 106.8456 BT) dan Helsinki, Finlandia (koordinat 60.1699 LU, 24.9384 BT) adalah:

Jarak Geodesik = 10.245 kilometer

Jika data dapat menembus inti bumi secara lurus di ruang hampa, RTT teoritis minimum adalah 68.3 milidetik. Namun, kabel serat optik bawah laut tidak pernah dipasang dalam garis lurus menembus daratan benua secara bebas. Kabel harus mengikuti rute maritim resmi, menghindari palung tektonik berbahaya, melewati selat sempit internasional, dan masuk ke stasiun pendaratan kabel (cable landing stations).

Rute kabel fisik aktual yang menghubungkan Jakarta ke Eropa Utara melewati sistem konsorsium kabel bawah laut seperti SEA-ME-WE 3, SEA-ME-WE 5, AAE-1, dan jalur darat transit Eropa:
1. Jakarta ke Singapura melalui Kabel B2JS atau Matrix: 1.150 km
2. Singapura melintasi Selat Malaka, Samudera Hindia, Laut Merah hingga Teluk Suez (Mesir): 8.400 km
3. Transit darat Mesir dari Zafarana ke Alexandria: 350 km
4. Alexandria melintasi Laut Mediterania ke Marseille (Prancis) atau Sisilia (Italia): 3.100 km
5. Jaringan tulang punggung darat Eropa dari Marseille menuju Frankfurt (Jerman): 1.050 km
6. Frankfurt menuju Helsinki (Finlandia) melalui Laut Baltik: 1.850 km

Total panjang kabel fisik riil yang ditempuh data:

Panjang Kabel Akumulatif = 15.900 kilometer

![Peta Topologi Rute Kabel Bawah Laut](../docs/assets/cable_route_map.svg)

### 1.3 Pembuktian Batas Minimum Latensi Antarbenua

Dengan panjang rute fisik 15.900 kilometer, batas bawah RTT transmisi optik murni adalah:

RTT optik = (15.900 km / 204.190 km per detik) × 2 = 155.73 milidetik

Selain waktu perambatan optik murni, terdapat penundaan pemrosesan perangkat keras (hardware processing delay) yang tidak dapat dihindari:
* Erbium Doped Fiber Amplifiers (EDFA) dan pengulang optik (optical repeaters) setiap 60 hingga 80 km.
* Transponder Optical Transport Network (OTN) dan Regenerator Sinyal.
* Konversi Fotonik ke Elektronik di stasiun router perbatasan BGP Autonomous System (AS).
* Penundaan antrean pada sekitar 15 hingga 25 hop router antarnegara (rata-rata 0.8 milidetik per hop router).

Penambahan jeda pemrosesan perangkat keras dan perutean:

Penundaan Peralatan Tambahan = 20 hop × 0.8 milidetik × 2 = 32 milidetik

Maka, batas latensi absolut yang mungkin dicapai dari Jakarta ke Helsinki adalah:

Latensi Minimum Riil = 155.73 milidetik + 32 milidetik = 187.73 milidetik

Kesimpulan Ilmiah Pertama:  
Setiap klaim yang menyatakan bahwa aplikasi perangkat lunak dapat menghasilkan ping di bawah 150 milidetik dari Indonesia ke server Finlandia tanpa memindahkan lokasi fisik pengguna adalah kemustahilan fisika. Data wajib melewati kabel fisik tersebut.

---

## 2. Mengapa Memperkecil Ukuran Paket Data Tidak Menurunkan Ping

Terdapat hipotesis awam yang menduga bahwa jika ukuran paket data game diperkecil (misalnya dikompresi menjadi setengah ukuran), maka latensi (milidetik) akan menurun secara drastis. Berikut pembuktian matematis dekonstruksi jeda jaringan.

### 2.1 Formula Dekomposisi Latensi Jaringan Total

Penundaan jaringan end to end dihitung melalui persamaan standar telekomunikasi:

D total = D prop + D trans + D queue + D proc

Keterangan variabel:
* D prop (Propagation Delay): Jeda waktu perambatan sinyal melalui media fisik. Rumus: Jarak / Kecepatan Perambatan.
* D trans (Transmission Delay): Jeda waktu yang dibutuhkan kartu jaringan (NIC) untuk mendorong bit paket ke dalam saluran transmisi. Rumus: Ukuran Paket (bit) / Bandwidth Saluran (bit per detik).
* D queue (Queuing Delay): Waktu yang dihabiskan paket dalam antrean memori penyangga (buffer) router sebelum diproses.
* D proc (Processing Delay): Waktu yang dibutuhkan prosesor router untuk membaca header IP, memeriksa checksum, dan memutuskan tabel perutean.

### 2.2 Komparasi Skala Matematis: D prop vs D trans

Karakteristik paket data real-time game seluler seperti Mobile Legends:
* Protokol: UDP (User Datagram Protocol)
* Ukuran muatan rata-rata (Payload Size): 64 byte hingga 128 byte
* Ukuran paket lengkap termasuk IP Header (20 byte) dan UDP Header (8 byte): 156 byte (1.248 bit)

Mari kita hitung D trans pada koneksi internet rumahan standar dengan kecepatan unduh dan unggah sebesar 20 Mbps (20.000.000 bit per detik):

D trans (Paket Asli 156 byte) = 1.248 bit / 20.000.000 bit per detik = 0.0000624 detik = 0.062 milidetik

Sekarang, mari kita asumsikan skenario ideal di mana aplikasi berhasil mengompresi ukuran muatan sebesar 50 persen, sehingga ukuran paket menjadi 78 byte (624 bit):

D trans (Paket Kompresi 78 byte) = 624 bit / 20.000.000 bit per detik = 0.0000312 detik = 0.031 milidetik

Perbedaan waktu transmisi yang dihemat:

Selisih Penghematan = 0.062 milidetik dikurangi 0.031 milidetik = 0.031 milidetik

Sekarang bandingkan penghematan 0.031 milidetik ini dengan D prop antarbenua yang bernilai 187.73 milidetik:

Persentase Pengaruh Kompresi = (0.031 milidetik / 187.73 milidetik) × 100 persen = 0.0165 persen

Kesimpulan Ilmiah Kedua:  
Mengompresi atau memperkecil paket game hanya menghasilkan efisiensi waktu sebesar 0.03 milidetik. Pada layar tampilan game, nilai ini sama sekali tidak terasa dan tidak akan mengubah angka ping (misalnya dari 200 milidetik menjadi 199.97 milidetik). Pengurangan ukuran paket bermanfaat untuk menghemat kuota data, namun tidak memiliki pengaruh nyata terhadap pengurangan latensi jarak jauh.

---

## 3. Sumber Masalah Sebenarnya: Mengapa Ping Menjadi Merah dan Loncat

Jika jarak fisik menentukan latensi dasar sebesar 190 milidetik, mengapa seorang pemain di ponsel pintar kerap mengalami lonjakan ping dari 190 milidetik melonjak ke 400 milidetik, 800 milidetik, hingga koneksi terputus?

Penyebab utamanya bukan kabel bawah laut itu sendiri, melainkan tiga fenomena kegagalan berikut:

### 3.1 Kontensi Nirkabel Wi-Fi (CSMA CA Half Duplex)

Wi-Fi (IEEE 802.11) beroperasi dengan prinsip Carrier Sense Multiple Access with Collision Avoidance (CSMA CA). Berbeda dengan kabel LAN Ethernet yang bersifat Full Duplex (bisa mengirim dan menerima secara bersamaan), Wi-Fi bersifat Half Duplex.
* Pada frekuensi 2.4 GHz atau 5 GHz, hanya satu perangkat yang dapat melakukan transmisi radio pada saat bersamaan dalam satu kanal radio.
* Jika ponsel pintar Samsung A53 sedang mengirim paket input analog gerak hero, sementara perangkat lain di rumah (misalnya Smart TV atau ponsel lain) sedang menerima frame video, ponsel harus menunggu (backoff timer).
* Jika terjadi interferensi gelombang mikro atau tabrakan sinyal udara, paket UDP akan hilang (drop) di udara. Protokol UDP tidak memiliki mekanisme pengiriman ulang bawaan. Akibatnya, server game kehilangan update posisi dan karakter game mengalami patah gerak (teleporting atau freeze).

### 3.2 Penyakit Penyangga Antrean (Bufferbloat) pada Router Rumahan

Bufferbloat adalah kondisi ketika antrean memori sementara pada router jaringan dipenuhi oleh paket data berukuran besar (seperti unduhan latar belakang, pembaruan aplikasi otomatis, atau unggahan berkas).
* Paket game UDP berukuran 128 byte terjebak di belakang paket TCP berukuran 1.500 byte (MTU maksimum).
* Waktu tunggu dalam antrean router (D queue) dapat bertambah dari 2 milidetik menjadi 300 milidetik secara mendadak. Inilah yang menyebabkan lonjakan ping merah lokal.

### 3.3 Fluktuasi Hemat Daya Radio Seluler dan Wi-Fi (Power Save Polling)

Sistem operasi seluler modern (termasuk Samsung One UI berbasis Android) mengaktifkan mode hemat daya radio secara agresif:
* Modul Wi-Fi pada chipset ponsel mematikan sirkuit penerima gelombang mikro selama beberapa milidetik di antara interval suar (DTIM Beacon Interval).
* Proses ini menyebabkan paket data game yang datang dari server tertahan di modul radio sebelum diserahkan ke CPU ponsel, memicu variasi jeda (jitter) berkisar antara 20 hingga 80 milidetik.

---

## 4. Solusi Rekayasa yang Valid: Arsitektur AetherPulse

Berdasarkan analisis ilmiah di atas, arsitektur stabilisator jaringan yang benar dan efektif harus berfokus pada:
1. Menghilangkan variasi jeda (jitter) lokal hingga mendekati nol.
2. Mencegah kehilangan paket (zero packet loss) tanpa menunggu permintaan ulang (zero retransmission latency).
3. Memberikan jalur prioritas udara tertinggi pada modul radio perangkat keras ponsel.

Arsitektur sistem ini diwujudkan melalui empat pilar rekayasa:
* Pilar Pertama: Multipath WAN Packet Duplication (Mengirimkan paket duplikat secara paralel melalui Wi-Fi dan Seluler 4G/5G secara serentak).
* Pilar Kedua: Forward Error Correction (FEC) berbasis XOR sliding window untuk merekonstruksi paket hilang secara instan.
* Pilar Ketiga: DSCP Voice Queue Prioritization (Penandaan paket dengan kelas Expedited Forwarding untuk melewati antrean bufferbloat).
* Pilar Keempat: Edge Relay Tunnel di Finlandia untuk memotong jalur BGP publik yang tidak efisien.
