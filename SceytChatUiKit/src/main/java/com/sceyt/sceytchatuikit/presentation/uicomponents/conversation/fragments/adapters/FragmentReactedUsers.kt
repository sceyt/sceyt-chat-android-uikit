package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.databinding.SceytFragmentReactedUsersBinding

class FragmentReactedUsers : Fragment() {
    private lateinit var binding: SceytFragmentReactedUsersBinding
    private var usersAdapter: ReactedUsersAdapter? = null
    private var data = listOf<Reaction>()
    private var clickListener: ((Reaction) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentReactedUsersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUsersAdapter(data)
    }

    private fun initUsersAdapter(reactions: List<Reaction>) {
        usersAdapter = ReactedUsersAdapter(reactions) {
            clickListener?.invoke(it)
        }
        binding.tvUsers.adapter = usersAdapter
    }

    fun setReactions(reactions: List<Reaction>) {
        data = reactions
    }

    fun setClickListener(listener: (Reaction) -> Unit) {
        clickListener = listener
    }
}