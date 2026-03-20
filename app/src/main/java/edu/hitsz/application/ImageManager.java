package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.R;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.BulletPlusProp;
import edu.hitsz.prop.BulletProp;

import java.util.HashMap;
import java.util.Map;

/**
 * 综合管理图片的加载，访问
 * 提供图片的静态访问方法
 *
 * @author hitsz
 */
public class ImageManager {

    /**
     * 类名-图片 映射，存储各基类的图片 <br>
     * 可使用 CLASSNAME_IMAGE_MAP.get( obj.getClass().getName() ) 获得 obj 所属基类对应的图片
     */
    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap BACKGROUND_IMAGE_EASY;
    public static Bitmap BACKGROUND_IMAGE_NORMAL;
    public static Bitmap BACKGROUND_IMAGE_HARD;

    public static Bitmap HERO_IMAGE;

    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;

    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap ELITE_PLUS_ENEMY_IMAGE;
    public static Bitmap BOSS_ENEMY_IMAGE;

    public static Bitmap PROP_BLOOD_IMAGE;
    public static Bitmap PROP_BOMB_IMAGE;
    public static Bitmap PROP_BULLET_IMAGE;
    public static Bitmap PROP_BULLET_PLUS_IMAGE;

    private static boolean initialized = false;

    private ImageManager() {
    }

    public static synchronized void initialize(Context context) {
        if (initialized) {
            return;
        }

        Context appContext = context.getApplicationContext();

        BACKGROUND_IMAGE_EASY = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.bg);
        BACKGROUND_IMAGE_NORMAL = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.bg2);
        BACKGROUND_IMAGE_HARD = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.bg3);

        HERO_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.hero);

        HERO_BULLET_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.bullet_hero);
        ENEMY_BULLET_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.bullet_enemy);

        MOB_ENEMY_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.mob);
        ELITE_ENEMY_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.elite);
        ELITE_PLUS_ENEMY_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.eliteplus);
        BOSS_ENEMY_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.boss);

        PROP_BLOOD_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.prop_blood);
        PROP_BOMB_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.prop_bomb);
        PROP_BULLET_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.prop_bullet);
        PROP_BULLET_PLUS_IMAGE = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.prop_bulletplus);

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);

        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);

        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ElitePlusEnemy.class.getName(), ELITE_PLUS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_ENEMY_IMAGE);

        CLASSNAME_IMAGE_MAP.put(BloodProp.class.getName(), PROP_BLOOD_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombProp.class.getName(), PROP_BOMB_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletProp.class.getName(), PROP_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletPlusProp.class.getName(), PROP_BULLET_PLUS_IMAGE);

        initialized = true;
    }

    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }

}
