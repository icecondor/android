package com.icecondor.eaglet.db;

import java.security.InvalidParameterException;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.icecondor.eaglet.Constants;

public class Activity implements Sqlitable {
    protected final JSONObject json;

    public Activity() {
        json = new JSONObject();
        try {
            json.put("id", UUID.randomUUID());
            json.put("class", getClass().getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTableName() {
        return Database.ACTIVITIES_TABLE;
    }

    static public Cursor getAll(Database db) {
        if(db == null) {
            Log.d(Constants.APP_TAG, "getAll called with null db!");
            throw new InvalidParameterException();
        } else {
            return db.
                    getReadonly().
                    query(Database.ACTIVITIES_TABLE, null,
                          null, null, null, null, "created_at desc", "150");
        }
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = new ContentValues();
        try {
            cv.put(Database.ACTIVITIES_UUID, json.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cv;
    }


}
