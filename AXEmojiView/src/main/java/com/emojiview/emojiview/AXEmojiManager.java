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


package com.emojiview.emojiview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import android.text.Spannable;
import android.text.TextUtils;
import android.view.View;

import com.emojiview.emojiview.emoji.AXEmojiLoader;
import com.emojiview.emojiview.emoji.Emoji;
import com.emojiview.emojiview.emoji.EmojiCategory;
import com.emojiview.emojiview.emoji.EmojiData;
import com.emojiview.emojiview.emoji.EmojiProvider;
import com.emojiview.emojiview.listener.EditTextInputListener;
import com.emojiview.emojiview.listener.EmojiVariantCreatorListener;
import com.emojiview.emojiview.listener.StickerViewCreatorListener;
import com.emojiview.emojiview.preset.AXPresetEmojiLoader;
import com.emojiview.emojiview.shared.RecentEmoji;
import com.emojiview.emojiview.shared.RecentEmojiManager;
import com.emojiview.emojiview.shared.VariantEmoji;
import com.emojiview.emojiview.shared.VariantEmojiManager;
import com.emojiview.emojiview.sticker.RecentSticker;
import com.emojiview.emojiview.sticker.RecentStickerManager;
import com.emojiview.emojiview.sticker.Sticker;
import com.emojiview.emojiview.sticker.StickerCategory;
import com.emojiview.emojiview.utils.EmojiRange;
import com.emojiview.emojiview.utils.EmojiReplacer;
import com.emojiview.emojiview.utils.EmojiSpan;
import com.emojiview.emojiview.utils.Utils;
import com.emojiview.emojiview.variant.AXSimpleEmojiVariantPopup;
import com.emojiview.emojiview.variant.AXTouchEmojiVariantPopup;
import com.emojiview.emojiview.view.AXEmojiBase;
import com.emojiview.emojiview.view.AXEmojiView;
import com.emojiview.emojiview.view.AXSingleEmojiView;
import com.emojiview.emojiview.view.AXStickerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Amir Hossein Aghajari
 * @version 1.5.0
 */
public class AXEmojiManager {

    private static boolean ripple = true;

    private static final EditTextInputListener defaultInputListener = (editText, emoji) -> {
        if (emoji != null) {
            final int start = editText.getSelectionStart();
            final int end = editText.getSelectionEnd();

            if (start < 0) {
                editText.append(emoji.getUnicode());
            } else {
                editText.getText().replace(Math.min(start, end), Math.max(start, end), emoji.getUnicode(), 0, emoji.getUnicode().length());
            }
        }
    };

    private static final StickerViewCreatorListener defaultStickerCreator = new StickerViewCreatorListener() {
        @Override
        public View onCreateStickerView(@NonNull Context context, @Nullable StickerCategory<?> category, boolean isRecent) {
            return new AppCompatImageView(context);
        }

        @Override
        public View onCreateCategoryView(@NonNull Context context) {
            return new AppCompatImageView(context);
        }
    };

    private static EditTextInputListener inputListener;
    private static StickerViewCreatorListener stickerViewCreatorListener;
    private static EmojiVariantCreatorListener emojiVariantCreatorListener;

    private AXEmojiManager() {
    }

    private static final Comparator<String> STRING_LENGTH_COMPARATOR =
            (first, second) -> Integer.compare(second.length(), first.length());

    private static final int GUESSED_UNICODE_AMOUNT = 4000;
    private static final int GUESSED_TOTAL_PATTERN_LENGTH = GUESSED_UNICODE_AMOUNT * 4;


    private static final EmojiReplacer DEFAULT_EMOJI_REPLACER =
            (context, view, text, emojiSize, fontMetrics) -> {
                if (text.length() == 0) return;
                final AXEmojiManager emojiManager = AXEmojiManager.getInstance();
                final EmojiSpan[] existingSpans = text.getSpans(0, text.length(), EmojiSpan.class);
                final List<Integer> existingSpanPositions = new ArrayList<>(existingSpans.length);

                final int size = existingSpans.length;
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < size; i++) {
                    existingSpanPositions.add(text.getSpanStart(existingSpans[i]));
                }

                final List<EmojiRange> findAllEmojis = emojiManager.findAllEmojis(text);

                for (int i = 0; i < findAllEmojis.size(); i++) {
                    final EmojiRange location = findAllEmojis.get(i);

                    if (!existingSpanPositions.contains(location.start)) {
                        text.setSpan(new EmojiSpan(context, location.emoji, emojiSize),
                                location.start, location.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            };

    final Map<String, Emoji> emojiMap = new LinkedHashMap<>(GUESSED_UNICODE_AMOUNT);
    private EmojiCategory[] categories;
    private Pattern emojiPattern;
    private Pattern emojiRepetitivePattern;

    private EmojiReplacer emojiReplacer;

    @SuppressLint("StaticFieldLeak")
    static AXEmojiManager INSTANCE = null;
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static AXEmojiManager getInstance() {
        return INSTANCE;
    }

    public static boolean isInstalled() {
        return INSTANCE != null;
    }

    private EmojiProvider provider;

    public EmojiData getEmojiData() {
        return provider.getEmojiData();
    }


    /**
     * Installs the given EmojiProvider.
     *
     * @param provider the provider that should be installed.
     */
    public static void install(Context context, final EmojiProvider provider) {
        AXEmojiManager.context = context.getApplicationContext();
        if (INSTANCE != null) destroy();
        INSTANCE = null;
        final AXEmojiManager INSTANCE2 = new AXEmojiManager();
        if (mEmojiTheme == null) mEmojiTheme = new AXEmojiTheme();
        if (mStickerTheme == null) mStickerTheme = new AXEmojiTheme();
        /*recentEmoji = null;
         recentSticker = null;
         variantEmoji = null;
         emojiLoader = null;*/
        INSTANCE2.provider = provider;

        setMaxRecentSize(48);
        setMaxStickerRecentSize(Utils.getStickerGridCount(context) * 3);
        INSTANCE2.categories = provider.getCategories();
        INSTANCE2.emojiMap.clear();
        INSTANCE2.emojiReplacer = provider instanceof EmojiReplacer ? (EmojiReplacer) provider : DEFAULT_EMOJI_REPLACER;
        if (inputListener == null) inputListener = defaultInputListener;
        if (stickerViewCreatorListener == null) stickerViewCreatorListener = defaultStickerCreator;
        if (emojiVariantCreatorListener == null) enableTouchEmojiVariantPopup();

        AXPresetEmojiLoader.globalQueue.postRunnable(() -> {
            final List<String> unicodesForPattern = new ArrayList<>(GUESSED_UNICODE_AMOUNT);

            final int categoriesSize = INSTANCE2.categories.length;
            for (int i = 0; i < categoriesSize; i++) {
                final Emoji[] emojis = INSTANCE2.categories[i].getEmojis();

                final int emojisSize = emojis.length;
                //noinspection ForLoopReplaceableByForEach
                for (int j = 0; j < emojisSize; j++) {
                    final Emoji emoji = emojis[j];
                    final String unicode = emoji.getUnicode();
                    final List<Emoji> variants = emoji.getVariants();

                    INSTANCE2.emojiMap.put(unicode, emoji);
                    unicodesForPattern.add(unicode);

                    for (int k = 0; k < variants.size(); k++) {
                        final Emoji variant = variants.get(k);
                        final String variantUnicode = variant.getUnicode();

                        INSTANCE2.emojiMap.put(variantUnicode, variant);
                        unicodesForPattern.add(variantUnicode);
                    }
                }
            }

            if (unicodesForPattern.isEmpty()) {
                throw new IllegalArgumentException("Your EmojiProvider must at least have one category with at least one emoji.");
            }

            // We need to sort the unicodes by length so the longest one gets matched first.
            Collections.sort(unicodesForPattern, STRING_LENGTH_COMPARATOR);

            final StringBuilder patternBuilder = new StringBuilder(GUESSED_TOTAL_PATTERN_LENGTH);

            final int unicodesForPatternSize = unicodesForPattern.size();
            for (int i = 0; i < unicodesForPatternSize; i++) {
                patternBuilder.append(Pattern.quote(unicodesForPattern.get(i))).append('|');
            }

            final String regex = patternBuilder.deleteCharAt(patternBuilder.length() - 1).toString();
            INSTANCE2.emojiPattern = Pattern.compile(regex);
            INSTANCE2.emojiRepetitivePattern = Pattern.compile('(' + regex + ")+");
            INSTANCE = INSTANCE2;
        });

    }

    /**
     * Destroys the EmojiManager. This means that all internal data structures are released as well as
     * all data associated with installed Emojis.
     * <p>
     * You have to install EmojiManager again (if you wanted to use it)
     */
    public static void destroy() {
        synchronized (AXEmojiManager.class) {
            INSTANCE.provider.destroy();
            INSTANCE.emojiMap.clear();
            INSTANCE.categories = null;
            INSTANCE.emojiPattern = null;
            INSTANCE.emojiRepetitivePattern = null;
            INSTANCE.emojiReplacer = null;
        }
    }

    public void replaceWithImages(final Context context, View view, final Spannable text, final float emojiSize, final Paint.FontMetrics fontMetrics) {
        if (INSTANCE == null) return;
        emojiReplacer.replaceWithImages(context, view, text, emojiSize, fontMetrics);
    }

    public void replaceWithImages(final Context context, final Spannable text, final float emojiSize, final Paint.FontMetrics fontMetrics) {
        if (INSTANCE == null) return;
        emojiReplacer.replaceWithImages(context, null, text, emojiSize, fontMetrics);
    }

    public EmojiCategory[] getCategories() {
        if (categories == null) return provider.getCategories();
        else
            return categories;
    }

    Pattern getEmojiRepetitivePattern() {
        return emojiRepetitivePattern;
    }

    public List<EmojiRange> findAllEmojis(final CharSequence text) {
        final List<EmojiRange> result = new ArrayList<>();

        if (!TextUtils.isEmpty(text)) {
            final Matcher matcher = emojiPattern.matcher(text);

            while (matcher.find()) {
                final Emoji found = findEmoji(text.subSequence(matcher.start(), matcher.end()));

                if (found != null) {
                    result.add(new EmojiRange(matcher.start(), matcher.end(), found));
                }
            }
        }

        return result;
    }

    public Emoji findEmoji(final CharSequence candidate) {
        // We need to call toString on the candidate, since the emojiMap may not find the requested entry otherwise, because
        // the type is different.
        return emojiMap.get(candidate.toString());
    }

    static AXEmojiTheme mEmojiTheme;

    /**
     * set AXEmojiView theme settings
     */
    public static void setEmojiViewTheme(AXEmojiTheme theme) {
        mEmojiTheme = theme;
    }

    /**
     * @return AXEmojiView theme settings
     */
    public static AXEmojiTheme getEmojiViewTheme() {
        return mEmojiTheme;
    }

    /**
     * use AXEmojiManager.getEmojiViewTheme() instead.
     *
     * @deprecated
     */
    public static AXEmojiTheme getTheme() {
        return getEmojiViewTheme();
    }

    static AXEmojiTheme mStickerTheme;

    /**
     * set AXStickerView theme settings
     */
    public static void setStickerViewTheme(AXEmojiTheme theme) {
        mStickerTheme = theme;
    }

    /**
     * @return AXStickerView theme settings
     */
    public static AXEmojiTheme getStickerViewTheme() {
        return mStickerTheme;
    }

    static boolean footer = true;

    /**
     * AXEmojiPager footer view. backspace will add on footer right icon.
     */
    public static void setFooterEnabled(boolean footer) {
        AXEmojiManager.footer = footer;
    }

    public static boolean isFooterEnabled() {
        return footer;
    }

    /**
     * set max emoji recent sizes in default RecentEmojiManager
     *
     * @see RecentEmojiManager
     */
    public static void setMaxRecentSize(int size) {
        RecentEmojiManager.MAX_RECENT = size;
    }

    public static int getMaxRecentSize() {
        return RecentEmojiManager.MAX_RECENT;
    }

    /**
     * fill recent history with default values (if recent was empty)
     * default is true
     *
     * @see RecentEmojiManager
     */
    public static void setFillRecentHistoryEnabled(boolean enabled) {
        RecentEmojiManager.FILL_DEFAULT_HISTORY = enabled;
    }

    /**
     * fill recent history with this values if recent was empty.
     *
     * @see RecentEmojiManager
     */
    public static void setFillRecentHistoryData(String[] newRecent) {
        RecentEmojiManager.FILL_DEFAULT_RECENT_DATA = newRecent;
    }

    public static String[] getFillRecentHistoryData() {
        return RecentEmojiManager.FILL_DEFAULT_RECENT_DATA;
    }

    /**
     * set max sticker recent sizes in default RecentStickerManager
     *
     * @see RecentStickerManager
     */
    public static void setMaxStickerRecentSize(int size) {
        RecentStickerManager.MAX_RECENTS = size;
    }

    public static int getMaxStickerRecentSize() {
        return RecentStickerManager.MAX_RECENTS;
    }

    static boolean recentVariant = true;

    /**
     * show emoji variants in recent tab
     */
    public static void setRecentVariantEnabled(boolean enabled) {
        recentVariant = enabled;
    }

    public static boolean isRecentVariantEnabled() {
        return recentVariant;
    }

    static boolean showEmptyRecent = false;

    /**
     * Show Recent Tab while there is no data to show
     * you can manage this with isEmptyA() method in RecentManagers
     */
    public static void setShowEmptyRecentEnabled(boolean value) {
        showEmptyRecent = value;
    }

    public static boolean isShowingEmptyRecentEnabled() {
        return showEmptyRecent;
    }

    private static boolean asyncLoad = false;

    public static boolean isAsyncLoadEnabled() {
        return asyncLoad;
    }


    /**
     * load emojis with an async task
     * default is true;
     */
    public static void setAsyncLoadEnabled(boolean asyncLoad) {
        AXEmojiManager.asyncLoad = asyncLoad;
    }

    static RecentEmoji recentEmoji;
    static RecentSticker recentSticker;
    static VariantEmoji variantEmoji;


    /**
     * set AXEmojiView EmojiRecentManager
     */
    public static void setRecentEmoji(RecentEmoji recentEmoji) {
        AXEmojiManager.recentEmoji = recentEmoji;
    }

    /**
     * set AXEmojiView StickerRecentManager
     */
    public static void setRecentSticker(RecentSticker recentSticker) {
        AXEmojiManager.recentSticker = recentSticker;
    }

    /**
     * set AXEmojiView VariantManager
     */
    public static void setVariantEmoji(VariantEmoji variantEmoji) {
        AXEmojiManager.variantEmoji = variantEmoji;
    }

    public static RecentEmoji getRecentEmoji() {
        if (recentEmoji == null) return new RecentEmojiManager(context);
        return recentEmoji;
    }

    public static RecentSticker getRecentSticker(final String defType) {
        if (recentSticker == null) return new RecentStickerManager(context, defType);
        return recentSticker;
    }

    public static RecentSticker getRecentSticker() {
        return recentSticker;
    }

    public static VariantEmoji getVariantEmoji() {
        if (variantEmoji == null) return new VariantEmojiManager(context);
        return variantEmoji;
    }

    /**
     * check AXEmojiBase is instance of AXEmojiView or AXSingleEmojiView
     */
    public static boolean isAXEmojiView(AXEmojiBase base) {
        return base instanceof AXEmojiView || base instanceof AXSingleEmojiView;
    }

    /**
     * check AXEmojiBase is instance of AXStickerView
     */
    public static boolean isAXStickerView(AXEmojiBase base) {
        return base instanceof AXStickerView;
    }

    static AXEmojiLoader emojiLoader;

    /**
     * set AXEmojiView EmojiLoader
     */
    public static void setEmojiLoader(AXEmojiLoader emojiLoader) {
        AXEmojiManager.emojiLoader = emojiLoader;
    }

    /**
     * set Emoji replacer
     */
    public void setEmojiReplacer(EmojiReplacer emojiReplacer) {
        this.emojiReplacer = emojiReplacer;
        if (emojiReplacer == null) {
            this.emojiReplacer = AXEmojiManager.DEFAULT_EMOJI_REPLACER;
        }
    }

    /**
     * @return the installed emoji provider
     */
    public EmojiProvider getEmojiProvider() {
        return provider;
    }

    public static void setEditTextInputListener(EditTextInputListener listener) {
        AXEmojiManager.inputListener = listener;
        if (listener == null) {
            AXEmojiManager.inputListener = AXEmojiManager.defaultInputListener;
        }
    }

    public static void setStickerViewCreatorListener(StickerViewCreatorListener listener) {
        AXEmojiManager.stickerViewCreatorListener = listener;
        if (listener == null) {
            AXEmojiManager.stickerViewCreatorListener = AXEmojiManager.defaultStickerCreator;
        }
    }

    public EditTextInputListener getEditTextInputListener() {
        if (inputListener == null) return defaultInputListener;
        return inputListener;
    }

    public StickerViewCreatorListener getStickerViewCreatorListener() {
        if (stickerViewCreatorListener == null) return defaultStickerCreator;
        return stickerViewCreatorListener;
    }

    public static void setEmojiVariantCreatorListener(EmojiVariantCreatorListener listener) {
        if (listener == null)
            listener = (a, b) -> null;
        AXEmojiManager.emojiVariantCreatorListener = listener;
    }

    public static void enableTouchEmojiVariantPopup() {
        AXEmojiManager.emojiVariantCreatorListener = AXTouchEmojiVariantPopup::new;
    }

    public static void enableSimpleEmojiVariantPopup() {
        AXEmojiManager.emojiVariantCreatorListener = AXSimpleEmojiVariantPopup::new;
    }

    public static EmojiVariantCreatorListener getEmojiVariantCreatorListener() {
        return emojiVariantCreatorListener;
    }

    public static AXEmojiLoader getEmojiLoader() {
        return emojiLoader;
    }

    /**
     * disable recent managers
     */
    public static void disableRecentManagers() {
        disableEmojiRecentManager();
        disableStickerRecentManager();
    }

    /**
     * disable sticker recent manager
     */
    public static void disableStickerRecentManager() {
        recentEmoji = new RecentEmoji() {
            @NonNull
            @Override
            public Collection<Emoji> getRecentEmojis() {
                return Collections.emptyList();
            }

            @Override
            public void addEmoji(@NonNull Emoji emoji) {
            }

            @Override
            public void persist() {
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public void reload() {
            }
        };
    }

    /**
     * disable emoji recent manager
     */
    public static void disableEmojiRecentManager() {
        recentSticker = new RecentSticker() {
            @SuppressWarnings("rawtypes")
            @NonNull
            @Override
            public Collection<Sticker> getRecentStickers() {
                return Collections.emptyList();
            }

            @Override
            public void addSticker(@SuppressWarnings("rawtypes") @NonNull Sticker sticker) {
            }

            @Override
            public void persist() {
            }

            @Override
            public boolean isEmpty() {
                return true;
            }
        };
    }

    public static void enableDefaultRecentManagers() {
        enableDefaultEmojiRecentManager();
        enableDefaultStickerRecentManager();
    }

    public static void enableDefaultStickerRecentManager() {
        recentSticker = null;
    }

    public static void enableDefaultEmojiRecentManager() {
        recentEmoji = null;
    }

    public static boolean isRippleEnabled() {
        return ripple;
    }

    public static void setRippleEnabled(boolean enabled) {
        ripple = enabled;
    }

    static boolean isUsingPopupWindow = false;

    public static void setUsingPopupWindow(boolean isUsingPopupWindow) {
        AXEmojiManager.isUsingPopupWindow = isUsingPopupWindow;
    }

    public static boolean isUsingPopupWindow() {
        return isUsingPopupWindow;
    }

    private static boolean backspaceCategoryEnabled = true;

    public static boolean isBackspaceCategoryEnabled() {
        return backspaceCategoryEnabled;
    }

    public static void setBackspaceCategoryEnabled(boolean enabled) {
        backspaceCategoryEnabled = enabled;
    }

    public static void resetTheme() {
        setEmojiViewTheme(new AXEmojiTheme());
        setStickerViewTheme(new AXEmojiTheme());
    }

    private static List<String> filterEmojisList = null;

    public static void filterEmojis(List<String> filterEmojisList) {
        AXEmojiManager.filterEmojisList = filterEmojisList;
    }

    @Nullable
    public static List<String> getFilteredEmojis() {
        return filterEmojisList;
    }
}
