package com.icecondor.eaglet.db;

import java.util.UUID;

import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

public class Activity implements Sqlitable {
    protected final JSONObject json;

    public Activity() {
        json = new JSONObject();
    }

    @Override
    public String getTableName() {
        return Database.ACTIVITIES_TABLE;
    }

    static public Cursor getAll(Database db) {
        return db.getReadonly().query(Database.ACTIVITIES_TABLE, null,
                                        null, null, null, null, "created_at desc", "50");
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = new ContentValues();
        cv.put(Database.ACTIVITIES_UUID, UUID.randomUUID().toString());
        return cv;
    }


}
