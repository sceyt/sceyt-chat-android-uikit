package com.sceyt.chat.ui.presentation.uicomponents.conversation.conversationinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chat.ui.databinding.FragmentChannelMembersBinding

class ChannelMembersFragment : Fragment() {
    private lateinit var binding: FragmentChannelMembersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupList() {
       /* listAdapter = ChannelMembersAdapter(object : ChannelMemberViewHolder.Callbacks {
            override fun changeRole(member: Member) {
                viewModel.onMemberSelectedForChangeRole(member)
                val action =
                        ChannelInfoFragmentDirections.actionChannelInfoFragmentToChooseRoleFragment()
                findNavController().navigate(action)
            }

            override fun setOwner(member: Member) {
                viewModel.onMemberSelectedForChangeRole(member)
                changeRole(Role("owner"))
            }

            override fun removeMember(member: Member) {
                viewModel.deleteMember(member)
            }

            override fun blockAndRemoveMember(member: Member) {
                viewModel.blockAndDeleteMember(member)
            }
        })

        channelMembersList.apply {
            setHasFixedSize(true)
            adapter = listAdapter
        }*/
    }
}