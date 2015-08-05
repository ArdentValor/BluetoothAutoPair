package com.hart.autopairing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Alex on 8/3/15.
 * Proprietary (Hart)
 */
public class AutoPairing
{
    public static BluetoothAdapter bluetoothAdapter;
    private static BroadcastReceiver broadcastReceiver;

    private static BluetoothLeScanner bluetoothLeScanner;
    private static ScanCallback scanCallback;
    private static BluetoothDevice currentDevice;

    private static Context context;
    private static CBInterface cbInterface;
    private static int currentScanID;
    private static String currentScanMode;
    private static boolean scanLock;
    private static boolean bondLock;

    private static HashMap<String, BTDevice> requiredDevices;


    private static Handler handler;


    public static void addRequiredDevice(String deviceName, String type, int id)
    {
        requiredDevices.put(deviceName, new BTDevice(deviceName, type, id));
    }


    public static void register(Context ctx, CBInterface callbacks)
    {
        context = ctx;
        cbInterface = callbacks;

        handler = new Handler();

        scanLock = false;
        bondLock = false;

        currentScanMode = "BLE";

        requiredDevices = new HashMap<>();

        broadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                if (currentScanMode.equals("BTC"))
                {
                    if (BluetoothDevice.ACTION_FOUND.equals(action))
                    {
                        // Get device from intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        device.setPairingConfirmation(true);

                        String name = device.getName();
                        if (name != null && name.contains(getNameByID(currentScanID)))
                        {
                            cbInterface.onDeviceFound(device.getName());
                            if (pairDevice(device))
                            {
                                cbInterface.onDevicePaired(currentScanID);
                                cbInterface.onScanResult(new BTScanResult(currentScanID, device.getAddress(), device.getAddress()));
                                releaseScanResources();
                            }
                        }
                    }
                }
            }
        };

        scanCallback = new ScanCallback()
        {
            @Override
            public void onScanResult(int callbackType, ScanResult result)
            {
                super.onScanResult(callbackType, result);

                String name = result.getDevice().getName();


                if (name != null && name.contains(getNameByID(currentScanID)))
                {

                    BluetoothDevice device = result.getDevice();


                    if (device.getName().contains("Nonin"))
                    {
                        if (!bondLock)
                        {
                            cbInterface.onDeviceFound("STARTING BOND!");
                            bluetoothLeScanner.flushPendingScanResults(scanCallback);
                            currentDevice = device;
                            device.connectGatt(context, true, null);
                            bondLock = true;
                            handler.postDelayed(noninBondState, 500L);
                        }
                        else
                        {
                            bluetoothLeScanner.flushPendingScanResults(scanCallback);
                            cbInterface.onDeviceFound("BONDING");
                        }
                    }
                    else if (!getBondState(getNameByID(currentScanID)))
                    {
                        //if (!bondLock)
                        {
                            cbInterface.onDeviceFound("STARTING BOND!");
                            bondLock = true;
                            currentDevice = device;
                            device.createBond();
                            device.getBondState();
                            handler.postDelayed(checkBondState, 300L);
                            releaseScanResources();
                        }
                    }
                }
            }
        };
    }

    private static Runnable noninBondState = new Runnable()
    {
        @Override
        public void run()
        {
            if (getBondState("Nonin"))
            {
                cbInterface.onDevicePaired(currentScanID);
                cbInterface.onScanResult(new BTScanResult(currentScanID, currentDevice.getAddress(), currentDevice.getName().replace("Nonin", "")));
                handler.removeCallbacks(noninBondState);
                releaseScanResources();
            }
            else
            {
                handler.postDelayed(noninBondState, 500L);
            }
        }
    };

    private static Runnable checkBondState = new Runnable()
    {
        @Override
        public void run()
        {
            if (getBondState(getNameByID(currentScanID)))
            {
                cbInterface.onDevicePaired(currentScanID);
                if (currentDevice.getName().contains("A&D_UA"))
                {
                    cbInterface.onScanResult(new BTScanResult(currentScanID, currentDevice.getAddress(), currentDevice.getName().replace("A&D_UA-", "")));
                }
                else if (currentDevice.getName().contains("A&D_UC"))
                {
                    cbInterface.onScanResult(new BTScanResult(currentScanID, currentDevice.getAddress(), currentDevice.getName().replace("A&D_UC-", "")));
                }
                handler.removeCallbacks(checkBondState);
                releaseScanResources();
            }
            else
            {
                handler.postDelayed(checkBondState, 300L);
                scanByID(currentScanID);
            }
        }
    };

    private static boolean getBondState(String name)
    {
        Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice d : bonded)
        {
            if (d.getName().contains(name))
            {
                return true;
            }
        }
        return false;
    }

    private static void releaseScanResources()
    {
        bondLock = false;
        cbInterface.onDeviceFound("Release Called!");
        if (currentScanMode.equals("BTC"))
        {
            bluetoothAdapter.cancelDiscovery();
        }
        else
        {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }


    public static void scanByID(int id)
    {
        currentScanID = id;
        scanLock = true;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (getTypeByID(id).equals("BTC"))
        {
            currentScanMode = "BTC";
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(broadcastReceiver, filter);
            bluetoothAdapter.startDiscovery();
        }
        else
        {
            currentScanMode = "BLE";
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        }
    }

    private static String getTypeByID(int id)
    {
        String rt = null;
        Iterator i = requiredDevices.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry pair = (Map.Entry) i.next();
            BTDevice cd = (BTDevice) pair.getValue();
            if (cd.deviceID == id)
            {
                rt = cd.deviceType;
                break;
            }
            i.remove();
        }
        return rt;
    }

    private static String getNameByID(int id)
    {
        String rt = null;
        Iterator i = requiredDevices.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry pair = (Map.Entry) i.next();
            BTDevice cd = (BTDevice) pair.getValue();
            if (cd.deviceID == id)
            {
                rt = cd.deviceName;
                break;
            }
            i.remove();
        }
        return rt;
    }

    private static boolean pairDevice(BluetoothDevice device)
    {
        try
        {
            Log.d("pairDevice()", "Start Pairing...");
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("pairDevice()", "Pairing finished.");
            return true;
        }
        catch (Exception e)
        {
            Log.e("pairDevice()", e.getMessage());
            return false;
        }
    }

    private void unpairDevice(BluetoothDevice device)
    {
        try
        {
            Log.d("unpairDevice()", "Start Un-Pairing...");
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            Log.d("unpairDevice()", "Un-Pairing finished.");
        }
        catch (Exception e)
        {
            Log.e("unpairDevice()", e.getMessage());
        }
    }
}
