package com.example.blinking_packet

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.PrintStream
import javax.net.ssl.HttpsURLConnection

enum class PacketType(val str: String) {
    ARP("arp"),
    DNS("dns"),
    HTTP("http"),
    PING("ping"),
}

class GsonData(val packet_type: String, val value: String) {}

class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var valueInput: TextInputEditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tab_layout)
        valueInput = findViewById(R.id.value_input)
        submitButton = findViewById(R.id.submit_button)

        // ツールバー
        toolbar.setTitle("blinking-packet")

        // 送信
        submitButton.setOnClickListener { _: View? ->
            val packetType = getPacketType()
            val value = valueInput.text.toString()
            val self = this;

            GlobalScope.launch {
                val succeeded = postRequest(packetType, value).await()
                if (!succeeded) {
                    runOnUiThread {
                        AlertDialog.Builder(self)
                            .setTitle("ネットワークエラー")
                            .setMessage("パケットの送信に失敗しました")
                            .show()
                    }
                }
            }
        }
    }

    // パケットタイプを取得する
    private fun getPacketType(): PacketType {
        return when(tabLayout.selectedTabPosition) {
            0 -> PacketType.ARP
            1 -> PacketType.PING
            2 -> PacketType.HTTP
            else -> PacketType.DNS
        }
    }

    // リクエストを送信する
    private fun postRequest(packetType: PacketType, value: String): Deferred<Boolean> = GlobalScope.async {
        val url = "https://www.example.com/"
        var connection: HttpsURLConnection? = null

        try {
            connection = URL(url).openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true

            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            // JSON を送信
            val data = GsonData(packetType.str, value)
            val json = Gson().toJson(data)
            val ps = PrintStream(connection.outputStream)
            ps.print(json)
            ps.close()

            connection.connect();
            val responseCode = connection.getResponseCode();

            // レスポンスをログ出力
            Log.d("HTTP_POST", url)
            Log.d("HTTP_POST", json)
            Log.d("HTTP_POST", responseCode.toString())
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@async false
            }
            connection.inputStream.bufferedReader().use {
                Log.d("HTTP_POST", it.readText())
            }
            return@async true
        } catch (e: Exception) {
            Log.d("HTTP_ERROR", e.toString())
            return@async false
        } finally {
            connection?.disconnect()
        }
    }
}
