package com.aslam.p2p.services;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

import java.util.List;

public interface P2PControllerListener {

    void onDiscoverChanged(int state);

    void onP2PStateChanged(int state);

    void onPeersChanged(List<WifiP2pDevice> peers);

    void onDeviceChanged(WifiP2pDevice device);

    void onConnectionInfoAvailable(WifiP2pInfo info);

    void onDeviceNameChanged(int state);

    void onGroupCreated(int state);

    void onGroupRemoved(int state);

    void onDeviceConnected(WifiP2pDevice device);

    void onDeviceNotConnected(int reason);

    void onDeviceDisconnected(WifiP2pDevice device);

    void onDeviceNotDisconnected(int reason);

    void onSocketServerStarted();

    void onSocketServerNewConnection(String host);

    void onSocketServerConnectionClosed(String host, int code, String reason, boolean remote);

    void onSocketServerMessage(String host, String message);

    void onSocketClientOpened(String host);

    void onSocketClientClosed(String host, int code, String reason, boolean remote);

    void onSocketClientMessage(String host, String message);

    void onConsoleLog(String message);

}
