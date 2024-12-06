package com.sceyt.chat.demo.presentation.login.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.sceyt.chat.demo.databinding.FragmentCreateAccountBinding
import com.sceyt.chat.demo.presentation.login.LoginActivity
import com.sceyt.chat.demo.presentation.main.MainActivity
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.root.PageState
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateProfileFragment : Fragment() {
    private val viewModel: CreateProfileViewModel by viewModel()
    private var _binding: FragmentCreateAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateAccountBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setEnabledOrNot(false)
        if (viewModel.isLoggedIn()) {
            launchMainActivity()
        }
        initViewModel()
        binding.initViews()
        initClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initViewModel() {
        viewModel.pageStateLiveData.observe(viewLifecycleOwner) { pageState ->
            when (pageState) {
                is PageState.StateLoading -> {
                    context?.let { context ->
                        if (pageState.isLoading) {
                            SceytLoader.showLoading(context)
                        } else {
                            SceytLoader.hideLoading()
                        }
                    }
                }

                is PageState.StateError -> customToastSnackBar(
                    pageState.errorMessage
                        ?: return@observe
                )

                else -> return@observe
            }
        }

        viewModel.logInLiveData.observe(viewLifecycleOwner) {
            if (it) {
                launchMainActivity()
            }
        }
    }

    private fun FragmentCreateAccountBinding.initViews() {
        etUserName.doAfterTextChanged { checkIfNextButtonShouldBeEnabled() }
        btnNext.setOnClickListener {
            requireActivity().hideSoftInput()
            viewModel.loginUser(
                etUserName.text?.trim().toString(),
                etFirstName.text?.trim().toString(),
                etLastName.text?.trim().toString(),
                etUserName.text?.trim().toString()
            )
        }

    }

    private fun checkIfNextButtonShouldBeEnabled() {
        binding.apply {
            val shouldEnable = etUserName.text?.isNotEmpty() == true
            btnNext.setEnabledOrNot(shouldEnable)
        }
    }

    private fun launchMainActivity() {
        context?.let { context ->
            context.launchActivity<MainActivity>()
            requireActivity().finish()
        }
    }

    private fun initClickListeners() {
        with(binding) {
            toolbar.setNavigationClickListener {
                (activity as? LoginActivity)?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }
}