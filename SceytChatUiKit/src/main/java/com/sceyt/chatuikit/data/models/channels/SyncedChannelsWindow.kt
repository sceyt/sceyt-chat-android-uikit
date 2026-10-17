package com.sceyt.chatuikit.data.models.channels

/**
 * The loaded channel-list window rebuilt from the DB after a channel sync finishes.
 *
 * @property channels the sorted window the UI should display (DB top-N plus visible pending channels).
 * @property hasNext whether more channels exist in the DB beyond the rebuilt window.
 * @property loadedCount number of DB-backed channels in the window (excludes pending); used as the next paging offset.
 */
data class SyncedChannelsWindow(
    val channels: List<SceytChannel>,
    val hasNext: Boolean,
    val loadedCount: Int,
)
