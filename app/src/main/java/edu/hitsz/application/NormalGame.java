package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class NormalGame extends BaseGame {

    public NormalGame(Context context) {
        super(context);
    }

    public NormalGame(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NormalGame(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected Bitmap resolveBackgroundBitmap() {
        return ImageManager.BACKGROUND_IMAGE_NORMAL;
    }

    @Override
    protected int resolveEnemyMaxNumber() {
        return 7;
    }

    @Override
    protected int resolveBossScoreThreshold() {
        return 700;
    }

    @Override
    protected int resolveHeroShootCycle() {
        return 220;
    }

    @Override
    protected double resolveEliteProbability() {
        return 0.30;
    }

    @Override
    protected double resolveElitePlusProbability() {
        return 0.10;
    }
}
