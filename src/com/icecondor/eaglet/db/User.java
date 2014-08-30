package com.icecondor.eaglet.db;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;

public class User implements Sqlitable {
    String id;
    String username;

    public User(JSONObject userJson) {
        try {
            id = userJson.getString("id");
            username= userJson.getString("username");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTableName() {
        return Database.TABLE_USERS;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = new ContentValues();
        cv.put(Database.USERS_UUID, id);
        cv.put(Database.USERS_USERNAME, username);
        return cv;
    }

    public void insertOrReplace(JSONObject user) {

    }
}
