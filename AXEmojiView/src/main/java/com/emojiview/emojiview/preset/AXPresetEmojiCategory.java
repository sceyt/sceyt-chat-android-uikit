package com.emojiview.emojiview.preset;

import androidx.annotation.NonNull;

import com.emojiview.emojiview.emoji.Emoji;
import com.emojiview.emojiview.emoji.EmojiCategory;
import com.emojiview.emojiview.emoji.EmojiData;

public class AXPresetEmojiCategory implements EmojiCategory {
    public Emoji[] emojiData;
    public String title;
    int icon;

    public AXPresetEmojiCategory(int i, int icon, EmojiData emojiData) {
        String[][] dataColored = emojiData.getDataColored();
        this.emojiData = new Emoji[dataColored[i].length];
        for (int j = 0; j < dataColored[i].length; j++)
            this.emojiData[j] = createEmoji(dataColored[i][j], i, j, emojiData);
        title = emojiData.getTitle(i);
        this.icon = icon;
    }

    protected Emoji createEmoji(String code, int category, int index, EmojiData emojiData){
        return new AXPresetEmoji(code, emojiData);
    }

    @NonNull
    @Override
    public Emoji[] getEmojis() {
        return emojiData;
    }

    @Override
    public int getIcon() {
        return icon;
    }

    @Override
    public CharSequence getTitle() {
        return title;
    }
}