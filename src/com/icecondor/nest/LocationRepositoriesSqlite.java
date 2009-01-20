package com.icecondor.nest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;

public class LocationRepositoriesSqlite extends SQLiteOpenHelper {

	public static final String NAME = "name";
	public static final String URL = "url";
	public static final String SERVICES_TABLE = "services";
	public static final String SHOUTS_TABLE = "shouts";
	public static final String ID = "_id";

	public LocationRepositoriesSqlite(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE locationrepositories (_id integer primary key, url text, access_token text)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}


}
