package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import android.content.Context
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
import com.sceyt.chatuikit.extensions.isLastItemDisplaying
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.viewmodel.ReactionsInfoViewModel
import com.sceyt.chatuikit.styles.reactions_info.ReactedUserListStyle
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class FragmentReactedUsers : Fragment, SceytKoinComponent {
    constructor() : super()

    constructor(style: ReactedUserListStyle) : super() {
        this.style = style
    }

    private lateinit var binding: SceytFragmentReactedUsersBinding
    private val viewModel: ReactionsInfoViewModel by viewModel(parameters = {
        parametersOf(
            arguments?.getString(REACTIONS_KEY) ?: "",
            arguments?.getLong(MESSAGE_ID_KEY) ?: 0,
        )
    })

    private var usersAdapter: ReactedUsersAdapter? = null
    private var clickListener: ((SceytReaction) -> Unit)? = null
    private lateinit var style: ReactedUserListStyle
    private val loafMoreDebounce by lazy { DebounceHelper(100, this) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Keep the style in the view model.
        // If the style is not initialized it will be taken from the view model.
        if (::style.isInitialized)
            viewModel.style = style
        else
            style = viewModel.style
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentReactedUsersBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.applyStyle()
        initViewModel()
        viewModel.getReactions(0)
    }

    private fun initViewModel() {
        viewModel.loadReactionsLiveData.observe(viewLifecycleOwner) {
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
            val factory = ReactedUserViewHolderFactory(style.itemStyle).also {
                it.setClickListener { reaction -> clickListener?.invoke(reaction.reaction) }
            }
            usersAdapter = ReactedUsersAdapter(factory).also { it.submitList(reactions) }

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
                                viewModel.getReactions(offset)
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

    fun getKey(): String {
        return arguments?.getString(REACTIONS_KEY) ?: ""
    }

    fun update() {
        viewModel.getReactions(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        usersAdapter = null
    }

    private fun SceytFragmentReactedUsersBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
    }

    companion object {
        private const val REACTIONS_KEY = "REACTIONS_KEY"
        private const val MESSAGE_ID_KEY = "MESSAGE_ID_KEY"

        fun newInstance(
                messageId: Long,
                key: String,
                style: ReactedUserListStyle
        ) = FragmentReactedUsers(style).apply {
            setBundleArguments {
                putString(REACTIONS_KEY, key)
                putLong(MESSAGE_ID_KEY, messageId)
            }
        }
    }
}
