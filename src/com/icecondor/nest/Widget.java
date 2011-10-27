package com.icecondor.nest;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.util.Log;

public class Widget extends AppWidgetProvider {
	static final String appTag = "Widget";
	
	@Override
	public void onUpdate(Context ctxt,
	AppWidgetManager mgr,
	int[] appWidgetIds) {
		Log.i(appTag, "onUpdate");
	}
}
