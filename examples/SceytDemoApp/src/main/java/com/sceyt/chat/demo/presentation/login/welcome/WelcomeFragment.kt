package com.sceyt.chat.demo.presentation.login.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sceyt.chat.demo.databinding.FragmentWelcomeBinding
import com.sceyt.chat.demo.presentation.login.LoginActivity
import com.sceyt.chat.demo.presentation.login.SelectAccountBottomSheetFragment
import com.sceyt.chat.demo.presentation.login.create.CreateProfileFragment

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initClickListeners() {
        with(binding) {
            btnCreateAccount.setOnClickListener {
                (activity as? LoginActivity)?.openFragment(CreateProfileFragment())
            }
            btnChooseAccount.setOnClickListener {
                SelectAccountBottomSheetFragment().show(parentFragmentManager, "SelectAccountBottomSheetFragment")
            }
        }
    }
}