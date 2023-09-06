package com.sceyt.chat.demo.presentation.changerole

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.demo.databinding.ActivityChooseRoleBinding
import com.sceyt.chat.demo.presentation.changerole.adapter.ChooseRoleAdapter
import com.sceyt.chat.demo.presentation.changerole.adapter.RoleItem
import com.sceyt.chat.demo.presentation.changerole.viewmodel.RoleViewModel
import com.sceyt.sceytchatuikit.R.anim
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.overrideTransitions
import com.sceyt.sceytchatuikit.extensions.parcelable
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class ChangeRoleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseRoleBinding
    private lateinit var roleAdapter: ChooseRoleAdapter
    private val viewModel: RoleViewModel by viewModels()
    private lateinit var member: SceytMember

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityChooseRoleBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        getBundleArguments()
        initViewModel()
        initViews()
        viewModel.getRoles()
    }

    private fun getBundleArguments() {
        member = requireNotNull(intent?.parcelable(MEMBER))
    }

    private fun initViewModel() {
        viewModel.rolesLiveData.observe(this) {
            it.forEach { roleItem ->
                if (roleItem.role.name == member.role.name) {
                    roleItem.checked = true
                    return@forEach
                }
            }
            setupList(it)
        }
    }

    private fun initViews() {
        binding.icBack.setOnClickListener {
            finish()
        }
    }

    private fun setupList(list: List<RoleItem>) {
        roleAdapter = ChooseRoleAdapter(list as ArrayList) {
            onChooseRole(it)
        }

        binding.rvRoles.apply {
            setHasFixedSize(true)
            adapter = roleAdapter
        }
    }

    private fun updateOldRole() {
        val oldItem = roleAdapter.getData().findIndexed { it.role.name == member.role.name }
        oldItem?.let {
            it.second.checked = false
            roleAdapter.notifyItemChanged(it.first, Unit)
        }
    }

    private fun onChooseRole(roleItem: RoleItem) {
        updateOldRole()
        val result = Intent()
        result.putExtra(CHOSEN_ROLE, roleItem.role.name)
        result.putExtra(MEMBER, intent.parcelable<SceytMember>(MEMBER))
        setResult(Activity.RESULT_OK, result)
        finish()
        overrideTransitions(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
    }

    override fun finish() {
        super.finish()
        overrideTransitions(anim.sceyt_anim_slide_hold, anim.sceyt_anim_slide_out_right, false)
    }

    companion object {
        const val CHOSEN_ROLE = "BUNDLE_KEY_CHOOSE_ROLE"
        const val MEMBER = "member"

        fun newInstance(context: Context, member: SceytMember): Intent {
            return Intent(context, ChangeRoleActivity::class.java).apply {
                putExtra(MEMBER, member)
            }
        }
    }
}