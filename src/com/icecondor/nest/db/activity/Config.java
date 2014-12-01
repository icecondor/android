package com.icecondor.nest.db.activity;

import org.json.JSONException;

import android.content.ContentValues;

import com.icecondor.nest.db.Activity;
import com.icecondor.nest.db.Database;

public class Config extends Activity  {
        private static final String VERB = "config";

        public Config(String key, String value) {
            super(VERB);
            try {
                json.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public ContentValues getAttributes() {
            ContentValues cv = super.getAttributes();
            cv.put(Database.ACTIVITIES_VERB, VERB);
            cv.put(Database.ACTIVITIES_DESCRIPTION, "");
            cv.put(Database.ACTIVITIES_JSON, json.toString());
            return cv;
        }

    }
