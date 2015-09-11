package com.hart.autopairing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends Activity
{
    private static final String TAG = "AutoPairingUtility";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        AutoPairing.register(this, new CBInterface()
        {
            @Override
            public void onListUpdate()
            {

            }

            @Override
            public void onDeviceFound(String deviceName)
            {
                Log.i(TAG, "Filter |------ " + deviceName);
            }

            @Override
            public void onDevicePaired(int deviceID)
            {
                Log.i(TAG, "Filter |------ " +  "DevicePaired");
            }

            @Override
            public void onScanResult(BTScanResult scanResult)
            {
                Log.i(TAG, "Filter |------ " +  "DeviceID = " + scanResult.deviceID + " | Mac Address = " + scanResult.macAddress + " | Serial Number = " + scanResult.serialNumber);
            }
        });

        AutoPairing.addRequiredDevice("myglucohealth", "BTC", 1);
        AutoPairing.addRequiredDevice("A&D_UA", "BLE", 2);
        AutoPairing.addRequiredDevice("Nonin", "BLE", 3);
        AutoPairing.addRequiredDevice("A&D_UC", "BLE", 4);
    }


    @Override
    protected void onStart()
    {
        super.onStart();
        // test each (these are called by the user/UI for each device individually
        //AutoPairing.scanByID(1);
        AutoPairing.scanByID(2);
        //AutoPairing.scanByID(3);


        //AutoPairing.scanByID(4);
    }
}
