package com.aslam.p2p.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public abstract class BaseForegroundService extends Service {

    // protected int REQUEST_CODE, NOTIFICATION_ID = 3000;
    // protected String CHANNEL_ID = "ForegroundService_ID";
    // protected String CHANNEL_NAME = "ForegroundService Channel";

    private ServiceBuilder serviceBuilder;

    protected abstract ServiceBuilder serviceBuilder();

    protected class ServiceBuilder {

        private int requestCode;
        private String channelId;
        private String channelName;
        private Notification notification;

        public ServiceBuilder(int requestCode, String channelId, String channelName) {
            this.requestCode = requestCode;
            this.channelId = channelId;
            this.channelName = channelName;
        }

        public ServiceBuilder build(Notification notification) {
            this.notification = notification;
            return this;
        }
    }

    protected final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public com.aslam.p2p.services.BaseForegroundService getService() {
            return com.aslam.p2p.services.BaseForegroundService.this;
        }
    }

    protected NotificationManager mNotificationManager;
    protected NotificationCompat.Builder mNotificationBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        serviceBuilder = serviceBuilder();
        startForeground(serviceBuilder.requestCode, serviceBuilder.notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected Notification createNotification(ServiceBuilder serviceBuilder, String title, String message, int smallIcon, int bigIcon, Class<?> intentClass) {

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (mNotificationBuilder == null) {
            mNotificationBuilder = new NotificationCompat.Builder(this, serviceBuilder.channelId);
        }

        mNotificationBuilder.setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true);

        if (smallIcon != 0) {
            mNotificationBuilder.setSmallIcon(smallIcon);
        }

        if (bigIcon != 0) {
            mNotificationBuilder.setLargeIcon(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), bigIcon), 128, 128, true));
        }

        if (intentClass != null) {
            Intent notificationIntent = new Intent(this, intentClass);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, serviceBuilder.requestCode, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(pendingIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(serviceBuilder.channelId, serviceBuilder.channelName, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
        }

        return mNotificationBuilder.build();
    }

    public static void start(Context context, Class<? extends BaseForegroundService> serviceClass) {
        ContextCompat.startForegroundService(context, new Intent(context, serviceClass));
    }

    public static void stop(Context context, Class<? extends BaseForegroundService> serviceClass) {
        context.stopService(new Intent(context, serviceClass));
    }

    public static BaseForegroundService from(IBinder service) {
        BaseForegroundService.LocalBinder binder = (BaseForegroundService.LocalBinder) service;
        return binder.getService();
    }
}