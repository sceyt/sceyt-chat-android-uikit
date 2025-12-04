package com.sceyt.chatuikit.services

import com.sceyt.chat.wrapper.ClientWrapper
import java.util.concurrent.atomic.AtomicLong

/**
 * Manage server time synchronization.
 * Calculates and maintains the time difference between local device time and server time.
 */
object ServerTimeSync {

    /**
     * Time difference in milliseconds between server time and local time.
     * Positive value means server is ahead of local time.
     * Negative value means server is behind local time.
     */
    private val timeDiffMillis = AtomicLong(0L)

    /**
     * Indicates whether the time difference has been calculated at least once.
     */
    @Volatile
    private var isInitialized = false

    /**
     * Updates the server time synchronization based on the last authentication time.
     * This should be called after successful connection to the chat server.
     *
     * The ClientWrapper.getLastAuthTime() returns the server timestamp when authentication occurred.
     * We compare it with the current local time to calculate the difference.
     */
    fun updateServerTime() {
        val serverAuthTime = ClientWrapper.getLastAuthTime()
        if (serverAuthTime > 0) {
            val localTime = System.currentTimeMillis()
            val diff = serverAuthTime - localTime
            timeDiffMillis.set(diff)
            isInitialized = true
        }
    }

    /**
     * Returns the current server time in milliseconds.
     *
     * @return Current server time calculated as: local time + time difference
     * If not initialized, returns current local time.
     */
    fun getCurrentServerTime(): Long {
        return if (isInitialized) {
            System.currentTimeMillis() + timeDiffMillis.get()
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * Returns the last authentication time.
     *
     * @return Last authentication time in milliseconds.
     * */
    fun getLastAuthTime(): Long {
        return ClientWrapper.getLastAuthTime()
    }

    /**
     * Returns the time difference between server and local time in milliseconds.
     * Positive value means server is ahead of local time.
     *
     * @return Time difference in milliseconds, or 0 if not initialized
     */
    fun getTimeDifference(): Long {
        return timeDiffMillis.get()
    }

    /**
     * Checks if the server time has been synchronized at least once.
     *
     * @return true if updateServerTime() has been called successfully, false otherwise
     */
    fun isTimeSynchronized(): Boolean {
        return isInitialized
    }

    /**
     * Converts a local timestamp to server timestamp.
     *
     * @param localTime Local timestamp in milliseconds
     * @return Corresponding server timestamp
     */
    fun localToServerTime(localTime: Long): Long {
        return if (isInitialized) {
            localTime + timeDiffMillis.get()
        } else {
            localTime
        }
    }

    /**
     * Converts a server timestamp to local timestamp.
     *
     * @param serverTime Server timestamp in milliseconds
     * @return Corresponding local timestamp
     */
    fun serverToLocalTime(serverTime: Long): Long {
        return if (isInitialized) {
            serverTime - timeDiffMillis.get()
        } else {
            serverTime
        }
    }

    /**
     * Resets the synchronization state.
     * This can be called on logout or when connection is lost.
     */
    fun reset() {
        timeDiffMillis.set(0L)
        isInitialized = false
    }
}