package com.icecondor.eaglet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.icecondor.eaglet.Constants;

public class Database {
    private final String DATABASE_NAME = "icecondor";
    private final OpenHelper dbHelper;
    public static final int DATABASE_VERSION = 1;
    public static final String ROW_CREATED_AT = "created_at";

    /* Users table */
    public static final String USERS_TABLE = "users";
    public static final String USERS_USERNAME = "username";
    public static final String USERS_SESSION_KEY = "session_key";

    /* Activities table */
    public static final String ACTIVITIES_TABLE = "activities";
    public static final String ACTIVITIES_UUID = "uuid";
    public static final String ACTIVITIES_VERB = "verb";
    public static final String ACTIVITIES_JSON = "json";
    public static final String ACTIVITIES_SYNCED_AT = "synced_at";

    private SQLiteDatabase db;

    public Database(Context context) {
        dbHelper = new OpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Database open() throws SQLException {
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public SQLiteDatabase getReadonly() {
        return dbHelper.getReadableDatabase();
    }

    public void close() {
        db.close();
    }

    private static class OpenHelper extends SQLiteOpenHelper {

        public OpenHelper(Context context, String name, CursorFactory factory,
                int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE "+USERS_TABLE+" ("+
                    "_id integer primary key, "+
                    USERS_USERNAME + " text," +
                    USERS_SESSION_KEY + " text," +
                    ROW_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            db.execSQL("CREATE TABLE "+ACTIVITIES_TABLE+" ("+
                    "_id integer primary key, "+
                    ACTIVITIES_UUID + " text," +
                    ACTIVITIES_VERB + " text," +
                    ACTIVITIES_JSON + " text," +
                    ACTIVITIES_SYNCED_AT + " text," +
                    ROW_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public void append(Sqlitable obj) {
        ContentValues cv = obj.getAttributes();
        Log.d(Constants.APP_TAG, "Database append() "+obj.getClass().getSimpleName()+" "+cv);
        db.insert(obj.getTableName(), null, cv);
    }

}
