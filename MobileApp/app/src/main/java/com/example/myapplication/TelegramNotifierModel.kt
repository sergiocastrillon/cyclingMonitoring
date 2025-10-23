package com.example.myapplication

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object TelegramNotifier {
    @RequiresPermission(Manifest.permission.INTERNET)
    fun sendFallNotification(context: Context, text: String = "Caída detectada") {
        val token = context.getString(R.string.api_token)
        val chatId = context.getString(R.string.chat_id)

        if (token.isBlank() || chatId.isBlank()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Token o chat id de Telegram no configurados", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
        val url = "https://api.telegram.org/bot${token}/sendMessage?chat_id=${chatId}&text=${encodedText}"
        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            {
            },
            { error ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Error al enviar notificación Telegram. Comprueba " +
                                "la conexión a Internet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        requestQueue.add(stringRequest)
    }
}