package edu.hitsz.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import edu.hitsz.R;

public final class GameAudioManager {

    private static final Object INSTANCE_LOCK = new Object();
    private static GameAudioManager instance;

    private final Object stateLock = new Object();
    private final Context appContext;
    private final SoundPool soundPool;
    private final int bulletSoundId;
    private final int bulletHitSoundId;
    private final int bombExplosionSoundId;
    private final int getSupplySoundId;
    private final int gameOverSoundId;

    private MediaPlayer bgmPlayer;
    private MediaPlayer bossBgmPlayer;
    private boolean musicEnabled = true;
    private boolean released = false;

    private GameAudioManager(Context context) {
        appContext = context.getApplicationContext();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(12)
                .setAudioAttributes(audioAttributes)
                .build();
        bulletSoundId = soundPool.load(appContext, R.raw.bullet, 1);
        bulletHitSoundId = soundPool.load(appContext, R.raw.bullet_hit, 1);
        bombExplosionSoundId = soundPool.load(appContext, R.raw.bomb_explosion, 1);
        getSupplySoundId = soundPool.load(appContext, R.raw.get_supply, 1);
        gameOverSoundId = soundPool.load(appContext, R.raw.game_over, 1);
    }

    public static GameAudioManager getInstance(Context context) {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new GameAudioManager(context);
            }
            return instance;
        }
    }

    public void setMusicEnabled(boolean enabled) {
        synchronized (stateLock) {
            if (released) {
                return;
            }
            musicEnabled = enabled;
            if (!musicEnabled) {
                stopPlayer(bgmPlayer);
                stopPlayer(bossBgmPlayer);
            }
        }
    }

    public boolean isMusicEnabled() {
        synchronized (stateLock) {
            return musicEnabled;
        }
    }

    public void playNormalBgm() {
        synchronized (stateLock) {
            if (released || !musicEnabled) {
                return;
            }
            ensureBgmPlayer();
            pausePlayer(bossBgmPlayer);
            startPlayer(bgmPlayer);
        }
    }

    public void playBossBgm() {
        synchronized (stateLock) {
            if (released || !musicEnabled) {
                return;
            }
            ensureBossBgmPlayer();
            pausePlayer(bgmPlayer);
            startPlayer(bossBgmPlayer);
        }
    }

    public void stopAllBgm() {
        synchronized (stateLock) {
            stopPlayer(bgmPlayer);
            stopPlayer(bossBgmPlayer);
        }
    }

    public void playBullet() {
        playEffect(bulletSoundId);
    }

    public void playBulletHit() {
        playEffect(bulletHitSoundId);
    }

    public void playBombExplosion() {
        playEffect(bombExplosionSoundId);
    }

    public void playGetSupply() {
        playEffect(getSupplySoundId);
    }

    public void playGameOver() {
        playEffect(gameOverSoundId);
    }

    public void release() {
        synchronized (stateLock) {
            if (released) {
                return;
            }
            releasePlayer(bgmPlayer);
            releasePlayer(bossBgmPlayer);
            bgmPlayer = null;
            bossBgmPlayer = null;
            soundPool.release();
            released = true;
            synchronized (INSTANCE_LOCK) {
                instance = null;
            }
        }
    }

    private void playEffect(int soundId) {
        synchronized (stateLock) {
            if (released || !musicEnabled) {
                return;
            }
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
        }
    }

    private void ensureBgmPlayer() {
        if (bgmPlayer == null) {
            bgmPlayer = MediaPlayer.create(appContext, R.raw.bgm);
            if (bgmPlayer != null) {
                bgmPlayer.setLooping(true);
            }
        }
    }

    private void ensureBossBgmPlayer() {
        if (bossBgmPlayer == null) {
            bossBgmPlayer = MediaPlayer.create(appContext, R.raw.bgm_boss);
            if (bossBgmPlayer != null) {
                bossBgmPlayer.setLooping(true);
            }
        }
    }

    private void startPlayer(MediaPlayer player) {
        if (player != null && !player.isPlaying()) {
            player.start();
        }
    }

    private void pausePlayer(MediaPlayer player) {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    private void stopPlayer(MediaPlayer player) {
        if (player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
        }
        player.seekTo(0);
    }

    private void releasePlayer(MediaPlayer player) {
        if (player != null) {
            try {
                player.release();
            } catch (RuntimeException ignored) {
            }
        }
    }
}
