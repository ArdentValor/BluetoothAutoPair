package com.hart.autopairing;

/**
 * Created by Alex on 8/5/15.
 * Proprietary (Hart)
 */
public class BTScanResult
{
    public int deviceID;
    public String macAddress;
    public String serialNumber;

    public BTScanResult(int id, String address, String sn)
    {
        deviceID = id;
        macAddress = address;
        serialNumber = sn;
    }

}
