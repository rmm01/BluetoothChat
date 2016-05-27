package com.yckir.bluetoothchat;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothDiscoverReceiver extends BroadcastReceiver{

    private BlueToothDiscoverListener mListener;

    public interface BlueToothDiscoverListener{
        void deviceDiscovered(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice);
        void discoveryStarted();
        void discoveryFinished();
    }

    public void setListener(BlueToothDiscoverListener listener){
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(mListener == null)
            return;

        String action = intent.getAction();

        if(BluetoothDevice.ACTION_FOUND.equals(action)){
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothClass bluetoothClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
            mListener.deviceDiscovered(bluetoothClass, device);
        }

        if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
            mListener.discoveryStarted();
        }

        if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
            mListener.discoveryFinished();
        }
    }
}
