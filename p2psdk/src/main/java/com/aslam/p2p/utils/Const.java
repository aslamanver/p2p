package com.aslam.p2p.utils;

import com.aslam.p2p.services.P2PController;
import com.aslam.p2p.services.P2PService;

public class Const {

    public static boolean DEBUG_MODE = false;

    public static final int P2P_PORT = 45456;

    public static final int WS_RECONNECT_INTERVAL = 1000 * 10; // millis
    public static final int WS_CONNECTION_TIMEOUT = 15; // seconds

    public static P2PController.ConnectionType CONNECTION_TYPE;
}
