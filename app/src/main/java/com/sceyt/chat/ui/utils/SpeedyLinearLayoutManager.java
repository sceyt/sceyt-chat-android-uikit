package com.sceyt.chat.ui.utils;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class SpeedyLinearLayoutManager extends LinearLayoutManager {

    private final float MILLISECONDS_PER_INCH = 25f; //default is 25f (bigger = slower)
    private float CUSTOM_MILLISECONDS_PER_INCH = 0;

    public SpeedyLinearLayoutManager(Context context) {
        super(context);
    }

    public SpeedyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public SpeedyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {

            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return super.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                if (CUSTOM_MILLISECONDS_PER_INCH != 0) {
                    float value = CUSTOM_MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                    CUSTOM_MILLISECONDS_PER_INCH = 0;
                    return value;
                } else return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }
        };

        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }


    public void smoothScrollToPositionWithDuration(RecyclerView recyclerView, int position, float millisecondsPerInch) {
        CUSTOM_MILLISECONDS_PER_INCH = millisecondsPerInch;
        recyclerView.smoothScrollToPosition(position);
    }
}
