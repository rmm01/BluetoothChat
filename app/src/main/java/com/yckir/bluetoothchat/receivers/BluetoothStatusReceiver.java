package com.yckir.bluetoothchat.receivers;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothStatusReceiver extends BroadcastReceiver{

    private BlueToothStatusListener mListener;

    public interface BlueToothStatusListener{
        void bluetoothOff();
        void bluetoothOn();
        void bluetoothTurningOff();
        void bluetoothTurningOn();
    }

    public void setListener(BlueToothStatusListener listener){
        mListener = listener;
    }

    public static IntentFilter getIntentFilter(){
        return new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(mListener == null)
            return;

        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    mListener.bluetoothOff();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    mListener.bluetoothTurningOff();
                    break;
                case BluetoothAdapter.STATE_ON:
                    mListener.bluetoothOn();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    mListener.bluetoothTurningOn();
                    break;
            }
        }
    }
}
