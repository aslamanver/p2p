package com.aslam.p2pdemo.services;

import android.net.wifi.p2p.WifiP2pDevice;

import com.aslam.p2p.services.P2PControllerActivityListener;
import com.aslam.p2p.services.P2PService;
import com.aslam.p2p.utils.Const;
import com.aslam.p2p.utils.LogUtils;

import java.util.List;

public class MyP2PService extends P2PService {

    // P2PControllerActivityListener activityListener;

    // public void setActivityListener(P2PControllerActivityListener activityListener) {
    //     this.activityListener = activityListener;
    // }
    //
    // @Override
    // public void onPeersChanged(List<WifiP2pDevice> peers) {
    //     if (activityListener != null) {
    //         activityListener.onPeersChanged(peers);
    //     }
    // }
    //
    // @Override
    // public void onSocketClientOpened(String host) {
    //     if (activityListener != null) {
    //         activityListener.onSocketClientOpened(host);
    //     }
    // }
    //
    // @Override
    // public void onSocketClientClosed(String host, int code, String reason, boolean remote) {
    //     if (activityListener != null) {
    //         activityListener.onSocketClientClosed(host, code, reason, remote);
    //     }
    // }
    //
    // @Override
    // public void onSocketClientMessage(String host, String message) {
    //     if (activityListener != null) {
    //         activityListener.onSocketClientMessage(host, message);
    //     }
    // }

    @Override
    public void onConsoleLog(String message) {
        super.onConsoleLog(message);
        LogUtils.consoleLog("MyP2PService", message);
        // if (activityListener != null) {
        //     activityListener.onConsoleLog(message);
        // }
        if (Const.DEBUG_MODE) {
            LogUtils.buzz(100);
        }
    }
}
