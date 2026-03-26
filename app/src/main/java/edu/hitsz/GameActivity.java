package edu.hitsz;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.application.BaseGame;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.HardGame;
import edu.hitsz.application.NormalGame;
import edu.hitsz.audio.GameAudioManager;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_MUSIC_ENABLED = "music_enabled";
    public static final String DIFFICULTY_EASY = "easy";
    public static final String DIFFICULTY_NORMAL = "normal";
    public static final String DIFFICULTY_HARD = "hard";

    private GameAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = GameAudioManager.getInstance(this);
        boolean musicEnabled = getIntent().getBooleanExtra(EXTRA_MUSIC_ENABLED, true);
        audioManager.setMusicEnabled(musicEnabled);
        String difficulty = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        setContentView(createGameView(difficulty));
    }

    @Override
    protected void onDestroy() {
        if (audioManager != null) {
            audioManager.release();
        }
        super.onDestroy();
    }

    private BaseGame createGameView(String difficulty) {
        if (DIFFICULTY_HARD.equalsIgnoreCase(difficulty)) {
            return new HardGame(this);
        }
        if (DIFFICULTY_NORMAL.equalsIgnoreCase(difficulty)) {
            return new NormalGame(this);
        }
        return new EasyGame(this);
    }
}
