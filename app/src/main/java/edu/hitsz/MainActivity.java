package edu.hitsz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;

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
        CheckBox musicSwitch = findViewById(R.id.switch_music);

        easyButton.setOnClickListener(v -> startGame(GameActivity.DIFFICULTY_EASY, musicSwitch.isChecked()));
        normalButton.setOnClickListener(v -> startGame(GameActivity.DIFFICULTY_NORMAL, musicSwitch.isChecked()));
        hardButton.setOnClickListener(v -> startGame(GameActivity.DIFFICULTY_HARD, musicSwitch.isChecked()));
    }

    private void startGame(String difficulty, boolean musicEnabled) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty);
        intent.putExtra(GameActivity.EXTRA_MUSIC_ENABLED, musicEnabled);
        startActivity(intent);
    }
}
