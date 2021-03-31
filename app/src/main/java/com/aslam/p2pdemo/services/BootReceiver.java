package com.aslam.p2pdemo.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MyP2PService.start(context, MyP2PService.class);
    }
}
