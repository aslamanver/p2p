package com.aslam.p2pdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.aslam.p2p.adapters.P2PDeviceAdapter;
import com.aslam.p2p.services.P2PController;
import com.aslam.p2p.services.P2PControllerActivityListener;
import com.aslam.p2p.utils.Const;
import com.aslam.p2p.utils.LogUtils;
import com.aslam.p2p.utils.PermissionUtils;
import com.aslam.p2pdemo.databinding.ActivityMainBinding;
import com.aslam.p2pdemo.services.MyP2PService;

import org.payable.ecr.ECRTerminal;
import org.payable.ecr.PAYableRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

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
                } else {
                    Toast.makeText(getApplicationContext(), "ECRTerminal is not connected", Toast.LENGTH_LONG).show();
                }
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