package com.example.xyzreader.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by kkyin on 9/4/2017.
 */

public class AspectRatio3x2_ImageView extends android.support.v7.widget.AppCompatImageView {
    public AspectRatio3x2_ImageView(Context context) {
        super(context);
    }

    public AspectRatio3x2_ImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatio3x2_ImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int height_3x2 = View.MeasureSpec.getSize(widthMeasureSpec) * 2 / 3;

        int heightSpec_3x2 =
            View.MeasureSpec.makeMeasureSpec(height_3x2, View.MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, heightSpec_3x2);
    }
}
