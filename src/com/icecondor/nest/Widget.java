package com.icecondor.nest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider implements Constants {
	static boolean on;
	
	@Override
	public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
		Log.i(APP_TAG, "Widget onUpdate");
		super.onUpdate(context, mgr, appWidgetIds);
		mgr.updateAppWidget(appWidgetIds, buildView(context));
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
		Log.i(APP_TAG, "Widget onReceive "+intent.getAction()+" \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
		super.onReceive(context, intent);
		if ("PowerToggle".equals(intent.getAction())) {
			Log.i(APP_TAG, "Widget PowerToggle from "+on);
			Intent pigeon_intent;
			if (on) {
				pigeon_intent = new Intent("com.icecondor.nest.PIGEON_OFF");
			} else {
				context.startService(new Intent(context, Pigeon.class));
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				settings.edit().putBoolean(SETTING_PIGEON_TRANSMITTING,true).commit();
				pigeon_intent = new Intent("com.icecondor.nest.PIGEON_ON");
			}
			Log.i(APP_TAG, "Widget PowerToggle broadcasting "+pigeon_intent);
			context.sendBroadcast(pigeon_intent);
		}
		if ("com.icecondor.nest.WIDGET_ON".equals(intent.getAction())) {
			on = true;
			ComponentName me=new ComponentName(context,Widget.class);
			AppWidgetManager mgr=AppWidgetManager.getInstance(context);
			RemoteViews views=buildView(context);
			views.setImageViewResource(R.id.widget_bird_button, R.drawable.widget_bird_on);
			mgr.updateAppWidget(me, views);
		}
		if ("com.icecondor.nest.WIDGET_OFF".equals(intent.getAction())) {
			on = false;
			ComponentName me=new ComponentName(context,Widget.class);
			AppWidgetManager mgr=AppWidgetManager.getInstance(context);
			RemoteViews views=buildView(context);
			views.setImageViewResource(R.id.widget_bird_button, R.drawable.widget_bird_off);
			mgr.updateAppWidget(me, views);
		}
	}
	
	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		context.sendBroadcast(new Intent("com.icecondor.nest.PIGEON_INQUIRE"));
		Log.i(APP_TAG, "Widget onEnabled \""+Thread.currentThread().getName()+"\""+" #"+Thread.currentThread().getId());
	}
	
	RemoteViews buildView(Context context) {
		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget);
		Intent i = new Intent(context, Widget.class);
		i.setAction("PowerToggle");
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		updateViews.setOnClickPendingIntent(R.id.widget_bird_button, pi);
		return updateViews;
	}
}
