package com.aslam.p2p.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DataCenter {

    @Expose
    @SerializedName("connected_device_address")
    public String connectedDeviceAddress;
}
