package com.vvf.smartmanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.data.local.dao.VaultItemDao
import com.vvf.smartmanager.data.local.entity.FileEntryEntity
import com.vvf.smartmanager.data.local.entity.FileEntryFts
import com.vvf.smartmanager.data.local.entity.VaultItemEntity

/**
 * Single encrypted database for the app. Opened with a SQLCipher [net.zetetic.database.sqlcipher.SupportOpenHelperFactory]
 * (wired in [com.vvf.smartmanager.core.di.DatabaseModule]) — the on-disk file is fully encrypted,
 * not just individual columns. Master Specification v2.0, Section 7.
 */
@Database(
    entities = [FileEntryEntity::class, FileEntryFts::class, VaultItemEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileEntryDao(): FileEntryDao
    abstract fun vaultItemDao(): VaultItemDao

    companion object {
        const val DATABASE_NAME = "vvf_smart_manager.db"
    }
}
