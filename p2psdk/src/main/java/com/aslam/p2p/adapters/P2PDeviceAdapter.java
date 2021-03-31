package com.aslam.p2p.adapters;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.widget.BaseAdapter;

import com.aslam.p2p.services.P2PController;
import com.aslam.p2p.services.P2PService;

import java.util.List;

public abstract class P2PDeviceAdapter extends BaseAdapter {

    public interface EventListener {
        void onClickEvent(int position, WifiP2pDevice device);
    }

    protected Context context;
    protected List<WifiP2pDevice> deviceList;
    protected EventListener eventListener;

    public P2PDeviceAdapter(Context context, List<WifiP2pDevice> deviceList, EventListener eventListener) {
        this.context = context;
        this.deviceList = deviceList;
        this.eventListener = eventListener;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public String getDisplayName(WifiP2pDevice device) {
        return device.deviceName + " " + P2PController.getStatusText(device.status) + (device.isGroupOwner() ? " OWNER" : "");
    }
}
