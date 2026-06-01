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
    version = 17,
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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paws_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
