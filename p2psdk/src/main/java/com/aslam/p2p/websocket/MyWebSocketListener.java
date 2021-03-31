package com.aslam.p2p.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;

import java.nio.ByteBuffer;

public class MyWebSocketListener {

    public interface Server {
        void onOpen(WebSocket conn, ClientHandshake handshake);

        void onClose(WebSocket conn, int code, String reason, boolean remote);

        void onMessage(WebSocket conn, String message);

        void onMessage(WebSocket conn, ByteBuffer message);

        void onError(WebSocket conn, Exception ex);

        void onStart();
    }

    public interface Client {
        void onOpen(ServerHandshake handshakeData);

        void onClose(int code, String reason, boolean remote);

        void onMessage(String message);

        void onMessage(ByteBuffer message);

        void onError(Exception ex);
    }

}
