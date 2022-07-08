package com.aslam.p2pdemo.services;

import com.aslam.p2p.services.P2PService;
import com.aslam.p2p.utils.Const;
import com.aslam.p2p.utils.LogUtils;

public class MyP2PService extends P2PService {

    @Override
    public void onConsoleLog(String message) {
        super.onConsoleLog(message);
        if (Const.DEBUG_MODE) {
            LogUtils.buzz(100);
        }
    }
}
