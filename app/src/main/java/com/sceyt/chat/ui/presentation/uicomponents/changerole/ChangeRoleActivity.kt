package com.sceyt.chat.ui.presentation.uicomponents.changerole

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.SceytMember
import com.sceyt.chat.ui.databinding.ActivityChooseRoleBinding
import com.sceyt.chat.ui.extensions.findIndexed
import com.sceyt.chat.ui.presentation.uicomponents.changerole.adapter.ChooseRoleAdapter
import com.sceyt.chat.ui.presentation.uicomponents.changerole.adapter.RoleItem
import com.sceyt.chat.ui.presentation.uicomponents.changerole.viewmodel.RoleViewModel

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

        getBundleArguments()
        initViewModel()
        initViews()
        viewModel.getRoles()
    }

    private fun getBundleArguments() {
        member = requireNotNull(intent?.getParcelableExtra(MEMBER))
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
            onBackPressed()
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
        result.putExtra(MEMBER, intent.getParcelableExtra<SceytMember>(MEMBER))
        setResult(Activity.RESULT_OK, result)
        finish()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right)
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