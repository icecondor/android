package com.icecondor.nest;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {
	static final String appTag = "Widget";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
		Log.i(appTag, "onUpdate");
		super.onUpdate(context, mgr, appWidgetIds);
		mgr.updateAppWidget(appWidgetIds, buildView(context));
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
		Log.i(appTag, intent.getAction());
		super.onReceive(context, intent);
		if ("PowerToggle".equals(intent.getAction())) {
			ComponentName me=new ComponentName(context,Widget.class);
			AppWidgetManager mgr=AppWidgetManager.getInstance(context);
			RemoteViews views=buildView(context);
			views.setImageViewResource(R.id.widget_bird_button, R.drawable.widget_bird_on);
			mgr.updateAppWidget(me, views);
		}
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
