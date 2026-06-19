package com.example.data

interface AuthRepository {
    suspend fun login(phoneOrEmail: String, pinOrPassword: String): Result<ProfileEntity>
    suspend fun register(profile: ProfileEntity, pinOrPassword: String): Result<Boolean>
}
