package com.sceyt.chatuikit.persistence.database.cleaner

import com.sceyt.chatuikit.persistence.database.SceytDatabase

internal class DatabaseCleanerImpl(
        private val database: SceytDatabase
) : DatabaseCleaner {

    override suspend fun cleanDatabase() {
        database.clearAllTables()
    }
}