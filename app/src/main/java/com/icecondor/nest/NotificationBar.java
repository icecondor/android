package com.icecondor.nest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.icecondor.hawk.R;
import com.icecondor.nest.ui.alist.Main;

public class NotificationBar {
    private NotificationManager notificationManager;
    private Notification ongoingNotification;
    private PendingIntent contentIntent;
    private Context ctx;

    public NotificationBar(Context ctx) {
        this.ctx = ctx;
        notificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        contentIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, Main.class),
                 PendingIntent.FLAG_IMMUTABLE );
    }

    public void updateText(String msg) {
        updateText(msg, System.currentTimeMillis());
    }

    public void updateText(String msg, long when) {
        ongoingNotification = buildNotification(msg, when);
        notificationManager.notify(1, ongoingNotification);
    }

    public void cancel() {
        notificationManager.cancel(1);
    }

    private Notification buildNotification(String msg, long when) {
        int icon = R.drawable.ic_notification;
        //Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), icon);
        return (new NotificationCompat.Builder(ctx))
               .setSmallIcon(icon)
               //.setLargeIcon(bitmap)
               .setOngoing(true)
               .setContentIntent(contentIntent)
               .setContentTitle("IceCondor")
               .setContentText(msg)
               .setPriority(NotificationCompat.PRIORITY_LOW)
               .setWhen(when)
               .build();
    }

}
