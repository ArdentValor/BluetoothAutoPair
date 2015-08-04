package com.hart.autopairing;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Alex on 8/3/15.
 * Proprietary (Hart)
 */
public class BTDevice
{
    public String deviceName;
    public BluetoothDevice device;
    public boolean isPaired;
    public String deviceType;
    public int deviceID;

    public BTDevice(String name, String type, int id)
    {
        deviceName = name;
        deviceType = type;
        deviceID = id;
    }
}
