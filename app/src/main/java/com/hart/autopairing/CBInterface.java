package com.hart.autopairing;

/**
 * Created by Alex on 8/3/15.
 * Proprietary (Hart)
 */
public interface CBInterface
{
    void onListUpdate();

    void onDeviceFound(String deviceName);

    void onDevicePaired(int deviceID);
}
