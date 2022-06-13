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

import androidx.annotation.NonNull;

import com.emojiview.emojiview.emoji.Emoji;

/**
 * Interface for providing some custom implementation for variant emojis.
 */
public interface VariantEmoji {
    /**
     * Returns the variant for the passed emoji. Could be loaded from a database, shared preferences or just hard
     * coded.<br>
     * <p>
     * This method will be called more than one time hence it is recommended to hold a collection of
     * desired emojis.
     *
     * @param desiredEmoji The emoji to retrieve the variant for. If none is found,
     *                     the passed emoji should be returned.
     */
    @NonNull
    Emoji getVariant(Emoji desiredEmoji);

    /**
     * Should add the emoji to the variants. After calling this method, {@link #getVariant(Emoji)}
     * should return the emoji that was just added.
     *
     * @param newVariant The new variant to save.
     */
    void addVariant(@NonNull Emoji newVariant);

    /**
     * Should persist all emojis.
     */
    void persist();
}
