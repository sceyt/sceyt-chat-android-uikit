package com.sceyt.chat.demo.presentation.main.profile

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.databinding.FragmentProfileBinding
import com.sceyt.chat.demo.presentation.login.LoginActivity
import com.sceyt.chat.demo.presentation.main.profile.edit.EditProfileFragment
import com.sceyt.chatuikit.config.defaults.DefaultMuteNotificationOptions
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.isNightMode
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.presentation.common.SceytDialog
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.MuteNotificationDialog
import com.sceyt.chatuikit.presentation.components.profile.viewmodel.ProfileViewModel
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.sceyt.chatuikit.R as SceytKitR

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private lateinit var displayNameDefaultBg: Drawable
    private val viewModel by viewModels<ProfileViewModel>()
    private val preference by inject<AppSharedPreference>()
    private var currentUser: SceytUser? = null
    private var avatarUrl: String? = null
    private var muted: Boolean = false
    private var updateThemeJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentProfileBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpThemeSwitch()
        binding.initViews()
        initViewModel()
        viewModel.getSettings()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getCurrentUser()
    }

    private fun initViewModel() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) {
            setUserDetails(user = it)
        }

        viewModel.settingsLiveData.observe(viewLifecycleOwner) {
            muted = it.mute.isMuted
            binding.switchNotifications.isChecked = !it.mute.isMuted
        }

        viewModel.muteUnMuteLiveData.observe(viewLifecycleOwner) {
            muted = it
            binding.switchNotifications.isChecked = !muted
        }

        viewModel.logOutLiveData.observe(viewLifecycleOwner) {
            preference.setString(AppSharedPreference.PREF_USER_ID, null)
            preference.setString(AppSharedPreference.PREF_USER_TOKEN, null)
            LoginActivity.launch(requireContext())
            requireActivity().finish()
        }

        viewModel.logOutErrorLiveData.observe(viewLifecycleOwner) {
            customToastSnackBar(it.toString())
        }
    }

    private fun FragmentProfileBinding.initViews() {
        displayNameDefaultBg = binding.displayName.background
        switchNotifications.setOnlyClickable()

        avatar.setAvatarImageLoadListener {
            loadingProfileImage.isVisible = it
        }

        switchNotifications.setOnClickListener {
            if (muted) {
                viewModel.unMuteNotifications()
                switchNotifications.isChecked = true
            } else {
                MuteNotificationDialog.showDialog(
                    context = requireContext(),
                    title = requireContext().getString(R.string.mute_notifications),
                    options = DefaultMuteNotificationOptions.getOptions(requireContext())
                ) {
                    viewModel.muteNotifications(it.timeInterval)
                    switchNotifications.isChecked = false
                }
            }
        }

        tvEdit.setOnClickListener {
            openEditProfileFragment()
        }

        logout.setOnClickListener {
            SceytDialog(requireContext()).setTitle(getString(R.string.log_out_title))
                .setDescription(getString(R.string.log_out_desc))
                .setPositiveButtonTitle(getString(R.string.log_out))
                .setPositiveButtonTextColor(requireContext().getCompatColor(SceytKitR.color.sceyt_color_warning))
                .setPositiveButtonClickListener {
                    viewModel.logout()
                }
                .show()
        }
    }

    private fun setUserDetails(user: SceytUser?) {
        currentUser = user
        avatarUrl = user?.avatarURL
        user?.apply {
            binding.avatar.setUserAvatar(user)
            var displayName = fullName.trim()
            if (displayName.isBlank())
                displayName = "@${user.id}"
            binding.displayName.text = displayName
        }
    }

    private fun setUpThemeSwitch() {
        binding.switchTheme.isChecked = requireContext().isNightMode()
        binding.switchTheme.setOnClickListener {
            updateThemeJob?.cancel()
            updateThemeJob = lifecycleScope.launch {
                delay(250)
                val isDarkMode = binding.switchTheme.isChecked
                if (isDarkMode) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun openEditProfileFragment() {
        requireActivity().supportFragmentManager.commit {
            setCustomAnimations(com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right, 0, 0, com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right)
            replace(R.id.mainContainer, EditProfileFragment())
            addToBackStack(null)
        }
    }
}