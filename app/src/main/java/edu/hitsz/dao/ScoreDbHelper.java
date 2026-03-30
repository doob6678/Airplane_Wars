package edu.hitsz.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.File;

/**
 * 排行榜数据库建表与升级管理。
 */
public class ScoreDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "ranklist.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_SCORE = "score_record";
    public static final String COL_ID = "_id";
    public static final String COL_PLAYER = "player_name";
    public static final String COL_SCORE = "score";
    public static final String COL_TIME = "record_time";

    private static final String SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_SCORE + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COL_PLAYER + " TEXT NOT NULL,"
            + COL_SCORE + " INTEGER NOT NULL,"
            + COL_TIME + " INTEGER NOT NULL"
            + ")";

    public ScoreDbHelper(@Nullable Context context) {
        super(context, buildDbPath(context), null, DATABASE_VERSION);
    }

    private static String buildDbPath(@Nullable Context context) {
        if (context == null) {
            return DATABASE_NAME;
        }
        Context appContext = context.getApplicationContext();
        File dbFile = new File(appContext.getFilesDir(), DATABASE_NAME);
        return dbFile.getAbsolutePath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCORE);
        onCreate(db);
    }
}
