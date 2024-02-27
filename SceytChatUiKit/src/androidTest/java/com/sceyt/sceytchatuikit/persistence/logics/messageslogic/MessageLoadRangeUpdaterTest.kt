package com.sceyt.sceytchatuikit.persistence.logics.messageslogic

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.persistence.dao.LoadRangeDao
import com.sceyt.sceytchatuikit.persistence.entity.messages.LoadRangeEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MessageLoadRangeUpdaterTest {
    private lateinit var database: SceytDatabase
    private lateinit var rangeDao: LoadRangeDao
    private lateinit var updater: MessageLoadRangeUpdater
    private val channelId = 1L

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SceytDatabase::class.java)
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        rangeDao = database.loadRangeDao()
        updater = MessageLoadRangeUpdater(rangeDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertRange() = runTest {
        val messageId = 1L
        updater.updateMessageLoadRange(messageId, 1, 10, channelId)

        val ranges = updater.getMessageLoadRange(channelId, messageId)

        print(ranges)

        val range = ranges.single()
        Truth.assertThat(messageId >= range.startId && messageId <= range.endId).isTrue()
    }

    @Test
    fun insertRangeShouldReplaceExistingRangesAndUpdateRangeWithMinMaxIfTheyIncludesInThisRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 25L
        updater.updateMessageLoadRange(start, start, end, channelId)

        val ranges = updater.getMessageLoadRange(channelId, start)

        Log.d("ranges", "$ranges")
        Log.d("allRanges", "${rangeDao.getAll()}")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && range.endId == 30L).isTrue()
    }

    @Test
    fun insertRangeShouldReplaceExistingSingleRangeAndUpdateRangeWithMinMax() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 15L
        updater.updateMessageLoadRange(start, start, end, channelId)

        val ranges = updater.getMessageLoadRange(channelId, start)

        Log.d("ranges", "$ranges")
        Log.d("allRanges", "${rangeDao.getAll()}")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && range.endId == end).isTrue()
    }

    @Test
    fun insertRangeShouldNotReplaceExistingRangesIfTheyDoNotIncludesInThisRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 60L
        val end = 70L
        updater.updateMessageLoadRange(start, start, end, channelId)

        val ranges = updater.getMessageLoadRange(channelId, start)

        val allRanges = rangeDao.getAll()
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && allRanges.size == 4).isTrue()
    }

    @Test
    fun insertRangeShouldNotUpdateAnythingIfRangeIsAlreadyExists() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 10L
        updater.updateMessageLoadRange(start, start, end, channelId)

        val ranges = updater.getMessageLoadRange(channelId, start)

        val allRanges = rangeDao.getAll()
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && range.endId == 10L && allRanges.size == 3).isTrue()
    }

    @Test
    fun insertRangeShouldNotUpdateAnythingIfAlreadyExistRangeWhichIncludesCurrentRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 5L
        updater.updateMessageLoadRange(start, start, end, channelId)

        val ranges = updater.getMessageLoadRange(channelId, start)

        val allRanges = rangeDao.getAll()
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && range.endId == 10L && allRanges.size == 3).isTrue()
    }
}