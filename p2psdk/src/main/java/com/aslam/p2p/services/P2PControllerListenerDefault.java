package com.aslam.p2p.services;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

import java.util.List;

class P2PControllerListenerDefault implements P2PControllerListener {


    @Override
    public void onDiscoverChanged(int state) {

    }

    @Override
    public void onP2PStateChanged(int state) {

    }

    @Override
    public void onPeersChanged(List<WifiP2pDevice> peers) {

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

    @Override
    public void onSocketClientOpened(String host) {

    }

    @Override
    public void onSocketClientClosed(String host, int code, String reason, boolean remote) {

    }

    @Override
    public void onSocketClientMessage(String host, String message) {

    }

    @Override
    public void onConsoleLog(String message) {

    }
}
