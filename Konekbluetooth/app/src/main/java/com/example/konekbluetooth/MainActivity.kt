package com.example.konekbluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceName = "ESP32" // Ganti dengan nama perangkat ESP32 Anda
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID untuk komunikasi Serial Bluetooth

    private lateinit var tvDistance: TextView
    private lateinit var btnBacaJarak: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton: Button = findViewById(R.id.connectButton)
        val btnActivateRelay: Button = findViewById(R.id.btnActivateRelay)
        val btnDeactivateRelay: Button = findViewById(R.id.btnDeactivateRelay)
        val btnBacaJarak: Button = findViewById(R.id.btnBacaJarak)
        tvDistance = findViewById(R.id.tvDistance)

        checkAndRequestPermissions()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
        }

        connectButton.setOnClickListener {
            val device = getPairedDeviceByName(deviceName)
            if (device != null) {
                connectToDevice(device)
            } else {
                Toast.makeText(this, "Perangkat ESP32 tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        btnActivateRelay.setOnClickListener {
            sendDataToESP32("1")
        }

        btnDeactivateRelay.setOnClickListener {
            sendDataToESP32("0")
        }

        btnBacaJarak.setOnClickListener {
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                sendDataToESP32("read")
                readDistanceFromESP32()
            } else {
                Toast.makeText(this, "Belum terhubung ke ESP32", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }

    private fun getPairedDeviceByName(name: String): BluetoothDevice? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin BLUETOOTH_CONNECT tidak diberikan", Toast.LENGTH_SHORT).show()
                null
            } else {
                bluetoothAdapter.bondedDevices.find { it.name == name }
            }
        } catch (e: SecurityException) {
            Log.e("Bluetooth", "SecurityException: ${e.message}")
            null
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Izin BLUETOOTH_CONNECT tidak diberikan", Toast.LENGTH_SHORT).show()
                return
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket?.connect()
            Toast.makeText(this, "Terhubung ke ESP32", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Tidak dapat terhubung ke perangkat", e)
            Toast.makeText(this, "Tidak dapat terhubung ke ESP32", Toast.LENGTH_SHORT).show()
            bluetoothSocket = null
        }
    }

    private fun sendDataToESP32(data: String) {
        try {
            bluetoothSocket?.outputStream?.write("$data\n".toByteArray())
        } catch (e: IOException) {
            Log.e("Bluetooth", "Gagal mengirim data", e)
            Toast.makeText(this, "Gagal mengirim data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readDistanceFromESP32() {
        Thread {
            try {
                val inputStream = bluetoothSocket?.inputStream
                val buffer = ByteArray(1024)
                val bytes = inputStream?.read(buffer) ?: 0
                val data = String(buffer, 0, bytes).trim()

                runOnUiThread {
                    tvDistance.text = "Jarak: $data cm"
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Gagal membaca data", e)
                runOnUiThread {
                    Toast.makeText(this, "Gagal membaca data dari ESP32", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Gagal menutup socket", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Izin Bluetooth diperlukan untuk aplikasi ini", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
