package com.icecondor.nest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
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
		db.execSQL("CREATE TABLE repositories (_id integer primary key, type text, name text, url text, request_url text, authorization_url text, access_url text, access_token text, access_token_secret text)");
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
	
	public static OAuthAccessor defaultAccessor(Context ctx) {
		String consumerKey = "icecondor-nest-"+ICECONDOR_VERSION;
		String consumerSecret = "";
		OAuthServiceProvider provider =  defaultProvider(ctx);
		Log.i("OAUTH", provider.requestTokenURL);
		OAuthConsumer consumer = new OAuthConsumer(ICECONDOR_OAUTH_CALLBACK, consumerKey,
                                                   consumerSecret, provider);
		OAuthAccessor accessor = new OAuthAccessor(consumer);
		return accessor;
	}

	public static void setDefaultAccessToken(String[] access_token_and_secret, Context ctx) {
		Log.i("OAUTH", "access token = "+access_token_and_secret);
		LocationRepositoriesSqlite locRepoDb = new LocationRepositoriesSqlite(ctx, "locationrepositories", null, 1);
		SQLiteDatabase repoDb = locRepoDb.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("access_token", access_token_and_secret[0]);
		values.put("access_token_secret", access_token_and_secret[1]);
		repoDb.update(LOCATION_REPOSITORIES_TABLE, values, null, null);
		repoDb.close();

	}
	public static String[] getDefaultAccessToken(Context ctx) {
		LocationRepositoriesSqlite locRepoDb = new LocationRepositoriesSqlite(ctx, "locationrepositories", null, 1);
		SQLiteDatabase repoDb = locRepoDb.getReadableDatabase();
		Cursor repos = repoDb.query(LocationRepositoriesSqlite.LOCATION_REPOSITORIES_TABLE, null, null, null, null, null, "_id asc");
		repos.moveToFirst();
		String token = repos.getString(repos.getColumnIndex("access_token"));
		String secret = repos.getString(repos.getColumnIndex("access_token_secret"));		
		repoDb.close();
		return new String[] {token, secret};
	}
	
	public static String[] convertToAccessTokenAndSecret(String request_token, Context ctx) {
		ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
		OAuthClient oclient = new OAuthClient(new HttpClient4());
		OAuthAccessor accessor = LocationRepositoriesSqlite.defaultAccessor(ctx);
		params.add(new OAuth.Parameter("oauth_token", request_token));
		try {
			OAuthMessage omessage = oclient.invoke(accessor, "POST",  
					                               accessor.consumer.serviceProvider.accessTokenURL, params);
			return new String[] {omessage.getParameter("oauth_token"), 
					             omessage.getParameter("oauth_token_secret")};
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
