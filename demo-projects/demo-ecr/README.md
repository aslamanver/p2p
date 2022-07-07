### PAYable P2P (Wifi-Direct) ECR SDKs - ECR Integration

![](https://i.imgur.com/P8L2Oc7.png)

PAYable P2P (Wifi-Direct) - [SDK](https://aslamanver.github.io/p2p/demo-projects/demo-ecr)

<hr>

### Integration 

Make sure the ECR payment service is running on the terminal as below in the notification bar. 

![](https://i.imgur.com/agTUUmw.png)

The connection between the terminal and host system will be established using WebSocket which is running inside the ECR application. 

Refer to the [ECR Documentation](https://payable.github.io/ecr-sdks/) to know more about PAYable ECR implementations.

### 1. Initialization

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
configurations {
    implementation {
        exclude group: "com.google.code.gson"
        exclude group: "org.java-websocket"
    }
}

dependencies {
    implementation "org.java-websocket:Java-WebSocket:1.5.1"
    implementation 'com.google.code.gson:gson:2.2.4'
    implementation('com.github.aslamanver:p2p:1.1.9')
}
```

1.4 Follow the [Wi-Fi Direct (peer-to-peer - P2P)](https://aslamanver.github.io/p2p#1-service-usage) documentation to connect the terminal through WIFI-P2P Network. (Ignore the Initialization section from P2P Docs)

1.5 Once it is connected through WIFI-P2P Network `onSocketClientOpened` method will be called along with the connected host IP address, now from there you can initiate the ECRTerminal connection to the host IP address.

1.6 Implement the [Java ECR SDK Integration](https://ecr-git-demo.payable.lk/#java-sdk-integration) to connect the ECR Terminal.

### 2. Demonstration

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
        MyP2PService.start(this, MyP2PService.class);
        MyP2PService.bindService(this, MyP2PService.class, mConnection);
    }

    @Override
    public void onStop() {
        super.onStop();
        disconnectECR();
        p2pService.setActivityListener(null);
        unbindService(mConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        Const.DEBUG_MODE = true;

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
                if (ecrTerminal != null && ecrTerminal.isOpen()) {
                    ecrTerminal.send(request.toJson());
                    p2pService.onConsoleLog("Sent to ECRTerminal");
                } else {
                    Toast.makeText(getApplicationContext(), "ECRTerminal is not connected", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void connectECR(String host) {

        try {

            disconnectECR();

            ecrTerminal = new ECRTerminal(host, "4DqxynHGtHNckmCrRzvVxkwuSfr8faRmPrLIX0hmkqw=", "ANDROID-" + P2PController.getDeviceSerial(this), new ECRTerminal.Listener() {

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

<hr/>

[![Screenshot](https://github.com/aslamanver/p2p/blob/master/demo-projects/demo-ecr/screenshots/1.gif?raw=true)](#demonstration)

Sample project and APK.

* [Demo Project](https://github.com/aslamanver/p2p/tree/master/demo-projects/demo-ecr)
* [P2P Demo App](https://github.com/aslamanver/p2p/blob/master/apks/ecr.apk)

Refer the below documentation for advanced usages.

* [PAYable ECR SDKs](https://ecr-git-demo.payable.lk)
* [Wi-Fi Direct (peer-to-peer - P2P)](https://aslamanver.github.io/p2p)