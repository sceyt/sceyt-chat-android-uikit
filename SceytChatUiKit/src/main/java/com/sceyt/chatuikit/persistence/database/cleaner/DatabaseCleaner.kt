package com.sceyt.chatuikit.persistence.database.cleaner

interface DatabaseCleaner {
   suspend fun cleanDatabase()
}