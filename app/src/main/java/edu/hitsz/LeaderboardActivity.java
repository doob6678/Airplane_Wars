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
 * 排行榜页面：负责 UI 展示与交互，数据操作委托给 DAO/网络层。
 *
 * 模式说明：
 * - 本地模式（简单/普通/困难）：读取 SQLite，记录每次成绩，支持删除/清空；
 * - 联机模式：仅展示服务器排行榜（不写本地、不支持本地删除）。
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

    /**
     * 页面入口：
     * - 识别本地/联机模式；
     * - 初始化视图与交互；
     * - 按模式加载对应排行榜数据。
     *
     * @param savedInstanceState 状态恢复对象
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        scoreDao = new SQLiteScoreDaoImpl(getApplicationContext());
        // 模式切换总开关：同一个页面承载本地榜与联机榜两套数据路径。
        isOnlineMode = getIntent().getBooleanExtra(EXTRA_IS_ONLINE, false);

        handleOnlineScores();
        initViews();
        if (isOnlineMode) {
            loadOnlineRanking();
        } else {
            // 本地模式先落当前分数，再刷新列表。
            maybeInsertCurrentScore();
            refreshList();
        }
    }

    /**
     * 渲染联机结算头部文案（本方分数 + 对手分数）。
     */
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
            // 联机结算头部文案只做展示，不参与本地存储。
            tvOnlineScores.setText("联机结算 - 你的得分: " + score
                    + " | 对手(" + opponentUserId + ")得分: " + opponentScore);
        }
    }

    /**
     * 初始化列表、删除按钮与返回首页按钮。
     * 联机模式下隐藏本地删除能力。
     */
    private void initViews() {
        ListView listView = findViewById(R.id.list_rank);
        Button clearButton = findViewById(R.id.btn_clear_all);
        Button backHomeButton = findViewById(R.id.btn_back_home);

        adapter = new ScoreListAdapter(this);
        listView.setAdapter(adapter);

        if (isOnlineMode) {
            // 联机排行榜禁止本地删除，避免误导为“可删除服务器数据”。
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

    /**
     * 本地模式写入当前局分数。
     * 联机模式下不写本地，避免混入服务器榜单语义。
     */
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

    /**
     * 从本地 DAO 读取数据并刷新列表。
     */
    private void refreshList() {
        List<ScoreRecord> scores = scoreDao.getAllScores();
        adapter.setData(scores);
    }

    /**
     * 弹窗确认并删除单条本地记录。
     *
     * @param record 被删除的记录
     */
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

    /**
     * 弹窗确认并清空本地排行榜。
     */
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

    /**
     * 异步加载联机排行榜并更新 UI。
     * 失败时保持当前显示，不阻塞主线程。
     */
    private void loadOnlineRanking() {
        new Thread(() -> {
            List<ScoreRecord> remoteScores = fetchRemoteRanking();
            if (remoteScores == null) {
                return;
            }
            // 联机榜只做展示，不覆盖本地表。
            runOnUiThread(() -> adapter.setData(remoteScores));
        }, "rank-sync-thread").start();
    }

    /**
     * 通过 Socket 请求服务端在线排行榜。
     *
     * @return 解析后的排行榜；失败返回 null
     */
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

    /**
     * 返回首页并清理中间页面回退栈。
     */
    private void navigateBackToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        // 清理回退栈：从排行榜返回首页后，Back 不再回到结算页。
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
