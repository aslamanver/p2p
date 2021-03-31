### Wi-Fi Direct (peer-to-peer or P2P) SDK

![https://i.imgur.com/DHFGWtF.png](https://i.imgur.com/DHFGWtF.png)

[ ![Download](https://api.bintray.com/packages/aslam/android/p2p/images/download.svg?version=1.0.5) ](https://bintray.com/aslam/android/p2p/1.0.5/link) [![](https://jitpack.io/v/aslamanver/p2p.svg)](https://jitpack.io/#aslamanver/p2p) [![Build Status](https://travis-ci.org/aslamanver/p2p.svg?branch=master)](https://travis-ci.org/aslamanver/p2p)

Wi-Fi Direct (P2P) allows Android 4.0 (API level 14) and higher devices with the appropriate hardware to connect directly to each other via Wi-Fi without an intermediate access point.

### Initialization

Add the below dependency into your module level `build.gradle` file.

```gradle
implementation 'com.aslam:p2p:1.0.5'
```

#### 1. Service Usage

Create an Android normal service that should be extended from `P2PService`, this is an abstract class that let you implement the below methods in `MyP2PService` class.

```java
public class MyP2PService extends P2PService {

    @Override
    public void onPeersChanged(List<WifiP2pDevice> peers) {

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
```

Declare the service in the `AndroidManifest.xml`.

```xml
<service
    android:name=".MyP2PService"
    android:icon="@drawable/ic_stat_settings_remote"
    android:label="P2PService" />
```

Start `MyP2PService` on the activity `onStart` method.

```java
MyP2PService.start(this, MyP2PService.class);
```

If the targeted version is above Android 5.1 the permission should be asked on the activity `onResume` method.

```java
PermissionUtils.askPermission(this);
```

That's all now the `MyP2PService` class methods will be called on each P2P event, if any device is discovered nearby the `onPeersChanged` method will be called with the device list.

In order to connect the Wifi P2P device from `onPeersChanged` device list, `connectDevice` method of P2P controller should be called.

```java
getP2PController().connectDevice(peers.get(i));
```

#### 2. Bind Service with Activity

The below explanation can guide you to establish the communication between the `P2PService` and Activity.

Create `P2PControllerActivityListener` activity listener variable and its setter in your service class, execute the same methods on UI thread from activity listener as below

```java
public class MyP2PService extends P2PService {

    private P2PControllerActivityListener activityListener;

    public void setActivityListener(P2PControllerActivityListener activityListener) {
        this.activityListener = activityListener;
    }

    @Override
    public void onPeersChanged(List<WifiP2pDevice> peers) {
        if (activityListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activityListener.onPeersChanged(peers);
                }
            });
        }
    }

    @Override
    public void onSocketClientOpened(String host) {
        if (activityListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activityListener.onSocketClientOpened(host);
                }
            });
        }
    }

    @Override
    public void onSocketClientClosed(String host, int code, String reason, boolean remote) {
        if (activityListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activityListener.onSocketClientClosed(host, code, reason, remote);
                }
            });
        }
    }

    @Override
    public void onSocketClientMessage(String host, String message) {
        if (activityListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activityListener.onSocketClientMessage(host, message);
                }
            });
        }
    }

    @Override
    public void onConsoleLog(String message) {
        LogUtils.consoleLog("MyP2PService", message);
        if (activityListener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    activityListener.onConsoleLog(message);
                }
            });
        }
    }
}
```

Bind the `MyP2PService` service class with your activity to manipulate the UI.

```java
public class MainActivity extends AppCompatActivity {

    MyP2PService p2pService;
    P2PController p2pController;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            p2pService = (MyP2PService) MyP2PService.from(service);
            p2pController = p2pService.getP2PController();

            setTitle(p2pController.getConnectionType().toString());

            if (p2pController.getConnectionType() == P2PController.ConnectionType.CLIENT) {
                getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(p2pController.isWebSocketClientConnected() ? "#008000" : "#FF0000")));
            }

            p2pService.setActivityListener(new P2PControllerActivityListener() {

                @Override
                public void onPeersChanged(List<WifiP2pDevice> peers) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // deviceAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onSocketClientOpened(final String host) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#008000")));
                            getSupportActionBar().setTitle("CLIENT HOST-IP: " + host);
                        }
                    });
                }

                @Override
                public void onSocketClientClosed(String host, int code, String reason, boolean remote) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF0000")));
                            getSupportActionBar().setTitle("CLIENT HOST-IP: NOT CONNECTED");
                        }
                    });
                }

                @Override
                public void onConsoleLog(final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("MainActivity", message);
                        }
                    });
                }
            });

            p2pService.onConsoleLog("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            p2pService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Const.DEBUG_MODE = true;
        MyP2PService.start(this, MyP2PService.class);
        bindService(new Intent(this, MyP2PService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(mConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionUtils.askPermission(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
```

#### 3. Controller Usage (Advanced)

If you are going to handle the background processes manually rather than a service class then implement the `P2PController` from your activity or service.

```java
public class MainActivity extends AppCompatActivity {

    P2PController p2pController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        p2pController = new P2PController(this, new P2PControllerListener() {

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
        });

        p2pController.registerConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        p2pController.destroy();
    }
}
```

#### References

[Bound services overview](https://developer.android.com/guide/components/bound-services)
[Wi-Fi Direct (peer-to-peer or P2P) overview](https://developer.android.com/guide/topics/connectivity/wifip2p)
