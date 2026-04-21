package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_select);
        bindDifficultyButtons();
    }

    private void bindDifficultyButtons() {
        Button easyButton = findViewById(R.id.btn_easy);
        Button normalButton = findViewById(R.id.btn_normal);
        Button hardButton = findViewById(R.id.btn_hard);
        Button onlineButton = findViewById(R.id.btn_online);

        CheckBox musicSwitch = findViewById(R.id.switch_music);
        EditText userIdInput = findViewById(R.id.et_user_id);

        easyButton.setOnClickListener(v -> startGame(
                GameActivity.DIFFICULTY_EASY,
                musicSwitch.isChecked(),
                normalizeUserId(userIdInput.getText().toString(), false)));
        normalButton.setOnClickListener(v -> startGame(
                GameActivity.DIFFICULTY_NORMAL,
                musicSwitch.isChecked(),
                normalizeUserId(userIdInput.getText().toString(), false)));
        hardButton.setOnClickListener(v -> startGame(
                GameActivity.DIFFICULTY_HARD,
                musicSwitch.isChecked(),
                normalizeUserId(userIdInput.getText().toString(), false)));
        onlineButton.setOnClickListener(v -> {
            String userId = normalizeUserId(userIdInput.getText().toString(), true);
            if (userId == null) {
                Toast.makeText(this, "联机模式必须输入 userId", Toast.LENGTH_SHORT).show();
                return;
            }
            startGame(GameActivity.DIFFICULTY_ONLINE, musicSwitch.isChecked(), userId);
        });
    }

    private String normalizeUserId(String rawUserId, boolean mustInput) {
        String trimmed = rawUserId == null ? "" : rawUserId.trim();
        if (trimmed.isEmpty()) {
            if (mustInput) {
                return null;
            }
            return "匿名玩家";
        }
        String normalized = trimmed.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (normalized.isEmpty()) {
            return mustInput ? null : "匿名玩家";
        }
        return normalized.length() > 24 ? normalized.substring(0, 24) : normalized;
    }

    private void startGame(String difficulty, boolean musicEnabled, String userId) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty);
        intent.putExtra(GameActivity.EXTRA_MUSIC_ENABLED, musicEnabled);
        intent.putExtra(GameActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }
}
