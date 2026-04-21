package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

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
    public static final String DIFFICULTY_ONLINE = "online";
    private static final int MSG_GAME_OVER = 1;
    private static final int MSG_ONLINE_GAME_OVER = 2;

    private GameAudioManager audioManager;
    private boolean leaderboardOpened = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper(), this::handleUiMessage);

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
        BaseGame gameView;
        if (DIFFICULTY_HARD.equalsIgnoreCase(difficulty)) {
            gameView = new HardGame(this);
        } else if (DIFFICULTY_NORMAL.equalsIgnoreCase(difficulty)) {
            gameView = new NormalGame(this);
        } else if (DIFFICULTY_ONLINE.equalsIgnoreCase(difficulty)) {
            gameView = new edu.hitsz.application.OnlineGame(this);
        } else {
            gameView = new EasyGame(this);
        }
        gameView.setGameEventListener(new BaseGame.GameEventListener() {
            @Override
            public void onGameOver(int score) {
                Message message = uiHandler.obtainMessage(MSG_GAME_OVER, score, 0);
                uiHandler.sendMessage(message);
            }

            @Override
            public void onOnlineGameOver(int score, int opponentScore) {
                Message message = uiHandler.obtainMessage(MSG_ONLINE_GAME_OVER, score, opponentScore);
                uiHandler.sendMessage(message);
            }
        });
        return gameView;
    }

    private boolean handleUiMessage(Message message) {
        if (message.what == MSG_GAME_OVER) {
            navigateToLeaderboard(message.arg1, false, 0);
            return true;
        } else if (message.what == MSG_ONLINE_GAME_OVER) {
            navigateToLeaderboard(message.arg1, true, message.arg2);
            return true;
        }
        return false;
    }

    private void navigateToLeaderboard(int score, boolean isOnline, int opponentScore) {
        if (leaderboardOpened || isFinishing() || isDestroyed()) {
            return;
        }
        leaderboardOpened = true;
        Intent intent = new Intent(this, LeaderboardActivity.class);
        intent.putExtra(LeaderboardActivity.EXTRA_SCORE, score);
        intent.putExtra(LeaderboardActivity.EXTRA_DIFFICULTY, getIntent().getStringExtra(EXTRA_DIFFICULTY));
        if (isOnline) {
            intent.putExtra(LeaderboardActivity.EXTRA_IS_ONLINE, true);
            intent.putExtra(LeaderboardActivity.EXTRA_OPPONENT_SCORE, opponentScore);
        }
        startActivity(intent);
        finish();
    }
}
