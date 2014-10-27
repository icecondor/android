package com.icecondor.nest.db;

import java.util.Date;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.icecondor.nest.Constants;

public class Database {
    private final String DATABASE_NAME = "icecondor";
    private final OpenHelper dbHelper;
    public static final int DATABASE_VERSION = 1;
    public static final String ROW_ID = "_id";
    public static final String ROW_CREATED_AT = "created_at";

    /* Users table */
    public static final String TABLE_USERS = "users";
    public static final String USERS_USERNAME = "username";
    public static final String USERS_UUID = "uuid";

    /* Activities table */
    public static final String TABLE_ACTIVITIES = "activities";
    public static final String ACTIVITIES_UUID = "uuid";
    public static final String ACTIVITIES_VERB = "verb";
    public static final String ACTIVITIES_DESCRIPTION = "description";
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
            db.execSQL("CREATE TABLE "+TABLE_USERS+" ("+
                    ROW_ID+" integer primary key, "+
                    USERS_USERNAME + " text," +
                    USERS_UUID + " text," +
                    ROW_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            db.execSQL("CREATE TABLE "+TABLE_ACTIVITIES+" ("+
                    ROW_ID+" integer primary key, "+
                    ACTIVITIES_UUID + " text," +
                    ACTIVITIES_VERB + " text," +
                    ACTIVITIES_DESCRIPTION + " text," +
                    ACTIVITIES_JSON + " text," +
                    ACTIVITIES_SYNCED_AT + " text," +
                    ROW_CREATED_AT + " DATETIME DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ','now'))" +
                    ")");

            db.execSQL("CREATE INDEX "+TABLE_ACTIVITIES+"_"+ACTIVITIES_SYNCED_AT+"_idx on "+TABLE_ACTIVITIES+"("+ACTIVITIES_SYNCED_AT+")");
            db.execSQL("CREATE INDEX "+TABLE_ACTIVITIES+"_"+ROW_CREATED_AT+"_idx on "+TABLE_ACTIVITIES+"("+ROW_CREATED_AT+")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public void append(Sqlitable obj) {
        if(rowCount(obj.getTableName()) > 1000) {
            trimTable(obj.getTableName(), 900);
        }
        ContentValues cv = obj.getAttributes();
        Date now = new Date();
        Log.d(Constants.APP_TAG, ""+now+" Database append("+obj.getClass().getSimpleName()+") "+cv);
        db.insert(obj.getTableName(), null, cv);
    }

    private void trimTable(String tableName, int i) {
        Cursor cursor = db.query(tableName, new String[] {ROW_ID},
                                 null, null, null, null, ROW_ID+" asc", ""+i);
        cursor.moveToFirst();
        long firstRowId = cursor.getLong(cursor.getColumnIndex(ROW_ID));
        db.delete(tableName, ROW_ID+" < ?", new String[] {""+firstRowId});
    }

    public void emptyTable(String tableName) {
        db.delete(tableName, null, null);
    }

    private int rowCount(String tableName) {
        Cursor cursor = db.query(tableName, null, null, null, null, null, null);
        return cursor.getCount();
    }

    public Cursor ActivitiesUnsynced() {
        return db.query(Database.TABLE_ACTIVITIES, null,
                        Database.ACTIVITIES_SYNCED_AT+" IS NULL", null,
                        null, null, ROW_ID+" desc", "");
    }

    public void updateUser(JSONObject userJson) {
        User user = new User(userJson);
        db.insertWithOnConflict(Database.TABLE_USERS, null, user.getAttributes(),
                                 SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void markActivitySynced(int id) {
        ContentValues cv = new ContentValues();
        cv.put(ACTIVITIES_SYNCED_AT, DateTime.now().toString());
        int rows = db.update(TABLE_ACTIVITIES, cv,
                              ROW_ID+" = ?", new String[]{Integer.toString(id)});
    }

    public JSONObject activityJson(int rowId) {
        Cursor c = db.query(Database.TABLE_ACTIVITIES, null,
                        ROW_ID +" is ?", new String[] {String.valueOf(rowId)},
                        null, null, ROW_ID+" desc", "");
        if(c.moveToFirst()) {
            try {
                return new JSONObject(c.getString(c.getColumnIndex(Database.ACTIVITIES_JSON)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
