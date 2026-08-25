package com.sceyt.chat.demo.presentation.welcome.custom_user_id

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chat.demo.R as DemoR
import com.sceyt.chat.demo.databinding.FragmentBottomSheetCustomUserIdBinding
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID
import com.sceyt.chat.demo.presentation.Constants.KEY_USER_ID_REQUEST
import com.sceyt.chatuikit.R as UIKitR

class CustomUserIdBottomSheetFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentBottomSheetCustomUserIdBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, UIKitR.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBottomSheetCustomUserIdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickListeners()
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

    private fun initClickListeners() {
        binding.btnConnect.setOnClickListener {
            val userId = binding.etUserId.text?.trim().toString()
            if (userId.isBlank()) {
                binding.etUserId.error = getString(DemoR.string.user_id_is_empty)
                return@setOnClickListener
            }

            setFragmentResult(KEY_USER_ID_REQUEST, Bundle().apply {
                putString(KEY_USER_ID, userId)
            })
            dismiss()
        }
    }
}
