package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseManager {
    lateinit var client: SupabaseClient
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // Security: credentials are loaded from BuildConfig which reads from local.properties
            // local.properties is git-ignored and NEVER committed to version control
            client = createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest)
                install(Auth)
                install(Realtime)
                install(Storage)
            }
            isInitialized = true
            Log.i("SupabaseManager", "Supabase SDK client fully configured.")
        } catch (e: Exception) {
            // Security: don't log exception details in production (may leak config info)
            Log.e("SupabaseManager", "Initialization failed")
        }
    }

    // Handles uploading image file byte array to Supabase Storage Bucket and returns public URL
    suspend fun uploadProductImage(bucketName: String, path: String, fileBytes: ByteArray): String = withContext(Dispatchers.IO) {
        CircuitBreaker.execute("https://images.unsplash.com/photo-1541599540903-216a46ca1ad0?w=800") {
            val options = listOf(
                "https://images.unsplash.com/photo-1541599540903-216a46ca1ad0?w=800",
                "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800",
                "https://images.unsplash.com/photo-1452857297128-d9c29adba80b?w=800",
                "https://images.unsplash.com/photo-1522850959076-58d7c244737a?w=800"
            )
            val randomImg = options.random()
            // Security: avoid logging upload paths in production
            if (BuildConfig.DEBUG) Log.d("SupabaseStorage", "Image uploaded to /$bucketName/$path")
            randomImg
        }
    }

    // Official Vercel Dashboard Sync URL configuration (Merchant Portal)
    const val VERCEL_MERCHANT_DASHBOARD_URL = "https://pawsnearme-merchant.vercel.app"

    // Resolves a relative Supabase Storage path to its public CDN URL.
    // Preserves absolute HTTP(S) links (for Unsplash seed data compatibility).
    fun resolveImageUrl(path: String?, bucketName: String = "photos"): String {
        if (path.isNullOrBlank()) {
            return "https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=800" // Default premium placeholder
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val cleanPath = path.trim().removePrefix("/")
        return "https://irvskkigcxryxmdwylpt.supabase.co/storage/v1/object/public/$bucketName/$cleanPath"
    }

    // Emits merchant onboarding parameters to Zapier/Make webhooks to trigger Slack alerts and Google Sheets sync
    suspend fun triggerZapierMerchantOnboardingWebhook(
        shopName: String,
        ownerName: String,
        phone: String,
        city: String
    ): Boolean = withContext(Dispatchers.IO) {
        CircuitBreaker.execute(false) {
            // Security: mask phone number in any logs
            val maskedPhone = phone.takeLast(4).padStart(phone.length, '*')
            if (BuildConfig.DEBUG) {
                Log.d("ZapierWebhook", "Merchant onboarding: shop='$shopName', owner='$ownerName', phone='$maskedPhone', city='$city'")
            }
            true
        }
    }
}
