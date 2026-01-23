package com.sceyt.chat.demo.presentation.welcome.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.sceyt.chat.demo.databinding.FragmentWelcomeBinding
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID_REQUEST
import com.sceyt.chat.demo.presentation.main.MainActivity
import com.sceyt.chat.demo.presentation.welcome.WelcomeActivity
import com.sceyt.chat.demo.presentation.welcome.accounts_bottomsheet.SelectAccountBottomSheetFragment
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.presentation.common.dialogs.SceytLoader.hideLoading
import com.sceyt.chatuikit.presentation.common.dialogs.SceytLoader.showLoading
import com.sceyt.chatuikit.presentation.root.PageState
import org.koin.androidx.viewmodel.ext.android.viewModel

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WelcomeViewModel by viewModel()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListeners()
        initViewModel()
        setResultListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViewModel() {
        viewModel.logInLiveData.observe(viewLifecycleOwner) {
            if (it) {
                with(requireActivity()) {
                    launchActivity<MainActivity>()
                    finish()
                }
            }
        }

        viewModel.pageStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is PageState.StateLoading -> showLoading(requireContext())
                is PageState.StateError -> {
                    customToastSnackBar(it.errorMessage)
                    hideLoading()
                }

                else -> hideLoading()
            }
        }
    }

    private fun initClickListeners() {
        with(binding) {
            btnCreateAccount.setOnClickListener {
                (activity as? WelcomeActivity)?.openCreateAccountFragment()
            }

            btnChooseAccount.setOnClickListener {
                SelectAccountBottomSheetFragment().show(
                    parentFragmentManager,
                    SelectAccountBottomSheetFragment::class.java.simpleName
                )
            }
        }
    }

    private fun setResultListener() {
        setFragmentResultListener(KEY_USER_ID_REQUEST) { _, bundle ->
            if (!isAdded) return@setFragmentResultListener
            val userId = bundle.getString(KEY_USER_ID)
            viewModel.loginUser(userId = userId ?: return@setFragmentResultListener)
        }
    }
}