package com.aslam.p2pdemo;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aslam.p2p.adapters.P2PDeviceAdapter;
import com.aslam.p2pdemo.databinding.DeviceLayoutRowBinding;

import java.util.List;

public class MyDeviceAdapter extends P2PDeviceAdapter {

    public MyDeviceAdapter(Context context, List<WifiP2pDevice> deviceList, P2PDeviceAdapter.EventListener eventListener) {
        super(context, deviceList, eventListener);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        DeviceLayoutRowBinding binding = DeviceLayoutRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

        final WifiP2pDevice device = deviceList.get(position);

        binding.txtName.setText(getDisplayName(device));
        binding.txtAddress.setText(device.deviceAddress);

        binding.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventListener.onClickEvent(position, device);
            }
        });

        return binding.getRoot();
    }
}
