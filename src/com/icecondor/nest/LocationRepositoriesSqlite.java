package com.icecondor.nest;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.httpclient4.HttpClient4;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

public class LocationRepositoriesSqlite extends SQLiteOpenHelper implements Constants {
	public static final String LOCATION_REPOSITORIES_TABLE = "repositories";
	public static final String ID = "_id";

	public LocationRepositoriesSqlite(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE repositories (_id integer primary key, type text, name text, url text, request_url text, authorization_url text, access_url text, access_token text)");
		// insert default location provider
		ContentValues values = new ContentValues();
		values.put("type", "icecondor");
		values.put("name", "IceCondor.com");
		values.put("url", ICECONDOR_URL);
		values.put("request_url", ICECONDOR_OAUTH_REQUEST_URL);
		values.put("authorization_url", ICECONDOR_OAUTH_AUTHORIZATION_URL);
		values.put("access_url", ICECONDOR_OAUTH_ACCESS_URL);
		db.insert(LOCATION_REPOSITORIES_TABLE, "_id", values);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	public static int repositoryCount(Context ctx) {
		LocationRepositoriesSqlite locRepoDb = new LocationRepositoriesSqlite(ctx, "locationrepositories", null, 1);
		SQLiteDatabase repoDb = locRepoDb.getReadableDatabase();
		Cursor repos = repoDb.query(LocationRepositoriesSqlite.LOCATION_REPOSITORIES_TABLE, null, null, null, null, null, null);
		return repos.getCount();
	}
	
	public static OAuthServiceProvider defaultProvider(Context ctx) {
		LocationRepositoriesSqlite locRepoDb = new LocationRepositoriesSqlite(ctx, "locationrepositories", null, 1);
		SQLiteDatabase repoDb = locRepoDb.getReadableDatabase();
		Cursor repos = repoDb.query(LocationRepositoriesSqlite.LOCATION_REPOSITORIES_TABLE, null, null, null, null, null, "_id asc");
		repos.moveToFirst();
		OAuthServiceProvider provider =  new OAuthServiceProvider(repos.getString(repos.getColumnIndex("request_url")),
				                        repos.getString(repos.getColumnIndex("authorization_url")),
				                        repos.getString(repos.getColumnIndex("access_url")));
		repos.close();
		repoDb.close();
		return provider;                
	}
	
	public static OAuthAccessor defaultClient(Context ctx) {
		String consumerKey = "icecondor-nest-"+ICECONDOR_VERSION;
		String consumerSecret = "";
		OAuthServiceProvider provider =  defaultProvider(ctx);
		Log.i("OAUTH", provider.requestTokenURL);
		OAuthConsumer consumer = new OAuthConsumer(ICECONDOR_OAUTH_CALLBACK, consumerKey,
                                                   consumerSecret, provider);
		OAuthAccessor accessor = new OAuthAccessor(consumer);
		OAuthClient client = new OAuthClient(new HttpClient4());
		try {
			client.getRequestToken(accessor);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return accessor;
	}

	public static void setDefaultAccessToken(String access_token, Context ctx) {
		Log.i("OAUTH", "access token = "+access_token);
		LocationRepositoriesSqlite locRepoDb = new LocationRepositoriesSqlite(ctx, "locationrepositories", null, 1);
		SQLiteDatabase repoDb = locRepoDb.getReadableDatabase();
		ContentValues values = new ContentValues();
		values.put("access_token", access_token);
		repoDb.update(LOCATION_REPOSITORIES_TABLE, values, null, null);
		repoDb.close();

	}
}
