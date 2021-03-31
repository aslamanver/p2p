package com.aslam.p2p.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class MyWebSocketServer extends WebSocketServer {

    private MyWebSocketListener.Server serverListener;
    // public Map<String, WebSocket> connections = new HashMap<>();

    public MyWebSocketServer(int port, MyWebSocketListener.Server serverListener) {
        super(new InetSocketAddress(port));
        this.serverListener = serverListener;
    }

    public MyWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        consoleLog("new connection to " + conn.getRemoteSocketAddress());
        // connections.put(conn.getRemoteSocketAddress().toString(), conn);
        this.serverListener.onOpen(conn, handshake);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        consoleLog("closed " + (conn != null ? conn.getRemoteSocketAddress() : "NULL") + " with exit code " + code + " additional info: " + reason);
        // for (String key : connections.keySet()) {
        //     if (connections.get(key).isClosing() || connections.get(key).isClosed()) {
        //         connections.remove(key);
        //     }
        // }
        this.serverListener.onClose(conn, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        consoleLog("received message from " + conn.getRemoteSocketAddress() + ": " + message);
        this.serverListener.onMessage(conn, message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        consoleLog("received ByteBuffer from " + conn.getRemoteSocketAddress());
        this.serverListener.onMessage(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        consoleLog("an error occurred on connection " + (conn != null ? conn.getRemoteSocketAddress() : "NULL") + ":" + ex);
        this.serverListener.onError(conn, ex);
    }

    @Override
    public void onStart() {
        consoleLog("server started successfully");
        setConnectionLostTimeout(10);
        this.serverListener.onStart();
    }

    public void send(String message) {
        for (WebSocket connection : getConnections()) {
            if (connection != null && connection.isOpen()) {
                connection.send(message);
            }
        }
    }

    public void send(String address, String message) {
        // WebSocket connection = connections.get(address);
        for (WebSocket connection : getConnections()) {
            if (connection != null && connection.isOpen() && connection.getAttachment().equals(address)) {
                connection.send(message);
            }
        }
    }

    public void consoleLog(String message) {
        // System.out.println("WEB_SOCKET_SERVER: " + message);
    }
}
