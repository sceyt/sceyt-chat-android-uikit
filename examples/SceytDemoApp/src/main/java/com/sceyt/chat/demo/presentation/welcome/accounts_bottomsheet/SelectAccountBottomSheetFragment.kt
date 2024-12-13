package com.sceyt.chat.demo.presentation.welcome.accounts_bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.demo.databinding.FragmentBottomSheetSelectAccountBinding
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID_REQUEST
import com.sceyt.chat.demo.presentation.welcome.adapters.SceytUsersAdapter
import com.sceyt.chatuikit.R
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectAccountBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentBottomSheetSelectAccountBinding
    private val viewModel: SelectAccountsBottomSheetViewModel by viewModel()

    private val usersAdapter = SceytUsersAdapter { user ->
        val id = user.id
        setFragmentResult(KEY_USER_ID_REQUEST, bundleOf(KEY_USER_ID to id))
        dismiss()
    }

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
        initViewModel()
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

    private fun initViewModel() {
        viewModel.accountsLiveData.observe(viewLifecycleOwner) {
            usersAdapter.submitList(it)
        }
    }

    private fun initRecyclerView() {
        binding.rvAccounts.adapter = usersAdapter
    }
}