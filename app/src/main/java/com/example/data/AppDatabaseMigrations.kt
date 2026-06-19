package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 23 rotates the local encryption passphrase source from a hardcoded
            // value to Android Keystore. The Room schema itself is unchanged from 22.
        }
    }

    val ALL = arrayOf(MIGRATION_22_23)
}
