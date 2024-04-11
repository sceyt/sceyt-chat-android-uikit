package com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.databinding.SceytFragmentReactedUsersBinding
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.viewmodels.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class FragmentReactedUsers : Fragment(), SceytKoinComponent {
    private lateinit var binding: SceytFragmentReactedUsersBinding
    private val viewModel: ReactionsInfoViewModel by inject()
    private var usersAdapter: ReactedUsersAdapter? = null
    private var clickListener: ((SceytReaction) -> Unit)? = null
    private lateinit var key: String
    private var messageId: Long = 0
    private val loafMoreDebounce by lazy { DebounceHelper(100, this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentReactedUsersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getBundleArguments()
        initViewModel()
        viewModel.getReactions(messageId, 0, key)
    }

    private fun getBundleArguments() {
        key = arguments?.getString(REACTIONS_KEY) ?: ""
        messageId = arguments?.getLong(MESSAGE_ID_KEY) ?: 0
    }

    private fun initViewModel() {
        viewModel.loadReactIonsLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is PaginationResponse.DBResponse -> {
                    if (!viewModel.checkIgnoreDatabasePagingResponse(it)) {
                        lifecycleScope.launch {
                            val data = viewModel.initDbResponse(it, usersAdapter?.currentList)
                            setOrUpdateUsersAdapter(data)
                        }
                    }
                }

                is PaginationResponse.ServerResponse -> {
                    lifecycleScope.launch {
                        val data = viewModel.initServerResponse(it)
                        setOrUpdateUsersAdapter(data)
                    }
                }

                is PaginationResponse.Nothing -> return@observe
            }
        }
    }

    private fun setOrUpdateUsersAdapter(reactions: List<ReactedUserItem>) {
        if (usersAdapter == null) {
            usersAdapter = ReactedUsersAdapter {
                clickListener?.invoke(it.reaction)
            }.also { it.submitList(reactions) }

            binding.tvUsers.apply {
                adapter = usersAdapter
                itemAnimator = DefaultItemAnimator().apply {
                    addDuration = 100
                    removeDuration = 100
                    moveDuration = 100
                }

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (isLastItemDisplaying()) {
                            loafMoreDebounce.submit {
                                if (!viewModel.canLoadNext()) return@submit

                                val offset = usersAdapter?.getSkip() ?: 0
                                viewModel.getReactions(messageId, offset, key)
                            }
                        }
                    }
                })
            }
        } else
            usersAdapter?.submitList(reactions)
    }

    fun setClickListener(listener: (SceytReaction) -> Unit) {
        clickListener = listener
    }

    fun getKey(): String? {
        return if (::key.isInitialized)
            key
        else null
    }

    fun update() {
        viewModel.getReactions(messageId, 0, key)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersAdapter = null
    }

    companion object {
        private const val REACTIONS_KEY = "REACTIONS_KEY"
        private const val MESSAGE_ID_KEY = "MESSAGE_ID_KEY"

        fun newInstance(messageId: Long, key: String): FragmentReactedUsers {
            return FragmentReactedUsers().apply {
                setBundleArguments {
                    putString(REACTIONS_KEY, key)
                    putLong(MESSAGE_ID_KEY, messageId)
                }
            }
        }
    }
}