package com.icecondor.nest.db;

import java.security.InvalidParameterException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.icecondor.nest.Constants;

public class Activity implements Sqlitable {
    protected final JSONObject json = new JSONObject();
    protected final String id;
    protected final ZonedDateTime date;

    public Activity(String type) {
        id = UUID.randomUUID().toString();
        date = ZonedDateTime.now(ZoneOffset.UTC);
        try {
            json.put("id", id);
            json.put("class", getClass().getName());
            json.put("date", date.format(DateTimeFormatter.ISO_INSTANT));
            json.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Activity(JSONObject ijson) throws JSONException {
        if (ijson.getString("class").equals(getClass().getName())) {
            id = ijson.getString("id");
            date = ZonedDateTime.parse(ijson.getString("date"));

            json.put("id", id);
            json.put("class", ijson.getString("class"));
            json.put("date", date.format(DateTimeFormatter.ISO_INSTANT));
            json.put("type", ijson.getString("type"));
        } else {
            throw new RuntimeException("Incompatible JSON for " + getClass().getName());
        }
    }

    public ZonedDateTime getDateTime() {
        return date;
    }

    @Override
    public String getTableName() {
        return Database.TABLE_ACTIVITIES;
    }

    static public Cursor getAll(Database db, int i) {
        if (db == null) {
            Log.d(Constants.APP_TAG, "getAll called with null db!");
            throw new InvalidParameterException();
        } else {
            return db.
                    getReadonly().
                    query(Database.TABLE_ACTIVITIES, null,
                            null, null, null, null, "created_at desc", Integer.toString(i));
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
