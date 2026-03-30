package edu.hitsz;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import edu.hitsz.adapter.ScoreListAdapter;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.dao.ScoreRecord;
import edu.hitsz.dao.SQLiteScoreDaoImpl;

/**
 * 排行榜页面：负责 UI 展示与交互，数据操作委托给 DAO。
 */
public class LeaderboardActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "final_score";

    private ScoreDao scoreDao;
    private ScoreListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        scoreDao = new SQLiteScoreDaoImpl(getApplicationContext());
        initViews();
        maybeInsertCurrentScore();
        refreshList();
    }

    private void initViews() {
        ListView listView = findViewById(R.id.list_rank);
        Button clearButton = findViewById(R.id.btn_clear_all);

        adapter = new ScoreListAdapter(this);
        listView.setAdapter(adapter);

        // 长按单条记录进行删除
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            ScoreRecord record = adapter.getItemRecord(position);
            showDeleteOneDialog(record);
            return true;
        });

        clearButton.setOnClickListener(v -> showDeleteAllDialog());
    }

    private void maybeInsertCurrentScore() {
        int score = getIntent().getIntExtra(EXTRA_SCORE, -1);
        if (score < 0) {
            return;
        }
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
}
