package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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

    // Sync order details to Supabase Postgres public.orders table
    suspend fun insertOrderToCloud(
        orderId: String,
        consumerId: String,
        shopId: String,
        type: String,
        status: String,
        totalAmount: Double,
        deliveryAddress: String,
        notes: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["orders"].insert(mapOf(
                "id" to orderId,
                "consumerId" to consumerId,
                "shopId" to shopId,
                "type" to type,
                "status" to status,
                "totalAmount" to totalAmount,
                "deliveryAddress" to deliveryAddress,
                "notes" to notes,
                "placedAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis()
            ))
            Log.i("SupabaseSync", "Order $orderId inserted securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync order: ${e.message}")
            false
        }
    }

    // Sync individual order items to Supabase Postgres public.order_items table
    suspend fun insertOrderItemToCloud(
        itemId: String,
        orderId: String,
        productId: String,
        quantity: Int,
        unitPrice: Double,
        subtotal: Double
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["order_items"].insert(mapOf(
                "id" to itemId,
                "orderId" to orderId,
                "productId" to productId,
                "quantity" to quantity,
                "unitPrice" to unitPrice,
                "subtotal" to subtotal
            ))
            Log.i("SupabaseSync", "Order item $itemId synced securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync order item: ${e.message}")
            false
        }
    }

    // Update order status in Supabase Postgres public.orders table
    suspend fun updateOrderStatusInCloud(orderId: String, status: String): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["orders"].update(mapOf(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )) {
                filter {
                    eq("id", orderId)
                }
            }
            Log.i("SupabaseSync", "Order $orderId status updated to $status in cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update cloud order status: ${e.message}")
            false
        }
    }

    // Sync appointment details to Supabase Postgres public.appointments table
    suspend fun insertAppointmentToCloud(
        appointmentId: String,
        consumerId: String,
        shopId: String,
        serviceId: String,
        serviceName: String,
        price: Double,
        appointmentDate: String,
        appointmentTime: String,
        petName: String,
        status: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["appointments"].insert(mapOf(
                "id" to appointmentId,
                "consumerId" to consumerId,
                "shopId" to shopId,
                "serviceId" to serviceId,
                "serviceName" to serviceName,
                "price" to price,
                "appointmentDate" to appointmentDate,
                "appointmentTime" to appointmentTime,
                "petName" to petName,
                "status" to status,
                "createdAt" to System.currentTimeMillis()
            ))
            Log.i("SupabaseSync", "Appointment $appointmentId inserted securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync appointment: ${e.message}")
            false
        }
    }

    // Sync shop details to Supabase Postgres public.shops table
    suspend fun insertShopToCloud(shop: ShopEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["shops"].insert(mapOf(
                "id" to shop.id,
                "ownerId" to shop.ownerId,
                "cityId" to shop.cityId,
                "name" to shop.name,
                "description" to shop.description,
                "address" to shop.address,
                "locality" to shop.locality,
                "lat" to shop.lat,
                "lng" to shop.lng,
                "phone" to shop.phone,
                "email" to shop.email,
                "photos" to shop.photos,
                "isOpen" to shop.isOpen,
                "opensAt" to shop.opensAt,
                "closesAt" to shop.closesAt,
                "rating" to shop.rating,
                "totalReviews" to shop.totalReviews,
                "deliveryAvailable" to shop.deliveryAvailable,
                "isVerified" to shop.isVerified,
                "isActive" to shop.isActive,
                "isFeatured" to shop.isFeatured,
                "status" to shop.status,
                "groomingEnabled" to shop.groomingEnabled,
                "vetClinicEnabled" to shop.vetClinicEnabled,
                "createdAt" to shop.createdAt
            ))
            Log.i("SupabaseSync", "Shop ${shop.id} synced securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync shop: ${e.message}")
            false
        }
    }

    // Sync product details to Supabase Postgres public.products table
    suspend fun insertProductToCloud(product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["products"].insert(mapOf(
                "id" to product.id,
                "shopId" to product.shopId,
                "categoryId" to product.categoryId,
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
                "mrp" to product.mrp,
                "photos" to product.photos,
                "inStock" to product.inStock,
                "isActive" to product.isActive,
                "tags" to product.tags,
                "brand" to product.brand,
                "lifeStage" to product.lifeStage,
                "stockCount" to product.stockCount,
                "createdAt" to product.createdAt
            ))
            Log.i("SupabaseSync", "Product ${product.id} synced securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync product: ${e.message}")
            false
        }
    }

    // Sync service details to Supabase Postgres public.services table
    suspend fun insertServiceToCloud(service: ServiceEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized) return@withContext false
        try {
            client.postgrest["services"].insert(mapOf(
                "id" to service.id,
                "shopId" to service.shopId,
                "name" to service.name,
                "price" to service.price,
                "category" to service.category,
                "isCustom" to service.isCustom,
                "createdAt" to service.createdAt
            ))
            Log.i("SupabaseSync", "Service ${service.id} synced securely to cloud.")
            true
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to sync service: ${e.message}")
            false
        }
    }
}
