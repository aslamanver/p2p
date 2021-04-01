### PAYable P2P (Wifi-Direct) ECR SDKs - ECR Integration

![](https://i.imgur.com/P8L2Oc7.png)

PAYable P2P (Wifi-Direct) - [SDK](https://aslamanver.github.io/p2p/demo-projects/demo-ecr)

<hr>

### Integration 

Make sure the ECR payment service is running on the terminal as below in the notification bar. 

![](https://i.imgur.com/agTUUmw.png)

The connection between the terminal and host system will be established using WebSocket which is running inside the ECR application. 

The server is implemented based on these WebSocket protocol versions

* [RFC 6455](https://tools.ietf.org/html/rfc6455) 
* [RFC 7692](https://tools.ietf.org/html/rfc7692)

Refer to the Mozilla [WebSocket APIs](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API) to write your WebSocket client or use any libraries available based on the above protocol versions.

#### 1. Initialization

1.1 Include the JAR file into your Android project as per the [Java ECR SDK Integration](https://ecr-git-demo.payable.lk/#java-sdk-integration) docs.

1.2 Add the below repository into your project level `build.gradle` file.

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

1.3 Add the below dependency into your app level `build.gradle` file.

```gradle
implementation('lk.payable:p2p:1.1.3') {
    exclude group: "com.google.code.gson"
    exclude group: "org.java-websocket"
}
```

1.4 Follow the [Wi-Fi Direct (peer-to-peer - P2P)](https://aslamanver.github.io/p2p) documentation to connect the terminal through WIFI-P2P Network.

1.5 Once it is connected through WIFI-P2P Network `onSocketClientOpened` method will be called along with the connected host IP address, now from there you can initiate the ECRTerminal connection to the host IP address.

1.6 Implement the [Java ECR SDK Integration](https://ecr-git-demo.payable.lk/#java-sdk-integration) to connect the ECR Terminal.

#### 2. Demonstration

```java
public class MainActivity extends AppCompatActivity {

    ECRTerminal ecrTerminal;
    ActivityMainBinding binding;
    boolean permissionGranted;
    MyDeviceAdapter deviceAdapter;

    MyP2PService p2pService;
    P2PController p2pController;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {

            p2pService = (MyP2PService) MyP2PService.from(service);
            p2pController = p2pService.getP2PController();

            setTitle(p2pController.getConnectionType().toString());

            if (p2pController.getConnectionType() == P2PController.ConnectionType.CLIENT) {
                if (p2pController.isWebSocketClientConnected()) {
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#008000")));
                    String address = p2pController.getWebSocketClientHost().split(":")[0].replace("/", "");
                    connectECR(address);
                } else {
                    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF0000")));
                }
            }

            p2pService.setActivityListener(new P2PControllerActivityListener() {

                @Override
                public void onPeersChanged(List<WifiP2pDevice> peers) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deviceAdapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onSocketClientOpened(final String host) {
                    String address = host.split(":")[0].replace("/", "");
                    connectECR(address);
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
                    disconnectECR();
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
                            consoleLog(message);
                        }
                    });
                }
            });

            deviceAdapter = new MyDeviceAdapter(getApplicationContext(), p2pController.peers, new P2PDeviceAdapter.EventListener() {
                @Override
                public void onClickEvent(int position, WifiP2pDevice device) {
                    p2pController.deviceClickEvent(device);
                }
            });

            binding.listView.setAdapter(deviceAdapter);
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
        p2pService.setActivityListener(null);
        unbindService(mConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (P2PController.getConnectionType(this) == P2PController.ConnectionType.CLIENT) {
            binding.btnCreateGroup.setVisibility(View.GONE);
            binding.btnRemoveGroup.setVisibility(View.GONE);
        }

        binding.btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p2pController.discoverPeers(1);
            }
        });

        binding.btnConnectionInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p2pController.requestConnectionInfo();
            }
        });

        binding.btnCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p2pController.createGroup();
            }
        });

        binding.btnRemoveGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                p2pController.removeGroup();
            }
        });

        binding.btnSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double amount = new Random().nextInt(1000);
                PAYableRequest request = new PAYableRequest(PAYableRequest.ENDPOINT_PAYMENT, new Random().nextInt(100), amount, PAYableRequest.METHOD_CARD);
                ecrTerminal.send(request.toJson());
            }
        });
    }

    private void connectECR(String host) {

        try {

            disconnectECR();

            ecrTerminal = new ECRTerminal(host, "4DqxynHGtHNckmCrRzvVxkwuSfr8faRmPrLIX0hmkqw=", "ANDROID-POS", new ECRTerminal.Listener() {

                @Override
                public void onOpen(String data) {
                    p2pService.onConsoleLog("ECRTerminal onOpen: " + data);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    p2pService.onConsoleLog("ECRTerminal onClose: " + reason);
                }

                @Override
                public void onMessage(String message) {
                    p2pService.onConsoleLog("ECRTerminal onMessage: " + message);
                }

                @Override
                public void onMessage(ByteBuffer message) {
                    p2pService.onConsoleLog("ECRTerminal onMessage: " + message);
                }

                @Override
                public void onError(Exception ex) {
                    p2pService.onConsoleLog("ECRTerminal onError: " + ex);
                }
            });

            ecrTerminal.setReuseAddr(true);
            ecrTerminal.setConnectionLostTimeout(15);
            ecrTerminal.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void disconnectECR() {
        try {
            if (ecrTerminal != null && ecrTerminal.isOpen()) {
                ecrTerminal.close(1009, "STATUS_DISCONNECTED");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectECR();
    }

    protected void onResume() {
        super.onResume();
        permissionGranted = PermissionUtils.askPermission(this);
    }

    private void consoleLog(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.txtLogs.setText(LogUtils.getLogs());
            }
        });
    }
}
```

[![Screenshot](/screenshots/1.gif)](#demonstration)

<hr/>

Refer the below documentation for advanced usages.

* [PAYable ECR SDKs](https://ecr-git-demo.payable.lk)
* [Wi-Fi Direct (peer-to-peer - P2P)](https://aslamanver.github.io/p2p)