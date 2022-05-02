package com.sceyt.chat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sceyt.chat.Types
import com.sceyt.chat.ui.databinding.FragmentChannelsBinding
import com.sceyt.chat.ui.presentation.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.channels.viewmodels.bindSearchView
import com.sceyt.chat.ui.presentation.channels.viewmodels.bindView


class ChannelsFragment : Fragment() {
    private lateinit var mBinding: FragmentChannelsBinding
    private val mViewModel: ChannelsViewModel by viewModels { MyViewModelFactory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentChannelsBinding.inflate(inflater, container, false).also {
            mBinding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel.bindView(mBinding.channelListView, viewLifecycleOwner)
        mViewModel.bindSearchView(mBinding.searchView)


        (requireActivity().application as SceytUiKitApp).sceytConnectionStatus.observe(viewLifecycleOwner) {
            if (it == Types.ConnectState.StateConnected) {
                mViewModel.loadChannels(0)
            }
        }
    }

    class MyViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChannelsViewModel() as T
        }
    }
}