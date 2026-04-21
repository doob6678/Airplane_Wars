package edu.hitsz;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import edu.hitsz.adapter.ScoreListAdapter;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;
import edu.hitsz.dao.SQLiteScoreDaoImpl;

/**
 * 排行榜页面：负责 UI 展示与交互，数据操作委托给 DAO。
 */
public class LeaderboardActivity extends AppCompatActivity {
    private static final String TAG = "LeaderboardActivity";
    private static final String SERVER_HOST = "10.0.2.2";
    private static final int SERVER_PORT = 9999;
    private static final int CONNECT_TIMEOUT_MS = 5000;

    public static final String EXTRA_SCORE = "final_score";
    public static final String EXTRA_IS_ONLINE = "is_online";
    public static final String EXTRA_OPPONENT_SCORE = "opponent_score";
    public static final String EXTRA_OPPONENT_USER_ID = "opponent_user_id";
    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_USER_ID = "user_id";

    private ScoreDao scoreDao;
    private ScoreListAdapter adapter;
    private boolean isOnlineMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        scoreDao = new SQLiteScoreDaoImpl(getApplicationContext());
        isOnlineMode = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);

        handleOnlineScores();
        initViews();
        if (isOnlineMode) {
            loadOnlineRanking();
        } else {
            maybeInsertCurrentScore();
            refreshList();
        }
    }

    private void handleOnlineScores() {
        boolean isOnline = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);
        if (isOnline) {
            int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
            int opponentScore = getIntent().getIntExtra(EXTRA_OPPONENT_SCORE, 0);
            String opponentUserId = getIntent().getStringExtra(EXTRA_OPPONENT_USER_ID);
            if (opponentUserId == null || opponentUserId.trim().isEmpty()) {
                opponentUserId = "Unknown";
            }
            TextView tvOnlineScores = findViewById(R.id.tv_online_scores);
            tvOnlineScores.setVisibility(android.view.View.VISIBLE);
            tvOnlineScores.setText("联机结算 - 你的得分: " + score
                    + " | 对手(" + opponentUserId + ")得分: " + opponentScore);
        }
    }

    private void initViews() {
        ListView listView = findViewById(R.id.list_rank);
        Button clearButton = findViewById(R.id.btn_clear_all);
        Button backHomeButton = findViewById(R.id.btn_back_home);

        adapter = new ScoreListAdapter(this);
        listView.setAdapter(adapter);

        if (isOnlineMode) {
            clearButton.setVisibility(android.view.View.GONE);
        } else {
            // 本地排行榜支持删除
            listView.setOnItemLongClickListener((parent, view, position, id) -> {
                ScoreRecord record = adapter.getItemRecord(position);
                showDeleteOneDialog(record);
                return true;
            });
            clearButton.setOnClickListener(v -> showDeleteAllDialog());
        }

        backHomeButton.setOnClickListener(v -> navigateBackToHome());
    }

    private void maybeInsertCurrentScore() {
        boolean isOnline = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);
        if (isOnline) {
            return;
        }

        int score = getIntent().getIntExtra(EXTRA_SCORE, -1);
        if (score < 0) {
            return;
        }
        // 本地榜按每次记录，不强制使用 userId
        ScoreRecord record = new ScoreRecord("匿名玩家", score);
        scoreDao.addScore(record);
    }

    private void refreshList() {
        List<ScoreRecord> scores = scoreDao.getAllScores();
        adapter.setData(scores);
    }

    private void showDeleteOneDialog(ScoreRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定删除该条排行榜记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    scoreDao.deleteScore(record.getId());
                    refreshList();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDeleteAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空排行榜")
                .setMessage("确定删除全部排行榜数据吗？")
                .setPositiveButton("清空", (dialog, which) -> {
                    scoreDao.deleteAll();
                    refreshList();
                    Toast.makeText(this, "排行榜已清空", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadOnlineRanking() {
        new Thread(() -> {
            List<ScoreRecord> remoteScores = fetchRemoteRanking();
            if (remoteScores == null) {
                return;
            }
            runOnUiThread(() -> adapter.setData(remoteScores));
        }, "rank-sync-thread").start();
    }

    private List<ScoreRecord> fetchRemoteRanking() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT), CONNECT_TIMEOUT_MS);
            try (PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out.println("GET_RANKING");
                String jsonLine = in.readLine();
                if (jsonLine == null || jsonLine.trim().isEmpty()) {
                    return null;
                }

                JSONArray array = new JSONArray(jsonLine);
                List<ScoreRecord> records = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String player = item.optString("userId", "匿名玩家");
                    int score = item.optInt("score", 0);
                    long time = item.optLong("time", System.currentTimeMillis());
                    records.add(new ScoreRecord(0L, player, score, new Date(time)));
                }
                return records;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to sync ranking from server", e);
            return null;
        }
    }

    private void navigateBackToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
