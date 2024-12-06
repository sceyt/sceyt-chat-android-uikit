package com.sceyt.chat.demo.presentation.login

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.demo.databinding.FragmentBottomSheetSelectAccountBinding
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytUser

class SelectAccountBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentBottomSheetSelectAccountBinding
    private val usersAdapter = SceytUsersAdapter {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetSelectAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usersAdapter.submitList(dummyUsers)
        initRecyclerView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet =
                    findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).isDraggable = false
            }
        }
    }

    private fun initRecyclerView() {
        binding.rvAccounts.adapter = usersAdapter
    }

    val dummyUsers = listOf(
        SceytUser(
            id = "1",
            username = "john.doe",
            firstName = "John",
            lastName = "Doe",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = mapOf("key1" to "value1"),
            presence = null,
            state = UserState.Active,
            blocked = false
        ),
        SceytUser(
            id = "2",
            username = "jane.smith",
            firstName = "Jane",
            lastName = "Smith",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = mapOf("key2" to "value2"),
            presence = null,
            state = UserState.Active,
            blocked = true
        ),
        SceytUser(
            id = "3",
            username = "mark.brown",
            firstName = "Mark",
            lastName = "Brown",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = null,
            presence = null,
            state = UserState.Active,
            blocked = false
        ),
        SceytUser(
            id = "4",
            username = "anna.white",
            firstName = "Anna",
            lastName = "White",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = null,
            presence = null,
            state = UserState.Active,
            blocked = false
        ),
        SceytUser(
            id = "5",
            username = "mike.jones",
            firstName = "Mike",
            lastName = "Jones",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = mapOf("key5" to "value5"),
            presence = null,
            state = UserState.Active,
            blocked = true
        ),
        SceytUser(
            id = "6",
            username = "emma.davis",
            firstName = "Emma",
            lastName = "Davis",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = null,
            presence = null,
            state = UserState.Active,
            blocked = false
        ),
        SceytUser(
            id = "7",
            username = "daniel.martin",
            firstName = "Daniel",
            lastName = "Martin",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = null,
            presence = null,
            state = UserState.Active,
            blocked = false
        ),
        SceytUser(
            id = "8",
            username = "sophia.lewis",
            firstName = "Sophia",
            lastName = "Lewis",
            avatarURL = "https://fastly.picsum.photos/id/758/200/300.jpg?hmac=lQtDVVjQGklGEIBCA-5yXBI3L8zkkeGObzmCi-rUFKo",
            metadataMap = mapOf("key8" to "value8"),
            presence = null,
            state = UserState.Active,
            blocked = false
        )
    )
}