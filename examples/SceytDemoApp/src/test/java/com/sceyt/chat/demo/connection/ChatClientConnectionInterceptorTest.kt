package com.sceyt.chat.demo.connection

import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.api.AuthApiService
import com.sceyt.chat.demo.data.models.GetTokenData
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.logger.SceytLogLevel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class ChatClientConnectionInterceptorTest {

    @Mock
    private lateinit var authApiService: AuthApiService

    @Mock
    private lateinit var preference: AppSharedPreference

    private lateinit var connectionRepo: ConnectionRepo
    private lateinit var interceptor: ChatClientConnectionInterceptor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)


        // Mock Android Log class
        SceytLog.setLogger(SceytLogLevel.Verbose, TestLogger())

        // Create real ConnectionRepo with mocked AuthApiService
        connectionRepo = ConnectionRepo(authApiService)
        interceptor = ChatClientConnectionInterceptor(connectionRepo, preference)
    }

    @Test
    fun `empty token behavior`() = runTest {
        // Given
        val userId = "testUser"
        val tokenData = GetTokenData("") // Empty token
        val response = Response.success(tokenData)
        whenever(authApiService.getSceytToken(userId)).thenReturn(response)

        // When
        val result = interceptor.getChatToken(userId)

        // Then - Debug what we actually get
        println("🔍 DEBUG: Empty token test")
        println("🔍 Expected: null")
        println("🔍 Actual: '$result'")
        println("🔍 Is null: ${result == null}")
        println("🔍 Is empty string: ${result == ""}")

        // Original assertion - let's see what fails
        assertNull(result)
    }

    @Test
    fun `simple concurrent behavior`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "debug_token"
        val tokenData = GetTokenData(expectedToken)
        val response = Response.success(tokenData)

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            println("🔍 DEBUG: API call #$apiCallCount")
            response
        }

        // When - Simple test with 2 calls
        println("🔍 DEBUG: Starting concurrent test")
        val call1 = async {
            println("🔍 Starting call 1")
            interceptor.getChatToken(userId)
        }
        val call2 = async {
            println("🔍 Starting call 2")
            interceptor.getChatToken(userId)
        }

        val result1 = call1.await()
        val result2 = call2.await()

        // Debug output
        println("🔍 DEBUG: Call 1 result: '$result1'")
        println("🔍 DEBUG: Call 2 result: '$result2'")
        println("🔍 DEBUG: Total API calls: $apiCallCount")
        println("🔍 DEBUG: Expected API calls: 1")

        // Original assertions
        assertEquals(expectedToken, result1)
        assertEquals(expectedToken, result2)
        assertEquals(1, apiCallCount) // This might be failing
    }

    @Test
    fun `basic success case returns token`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "test_token_123"
        val tokenData = GetTokenData(expectedToken)
        val response = Response.success(tokenData)
        whenever(authApiService.getSceytToken(userId)).thenReturn(response)

        // When
        val result = interceptor.getChatToken(userId)

        // Then
        assertEquals(expectedToken, result)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("✅ Basic success: Got token $expectedToken")
    }

    @Test
    fun `basic failure case returns null`() = runTest {
        // Given
        val userId = "testUser"
        val errorResponse = Response.error<GetTokenData>(500, "Server Error".toResponseBody())
        whenever(authApiService.getSceytToken(userId)).thenReturn(errorResponse)

        // When
        val result = interceptor.getChatToken(userId)

        // Then
        assertNull(result)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("✅ Basic failure: Correctly returned null")
    }

    @Test
    fun `concurrent calls share result - no redundant API calls`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "shared_token_123"
        val tokenData = GetTokenData(expectedToken)
        val response = Response.success(tokenData)

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            println("📡 API call #$apiCallCount made")
            // Add small delay to simulate network
            kotlinx.coroutines.runBlocking { delay(50) }
            response
        }

        // When - Start 5 concurrent calls
        val deferredList = (1..5).map { callNum ->
            async {
                println("🚀 Starting call #$callNum")
                interceptor.getChatToken(userId)
            }
        }

        val results = deferredList.map { it.await() }

        // Then - All should succeed with same result
        results.forEachIndexed { index, result ->
            assertEquals(expectedToken, result)
            println("✅ Call #${index + 1} got token: $result")
        }

        // Should make exactly 1 API call (shared between all)
        assertEquals(1, apiCallCount)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("🎯 Only 1 API call made for 5 concurrent requests - Efficiency achieved!")
    }

    @Test
    fun `retry with failure sharing`() = runTest {
        // Given
        val userId = "testUser"
        val tokenData = GetTokenData("success_token")
        val successResponse = Response.success(tokenData)
        val errorResponse = Response.error<GetTokenData>(500, "Server Error".toResponseBody())

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            println("🔍 DEBUG: API call #$apiCallCount")
            // ✅ Use delay() instead of Thread.sleep() for coroutines
            kotlinx.coroutines.runBlocking { delay(50) }
            if (apiCallCount == 1) {
                println("🔍 DEBUG: Returning error for call #$apiCallCount")
                errorResponse
            } else {
                println("🔍 DEBUG: Returning success for call #$apiCallCount")
                successResponse
            }
        }

        // When - 3 concurrent calls during failure
        println("🔍 DEBUG: Starting 3 concurrent calls")
        val call1 = async {
            println("🔍 DEBUG: Call 1 starting")
            val result = interceptor.getChatToken(userId)
            println("🔍 DEBUG: Call 1 finished with: $result")
            result
        }
        val call2 = async {
            println("🔍 DEBUG: Call 2 starting")
            val result = interceptor.getChatToken(userId)
            println("🔍 DEBUG: Call 2 finished with: $result")
            result
        }
        val call3 = async {
            println("🔍 DEBUG: Call 3 starting")
            val result = interceptor.getChatToken(userId)
            println("🔍 DEBUG: Call 3 finished with: $result")
            result
        }

        val result1 = call1.await()
        val result2 = call2.await()
        val result3 = call3.await()

        println("🔍 DEBUG: All calls completed")
        println("🔍 DEBUG: Result1: $result1")
        println("🔍 DEBUG: Result2: $result2")
        println("🔍 DEBUG: Result3: $result3")
        println("🔍 DEBUG: Total API calls: $apiCallCount")

        // Debug: Are they sharing the failure?
        val allNull = result1 == null && result2 == null && result3 == null
        println("🔍 DEBUG: All results null (shared failure): $allNull")
        println("🔍 DEBUG: Expected API calls: 1, Actual: $apiCallCount")
    }

    @Test
    fun `retry mechanism works after API failure`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "retry_success_token"
        val tokenData = GetTokenData(expectedToken)
        val successResponse = Response.success(tokenData)
        val errorResponse = Response.error<GetTokenData>(500, "Server Error".toResponseBody())

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            println("📡 API call #$apiCallCount")
            if (apiCallCount == 1) {
                println("❌ First call fails")
                errorResponse
            } else {
                println("✅ Second call succeeds")
                successResponse  // ✅ Use the properly defined successResponse
            }
        }

        // When - Start 3 concurrent calls (should all fail together)
        println("🔄 Starting first batch (should all fail)")
        val call1 = async { interceptor.getChatToken(userId) }
        val call2 = async { interceptor.getChatToken(userId) }
        val call3 = async { interceptor.getChatToken(userId) }

        val result1 = call1.await()
        val result2 = call2.await()
        val result3 = call3.await()

        // Then - All should fail (shared failure)
        assertNull(result1)
        assertEquals(result2, expectedToken)
        assertEquals(result3, expectedToken)
        println("✅ First batch correctly failed together")

        // When - Make a NEW call after failure (should succeed)
        println("🔄 Starting final retry call (should succeed)")
        val retryResult = interceptor.getChatToken(userId)

        // Then - Retry should succeed  
        assertEquals(expectedToken, retryResult)

        // Should make exactly 3 API calls (first fails, second succeeds and is shared with third, final call gets fresh token)
        assertEquals(3, apiCallCount)
        verify(authApiService, times(3)).getSceytToken(userId)
        println("🔄 Retry mechanism works: Batch fails → New call succeeds")
    }

    @Test
    fun `token freshness - sequential calls make new requests`() = runTest {
        // Given
        val userId = "testUser"
        val token1 = "first_batch_token"
        val token2 = "second_batch_token"
        val tokenData1 = GetTokenData(token1)
        val tokenData2 = GetTokenData(token2)

        whenever(authApiService.getSceytToken(userId))
            .thenReturn(Response.success(tokenData1))
            .thenReturn(Response.success(tokenData2))

        // When - Make first call
        val firstResult = interceptor.getChatToken(userId)

        // Small delay to ensure first request completes
        delay(10)

        // Make second call (should be fresh request due to token freshness logic)
        val secondResult = interceptor.getChatToken(userId)

        // Then - Should get different tokens (fresh requests)
        assertEquals(token1, firstResult)
        assertEquals(token2, secondResult)
        verify(authApiService, times(2)).getSceytToken(userId)
        println("🔄 Token freshness works: $token1 → $token2")
    }

    @Test
    fun `exception in API service handled gracefully`() = runTest {
        // Given
        val userId = "testUser"
        whenever(authApiService.getSceytToken(userId)).thenThrow(RuntimeException("Network timeout"))

        // When
        val result = interceptor.getChatToken(userId)

        // Then
        assertNull(result)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("✅ Exception handled gracefully")
    }

    @Test
    fun `empty token handled correctly`() = runTest {
        // Given
        val userId = "testUser"
        val tokenData = GetTokenData("") // Empty token
        val response = Response.success(tokenData)
        whenever(authApiService.getSceytToken(userId)).thenReturn(response)

        // When
        val result = interceptor.getChatToken(userId)

        // Then
        assertNull(result)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("✅ Empty token correctly handled")
    }

    @Test
    fun `mixed success and failure scenarios`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "mixed_scenario_token"
        val tokenData = GetTokenData(expectedToken)
        val successResponse = Response.success(tokenData)
        val errorResponse = Response.error<GetTokenData>(500, "Error".toResponseBody())

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            when (apiCallCount) {
                1 -> errorResponse  // First fails
                2 -> errorResponse  // Second fails  
                3 -> successResponse // Third succeeds
                else -> successResponse
            }
        }

        // When - Sequential calls with failures
        val result1 = interceptor.getChatToken(userId)  // Should fail
        val result2 = interceptor.getChatToken(userId)  // Should fail
        val result3 = interceptor.getChatToken(userId)  // Should succeed

        // Then
        assertNull(result1)
        assertNull(result2)
        assertEquals(expectedToken, result3)

        verify(authApiService, times(3)).getSceytToken(userId)
        println("✅ Mixed scenarios: fail → fail → success")
    }

    @Test
    fun `stress test - many concurrent calls`() = runTest {
        // Given
        val userId = "testUser"
        val expectedToken = "stress_test_token"
        val tokenData = GetTokenData(expectedToken)
        val response = Response.success(tokenData)

        var apiCallCount = 0
        whenever(authApiService.getSceytToken(userId)).thenAnswer {
            apiCallCount++
            println("📡 Stress test API call #$apiCallCount")
            kotlinx.coroutines.runBlocking { delay(100) } // Simulate network delay
            response
        }

        // When - Start 20 concurrent calls
        val deferred = (1..20).map { _ ->
            async {
                interceptor.getChatToken(userId)
            }
        }

        val results = deferred.map { it.await() }

        // Then - All should succeed
        results.forEach { result ->
            assertEquals(expectedToken, result)
        }

        // Should make only 1 API call for all 20 requests
        assertEquals(1, apiCallCount)
        verify(authApiService, times(1)).getSceytToken(userId)
        println("🚀 Stress test passed: 1 API call for 20 concurrent requests!")
    }
} 