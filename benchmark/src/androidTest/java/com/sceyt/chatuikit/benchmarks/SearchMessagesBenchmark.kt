package com.sceyt.chatuikit.benchmarks

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchMessagesBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun searchMessages_singleWordQuery_inJoinedChannels() {
        benchmarkSearch(query = SearchMessagesBenchmarkFixture.SINGLE_WORD_QUERY, senderId = null)
    }

    @Test
    fun searchMessages_multiWordQuery_inJoinedChannels() {
        benchmarkSearch(query = SearchMessagesBenchmarkFixture.MULTI_WORD_QUERY, senderId = null)
    }

    @Test
    fun searchMessages_senderFilteredQuery_inJoinedChannels() {
        benchmarkSearch(
            query = SearchMessagesBenchmarkFixture.SINGLE_WORD_QUERY,
            senderId = SearchMessagesBenchmarkFixture.ALICE_ID,
        )
    }

    @Test
    fun searchMessages_blankQuery_senderFilteredInJoinedChannels() {
        benchmarkSearch(query = "", senderId = SearchMessagesBenchmarkFixture.SELECTED_MEMBER_ID)
    }

    private fun benchmarkSearch(
        query: String,
        senderId: String?,
    ) {
        val harness = requireNotNull(searchBenchmarkHarness)
        var resultSize = 0
        var firstMessageId = -1L

        benchmarkRule.measureRepeated {
            val summary = harness.search(
                query = query,
                senderId = senderId,
            )
            resultSize = summary.resultSize
            firstMessageId = summary.firstMessageId
        }

        assertEquals(SearchMessagesBenchmarkFixture.QUERY_LIMIT, resultSize)
        assertTrue(firstMessageId > 0L)
    }

    companion object {
        private var searchBenchmarkHarness: SearchMessagesBenchmarkBridge? = null

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            searchBenchmarkHarness = SearchMessagesBenchmarkBridge.create(context).also {
                it.seedAndVerify()
            }
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            searchBenchmarkHarness?.close()
            searchBenchmarkHarness = null
        }
    }
}
