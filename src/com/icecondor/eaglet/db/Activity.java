package com.icecondor.eaglet.db;

import java.security.InvalidParameterException;
import java.util.UUID;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.icecondor.eaglet.Constants;

public class Activity implements Sqlitable {
    protected final JSONObject json;
    protected final String id;
    protected final DateTime date;

    public Activity() {
        json = new JSONObject();
        id = UUID.randomUUID().toString();
        date = new DateTime();
        try {
            json.put("id", id);
            json.put("class", getClass().getName());
            json.put("date", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTableName() {
        return Database.TABLE_ACTIVITIES;
    }

    static public Cursor getAll(Database db) {
        if(db == null) {
            Log.d(Constants.APP_TAG, "getAll called with null db!");
            throw new InvalidParameterException();
        } else {
            return db.
                    getReadonly().
                    query(Database.TABLE_ACTIVITIES, null,
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
