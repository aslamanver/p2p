package com.aslam.p2p.services;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.aslam.p2p.models.DataCenter;
import com.aslam.p2p.utils.Const;
import com.aslam.p2p.utils.LogUtils;
import com.aslam.p2p.utils.StorageHelper;
import com.aslam.p2p.websocket.MyWebSocketClient;
import com.aslam.p2p.websocket.MyWebSocketListener;
import com.aslam.p2p.websocket.MyWebSocketServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class P2PController {

    public enum ConnectionType {SERVER, CLIENT}

    private Handler discoverHandler = new Handler();
    private Runnable discoverRunnable = getDiscoverRunnable();

    private Context mContext;
    private P2PControllerListener controllerListener;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;
    private PowerManager powerManager;
    private WifiManager wifiManager;

    private WifiP2pManager p2pManager;
    private WifiP2pManager.Channel p2pChannel;
    public List<WifiP2pDevice> peers = new ArrayList<>();

    private WifiP2pDevice currentP2PDevice;
    private DataCenter dataCenter;
    private boolean isP2PEnabled;

    public MyWebSocketServer webSocketServer;
    public MyWebSocketClient webSocketClient;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            consoleLog("onReceive: " + action);

            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                    consoleLog("Wifi P2P is enabled");
                    isP2PEnabled = true;
                    getControllerListener().onP2PStateChanged(state);
                    startConnection();

                } else {

                    consoleLog("Wi-Fi P2P is not enabled");
                    isP2PEnabled = false;
                    getControllerListener().onP2PStateChanged(state);
                    // if (!wifiManager.isWifiEnabled()) {
                    //     wifiManager.setWifiEnabled(true);
                    // }
                }

            } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {

                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);

                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {

                    consoleLog("Wifi P2P discovery started");
                    getControllerListener().onDiscoverChanged(state);
                    if (getConnectionType() == ConnectionType.CLIENT) {
                        p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                if (info.groupFormed && !isWebSocketClientConnected()) {
                                    startWebSocketClient(info.groupOwnerAddress.getHostAddress());
                                }
                            }
                        });
                    }

                } else {

                    consoleLog("Wifi P2P discovery stopped");
                    getControllerListener().onDiscoverChanged(state);
                    if (getConnectionType() == ConnectionType.CLIENT) {
                        p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                if (info.groupFormed && !isWebSocketClientConnected()) {
                                    startWebSocketClient(info.groupOwnerAddress.getHostAddress());
                                } else if (!info.groupFormed) {
                                    discoverPeers(1);
                                }
                            }
                        });
                    }
                }

            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                WifiP2pDeviceList deviceList = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
                consoleLog("peerDevices: " + deviceList.getDeviceList().size());

                peers.clear();
                peers.addAll(deviceList.getDeviceList());
                getControllerListener().onPeersChanged(peers);

                p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {

                        consoleLog("requestConnectionInfo: requestPeers " + " groupFormed: " + info.groupFormed);

                        for (WifiP2pDevice device : peers) {

                            consoleLog("peerDevice ---> " + device.deviceName + " " + getStatusText(device.status));

                            if (getConnectionType() == ConnectionType.CLIENT) {

                                if (!info.groupFormed
                                        && device.status != WifiP2pDevice.CONNECTED
                                        && dataCenter.connectedDeviceAddress != null
                                        && device.deviceAddress.equals(dataCenter.connectedDeviceAddress)) {

                                    connectDevice(device);

                                } else if (info.groupFormed
                                        && device.status == WifiP2pDevice.CONNECTED
                                        && info.groupOwnerAddress != null) {

                                    consoleLog("peerDevice ---> " + device.deviceName + " already connected");
                                    startWebSocketClient(info.groupOwnerAddress.getHostAddress());
                                }
                            }
                        }
                    }
                });

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

                // if (isWebSocketConnected) return;

                // NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                // WifiP2pInfo wifiP2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);
                //
                // if (networkInfo.isConnected()) {
                //
                //     consoleLog("Connected to p2p network. Requesting network details");
                //
                //     p2pManager.requestConnectionInfo(p2pChannel, info -> {
                //
                //         consoleLog("requestConnectionInfo: CONNECTION_CHANGED onConnectionInfoAvailable groupFormed " + info.groupFormed);
                //         if (info.groupFormed && info.isGroupOwner) {
                //
                //             consoleLog("SERVER " + info.groupOwnerAddress);
                //
                //         } else if (info.groupFormed) {
                //
                //             consoleLog("CLIENT OF " + info.groupOwnerAddress);
                //             startWebSocketClient(info.groupOwnerAddress.getHostAddress());
                //             p2pManager.stopPeerDiscovery(p2pChannel, null);
                //         }
                //
                //     });
                // } else {
                //
                //     consoleLog("Disconnected from p2p network");
                //     if (getConnectionType() == ConnectionType.CLIENT) {
                //         discoverPeers(1000);
                //         stopWebSocketClient();
                //     }
                // }

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                currentP2PDevice = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                getControllerListener().onDeviceChanged(currentP2PDevice);
            }
        }
    };

    public P2PController(Context context, P2PControllerListener listener) {
        mContext = context;
        controllerListener = listener;
        dataCenter = StorageHelper.getDataCenter(mContext);
        powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        p2pManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        p2pChannel = p2pManager.initialize(mContext, mContext.getMainLooper(), null);
    }

    public P2PControllerListener getControllerListener() {
        return controllerListener == null ? new P2PControllerListenerDefault() : controllerListener;
    }

    public void registerConnection() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);

        if (getConnectionType() == ConnectionType.SERVER) {
            startWebSocketServer();
        }

        acquireCPU();
    }

    public void acquireCPU() {
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "myApp:MyWifiLock");
        wifiLock.acquire();
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myApp:MyWakeLock");
        wakeLock.acquire();
    }

    public void startConnection() {
        setDeviceName();
        if (getConnectionType() == ConnectionType.SERVER) {
            createGroup();
        }
        discoverPeers(100);
    }

    public void requestConnectionInfo() {
        p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {

                consoleLog("requestConnectionInfo: onConnectionInfoAvailable groupFormed " + info.groupFormed);
                getControllerListener().onConnectionInfoAvailable(info);

                if (info.groupFormed && info.isGroupOwner) {

                    consoleLog("SERVER " + info.groupOwnerAddress);

                } else if (info.groupFormed) {

                    consoleLog("CLIENT OF " + info.groupOwnerAddress);
                }
            }
        });
    }

    public void setDeviceName() {
        try {
            final String deviceName = "P2P-" + getDeviceSerial(mContext);
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = p2pManager.getClass().getMethod("setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);
            Object[] argList = new Object[3];
            argList[0] = p2pChannel;
            argList[1] = deviceName;
            argList[2] = new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    consoleLog("setDeviceName: onSuccess " + deviceName);
                    getControllerListener().onDeviceNameChanged(10);
                }

                @Override
                public void onFailure(int reason) {
                    consoleLog("setDeviceName: onFailure " + deviceName);
                    getControllerListener().onDeviceNameChanged(-10);
                }
            };
            setDeviceName.invoke(p2pManager, argList);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void createGroup() {
        p2pManager.requestGroupInfo(p2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {

                if (group == null) {

                    p2pManager.createGroup(p2pChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            consoleLog("createGroup: onSuccess");
                            getControllerListener().onGroupCreated(10);
                        }

                        @Override
                        public void onFailure(int reason) {
                            consoleLog("createGroup: onFailure");
                            getControllerListener().onGroupCreated(-10);
                        }
                    });

                } else {
                    consoleLog("group is already created " + group.getNetworkName());
                    getControllerListener().onGroupCreated(20);
                }
            }
        });
    }

    public void removeGroup() {
        p2pManager.requestGroupInfo(p2pChannel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {

                if (group != null) {

                    p2pManager.removeGroup(p2pChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            consoleLog("removeGroup: onSuccess");
                            getControllerListener().onGroupRemoved(10);
                        }

                        @Override
                        public void onFailure(int reason) {
                            consoleLog("removeGroup: onFailure");
                            getControllerListener().onGroupRemoved(-10);
                        }
                    });

                } else {
                    consoleLog("group is already removed");
                    getControllerListener().onGroupRemoved(20);
                }
            }
        });
    }

    private Runnable getDiscoverRunnable() {

        return new Runnable() {

            @Override
            public void run() {

                peers.clear();
                getControllerListener().onPeersChanged(peers);

                if (isP2PEnabled) {

                    p2pManager.discoverPeers(p2pChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            consoleLog("discoverPeers: onSuccess");
                            getControllerListener().onDiscoverChanged(10);
                        }

                        @Override
                        public void onFailure(int reason) {
                            consoleLog("discoverPeers: onFailure");
                            getControllerListener().onDiscoverChanged(-10);
                            discoverPeers(1000 * 10);
                        }
                    });
                }
            }
        };
    }

    public void discoverPeers(int delay) {
        consoleLog("discoverPeers: delay: " + delay);
        discoverHandler.removeCallbacks(discoverRunnable);
        discoverHandler.postDelayed(discoverRunnable, delay);
    }

    public void connectDevice(final WifiP2pDevice device) {

        if (device.status == WifiP2pDevice.INVITED || device.status == WifiP2pDevice.CONNECTED) {

            consoleLog("connectDevice: already invited | connected " + device.status + " : " + device.deviceName);
            getControllerListener().onDeviceConnected(device);

        } else {

            WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
            wifiP2pConfig.deviceAddress = device.deviceAddress;

            p2pManager.connect(p2pChannel, wifiP2pConfig, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    consoleLog("connectDevice: onSuccess " + device.deviceName);
                    dataCenter.connectedDeviceAddress = device.deviceAddress;
                    StorageHelper.storeDataCenter(mContext, dataCenter);
                    getControllerListener().onDeviceConnected(device);
                }

                @Override
                public void onFailure(int reason) {
                    consoleLog("connectDevice: onFailure " + device.deviceName);
                    getControllerListener().onDeviceNotConnected(reason);
                }
            });
        }
    }

    public void disconnectDevice(final WifiP2pDevice device) {

        if (device.status == WifiP2pDevice.INVITED || device.status == WifiP2pDevice.CONNECTED) {

            dataCenter.connectedDeviceAddress = null;
            StorageHelper.storeDataCenter(mContext, dataCenter);

            p2pManager.cancelConnect(p2pChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    consoleLog("cancelConnect: onSuccess " + device.deviceName);
                    removeGroup();
                    discoverPeers(1);
                    getControllerListener().onDeviceDisconnected(device);
                }

                @Override
                public void onFailure(int reason) {
                    consoleLog("cancelConnect: onFailure " + device.deviceName);
                    removeGroup();
                    discoverPeers(1);
                    if (reason == 2) {
                        getControllerListener().onDeviceDisconnected(device);
                    } else {
                        getControllerListener().onDeviceNotDisconnected(reason);
                    }
                }
            });

        } else {
            consoleLog("cancelConnect: already disconnected " + device.status + " : " + device.deviceName);
            getControllerListener().onDeviceDisconnected(device);
        }
    }

    public void deviceClickEvent(WifiP2pDevice device) {
        if (device.isGroupOwner()) {
            if (device.status == WifiP2pDevice.CONNECTED) {
                disconnectDevice(device);
            } else {
                connectDevice(device);
            }
        } else {
            showToast("It's not an owner");
        }
    }

    public void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static String getStatusText(int statusCode) {
        String status = "UNKNOWN";
        switch (statusCode) {
            case WifiP2pDevice.AVAILABLE:
                status = "AVAILABLE";
                break;
            case WifiP2pDevice.CONNECTED:
                status = "CONNECTED";
                break;
            case WifiP2pDevice.FAILED:
                status = "FAILED";
                break;
            case WifiP2pDevice.INVITED:
                status = "INVITED";
                break;
            case WifiP2pDevice.UNAVAILABLE:
                status = "UNAVAILABLE";
                break;
        }
        return status;
    }

    @SuppressLint("NewApi")
    public static String getDeviceSerial(Context context) {
        String serial;
        try {
            serial = Build.SERIAL.equals(Build.UNKNOWN) ? Build.getSerial() : Build.SERIAL;
        } catch (Exception ex) {
            serial = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID).toUpperCase();
        }
        return serial;
    }

    public void startWebSocketServer() {

        stopWebSocketServer();

        webSocketServer = new MyWebSocketServer(Const.P2P_PORT, new MyWebSocketListener.Server() {

            @Override
            public void onOpen(WebSocket conn, ClientHandshake clientHandshake) {
                consoleLog("WebSocket: new connection to " + conn.getRemoteSocketAddress());
                getControllerListener().onSocketServerNewConnection(conn.getRemoteSocketAddress().toString());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                consoleLog("WebSocket: closed " + (conn != null ? conn.getRemoteSocketAddress() : "NULL") + " with exit code " + code + " additional info: " + reason);
                getControllerListener().onSocketServerConnectionClosed(conn.getRemoteSocketAddress().toString(), code, reason, remote);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                consoleLog("WebSocket: received message from " + conn.getRemoteSocketAddress() + " : " + message);
                getControllerListener().onSocketServerMessage(conn.getRemoteSocketAddress().toString(), message);
            }

            @Override
            public void onMessage(WebSocket conn, ByteBuffer message) {
                consoleLog("WebSocket: received ByteBuffer from " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                consoleLog("WebSocket: an error occurred on connection " + (conn != null ? conn.getRemoteSocketAddress() : "NULL") + ":" + ex);
            }

            @Override
            public void onStart() {
                consoleLog("WebSocket: server started successfully");
                getControllerListener().onSocketServerStarted();
            }
        });

        webSocketServer.setReuseAddr(true);
        webSocketServer.start();
    }

    public void stopWebSocketServer() {
        if (webSocketServer != null) {
            try {
                webSocketServer.stop(1000);
                consoleLog("WebSocket Server stopped");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            webSocketServer = null;
        }
    }

    public void startWebSocketClient(final String host) {

        if (webSocketClient != null && webSocketClient.isConnecting()) {
            consoleLog("startWebSocketClient: isConnecting " + host);
            return;
        }

        if (webSocketClient != null && webSocketClient.isOpen() && webSocketClient.host.equals(host)) {
            consoleLog("startWebSocketClient: isOpen " + host);
            return;
        }

        consoleLog("startWebSocketClient: " + host);

        stopWebSocketClient();

        try {

            URI uri = new URI("ws://" + host + ":" + Const.P2P_PORT);

            webSocketClient = new MyWebSocketClient(host, uri, new MyWebSocketListener.Client() {

                @Override
                public void onOpen(ServerHandshake handshakeData) {
                    if (webSocketClient.isOpen()) {
                        consoleLog("WebSocket: new connection opened");
                        getControllerListener().onSocketClientOpened(webSocketClient.getRemoteSocketAddress().toString());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!webSocketClient.isOpen()) {
                        consoleLog("WebSocket: closed with exit code " + code + " additional info: " + reason);
                        getControllerListener().onSocketClientClosed(host, code, reason, remote);
                        p2pManager.requestConnectionInfo(p2pChannel, new WifiP2pManager.ConnectionInfoListener() {
                            @Override
                            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                                if (!info.groupFormed) {
                                    discoverPeers(1);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onMessage(String message) {
                    consoleLog("WebSocket: received message from " + webSocketClient.getRemoteSocketAddress() + " : " + message);
                    getControllerListener().onSocketClientMessage(webSocketClient.getRemoteSocketAddress().toString(), message);
                }

                @Override
                public void onMessage(ByteBuffer message) {
                    consoleLog("WebSocket: received ByteBuffer");
                }

                @Override
                public void onError(Exception ex) {
                    consoleLog("WebSocket: an error occurred:" + ex);
                }
            });

            webSocketClient.setReuseAddr(true);
            webSocketClient.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void stopWebSocketClient() {
        if (webSocketClient != null) {
            webSocketClient.forceClose();
            webSocketClient = null;
            consoleLog("WebSocket Client stopped");
        }
    }

    public ConnectionType getConnectionType() {
        return getConnectionType(mContext);
    }

    public static ConnectionType getConnectionType(Context context) {
        return Const.CONNECTION_TYPE == null ? Build.MODEL.contains("WPOS") ? ConnectionType.SERVER : ConnectionType.CLIENT : Const.CONNECTION_TYPE;
    }

    public void destroy() {
        mContext.unregisterReceiver(mBroadcastReceiver);
        releaseCPU();
    }

    public void releaseCPU() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
    }

    public boolean isWebSocketClientConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public String getWebSocketClientHost() {
        return isWebSocketClientConnected() ? webSocketClient.host : null;
    }

    public boolean isWebSocketServerConnected() {
        return webSocketServer != null;
    }

    public void send(ConnectionType connectionType, String message) {
        if (connectionType == ConnectionType.CLIENT && isWebSocketClientConnected()) {
            webSocketClient.send(message);
        } else if (connectionType == ConnectionType.SERVER && webSocketServer != null) {
            webSocketServer.send(message);
        }
    }

    public void consoleLog(String message) {
        // LogUtils.consoleLog("P2PController", message);
        getControllerListener().onConsoleLog(message);
    }
}