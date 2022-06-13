/*
 * Copyright (C) 2020 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.emojiview.emojiview.variant;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.emojiview.emojiview.AXEmojiManager;
import com.aghajari.emojiview.R;
import com.emojiview.emojiview.emoji.Emoji;
import com.emojiview.emojiview.listener.OnEmojiActions;
import com.emojiview.emojiview.utils.Utils;
import com.emojiview.emojiview.view.AXEmojiImageView;


import java.util.ArrayList;
import java.util.List;

import static android.view.View.MeasureSpec.makeMeasureSpec;

public class AXSimpleEmojiVariantPopup extends AXEmojiVariantPopup {
    private static final int MARGIN = 1;

    @NonNull
    final View rootView;
    @Nullable
    private PopupWindow popupWindow;

    @Nullable
    final OnEmojiActions listener;
    @Nullable
    AXEmojiImageView rootImageView;

    public AXSimpleEmojiVariantPopup(@NonNull final View rootView, @Nullable final OnEmojiActions listener) {
        super(rootView, listener);
        this.rootView = rootView;
        this.listener = listener;
    }

    View content;
    boolean isShowing;

    public boolean isShowing() {
        return isShowing;
    }

    public void show(@NonNull final AXEmojiImageView clickedImage, @NonNull final Emoji emoji, final boolean fromRecent) {
        dismiss();
        isShowing = true;
        rootImageView = clickedImage;

        content = initView(clickedImage.getContext(), emoji, clickedImage.getWidth(), fromRecent);

        popupWindow = new PopupWindow(content, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setOnDismissListener(() -> isShowing = false);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(clickedImage.getContext().getResources(), (Bitmap) null));

        content.measure(makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        final Point location = Utils.locationOnScreen(clickedImage);
        final Point desiredLocation = new Point(
                location.x - content.getMeasuredWidth() / 2 + clickedImage.getWidth() / 2,
                location.y - content.getMeasuredHeight()
        );

        popupWindow.showAtLocation(rootView, Gravity.NO_GRAVITY, desiredLocation.x, desiredLocation.y);
        rootImageView.getParent().requestDisallowInterceptTouchEvent(true);
        Utils.fixPopupLocation(popupWindow, desiredLocation);
    }

    public void dismiss() {
        isShowing = false;
        rootImageView = null;

        if (popupWindow != null) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }

    FrameLayout imageContainer;

    private View initView(@NonNull final Context context, @NonNull final Emoji emoji, final int width, final boolean fromRecent) {
        final View result = View.inflate(context, R.layout.emoji_skin_popup, null);
        imageContainer = result.findViewById(R.id.container);
        CardView cardView = result.findViewById(R.id.cardview);
        cardView.setCardBackgroundColor(AXEmojiManager.getEmojiViewTheme().getVariantPopupBackgroundColor());

        final List<Emoji> variants = new ArrayList<>(emoji.getBase().getVariants());
        variants.add(0, emoji.getBase());

        final LayoutInflater inflater = LayoutInflater.from(context);

        int index = 0;
        boolean isCustom = variants.size() > 6;
        final int margin = Utils.dpToPx(context, MARGIN);
        for (final Emoji variant : variants) {
            int i = isCustom ? (index == 0 ? 0 : (index - 1) % 5) : index;

            final ImageView emojiImage = (ImageView) inflater.inflate(R.layout.emoji_item, imageContainer, false);
            final ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) emojiImage.getLayoutParams();

            // Use the same size for Emojis as in the picker.
            layoutParams.width = width;
            int row = (!isCustom || index == 0) ? 0 : ((index - 1) / 5 + 1);
            if (isCustom && index == 0)
                i = 2;

            layoutParams.setMargins((i + 1) * margin + i * width, row * width + (row + 1) * margin, margin, margin);
            emojiImage.setImageDrawable(variant.getDrawable(emojiImage));

            emojiImage.setOnClickListener(view -> {
                if (listener != null && rootImageView != null) {
                    rootImageView.updateEmoji(variant);
                    listener.onClick(rootImageView, variant, fromRecent, true);
                }
            });

            index++;
            imageContainer.addView(emojiImage);
        }

        return result;
    }
}
