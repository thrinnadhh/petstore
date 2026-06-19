package com.example.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseAuthRepositoryImpl(private val pawsDao: PawsDao) : AuthRepository {
    
    override suspend fun login(phoneOrEmail: String, pinOrPassword: String): Result<ProfileEntity> = withContext(Dispatchers.IO) {
        try {
            val email = if (phoneOrEmail.contains("@")) {
                phoneOrEmail.lowercase().trim()
            } else {
                "${phoneOrEmail.trim()}@pawsapp.com"
            }
            
            // Log in using Supabase Auth Email provider
            val client = SupabaseManager.client
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pinOrPassword
            }
            
            // Retrieve current user details from current session
            val user = client.auth.retrieveUserForCurrentSession()
            val userId = user.id
            
            // Query profile from local DB, or create it if missing locally but present in auth session
            var profile = pawsDao.getProfile(userId)
            if (profile == null) {
                profile = ProfileEntity(
                    id = userId,
                    fullName = "Paws User",
                    phone = phoneOrEmail,
                    cityId = "hyd",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop",
                    role = "consumer",
                    email = email,
                    password = BCryptHelper.hashPassword(pinOrPassword)
                )
                pawsDao.insertProfile(profile)
            }
            Result.success(profile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun register(profile: ProfileEntity, pinOrPassword: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val email = profile.email ?: "${profile.phone}@pawsapp.com"
            val client = SupabaseManager.client
            
            // Sign up in Supabase Auth
            val user = client.auth.signUpWith(Email) {
                this.email = email
                this.password = pinOrPassword
            }
            
            val userId = user?.id ?: profile.id
            val securedProfile = profile.copy(id = userId, password = BCryptHelper.hashPassword(pinOrPassword))
            
            // Write to local database
            pawsDao.insertProfile(securedProfile)
            
            // Also sync profile to Supabase public.profiles table
            client.postgrest["profiles"].insert(mapOf(
                "id" to userId,
                "name" to profile.fullName,
                "email" to email,
                "phone" to profile.phone,
                "role" to profile.role
            ))
            
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
