package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class,
        CityEntity::class,
        ShopEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ReviewEntity::class,
        WishlistEntity::class,
        BannerEntity::class,
        ChatMessageEntity::class,
        WishlistProductEntity::class,
        ServiceEntity::class,
        AppointmentEntity::class,
        ReminderEntity::class,
        ProductSpecEntity::class,
        PetEntity::class,
        CaptainEntity::class,
        ProblemEntity::class,
        GroupRfqSessionEntity::class,
        GroupRfqMemberItemEntity::class,
        MerchantQuotationEntity::class
    ],
    version = 18,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pawsDao(): PawsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Initialize SQLCipher factory helper for secure SQLite encryption at rest
                val passphrase = "secure_paws_key_passphrase_to_encrypt_sqlite".toByteArray()
                val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paws_database"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
