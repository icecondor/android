package com.icecondor.nest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class GeoRssSqlite extends SQLiteOpenHelper {
	public static final String NAME = "name";
	public static final String URL = "url";
	public static final String SERVICES_TABLE = "services";

	public GeoRssSqlite(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table services (_id integer primary key, name text, url text)");
		db.execSQL("create table shouts (_id integer primary key, guid text, name text, lat float, long float)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
