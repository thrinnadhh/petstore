package com.example.data

class DemoAuthRepositoryImpl(private val pawsDao: PawsDao) : AuthRepository {
    
    override suspend fun login(phoneOrEmail: String, pinOrPassword: String): Result<ProfileEntity> {
        val profile = if (phoneOrEmail.contains("@")) {
            pawsDao.getProfileByEmail(phoneOrEmail.lowercase().trim())
        } else {
            pawsDao.getProfileByPhone(phoneOrEmail.trim())
        }
        
        if (profile == null) {
            return Result.failure(Exception("Account not found! Please register first as a Customer or Shop."))
        }
        
        val storedPassword = profile.password
        if (storedPassword == null) {
            return Result.failure(Exception("No password configured for this account."))
        }

        // Support both hashed passwords and standard plaintext passwords (for old debug sessions, though seeding uses hashing now)
        val isVerified = if (BCryptHelper.isHashedPassword(storedPassword)) {
            BCryptHelper.verifyPassword(pinOrPassword, storedPassword)
        } else {
            storedPassword == pinOrPassword
        }
        
        return if (isVerified) {
            Result.success(profile)
        } else {
            Result.failure(Exception("Incorrect 4-digit PIN password!"))
        }
    }

    override suspend fun register(profile: ProfileEntity, pinOrPassword: String): Result<Boolean> {
        val hashedPassword = BCryptHelper.hashPassword(pinOrPassword)
        val securedProfile = profile.copy(password = hashedPassword)
        pawsDao.insertProfile(securedProfile)
        return Result.success(true)
    }
}
