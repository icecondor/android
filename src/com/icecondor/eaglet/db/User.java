package com.icecondor.eaglet.db;

import android.content.ContentValues;

public class User implements Sqlitable {

    @Override
    public String getTableName() {
        return Database.USERS_TABLE;
    }

    @Override
    public ContentValues getAttributes() {
        ContentValues cv = new ContentValues();
        return cv;
    }

}
