package com.icecondor.nest.db;

import android.content.ContentValues;

public interface Sqlitable {
    public String getTableName();
    public ContentValues getAttributes();
}
