package com.icecondor.eaglet.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class Database {
    private final String DATABASE_NAME = "icecondor";
    private final OpenHelper dbHelper;
    public static final int DATABASE_VERSION = 1;

    /* Users table */
    private static final String USERS_TABLE = "users";
    private static final String USERS_USERNAME = "username";
    private static final String USERS_SESSION_KEY = "session_key";

    /* Activities table */
    private static final String ACTIVITIES_TABLE = "activities";
    private static final String ACTIVITIES_UUID = "uuid";
    private static final String ACTIVITIES_JSON = "json";
    private static final String ACTIVITIES_SYNCED_AT = "synced_at";

    private SQLiteDatabase db;

    public Database(Context context) {
        dbHelper = new OpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public Database open() throws SQLException {
        db = dbHelper.getWritableDatabase();
        return this;
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
                    USERS_SESSION_KEY + " text" +
                    ")");

            db.execSQL("CREATE TABLE "+ACTIVITIES_TABLE+" ("+
                    "_id integer primary key, "+
                    ACTIVITIES_UUID + " text," +
                    ACTIVITIES_JSON + " text," +
                    ACTIVITIES_SYNCED_AT + " text" +
                    ")");

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

}
