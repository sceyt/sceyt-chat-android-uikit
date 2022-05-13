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

package com.emojiview.emojiview.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.emojiview.emojiview.emoji.Emoji;

import java.lang.ref.WeakReference;

final class ImageLoadingTask extends AsyncTask<Emoji, Void, Drawable> {
    private final WeakReference<AppCompatImageView> imageViewReference;
    private final WeakReference<Context> contextReference;

    ImageLoadingTask(final AppCompatImageView imageView) {
        imageViewReference = new WeakReference<>(imageView);
        contextReference = new WeakReference<>(imageView.getContext());
    }

    @Override
    protected Drawable doInBackground(final Emoji... emoji) {
        final Context context = contextReference.get();

        if (context != null && !isCancelled()) {
            return emoji[0].getDrawable(context);
        }

        return null;
    }

    @Override
    protected void onPostExecute(@Nullable final Drawable drawable) {
        if (!isCancelled() && drawable != null) {
            final AppCompatImageView imageView = imageViewReference.get();

            if (imageView != null) {
                imageView.setImageDrawable(drawable);
            }
        }
    }
}
