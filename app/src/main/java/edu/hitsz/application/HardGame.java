package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class HardGame extends BaseGame {

    public HardGame(Context context) {
        super(context);
    }

    public HardGame(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HardGame(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected Bitmap resolveBackgroundBitmap() {
        return ImageManager.BACKGROUND_IMAGE_HARD;
    }

    @Override
    protected int resolveEnemyMaxNumber() {
        return 9;
    }

    @Override
    protected int resolveBossScoreThreshold() {
        return 500;
    }

    @Override
    protected int resolveHeroShootCycle() {
        return 260;
    }

    @Override
    protected double resolveEliteProbability() {
        return 0.35;
    }

    @Override
    protected double resolveElitePlusProbability() {
        return 0.15;
    }
}
