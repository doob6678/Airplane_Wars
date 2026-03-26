package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

/**
 * 简单模式游戏视图。
 */
public class EasyGame extends BaseGame {

    public EasyGame(Context context) {
        super(context);
    }

    public EasyGame(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EasyGame(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected Bitmap resolveBackgroundBitmap() {
        return ImageManager.BACKGROUND_IMAGE_EASY;
    }

    @Override
    protected int resolveEnemyMaxNumber() {
        return 5;
    }

    @Override
    protected int resolveBossScoreThreshold() {
        return 900;
    }

    @Override
    protected int resolveHeroShootCycle() {
        return 180;
    }

    @Override
    protected double resolveEliteProbability() {
        return 0.20;
    }

    @Override
    protected double resolveElitePlusProbability() {
        return 0.05;
    }
}
