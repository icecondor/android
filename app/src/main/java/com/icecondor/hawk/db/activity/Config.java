package com.icecondor.hawk.db.activity;

import org.json.JSONException;

import android.content.ContentValues;

import com.icecondor.hawk.db.Activity;
import com.icecondor.hawk.db.Database;

public class Config extends Activity  {
        private static final String VERB = "config";
        private String desc = "";

        public Config(String key, String value) {
            super(VERB);
            try {
                json.put(key, value);
                desc = key+" "+value;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public ContentValues getAttributes() {
            ContentValues cv = super.getAttributes();
            cv.put(Database.ACTIVITIES_VERB, VERB);
            cv.put(Database.ACTIVITIES_DESCRIPTION, desc);
            cv.put(Database.ACTIVITIES_JSON, json.toString());
            return cv;
        }

    }
