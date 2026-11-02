package com.example.livekit

import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val TAG = "LiveKitToken"

/**
 * Official LiveKit Access Token (JWT) generator.
 * Implements the standard LiveKit HS256 token specification for client authorization.
 * https://docs.livekit.io/transport/sdk-platforms/
 */
object LiveKitTokenGenerator {

    /**
     * Generates a signed LiveKit Access Token JWT.
     */
    fun createToken(
        apiKey: String,
        apiSecret: String,
        roomName: String = "nola-agent-room",
        identity: String = "user-${UUID.randomUUID().toString().take(8)}",
        participantName: String = "User",
        ttlSeconds: Long = 86400L // 24 hours
    ): String {
        try {
            // 1. JWT Header
            val headerJson = JSONObject().apply {
                put("alg", "HS256")
                put("typ", "JWT")
            }
            val headerEncoded = base64UrlEncode(headerJson.toString().toByteArray(StandardCharsets.UTF_8))

            // 2. JWT Claims and Video Grants
            val nowSeconds = System.currentTimeMillis() / 1000L
            val expSeconds = nowSeconds + ttlSeconds

            val videoGrants = JSONObject().apply {
                put("room", roomName)
                put("roomJoin", true)
                put("canPublish", true)
                put("canSubscribe", true)
                put("canPublishData", true)
            }

            val payloadJson = JSONObject().apply {
                put("iss", apiKey)
                put("sub", identity)
                put("name", participantName)
                put("nbf", nowSeconds)
                put("exp", expSeconds)
                put("video", videoGrants)
            }
            val payloadEncoded = base64UrlEncode(payloadJson.toString().toByteArray(StandardCharsets.UTF_8))

            // 3. HMAC-SHA256 Signature
            val signingInput = "$headerEncoded.$payloadEncoded"
            val mac = Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(apiSecret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
            }
            val signatureBytes = mac.doFinal(signingInput.toByteArray(StandardCharsets.UTF_8))
            val signatureEncoded = base64UrlEncode(signatureBytes)

            val jwt = "$signingInput.$signatureEncoded"
            Log.d(TAG, "Successfully generated LiveKit Cloud token for identity: $identity, room: $roomName")
            return jwt
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create LiveKit token: ${e.message}", e)
            return ""
        }
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .trimEnd('=')
    }
}
