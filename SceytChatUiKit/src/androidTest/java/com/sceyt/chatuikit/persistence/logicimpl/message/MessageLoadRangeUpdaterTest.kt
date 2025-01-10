package com.sceyt.chatuikit.persistence.logicimpl.message

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        updater.updateLoadRange(messageId, 1, 10, channelId)

        val ranges = rangeDao.getLoadRanges(start = 1, end = 10L, messageId = messageId, channelId = channelId)

        print(ranges)

        val range = ranges.single()
        Truth.assertThat(messageId >= range.startId && messageId <= range.endId).isTrue()
    }

    @Test
    fun insertManyRangesWithDifferentThreadShouldBeWork() = runTest {

        suspend fun update(start: Int, end: Int) {
            updater.updateLoadRange(start.toLong(), start.toLong(), end.toLong(), channelId)
        }


        val min = 2L
        val max = 500L

        val deferredList = mutableListOf<Deferred<Unit>>()
        deferredList.add(async(Dispatchers.IO) {
            update(35, 50)
        })

        deferredList.add(async(Dispatchers.Default) {
            update(30, 66)
        })

        deferredList.add(async(Dispatchers.Unconfined) {
            update(min.toInt(), 80)
        })

        deferredList.add(async(Dispatchers.Unconfined) {
            update(70, 99)
        })

        deferredList.add(async(Dispatchers.Unconfined) {
            update(5, max.toInt())
        })

        deferredList.add(async(Dispatchers.Unconfined) {
            update(7, 232)
        })

        deferredList.awaitAll()
        Log.d("allRanges", "${rangeDao.getAll(channelId)}")

        val range = rangeDao.getAll(channelId).single()
        Truth.assertThat(range.startId == min && range.endId == max).isTrue()
    }

    /**  /-----Range  -----/              \------Range B------\
     *    ----------------------------------------------------- */
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
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)


        val allRanges = rangeDao.getAll(channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && allRanges.size == 4).isTrue()
    }

    /**  //----------Range 1-10 ------\\---//---insert range 7-20-------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfTheRangeStartIncludesInRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 7L
        val end = 20L
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)

        val allRanges = rangeDao.getAll(channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == 1L && range.endId == 20L && allRanges.size == 2).isTrue()
    }


    /**  //----------Range 1-10 ------\\//---insert range 10-20-------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfTheRangeStartEqualsInRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 10L
        val end = 20L
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)

        val allRanges = rangeDao.getAll(channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == 1L && range.endId == 20L && allRanges.size == 2).isTrue()
    }

    /**  //----------insert range 1-10 ------\\---//---Range 10-20-------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfTheRangeEndIncludesInRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(10, 20, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 10L
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)
        val allRanges = rangeDao.getAll(channelId)

        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == 1L && range.endId == 20L && allRanges.size == 2).isTrue()
    }

    /**  //----------insert range 1-10 ------\\---//---Range 7-20-------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfTheRangeEndEqualsInRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(7, 20, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 10L
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)
        val allRanges = rangeDao.getAll(channelId)

        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == 1L && range.endId == 20L && allRanges.size == 2).isTrue()
    }

    /**  //--------Range 1-10 -----\\---//-------insert Range 2-25 ------//----\\---------Range 20-30 ------//
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldReplaceExistingRangesAndUpdateRangeWithMinMaxIfTheyIncludesInThisRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 2L
        val end = 25L
        updater.updateLoadRange(messageId = start, start = start, end = end, channelId = channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)
        val allRanges = rangeDao.getAll(channelId)


        Log.d("ranges", "$ranges")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == 1L && range.endId == 30L && allRanges.size == 2).isTrue()
    }

    /**  //\\---------insert range 1-10 ------------Range 1-10----//\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldNotUpdateAnythingIfRangeIsAlreadyExists() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
            ))

        val start = 1L
        val end = 10L
        updater.updateLoadRange(start, start, end, channelId)

        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)
        val allRanges = rangeDao.getAll(channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()
        Truth.assertThat(range.startId == start && range.endId == end && allRanges.size == 2).isTrue()
    }


    /**  //----\\---------insert range 3-8 ------------Range 1-10----//------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldNotUpdateAnythingIfAlreadyExistRangeWhichIncludesCurrentRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(1, 10, channelId),
                LoadRangeEntity(20, 30, channelId),
            ))

        val start = 3L
        val end = 8L
        updater.updateLoadRange(start, start, end, channelId)

        val allRanges = rangeDao.getAll(channelId)
        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = start, channelId = channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()

        Truth.assertThat(range.startId == 1L && range.endId == 10L && allRanges.size == 2).isTrue()
    }




    ////Message id


    /**  //--------insert range 1-8 --------//--------------\\----Msg id= 10--------Range 10-20------------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfRangesNotCrossedButMessageIsTheStartIdAnotherRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(10, 20, channelId),
                LoadRangeEntity(40, 50, channelId),
            ))

        val start = 1L
        val end = 8L
        val msgId = 10L
        updater.updateLoadRange(messageId = msgId, start = start, end = end, channelId = channelId)

        val allRanges = rangeDao.getAll(channelId)
        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = msgId, channelId = channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()

        Truth.assertThat(range.startId == 1L && range.endId == 20L && allRanges.size == 2).isTrue()
    }



    /**  //--------Range 10-20--------//-----\\----Msg id= 20--------insert range 25-40 ------------\\
     *    ----------------------------------------------------- */
    @Test
    fun insertRangeShouldUpdateBoundsIfRangesNotCrossedButMessageIsTheEndAnotherRange() = runTest {
        rangeDao.insertAll(
            listOf(
                LoadRangeEntity(10, 20, channelId),
                LoadRangeEntity(50, 60, channelId),
            ))

        val start = 25L
        val end = 40L
        val msgId = 20L
        updater.updateLoadRange(messageId = msgId, start = start, end = end, channelId = channelId)

        val allRanges = rangeDao.getAll(channelId)
        val ranges = rangeDao.getLoadRanges(start = start, end = end, messageId = msgId, channelId = channelId)
        Log.d("ranges", "$ranges ")
        Log.d("allRanges", "$allRanges")
        val range = ranges.single()

        Truth.assertThat(range.startId == 10L && range.endId == 40L && allRanges.size == 2).isTrue()
    }
}