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
import com.sceyt.chat.demo.presentation.main.profile.dialogs.LogoutDialog
import com.sceyt.chat.demo.presentation.main.profile.edit.EditProfileFragment
import com.sceyt.chat.demo.presentation.welcome.WelcomeActivity
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.defaults.DefaultMuteNotificationOptions
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNightMode
import com.sceyt.chatuikit.extensions.setOnlyClickable
import com.sceyt.chatuikit.presentation.components.channel_info.dialogs.MuteNotificationDialog
import com.sceyt.chatuikit.presentation.components.profile.viewmodel.ProfileViewModel
import com.sceyt.chatuikit.presentation.extensions.setUserAvatar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private var displayNameDefaultBg: Drawable? = null
    private val viewModel by viewModels<ProfileViewModel>(
        factoryProducer = { factory }
    )
    private val userProfileViewModel: UserProfileViewModel by viewModel()
    private val preference by inject<AppSharedPreference>()
    private var currentUser: SceytUser? = null
    private var avatarUrl: String? = null
    private var muted: Boolean = false
    private var updateThemeJob: Job? = null

    private val factory: ProfileViewModelFactory by lazy(LazyThreadSafetyMode.NONE) {
        ProfileViewModelFactory(
            SceytChatUIKit.currentUserId
                ?: preference.getString(AppSharedPreference.PREF_USER_ID).orEmpty()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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

    private fun initViewModel() {
        viewModel.currentUserAsFlow
            .onEach(::setUserDetails)
            .launchIn(lifecycleScope)

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
            WelcomeActivity.launch(requireContext())
            requireActivity().finish()
        }

        userProfileViewModel.deleteUserErrorLiveData.observe(viewLifecycleOwner) {
            customToastSnackBar(it.toString())
        }
    }

    private fun FragmentProfileBinding.initViews() {
        displayNameDefaultBg = binding.displayName.background
        ivEdit.isVisible = !userProfileViewModel.isDemoUser
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

        ivEdit.setOnClickListener {
            openEditProfileFragment()
        }

        logout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        LogoutDialog(requireContext())
            .setIsDemoUser(userProfileViewModel.isDemoUser)
            .setAcceptCallback { deleteUser ->
                userProfileViewModel.logout(deleteUser) {
                    viewModel.logout()
                }
            }
            .show()
    }

    private fun setUserDetails(user: SceytUser?) {
        user ?: return
        currentUser = user
        avatarUrl = user.avatarURL
        binding.avatar.setUserAvatar(user)
        var displayName = user.fullName.trim()
        if (displayName.isBlank())
            displayName = "@${user.id}"
        binding.displayName.text = displayName
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
            setCustomAnimations(
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                0,
                0,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_out_right
            )
            replace(R.id.mainContainer, EditProfileFragment())
            addToBackStack(null)
        }
    }
}