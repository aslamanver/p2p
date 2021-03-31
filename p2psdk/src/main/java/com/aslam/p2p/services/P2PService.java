package com.aslam.p2p.services;

import android.app.Notification;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

import com.aslam.p2p.R;

public abstract class P2PService extends BaseForegroundService implements P2PControllerListener {

    private P2PController p2pController;

    @Override
    public void onCreate() {
        super.onCreate();
        p2pController = new P2PController(this, this);
        p2pController.registerConnection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        p2pController.destroy();
    }

    @Override
    protected ServiceBuilder serviceBuilder() {
        ServiceBuilder serviceBuilder = new ServiceBuilder(3005, "P2PService_Service_ID", "P2PService Service Channel");
        Notification notification = createNotification(serviceBuilder, "P2PService", "P2PService", R.drawable.ic_stat_settings_remote, R.drawable.ic_launcher, null);
        return serviceBuilder.build(notification);
    }

    public P2PController getP2PController() {
        return p2pController;
    }

    @Override
    public void onDiscoverChanged(int state) {

    }

    @Override
    public void onP2PStateChanged(int state) {

    }

    @Override
    public void onDeviceChanged(WifiP2pDevice device) {

    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }

    @Override
    public void onDeviceNameChanged(int state) {

    }

    @Override
    public void onGroupCreated(int state) {

    }

    @Override
    public void onGroupRemoved(int state) {

    }

    @Override
    public void onDeviceConnected(WifiP2pDevice device) {

    }

    @Override
    public void onDeviceNotConnected(int reason) {

    }

    @Override
    public void onDeviceDisconnected(WifiP2pDevice device) {

    }

    @Override
    public void onDeviceNotDisconnected(int reason) {

    }

    @Override
    public void onSocketServerStarted() {

    }

    @Override
    public void onSocketServerNewConnection(String host) {

    }

    @Override
    public void onSocketServerConnectionClosed(String host, int code, String reason, boolean remote) {

    }

    @Override
    public void onSocketServerMessage(String host, String message) {

    }
}
