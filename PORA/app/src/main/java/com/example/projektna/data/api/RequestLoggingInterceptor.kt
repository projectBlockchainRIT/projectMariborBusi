package com.example.projektna.data.api

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset

/**
 * OkHttp Interceptor za logiranje vseh HTTP requestov.
 * Vedno izpiše podatke o requestu v konzolo (Logcat), ne glede na build tip.
 *
 * Izpisuje:
 * - HTTP metodo in URL
 * - Headerje
 * - Request body (če obstaja)
 * - Response status code
 * - Trajanje zahteve
 */
class RequestLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "API_REQUEST"
        private const val MAX_BODY_LENGTH = 4096 // Omejitev dolžine body za izpis
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()

        // Log request
        val logBuilder = StringBuilder()
        logBuilder.appendLine("┌────────────────────────────────────────────────────────────")
        logBuilder.appendLine("│ REQUEST: ${request.method} ${request.url}")
        logBuilder.appendLine("├────────────────────────────────────────────────────────────")

        // Headers
        logBuilder.appendLine("│ Headers:")
        request.headers.forEach { (name, value) ->
            // Skrij Authorization token (varnost)
            val displayValue = if (name.equals("Authorization", ignoreCase = true)) {
                "Bearer ***"
            } else {
                value
            }
            logBuilder.appendLine("│   $name: $displayValue")
        }

        // Body
        request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            val bodyString = buffer.readString(Charset.forName("UTF-8"))

            if (bodyString.isNotEmpty()) {
                logBuilder.appendLine("├────────────────────────────────────────────────────────────")
                logBuilder.appendLine("│ Body:")
                val truncatedBody = if (bodyString.length > MAX_BODY_LENGTH) {
                    bodyString.take(MAX_BODY_LENGTH) + "... (truncated)"
                } else {
                    bodyString
                }
                // Formatiraj JSON za boljšo berljivost
                truncatedBody.lines().forEach { line ->
                    logBuilder.appendLine("│   $line")
                }
            }
        }

        logBuilder.appendLine("└────────────────────────────────────────────────────────────")

        Log.d(TAG, logBuilder.toString())

        // Execute request
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "┌── RESPONSE ERROR ──────────────────────────────────────────")
            Log.e(TAG, "│ ${request.method} ${request.url}")
            Log.e(TAG, "│ Error: ${e.message}")
            Log.e(TAG, "│ Duration: ${duration}ms")
            Log.e(TAG, "└────────────────────────────────────────────────────────────")
            throw e
        }

        val duration = System.currentTimeMillis() - startTime

        // Log response
        val responseLog = StringBuilder()
        responseLog.appendLine("┌── RESPONSE ────────────────────────────────────────────────")
        responseLog.appendLine("│ ${request.method} ${request.url}")
        responseLog.appendLine("│ Status: ${response.code} ${response.message}")
        responseLog.appendLine("│ Duration: ${duration}ms")
        responseLog.appendLine("└────────────────────────────────────────────────────────────")

        if (response.isSuccessful) {
            Log.d(TAG, responseLog.toString())
        } else {
            Log.w(TAG, responseLog.toString())
        }

        return response
    }
}
