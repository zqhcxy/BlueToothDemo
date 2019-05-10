package com.example.zqh.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i("BLUETOOTHTAG", "onReceive action: "+action);
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            int bluetoothState = device.getBondState();
            Log.i("BLUETOOTHTAG", "connect device deviceName : " + deviceName + "\n deviceHardwareAddress : " + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState);
            Toast.makeText(context,"connect device deviceName : " + deviceName + "\n deviceHardwareAddress : "
                    + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState,Toast.LENGTH_LONG).show();
        }else if(BluetoothDevice.ACTION_FOUND.equals(action)){//发现设备
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            int bluetoothState = device.getBondState();
//            updateBluetoothDiscoverTip(true,"find device:  "+deviceName+"  address: "+deviceHardwareAddress);
            Log.i("BLUETOOTHTAG", "find device deviceName : " + deviceName + "\n deviceHardwareAddress : " + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState);
            Toast.makeText(context,"connect device deviceName : " + deviceName + "\n deviceHardwareAddress : "
                    + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState,Toast.LENGTH_LONG).show();
        }
    }
}
