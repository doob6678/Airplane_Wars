package edu.hitsz.application;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.AbstractEnemy;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.audio.GameAudioManager;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.factory.enemy.EnemyType;
import edu.hitsz.factory.enemy.UnifiedEnemyFactory;
import edu.hitsz.observer.BombPublisher;
import edu.hitsz.observer.BombSubscriber;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BombProp;

/**
 * 安卓基础游戏视图，负责接收触屏输入并驱动英雄机移动。
 */
public class BaseGame extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public interface GameEventListener {
        void onGameOver(int score);
    }

    private final SurfaceHolder surfaceHolder;
    private final Paint paint;

    private Thread drawThread;
    private volatile boolean isDrawing;

    private int screenWidth;
    private int screenHeight;
    private float renderScale = 1f;
    private float viewportLeft = 0f;
    private float viewportTop = 0f;
    private float viewportWidth = Main.WINDOW_WIDTH;
    private float viewportHeight = Main.WINDOW_HEIGHT;

    private HeroAircraft heroAircraft;

    private Bitmap backgroundBitmap;

    private final List<AbstractAircraft> enemyAircraft = new LinkedList<>();
    private final List<BaseBullet> heroBullets = new LinkedList<>();
    private final List<BaseBullet> enemyBullets = new LinkedList<>();
    private final List<AbstractProp> props = new LinkedList<>();
    private final UnifiedEnemyFactory enemyFactory = new UnifiedEnemyFactory().enableRandom(0.25, 0.0);

    private int score = 0;
    private int cycleTime = 0;
    private int enemyMaxNumber = 5;
    private int bossScoreThreshold = Integer.MAX_VALUE;
    private boolean bossExists = false;
    private double eliteProbability = 0.25;
    private double elitePlusProbability = 0.0;
    private GameAudioManager audioManager;
    private boolean bossBgmActive = false;
    private boolean gameOverHandled = false;
    private GameEventListener gameEventListener;

    private static final int TIME_INTERVAL = 40;
    private static final int CYCLE_DURATION = 600;
    private static final float AIRCRAFT_RENDER_SCALE = 0.3f;
    private static final float BULLET_RENDER_SCALE = 0.45f;
    private static final float PROP_RENDER_SCALE = 0.35f;

    public BaseGame(Context context) {
        super(context);
        surfaceHolder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        initCore();
    }

    public BaseGame(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        surfaceHolder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        initCore();
    }

    public BaseGame(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        surfaceHolder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        initCore();
    }

    private void initCore() {
        surfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        initTouchControl();
    }

    public void setGameEventListener(@Nullable GameEventListener gameEventListener) {
        this.gameEventListener = gameEventListener;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initTouchControl() {
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getActionMasked();
                if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE) {
                    return false;
                }

                if (heroAircraft == null) {
                    return false;
                }

                float touchX = motionEvent.getX();
                float touchY = motionEvent.getY();

                float maxX = screenWidth > 0 ? screenWidth : view.getWidth();
                float maxY = screenHeight > 0 ? screenHeight : view.getHeight();

                float effectiveScale = renderScale > 0 ? renderScale
                        : Math.min(maxX / Main.WINDOW_WIDTH, maxY / Main.WINDOW_HEIGHT);
                if (effectiveScale <= 0) {
                    effectiveScale = 1f;
                }

                float left = viewportLeft;
                float top = viewportTop;
                float right = left + viewportWidth;
                float bottom = top + viewportHeight;

                float clampedX = Math.max(left, Math.min(touchX, right));
                float clampedY = Math.max(top, Math.min(touchY, bottom));

                float worldX = (clampedX - left) / effectiveScale;
                float worldY = (clampedY - top) / effectiveScale;
                worldX = Math.max(0, Math.min(worldX, Main.WINDOW_WIDTH));
                worldY = Math.max(0, Math.min(worldY, Main.WINDOW_HEIGHT));

                heroAircraft.setLocation(worldX, worldY);
                return true;
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        ImageManager.initialize(getContext());
        heroAircraft = HeroAircraft.getInstance();
        backgroundBitmap = resolveBackgroundBitmap();
        enemyMaxNumber = resolveEnemyMaxNumber();
        bossScoreThreshold = resolveBossScoreThreshold();
        eliteProbability = resolveEliteProbability();
        elitePlusProbability = resolveElitePlusProbability();
        enemyFactory.enableRandom(eliteProbability, elitePlusProbability);
        heroAircraft.setShootCycle(resolveHeroShootCycle());
        audioManager = GameAudioManager.getInstance(getContext());
        bossBgmActive = false;
        gameOverHandled = false;
        audioManager.playNormalBgm();

        isDrawing = true;
        drawThread = new Thread(this, "base-game-draw");
        drawThread.start();
    }

    protected Bitmap resolveBackgroundBitmap() {
        return ImageManager.BACKGROUND_IMAGE_NORMAL;
    }

    protected int resolveEnemyMaxNumber() {
        return 5;
    }

    protected int resolveBossScoreThreshold() {
        return Integer.MAX_VALUE;
    }

    protected int resolveHeroShootCycle() {
        return 200;
    }

    protected double resolveEliteProbability() {
        return 0.25;
    }

    protected double resolveElitePlusProbability() {
        return 0.0;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;

        if (width > 0 && height > 0) {
            float sx = width * 1f / Main.WINDOW_WIDTH;
            float sy = height * 1f / Main.WINDOW_HEIGHT;
            renderScale = Math.min(sx, sy);
            viewportWidth = Main.WINDOW_WIDTH * renderScale;
            viewportHeight = Main.WINDOW_HEIGHT * renderScale;
            viewportLeft = (width - viewportWidth) / 2f;
            viewportTop = (height - viewportHeight) / 2f;
        } else {
            renderScale = 1f;
            viewportWidth = Main.WINDOW_WIDTH;
            viewportHeight = Main.WINDOW_HEIGHT;
            viewportLeft = 0f;
            viewportTop = 0f;
        }

        if (heroAircraft != null) {
            if (heroAircraft.getLocationX() <= 0 || heroAircraft.getLocationY() <= 0) {
                int initX = Main.WINDOW_WIDTH / 2;
                int heroHeight = ImageManager.HERO_IMAGE != null ? ImageManager.HERO_IMAGE.getHeight() : 0;
                int initY = Math.max(0, Main.WINDOW_HEIGHT - heroHeight);
                heroAircraft.setLocation(initX, initY);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDrawing = false;
        if (audioManager != null) {
            audioManager.stopAllBgm();
        }
        if (drawThread != null) {
            try {
                drawThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            drawThread = null;
        }
    }

    @Override
    public void run() {
        long lastTick = System.currentTimeMillis();
        while (isDrawing) {
            long start = System.currentTimeMillis();

            if (start - lastTick >= TIME_INTERVAL) {
                updateGame();
                lastTick = start;
            }

            drawFrame();
            long cost = System.currentTimeMillis() - start;
            long sleep = Math.max(0, 16 - cost);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void updateGame() {
        if (heroAircraft == null || heroAircraft.notValid()) {
            handleGameOverIfNeeded();
            return;
        }

        cycleTime += TIME_INTERVAL;
        if (cycleTime >= CYCLE_DURATION) {
            cycleTime %= CYCLE_DURATION;
            spawnEnemies();
        }

        shootAction();
        bulletsMoveAction();
        aircraftMoveAction();
        propsMoveAction();
        crashCheckAction();
        postProcessAction();
        syncBgmState();
    }

    private void spawnEnemies() {
        if (enemyAircraft.size() < enemyMaxNumber) {
            enemyAircraft.add(enemyFactory.createEnemy());
        }
        if (!bossExists && score >= bossScoreThreshold) {
            enemyFactory.disableRandom();
            enemyAircraft.add(enemyFactory.setType(EnemyType.BOSS).createEnemy());
            enemyFactory.enableRandom(eliteProbability, elitePlusProbability);
            bossExists = true;
            bossScoreThreshold += 500;
        }
    }

    private void shootAction() {
        for (AbstractAircraft enemy : enemyAircraft) {
            if ((enemy instanceof EliteEnemy || enemy instanceof ElitePlusEnemy || enemy instanceof BossEnemy)
                    && enemy.updateShootTimer(TIME_INTERVAL)) {
                List<BaseBullet> bullets = enemy.shoot();
                if (bullets != null) {
                    enemyBullets.addAll(bullets);
                }
            }
        }

        if (heroAircraft.updateShootTimer(TIME_INTERVAL)) {
            List<BaseBullet> bullets = heroAircraft.shoot();
            if (bullets != null) {
                heroBullets.addAll(bullets);
                if (!bullets.isEmpty() && audioManager != null) {
                    audioManager.playBullet();
                }
            }
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftMoveAction() {
        for (AbstractAircraft enemy : enemyAircraft) {
            enemy.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    private void crashCheckAction() {
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
                if (audioManager != null) {
                    audioManager.playBulletHit();
                }
            }
        }

        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemy : enemyAircraft) {
                if (enemy.notValid()) {
                    continue;
                }
                if (enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    if (audioManager != null) {
                        audioManager.playBulletHit();
                    }
                    if (enemy.notValid() && enemy instanceof AbstractEnemy) {
                        AbstractEnemy abstractEnemy = (AbstractEnemy) enemy;
                        score += abstractEnemy.getScore();
                        props.addAll(abstractEnemy.mayDrop());
                        if (enemy instanceof BossEnemy) {
                            bossExists = false;
                        }
                    }
                }
            }
        }

        for (AbstractAircraft enemy : enemyAircraft) {
            if (enemy.notValid()) {
                continue;
            }
            if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                enemy.vanish();
                heroAircraft.decreaseHp(Integer.MAX_VALUE);
                if (audioManager != null) {
                    audioManager.playBombExplosion();
                }
            }
        }

        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }
            if (heroAircraft.crash(prop)) {
                prop.effect(heroAircraft);
                if (audioManager != null) {
                    audioManager.playGetSupply();
                }
                if (prop instanceof BombProp) {
                    score += BombPublisher.drainLastScore();
                    if (audioManager != null) {
                        audioManager.playBombExplosion();
                    }
                }
                prop.vanish();
            }
        }
    }

    private void handleGameOverIfNeeded() {
        if (gameOverHandled) {
            return;
        }
        gameOverHandled = true;
        if (audioManager != null) {
            audioManager.stopAllBgm();
            audioManager.playGameOver();
        }
        GameEventListener listener = gameEventListener;
        if (listener != null) {
            listener.onGameOver(score);
        }
    }

    private void syncBgmState() {
        if (audioManager == null) {
            return;
        }
        if (bossExists && !bossBgmActive) {
            bossBgmActive = true;
            audioManager.playBossBgm();
            return;
        }
        if (!bossExists && bossBgmActive) {
            bossBgmActive = false;
            audioManager.playNormalBgm();
        }
    }

    private void postProcessAction() {
        for (Iterator<BaseBullet> it = enemyBullets.iterator(); it.hasNext();) {
            BaseBullet b = it.next();
            if (b.notValid()) {
                if (b instanceof BombSubscriber) {
                    BombPublisher.unregister((BombSubscriber) b);
                }
                it.remove();
            }
        }

        heroBullets.removeIf(AbstractFlyingObject::notValid);

        for (Iterator<AbstractAircraft> it = enemyAircraft.iterator(); it.hasNext();) {
            AbstractAircraft a = it.next();
            if (a.notValid()) {
                if (a instanceof BombSubscriber) {
                    BombPublisher.unregister((BombSubscriber) a);
                }
                it.remove();
            }
        }

        props.removeIf(AbstractFlyingObject::notValid);
    }

    private void drawFrame() {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            return;
        }
        try {
            canvas.drawColor(Color.BLACK);

            if (backgroundBitmap != null && viewportWidth > 0 && viewportHeight > 0) {
                Rect dst = new Rect(
                        (int) viewportLeft,
                        (int) viewportTop,
                        (int) (viewportLeft + viewportWidth),
                        (int) (viewportTop + viewportHeight));
                canvas.drawBitmap(backgroundBitmap, null, dst, paint);
            }

            drawFlyingObjects(canvas, enemyBullets);
            drawFlyingObjects(canvas, heroBullets);
            drawFlyingObjects(canvas, enemyAircraft);
            drawFlyingObjects(canvas, props);

            if (heroAircraft != null && !heroAircraft.notValid()) {
                drawFlyingObject(canvas, heroAircraft, ImageManager.HERO_IMAGE);
            }

            paint.setColor(Color.RED);
            paint.setTextSize(36f);
            canvas.drawText("SCORE: " + score, viewportLeft + 20, viewportTop + 46, paint);
            if (heroAircraft != null) {
                canvas.drawText("LIFE: " + heroAircraft.getHp(), viewportLeft + 20, viewportTop + 88, paint);
            }
        } finally {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawFlyingObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            if (object.notValid()) {
                continue;
            }
            drawFlyingObject(canvas, object, object.getImage());
        }
    }

    private void drawFlyingObject(Canvas canvas, AbstractFlyingObject object, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        float objectWidth = object.getWidth();
        float objectHeight = object.getHeight();
        float typeScale = 1f;
        if (object instanceof AbstractAircraft) {
            typeScale = AIRCRAFT_RENDER_SCALE;
        } else if (object instanceof BaseBullet) {
            typeScale = BULLET_RENDER_SCALE;
        } else if (object instanceof AbstractProp) {
            typeScale = PROP_RENDER_SCALE;
        }
        float renderWidth = objectWidth * typeScale;
        float renderHeight = objectHeight * typeScale;

        float left = viewportLeft + (object.getLocationX() - renderWidth / 2f) * renderScale;
        float top = viewportTop + (object.getLocationY() - renderHeight / 2f) * renderScale;
        float right = left + renderWidth * renderScale;
        float bottom = top + renderHeight * renderScale;
        Rect dst = new Rect((int) left, (int) top, (int) right, (int) bottom);
        canvas.drawBitmap(bitmap, null, dst, paint);
    }
}
