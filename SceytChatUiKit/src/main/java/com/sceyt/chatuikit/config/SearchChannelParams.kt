package com.sceyt.chatuikit.config

import com.sceyt.chat.models.SearchQueryOperator
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListFilterKey

/**
 * Params for searching channel list.
 * @param queryType Type of search query
 * @param filterKey Filter key for the search query.
 * */
data class SearchChannelParams(
        val queryType: SearchQueryOperator,
        val filterKey: ChannelListFilterKey,
) {

    companion object {
        val default = SearchChannelParams(
            queryType = SearchQueryOperator.SearchQueryOperatorContains,
            filterKey = ChannelListFilterKey.ListQueryChannelFilterKeySubject,
        )
    }
}