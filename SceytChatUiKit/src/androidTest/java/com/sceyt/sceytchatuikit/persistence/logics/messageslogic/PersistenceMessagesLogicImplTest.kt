package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PersistenceMessagesLogicImplTest {
    private lateinit var database: SceytDatabase
    private lateinit var messageDao: MessageDao

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SceytDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertMessage() = runTest {
        val entity = MessageEntity(1L, 0L, 1L, "hello", "text", null, 0L, 0L, incoming = false, isTransient = false, silent = false,
            deliveryStatus = DeliveryStatus.Displayed, state = MessageState.Unmodified, fromId = "1", markerCount = null, mentionedUsersIds = null, parentId = null, replyCount = 0L, displayCount = 0, autoDeleteAt = null, forwardingDetailsDb = null, bodyAttribute = null, unList = false)

        messageDao.upsertMessageEntity(entity)

        val messages = messageDao.getMessageByTid(entity.tid)

        assertThat(messages != null).isTrue()
    }
}