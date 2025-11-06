package com.sceyt.chatuikit.persistence.database

internal object DatabaseConstants {
    // Database name
    const val SCEYT_CHAT_UI_KIT_DATABASE_NAME = "sceyt_ui_kit_database"

    // User
    const val USER_TABLE = "sceyt_user_table"
    const val USER_METADATA_TABLE = "sceyt_user_metadata_table"

    // Channel
    const val CHANNEL_TABLE = "sceyt_channel_table"
    const val CHAT_USER_REACTION_TABLE = "sceyt_chat_user_reaction_table"
    const val USER_CHAT_LINK_TABLE = "sceyt_user_chat_link_table"

    // Message
    const val MESSAGE_TABLE = "sceyt_message_table"
    const val DRAFT_MESSAGE_TABLE = "sceyt_draft_message_table"
    const val DRAFT_ATTACHMENT_TABLE = "sceyt_draft_attachment_table"
    const val DRAFT_VOICE_ATTACHMENT_TABLE = "sceyt_draft_voice_attachment_table"
    const val DRAFT_MESSAGE_USER_LINK_TABLE = "sceyt_draft_message_user_link_table"
    const val AUTO_DELETE_MESSAGES_TABLE = "sceyt_auto_delete_messages_table"
    const val PENDING_MESSAGE_STATE_TABLE = "sceyt_pending_message_state_table"
    const val LOAD_RANGE_TABLE = "sceyt_load_range_table"

    // Attachment
    const val ATTACHMENT_TABLE = "sceyt_attachment_table"
    const val ATTACHMENT_PAYLOAD_TABLE = "sceyt_attachment_payload_table"
    const val FILE_CHECKSUM_TABLE = "sceyt_file_checksum_table"
    const val LINK_DETAILS_TABLE = "sceyt_link_details_table"

    // Reaction
    const val REACTION_TABLE = "sceyt_reaction_table"
    const val REACTION_TOTAL_TABLE = "sceyt_reaction_total_table"
    const val PENDING_REACTION_TABLE = "sceyt_pending_reaction_table"

    // Mentions
    const val MENTION_USER_MESSAGE_LINK_TABLE = "sceyt_mention_user_message_link_table"

    // Marker
    const val MARKER_TABLE = "sceyt_marker_table"
    const val PENDING_MARKER_TABLE = "sceyt_pending_marker_table"

    // Poll
    const val POLL_TABLE = "sceyt_poll_table"
    const val POLL_OPTION_TABLE = "sceyt_poll_option_table"
    const val POLL_VOTE_TABLE = "sceyt_poll_vote_table"
    const val PENDING_POLL_VOTE_TABLE = "sceyt_pending_poll_vote_table"
}