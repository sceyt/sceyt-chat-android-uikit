package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.stickydate


interface StickyHeaderInterface {
    /**
     * This method gets called by [DateStickHeaderItemDecoration] to setup the header View.
     * @param header View. Header to set the data on.
     * @param headerPosition int. Position of the header item in the adapter.
     */
    fun bindHeaderData(header: StycyDateView, headerPosition: Int)

    /**
     * This method gets called by [DateStickHeaderItemDecoration] to verify whether the item represents a header.
     * @param itemPosition int.
     * @return true, if item at the specified adapter's position represents a header.
     */
    fun isHeader(itemPosition: Int): Boolean
}