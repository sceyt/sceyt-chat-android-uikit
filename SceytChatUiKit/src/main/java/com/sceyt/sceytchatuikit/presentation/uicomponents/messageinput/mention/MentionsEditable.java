/*
* Copyright 2015 LinkedIn Corp. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/

package com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Annotation;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MentionsEditable extends SpannableStringBuilder implements Parcelable {

    public MentionsEditable(@NonNull CharSequence text) {
        super(text);
    }

    public MentionsEditable(@NonNull CharSequence text, int start, int end) {
        super(text, start, end);
    }

    public MentionsEditable(@NonNull Parcel in) {
        super(in.readString());
        int length = in.readInt();
        if (length > 0) {
            for (int index = 0; index < length; index++) {
                int start = in.readInt();
                int end = in.readInt();
                Annotation span = new Annotation(in);
                setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // --------------------------------------------------
    // Overrides
    // --------------------------------------------------

    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        // Do not add any spans that affect the character appearance of a mention (i.e. they overlap
        // with a Annotation). This helps prevent mentions from having a red underline due to the spell
        // checker. Note: SuggestionSpan was added in ICS, and different keyboards may use other kinds
        // of spans (i.e. the Motorola SpellCheckerMarkupSpan). Therefore, we cannot just filter out
        // SuggestionSpans, but rather, any span that would change the appearance of our Annotations.
        if (what instanceof CharacterStyle) {
            Annotation[] Annotations = getSpans(start, end, Annotation.class);
            if (Annotations != null && Annotations.length > 0) {
                return;
            }
        }

        // Ensure that the start and end points are set at zero initially
        // Note: This issue was seen on a Gingerbread device (start and end were both -1) and
        // prevents the device from crashing.
        if ((what == Selection.SELECTION_START || what == Selection.SELECTION_END) && length() == 0) {
            start = 0;
            end = 0;
        }

        // For added safety, check that the start and end indices are valid
        if (start >= 0 && end >= start && end <= length()) {
            super.setSpan(what, start, end, flags);
        } else {
            Log.w(getClass().getName(),
                  "Attempted to set span at invalid indices, start=" + start + ", end=" + end);
        }
    }

    @NonNull
    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart, int tbend) {
        // On certain software keyboards, the editor appears to append a word minus the last character when it is really
        // trying to just delete the last character. Until we can figure out the root cause of this issue, the following
        // code remaps this situation to do a proper delete.
        //
        // More details: https://github.com/linkedin/Spyglass/issues/105#issuecomment-674751603
        if (start == end && start - tbend - 1 >= 0 && tb.length() > 1) {
            String insertString = tb.subSequence(tbstart, tbend).toString();
            int prevStart = start - tbend - 1;
            int prevEnd = start - 1;
            String prevString = subSequence(prevStart, prevEnd).toString();
            Annotation[] prevSpans = getSpans(prevStart, prevEnd, Annotation.class);

            // If the insert string matches the previous string and the previous string contains a mention, then
            // we will just delete the previous character instead of appending the word.
            if (insertString.equals(prevString) && prevSpans.length > 0) {
                return super.replace(start - 1, start, "", 0, 0);
            }
        }

        return super.replace(start, end, tb, tbstart, tbend);
    }

    // --------------------------------------------------
    // Custom Public Methods
    // --------------------------------------------------

    /**
     * Implementation of {@link String#trim()} for an {@link Editable}.
     *
     * @return a new {@link MentionsEditable} with whitespace characters removed from the beginning and the end
     */
    @NonNull
    public MentionsEditable trim() {
        // Delete beginning spaces
        while (length() > 0 && Character.isWhitespace(charAt(0))) {
            delete(0, 1);
        }
        // Delete ending spaces
        int len = length();
        while (len > 0 && Character.isWhitespace(charAt(len - 1))) {
            delete(len - 1, len);
            // Note: we must reset len this way after delete to avoid IndexOutOfBoundsException crashes similar to what we saw
            // happening with certain keyboards and {@link MentionsEditable#replace(int, int, CharSequence, int, int)}.
            len = length();
        }
        return new MentionsEditable(this, 0, length());
    }

    @NonNull
    public List<Annotation> getAnnotations() {
        Annotation[] Annotations = getSpans(0, length(), Annotation.class);
        return (Annotations != null) ? Arrays.asList(Annotations) : new ArrayList<Annotation>();
    }

    /**
     * Given an integer offset, return the {@link Annotation} located at the offset in the text of the
     * {@link android.widget.EditText}, if it exists. Otherwise, return null.
     *
     * @param index integer offset in text
     *
     * @return a {@link Annotation} located at index in text, or null
     */
    @Nullable
    public Annotation getAnnotationAtOffset(int index) {
        Annotation[] spans = getSpans(index, index, Annotation.class);
        return (spans != null && spans.length > 0) ? spans[0] : null;
    }

    /**
     * Get the {@link Annotation} starting at the given index in the text, or null if there is no {@link Annotation}
     * starting at that index.
     *
     * @param index integer offset in text
     *
     * @return a {@link Annotation} starting at index in text, or null
     */
    @Nullable
    public Annotation getAnnotationStartingAt(int index) {
        Annotation[] spans = getSpans(0, length(), Annotation.class);
        if (spans != null) {
            for (Annotation span : spans) {
                if (getSpanStart(span) == index) {
                    return span;
                }
            }
        }
        return null;
    }

    /**
     * Get the {@link Annotation} ending at the given index in the text, or null if there is no {@link Annotation}
     * ending at that index.
     *
     * @param index integer offset in text
     *
     * @return a {@link Annotation} ending at index in text, or null
     */
    @Nullable
    public Annotation getAnnotationEndingAt(int index) {
        Annotation[] spans = getSpans(0, length(), Annotation.class);
        if (spans != null) {
            for (Annotation span : spans) {
                if (getSpanEnd(span) == index) {
                    return span;
                }
            }
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
        int length = getAnnotations().size();
        dest.writeInt(length);
        if (length > 0) {
            for (int index = 0; index < length; index++) {
                Annotation span = getAnnotations().get(index);
                dest.writeInt(getSpanStart(span));
                dest.writeInt(getSpanEnd(span));
                span.writeToParcel(dest, flags);
            }
        }
    }

    public static final Creator<MentionsEditable> CREATOR
            = new Creator<MentionsEditable>() {
        public MentionsEditable createFromParcel(Parcel in) {
            return new MentionsEditable(in);
        }

        public MentionsEditable[] newArray(int size) {
            return new MentionsEditable[size];
        }
    };
}
