package com.aslam.p2p.websocket;

import com.aslam.p2p.utils.Const;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class MyWebSocketClient extends WebSocketClient {

    MyWebSocketListener.Client clientListener;
    private boolean isReconnect = true;
    private boolean isConnecting;
    private boolean isConnected;
    public String host;

    public MyWebSocketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public MyWebSocketClient(String host, URI serverURI, MyWebSocketListener.Client clientListener) {
        super(serverURI);
        this.clientListener = clientListener;
        this.host = host;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        consoleLog("new connection opened");
        isConnecting = false;
        isConnected = true;
        setConnectionLostTimeout(Const.WS_CONNECTION_TIMEOUT);
        clientListener.onOpen(handshakeData);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        consoleLog("closed with exit code " + code + " additional info: " + reason);
        isConnecting = false;
        isConnected = false;
        clientListener.onClose(code, reason, remote);
        if (isReconnect) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(Const.WS_RECONNECT_INTERVAL);
                        reconnect();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onMessage(String message) {
        consoleLog("received message: " + message);
        clientListener.onMessage(message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        consoleLog("received ByteBuffer");
        clientListener.onMessage(message);
    }

    @Override
    public void onError(Exception ex) {
        consoleLog("an error occurred:" + ex);
        clientListener.onError(ex);
    }

    @Override
    public void send(String text) {
        if (isOpen()) {
            super.send(text);
        }
    }

    @Override
    public void connect() {
        super.connect();
        isConnecting = true;
        isConnected = false;
    }

    private void setReconnect(boolean isReconnect) {
        this.isReconnect = isReconnect;
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void forceClose(String reason) {
        setReconnect(false);
        close(1009, reason);
    }

    public void consoleLog(String message) {
        // Log.e("WEB_SOCKET_CLIENT", message);
    }
}
