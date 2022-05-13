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


package com.emojiview.emojiview.adapters;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.view.ViewGroup;

import com.emojiview.emojiview.AXEmojiManager;
import com.emojiview.emojiview.listener.FindVariantListener;
import com.emojiview.emojiview.listener.OnEmojiActions;
import com.emojiview.emojiview.shared.RecentEmoji;
import com.emojiview.emojiview.shared.VariantEmoji;
import com.emojiview.emojiview.view.AXEmojiRecyclerView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AXEmojiViewPagerAdapter extends PagerAdapter {
    OnEmojiActions events;
    RecyclerView.OnScrollListener scrollListener;
    RecentEmoji recentEmoji;
    VariantEmoji variantEmoji;
    public List<AXEmojiRecyclerView> recyclerViews;
    public int add = 0;

    private final Queue<View> destroyedItems = new LinkedList<>();
    FindVariantListener findVariantListener;

    public AXEmojiViewPagerAdapter(OnEmojiActions events, RecyclerView.OnScrollListener scrollListener,
                                   RecentEmoji recentEmoji, VariantEmoji variantEmoji, FindVariantListener listener) {
        this.events = events;
        this.findVariantListener = listener;
        this.scrollListener = scrollListener;
        this.recentEmoji = recentEmoji;
        this.variantEmoji = variantEmoji;
        recyclerViews = new ArrayList<>();
    }

    public RecyclerView.ItemDecoration itemDecoration = null;

    @NonNull
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        AXEmojiRecyclerView recycler;
        try {
            recycler = (AXEmojiRecyclerView) destroyedItems.poll();
        } catch (Exception e) {
            recycler = null;
        }

        if (recycler == null)
            recycler = new AXEmojiRecyclerView(collection.getContext(), findVariantListener);
        collection.addView(recycler);

        if (position == 0 && add == 1) {
            recycler.setAdapter(new AXRecentEmojiRecyclerAdapter(recentEmoji, events, variantEmoji));
        } else {
            recycler.setAdapter(new AXEmojiRecyclerAdapter(AXEmojiManager.getInstance().getCategories()[position - add].getEmojis(),
                    events, variantEmoji));
        }

        recyclerViews.add(recycler);
        if (itemDecoration != null) {
            recycler.removeItemDecoration(itemDecoration);
            recycler.addItemDecoration(itemDecoration);
        }
        if (scrollListener != null) {
            recycler.removeOnScrollListener(scrollListener);
            recycler.addOnScrollListener(scrollListener);
        }
        return recycler;
    }

    @Override
    public int getCount() {
        if (!recentEmoji.isEmpty()) {
            add = 1;
        } else {
            add = 0;
        }
        return AXEmojiManager.getInstance().getCategories().length + add;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(@NonNull View arg0, @NonNull Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
        recyclerViews.remove(object);
        destroyedItems.add((View) object);
    }
}
