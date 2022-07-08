package com.aslam.p2p.services;

import android.app.Notification;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;

import androidx.annotation.CallSuper;

import com.aslam.p2p.R;
import com.aslam.p2p.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class P2PService extends BaseForegroundService implements P2PControllerListener {

    private P2PController p2pController;
    private final List<P2PControllerListener> controllerListeners = new ArrayList<>();

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        p2pController = new P2PController(this, this);
        p2pController.registerConnection();
    }

    @Override
    @CallSuper
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

    public final P2PController getP2PController() {
        return p2pController;
    }

    public final void addListener(P2PControllerListener controllerListener) {
        controllerListeners.add(controllerListener);
    }

    public final void removeListener(P2PControllerListener controllerListener) {
        if (controllerListener == null) {
            controllerListeners.clear();
        } else {
            controllerListeners.remove(controllerListener);
        }
    }

    @Override
    @CallSuper
    public void onDiscoverChanged(int state) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDiscoverChanged(state);
        }
    }

    @Override
    @CallSuper
    public void onP2PStateChanged(int state) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onP2PStateChanged(state);
        }
    }

    @Override
    @CallSuper
    public void onDeviceChanged(WifiP2pDevice device) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceChanged(device);
        }
    }

    @Override
    @CallSuper
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onConnectionInfoAvailable(info);
        }
    }

    @Override
    @CallSuper
    public void onDeviceNameChanged(int state) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceNameChanged(state);
        }
    }

    @Override
    @CallSuper
    public void onGroupCreated(int state) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onGroupCreated(state);
        }
    }

    @Override
    @CallSuper
    public void onGroupRemoved(int state) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onGroupRemoved(state);
        }
    }

    @Override
    @CallSuper
    public void onDeviceConnected(WifiP2pDevice device) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceConnected(device);
        }
    }

    @Override
    @CallSuper
    public void onDeviceNotConnected(int reason) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceNotConnected(reason);
        }
    }

    @Override
    @CallSuper
    public void onDeviceDisconnected(WifiP2pDevice device) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceDisconnected(device);
        }
    }

    @Override
    @CallSuper
    public void onDeviceNotDisconnected(int reason) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onDeviceNotDisconnected(reason);
        }
    }

    @Override
    @CallSuper
    public void onSocketServerStarted() {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketServerStarted();
        }
    }

    @Override
    @CallSuper
    public void onSocketServerNewConnection(String host) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketServerNewConnection(host);
        }
    }

    @Override
    @CallSuper
    public void onSocketServerConnectionClosed(String host, int code, String reason, boolean remote) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketServerConnectionClosed(host, code, reason, remote);
        }
    }

    @Override
    @CallSuper
    public void onSocketServerMessage(String host, String message) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketServerMessage(host, message);
        }
    }

    //

    @Override
    @CallSuper
    public void onPeersChanged(List<WifiP2pDevice> peers) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onPeersChanged(peers);
        }
    }

    @Override
    @CallSuper
    public void onSocketClientOpened(String host) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketClientOpened(host);
        }
    }

    @Override
    @CallSuper
    public void onSocketClientClosed(String host, int code, String reason, boolean remote) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketClientClosed(host, code, reason, remote);
        }
    }

    @Override
    @CallSuper
    public void onSocketClientMessage(String host, String message) {
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onSocketClientMessage(host, message);
        }
    }

    @Override
    @CallSuper
    public void onConsoleLog(String message) {
        LogUtils.consoleLog("P2PService", message);
        for (P2PControllerListener controllerListener : controllerListeners) {
            controllerListener.onConsoleLog(message);
        }
    }
}
