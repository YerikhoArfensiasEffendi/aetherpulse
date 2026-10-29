package com.bridgeplay.stabilizer.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.bridgeplay.stabilizer.R
import com.bridgeplay.stabilizer.service.BridgeVpnService

class MainActivity : Activity() {

    private val vpnRequestCode = 1001
    private var isServiceActive = false

    private lateinit var etServerIp: EditText
    private lateinit var etServerPort: EditText
    private lateinit var btnToggle: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etServerIp = findViewById(R.id.etServerIp)
        etServerPort = findViewById(R.id.etServerPort)
        btnToggle = findViewById(R.id.btnToggle)
        tvStatus = findViewById(R.id.tvStatus)

        btnToggle.setOnClickListener {
            if (!isServiceActive) {
                val intent = VpnService.prepare(this)
                if (intent != null) {
                    startActivityForResult(intent, vpnRequestCode)
                } else {
                    onActivityResult(vpnRequestCode, RESULT_OK, null)
                }
            } else {
                stopVpnService()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequestCode && resultCode == RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(this, "Izin VPN ditolak oleh pengguna", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVpnService() {
        val ip = etServerIp.text.toString().trim()
        val port = etServerPort.text.toString().trim().toIntOrNull() ?: 8910

        if (ip.isEmpty()) {
            Toast.makeText(this, "Masukkan alamat IP server relay terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val serviceIntent = Intent(this, BridgeVpnService::class.java).apply {
            putExtra("SERVER_IP", ip)
            putExtra("SERVER_PORT", port)
        }
        startService(serviceIntent)

        isServiceActive = true
        tvStatus.text = "Status: Aktif (Jalur Ganda Wi-Fi + Seluler Terhubung)"
        btnToggle.text = "Nonaktifkan Penstabil Jaringan"
        btnToggle.setBackgroundColor(0xFFDC2626.toInt()) // Red color
    }

    private fun stopVpnService() {
        val serviceIntent = Intent(this, BridgeVpnService::class.java)
        stopService(serviceIntent)

        isServiceActive = false
        tvStatus.text = "Status: Tidak Aktif"
        btnToggle.text = "Aktifkan Penstabil Jaringan"
        btnToggle.setBackgroundColor(0xFF0284C7.toInt()) // Blue color
    }
}
