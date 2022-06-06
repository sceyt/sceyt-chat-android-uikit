package com.sceyt.chat.ui.utils

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.sceyt.chat.ui.R

object UIUtils {


    fun openFileChooser(
        context: Context,
        chooseListener: (type: ProfilePhotoChooseType) -> Unit
    ) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.SceytDialogNoTitle))

        builder.setItems(R.array.image_picker) { _, which ->
            when (which) {
                0 -> {
                    chooseListener.invoke(ProfilePhotoChooseType.CAMERA)
                }
                1 -> {
                    chooseListener.invoke(ProfilePhotoChooseType.GALLERY)
                }
                2 -> chooseListener.invoke(ProfilePhotoChooseType.FILE)
            }
        }

        val dialog = builder.create()

        dialog.show()
    }

    fun openDialogue(context: Context, message: String, btnText: String? = null) {
        openDialogue(context, message, btnText, null)
    }

    fun openDialogue(
        context: Context, message: String, btnText: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)
        builder.setPositiveButton(btnText) { dialog, which ->
            dialog.dismiss()
            listener?.onClick(dialog, which)
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun openDialogue(
        context: Context,
        title: String? = null,
        message: String,
        btnTextPositive: String?,
        btnTextNegative: String?,
        listener: DialogInterface.OnClickListener?,
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(btnTextPositive) { dialog, which ->
            dialog.dismiss()
            listener?.onClick(dialog, which)
        }
        builder.setNegativeButton(btnTextNegative) { dialog, which ->
            dialog.dismiss()
            listener?.onClick(dialog, which)
        }
        val dialog = builder.create()
        dialog.show()
    }


    fun createCircleDrawable(color: Int): GradientDrawable {
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(color, color)
        )
        gradient.shape = GradientDrawable.OVAL
        gradient.cornerRadius = 180f
        return gradient
    }

    fun createLoadingDrawable(context: Context): CircularProgressDrawable {
        val drawable = CircularProgressDrawable(context)
        drawable.setColorSchemeColors(
            R.color.sceyt_color_primary,
            R.color.sceyt_color_primary_dark,
            R.color.sceyt_color_accent
        )
        drawable.centerRadius = 30f
        drawable.strokeWidth = 5f
        return drawable
    }

    enum class ProfilePhotoChooseType {
        GALLERY, CAMERA, FILE, DELETE
    }
}