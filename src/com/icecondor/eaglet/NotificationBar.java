package com.icecondor.eaglet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.icecondor.eaglet.ui.alist.Main;

public class NotificationBar {
    private NotificationManager notificationManager;
    private Notification ongoingNotification;
    private PendingIntent contentIntent;
    private Context ctx;
    private String last_msg;

    public NotificationBar(Context ctx) {
        this.ctx = ctx;
        notificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        contentIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, Main.class),
                                          Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public void updateText(String msg) {
        if(ongoingNotification == null) {
            ongoingNotification = buildNotification();
        }
        // preserve the last message to rebuild the notification with a different icon later
        last_msg = msg;
        ongoingNotification.setLatestEventInfo(ctx, "IceCondor", msg, contentIntent);
        ongoingNotification.when = System.currentTimeMillis();
        notificationManager.notify(1, ongoingNotification);
    }

    public void cancel() {
        notificationManager.cancel(1);
    }

    private Notification buildNotification() {
        int icon = R.drawable.ic_launcher;
        Notification notification = new Notification(icon, null, System.currentTimeMillis());
        notification.flags = notification.flags ^ Notification.FLAG_ONGOING_EVENT;
        notification.setLatestEventInfo(ctx, "IceCondor", "", contentIntent);
        return notification;
    }
}
