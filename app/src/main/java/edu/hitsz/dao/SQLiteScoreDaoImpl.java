package edu.hitsz.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Android 平台 SQLite 排行榜 DAO 实现。
 */
public class SQLiteScoreDaoImpl implements ScoreDao {

    private final ScoreDbHelper dbHelper;

    public SQLiteScoreDaoImpl(Context context) {
        Context appContext = context.getApplicationContext();
        this.dbHelper = new ScoreDbHelper(appContext);
    }

    @Override
    public void addScore(ScoreRecord record) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ScoreDbHelper.COL_PLAYER, record.getPlayerName());
        values.put(ScoreDbHelper.COL_SCORE, record.getScore());
        values.put(ScoreDbHelper.COL_TIME, record.getRecordTimeMillis());
        db.insert(ScoreDbHelper.TABLE_SCORE, null, values);
    }

    @Override
    public List<ScoreRecord> getAllScores() {
        List<ScoreRecord> result = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                ScoreDbHelper.TABLE_SCORE,
                new String[] {
                        ScoreDbHelper.COL_ID,
                        ScoreDbHelper.COL_PLAYER,
                        ScoreDbHelper.COL_SCORE,
                        ScoreDbHelper.COL_TIME
                },
                null,
                null,
                null,
                null,
                ScoreDbHelper.COL_SCORE + " DESC, " + ScoreDbHelper.COL_TIME + " ASC")) {
            int idIndex = cursor.getColumnIndexOrThrow(ScoreDbHelper.COL_ID);
            int playerIndex = cursor.getColumnIndexOrThrow(ScoreDbHelper.COL_PLAYER);
            int scoreIndex = cursor.getColumnIndexOrThrow(ScoreDbHelper.COL_SCORE);
            int timeIndex = cursor.getColumnIndexOrThrow(ScoreDbHelper.COL_TIME);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idIndex);
                String player = cursor.getString(playerIndex);
                int score = cursor.getInt(scoreIndex);
                long time = cursor.getLong(timeIndex);
                result.add(new ScoreRecord(id, player, score, new Date(time)));
            }
        }

        return result;
    }

    @Override
    public void saveAll(List<ScoreRecord> scores) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(ScoreDbHelper.TABLE_SCORE, null, null);
            for (ScoreRecord record : scores) {
                ContentValues values = new ContentValues();
                values.put(ScoreDbHelper.COL_PLAYER, record.getPlayerName());
                values.put(ScoreDbHelper.COL_SCORE, record.getScore());
                values.put(ScoreDbHelper.COL_TIME, record.getRecordTimeMillis());
                db.insert(ScoreDbHelper.TABLE_SCORE, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteScore(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(
                ScoreDbHelper.TABLE_SCORE,
                ScoreDbHelper.COL_ID + "=?",
                new String[] { String.valueOf(id) });
    }

    @Override
    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(ScoreDbHelper.TABLE_SCORE, null, null);
    }
}
