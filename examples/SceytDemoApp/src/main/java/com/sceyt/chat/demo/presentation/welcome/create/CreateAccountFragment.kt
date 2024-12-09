package com.sceyt.chat.demo.presentation.welcome.create

import android.animation.LayoutTransition
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.demo.databinding.FragmentCreateAccountBinding
import com.sceyt.chat.demo.presentation.common.ui.handleUsernameValidation
import com.sceyt.chat.demo.presentation.main.MainActivity
import com.sceyt.chat.demo.presentation.welcome.WelcomeActivity
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel

class CreateAccountFragment : Fragment() {
    private val viewModel: CreateAccountViewModel by viewModel()
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnNext.setEnabledOrNot(false)

        initViewModel()
        binding.initViews()
        initClickListeners()
        binding.btnNext.setEnabledOrNot(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
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
                with(requireActivity()) {
                    launchActivity<MainActivity>()
                    finish()
                }
            }
        }

        viewModel.correctUsernameValidatorLiveData.observe(viewLifecycleOwner) {
            handleUsernameValidation(
                context = requireContext(),
                validationState = it,
                setAlert = { color, message ->
                    setUsernameAlert(color, message)
                },
                isUsernameCorrect = { isUsernameCorrect ->
                    viewModel.setUserNameValidState(isUsernameCorrect)
                }
            )
        }

        viewModel.usernameInput
            .onEach { username ->
                viewModel.updateUsernameInput(username)
            }.launchIn(lifecycleScope)

        viewModel.nextButtonEnabledLiveData.observe(viewLifecycleOwner) {
            binding.btnNext.setEnabledOrNot(it)
        }
    }

    private fun FragmentCreateAccountBinding.initViews() {
        binding.root.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }

        etUserName.doAfterTextChanged {
            tvUsernameAlert.isVisible = it.isNotNullOrBlank()
            viewModel.updateUsernameInput(it.toString())
        }
        etFirstName.doAfterTextChanged {
            viewModel.setFirstNameValidState(it.isNotNullOrBlank())
        }

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

    private fun initClickListeners() {
        with(binding) {
            toolbar.setNavigationClickListener {
                (activity as? WelcomeActivity)?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun setUsernameAlert(color: Int, message: String) {
        binding.apply {
            tvUsernameAlert.text = message
            tvUsernameAlert.setTextColor(color)
        }
    }
}