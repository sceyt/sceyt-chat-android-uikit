package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.polls

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.databinding.SceytItemVoterAvatarBinding
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle
import com.sceyt.chatuikit.styles.messages_list.item.VoterAvatarRendererAttributes

class VoterAvatarAdapter(
    private val pollStyle: PollStyle,
    private val bubbleBackgroundStyleProvider: () -> BackgroundStyle,
    private val onVoterClick: (() -> Unit)? = null,
) : ListAdapter<SceytUser, VoterAvatarAdapter.VoterAvatarViewHolder>(DIFF_CALLBACK) {

    private var shouldAnimate = false

    init {
        setHasStableIds(true)
    }

    companion object {
        private const val ANIMATION_DURATION = 250L

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SceytUser>() {
            override fun areItemsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SceytUser, newItem: SceytUser): Boolean {
                return oldItem == newItem
            }

            override fun getChangePayload(oldItem: SceytUser, newItem: SceytUser): Any? {
                return Any()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoterAvatarViewHolder {
        val binding = SceytItemVoterAvatarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VoterAvatarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VoterAvatarViewHolder, position: Int) {
        holder.bind(getItem(position), shouldAnimate = shouldAnimate)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    override fun onViewRecycled(holder: VoterAvatarViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelAnimations()
    }

    fun submitData(users: List<SceytUser>, animate: Boolean) {
        shouldAnimate = animate
        submitList(users)
    }

    inner class VoterAvatarViewHolder(
        private val binding: SceytItemVoterAvatarBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAnimator: AnimatorSet? = null

        init {
            binding.root.setOnClickListener {
                onVoterClick?.invoke()
            }
        }

        fun bind(user: SceytUser, shouldAnimate: Boolean = false) {
            // Cancel any ongoing animation
            cancelAnimations()

            pollStyle.voterAvatarRenderer.render(
                context = itemView.context,
                from = VoterAvatarRendererAttributes(
                    bubbleBackgroundStyle = bubbleBackgroundStyleProvider(),
                    voter = user
                ),
                style = pollStyle.voterAvatarStyle,
                avatarView = binding.avatar
            )

            // Animate new voter avatars appearing
            if (shouldAnimate && binding.avatar.isVisible) {
                animateAvatarEntry()
            }
        }

        private fun animateAvatarEntry() {
            // Start from invisible state
            binding.avatar.alpha = 0f
            binding.avatar.scaleX = 0.3f
            binding.avatar.scaleY = 0.3f

            // Create scale and fade animations
            val scaleXAnimator = ObjectAnimator.ofFloat(binding.avatar, "scaleX", 0.3f, 1f)
            val scaleYAnimator = ObjectAnimator.ofFloat(binding.avatar, "scaleY", 0.3f, 1f)
            val alphaAnimator = ObjectAnimator.ofFloat(binding.avatar, "alpha", 0f, 1f)

            currentAnimator = AnimatorSet().apply {
                playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
                duration = ANIMATION_DURATION
                interpolator = OvershootInterpolator(1.2f)
                start()
            }
        }

        fun cancelAnimations() {
            currentAnimator?.cancel()
            currentAnimator = null

            // Reset to normal state
            binding.avatar.alpha = 1f
            binding.avatar.scaleX = 1f
            binding.avatar.scaleY = 1f
        }
    }
}

