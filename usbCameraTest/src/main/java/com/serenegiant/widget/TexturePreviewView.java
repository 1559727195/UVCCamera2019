/*
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.serenegiant.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.serenegiant.usbcameratest.R;


/**
 * 基于 系统TextureView实现的预览View;
 */
public class TexturePreviewView extends FrameLayout implements PreviewView, AspectRatioViewInterface {
    private double mRequestedAspect = -1.0;
    private TextureView textureView;

    private int videoWidth = 0;
    private int videoHeight = 0;
    private boolean mirrored = true;
    private Animation animation;
    private boolean landscape;

    public TexturePreviewView(@NonNull Context context) {
        super(context);
        init();
    }

    public TexturePreviewView(@NonNull Context context,
                              @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TexturePreviewView(@NonNull Context context, @Nullable AttributeSet attrs,
                              @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textureView = new TextureView(getContext());
        animation = AnimationUtils.loadAnimation(getContext(), R.anim.scale_big);
        addView(textureView);
    }

    /**
     * 有些ImageSource如系统相机前置设置头为镜面效果。这样换算坐标的时候会不一样
     *
     * @param mirrored 是否为镜面效果。
     */
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int selfWidth = getWidth();
        int selfHeight = getHeight();
        if (videoWidth == 0 || videoHeight == 0 || selfWidth == 0 || selfHeight == 0) {
            return;
        }
        PreviewView.ScaleType scaleType = resolveScaleType();
        if (scaleType == PreviewView.ScaleType.FIT_HEIGHT) {
            int targetWith = videoWidth * selfHeight / videoHeight;
            int delta = (targetWith - selfWidth) / 2;
            textureView.layout(left - delta, top, right + delta, bottom);
            if (landscape) {
                TexturePreviewView.this.setScaleX(1.0f);
                TexturePreviewView.this.setScaleY(1.0f);
            }
        } else {
            int targetHeight = videoHeight * selfWidth / videoWidth;
            int delta = (targetHeight - selfHeight) / 2;
//            textureView.layout(left, top - delta, right, bottom + delta);//0,-441,1200,1783-441

            textureView.layout(left, top - delta, right, bottom + delta);//0,-441,1200,1783-441-宽度已达到边界
            // 计算缩放比例.
            float scaleWidth = ((float) selfWidth) / videoHeight;
            float scaleHeight = ((float) selfHeight) / videoWidth;
            landscape = true;
            TexturePreviewView.this.setScaleX(1.5f);
            TexturePreviewView.this.setScaleY(1.4f);
        }
    }


    private void setScale(View v, float scale) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", v.getScaleX(), scale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", v.getScaleY(), scale);
        //组合
        AnimatorSet animatorSet = new AnimatorSet();
        //放到一起
        animatorSet.play(scaleX).with(scaleY);
        animatorSet.setDuration(500);
        // animationSet.setRepeatMode(Animation.RESTART);
        // animationSet.setRepeatCount(1);
        animatorSet.start();
    }


    @Override
    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        if (mRequestedAspect != aspectRatio) {
            mRequestedAspect = aspectRatio;
            requestLayout();
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//
//        if (mRequestedAspect > 0) {
//            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
//            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);
//
//            final int horizPadding = getPaddingLeft() + getPaddingRight();
//            final int vertPadding = getPaddingTop() + getPaddingBottom();
//            initialWidth -= horizPadding;
//            initialHeight -= vertPadding;
//
//            final double viewAspectRatio = (double)initialWidth / initialHeight;
//            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;
//
//            if (Math.abs(aspectDiff) > 0.01) {
//                if (aspectDiff > 0) {
//                    // width priority decision
//                    initialHeight = (int) (initialWidth / mRequestedAspect);
//                } else {
//                    // height priority decison
//                    initialWidth = (int) (initialHeight * mRequestedAspect);
//                }
//                initialWidth += horizPadding;
//                initialHeight += vertPadding;
//                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
//                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
//            }
//        }
//
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    }


    @Override
    public TextureView getTextureView() {
        return textureView;
    }

    @Override
    public void setPreviewSize(int width, int height) {
        if (this.videoWidth == width && this.videoHeight == height) {
            return;
        }
        this.videoWidth = width;
        this.videoHeight = height;
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });

    }

    @Override
    public void mapToOriginalRect(RectF rectF) {

        int selfWidth = getWidth();
        int selfHeight = getHeight();
        if (videoWidth == 0 || videoHeight == 0 || selfWidth == 0 || selfHeight == 0) {
            return;
            // TODO
        }

        Matrix matrix = new Matrix();
        ScaleType scaleType = resolveScaleType();
        if (scaleType == PreviewView.ScaleType.FIT_HEIGHT) {
            int targetWith = videoWidth * selfHeight / videoHeight;
            int delta = (targetWith - selfWidth) / 2;
            float ratio = 1.0f * videoHeight / selfHeight;
            matrix.postTranslate(delta, 0);
            matrix.postScale(ratio, ratio);
        } else {
            int targetHeight = videoHeight * selfWidth / videoWidth;
            int delta = (targetHeight - selfHeight) / 2;

            float ratio = 1.0f * videoWidth / selfWidth;
            matrix.postTranslate(0, delta);
            matrix.postScale(ratio, ratio);
        }
        matrix.mapRect(rectF);
    }

    @Override
    public void mapFromOriginalRect(RectF rectF) {
        int selfWidth = getWidth();
        int selfHeight = getHeight();
        if (videoWidth == 0 || videoHeight == 0 || selfWidth == 0 || selfHeight == 0) {
            return;
            // TODO
        }

        Matrix matrix = new Matrix();

        ScaleType scaleType = resolveScaleType();
        if (scaleType == ScaleType.FIT_HEIGHT) {
            int targetWith = videoWidth * selfHeight / videoHeight;
            int delta = (targetWith - selfWidth) / 2;

            float ratio = 1.0f * selfHeight / videoHeight;

            matrix.postScale(ratio, ratio);
            matrix.postTranslate(-delta, 0);
        } else {
            int targetHeight = videoHeight * selfWidth / videoWidth;
            int delta = (targetHeight - selfHeight) / 2;

            float ratio = 1.0f * selfWidth / videoWidth;

            matrix.postScale(ratio, ratio);
            matrix.postTranslate(0, -delta);
        }
        matrix.mapRect(rectF);

        if (mirrored) {
            float left = selfWidth - rectF.right;
            float right = left + rectF.width();
            rectF.left = left;
            rectF.right = right;
        }
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        this.scaleType = scaleType;
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    @Override
    public PreviewView.ScaleType getScaleType() {
        return scaleType;
    }

    private PreviewView.ScaleType resolveScaleType() {
        float selfRatio = 1.0f * getWidth() / getHeight();
        float targetRatio = 1.0f * videoWidth / videoHeight;

        ScaleType scaleType = this.scaleType;
        if (this.scaleType == ScaleType.CROP_INSIDE) {
            scaleType = selfRatio > targetRatio ? ScaleType.FIT_WIDTH : ScaleType.FIT_HEIGHT;
        }
        return scaleType;
    }

    private ScaleType scaleType = ScaleType.CROP_INSIDE;
    private Handler handler = new Handler(Looper.getMainLooper());

}
