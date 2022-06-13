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


package com.emojiview.emojiview.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.emojiview.emojiview.AXEmojiManager;
import com.emojiview.emojiview.emoji.Emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class VariantEmojiManager implements VariantEmoji {
    private static final String PREFERENCE_NAME = "variant-emoji-manager";
    private static final String EMOJI_DELIMITER = "~";
    private static final String VARIANT_EMOJIS = "variant-emojis";
    static final int EMOJI_GUESS_SIZE = 5;

    @NonNull
    private final Context context;
    private List<Emoji> variantsList = null;

    public VariantEmojiManager(@NonNull final Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Emoji getVariant(final Emoji desiredEmoji) {
        if (variantsList == null || variantsList.isEmpty()) {
            initFromSharedPreferences();
        }

        final Emoji baseEmoji = desiredEmoji.getBase();

        for (int i = 0; i < variantsList.size(); i++) {
            final Emoji emoji = variantsList.get(i);

            if (baseEmoji.equals(emoji.getBase())) {
                return emoji;
            }
        }

        return desiredEmoji;
    }

    @Override
    public void addVariant(@NonNull final Emoji newVariant) {
        final Emoji newVariantBase = newVariant.getBase();

        for (int i = 0; i < variantsList.size(); i++) {
            final Emoji variant = variantsList.get(i);

            if (variant.getBase().equals(newVariantBase)) {
                if (!variant.equals(newVariant)) {
                    variantsList.remove(i);
                    variantsList.add(newVariant);
                }
                return;
            }
        }

        variantsList.add(newVariant);
    }

    @Override
    public void persist() {
        if (variantsList == null)
            return;

        if (variantsList.size() > 0) {
            final StringBuilder stringBuilder = new StringBuilder(variantsList.size() * EMOJI_GUESS_SIZE);

            for (int i = 0; i < variantsList.size(); i++) {
                stringBuilder.append(variantsList.get(i).getUnicode()).append(EMOJI_DELIMITER);
            }

            stringBuilder.setLength(stringBuilder.length() - EMOJI_DELIMITER.length());

            getPreferences().edit().putString(VARIANT_EMOJIS, stringBuilder.toString()).apply();
        } else {
            getPreferences().edit().remove(VARIANT_EMOJIS).apply();
        }
    }

    private void initFromSharedPreferences() {
        final String savedRecentVariants = getPreferences().getString(VARIANT_EMOJIS, "");

        if (!TextUtils.isEmpty(savedRecentVariants)) {
            final StringTokenizer stringTokenizer = new StringTokenizer(savedRecentVariants, EMOJI_DELIMITER);
            variantsList = new ArrayList<>(stringTokenizer.countTokens());

            while (stringTokenizer.hasMoreTokens()) {
                final String token = stringTokenizer.nextToken();
                final Emoji emoji = AXEmojiManager.getInstance().findEmoji(token);

                if (emoji != null && emoji.getLength() == token.length()) {
                    variantsList.add(emoji);
                }
            }
        } else {
            variantsList = new ArrayList<>(0);
        }
    }

    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
    }
}
