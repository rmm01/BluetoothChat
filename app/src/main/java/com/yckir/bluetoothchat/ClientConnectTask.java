package com.yckir.bluetoothchat;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ClientConnectTask extends AsyncTask<Void, Void, BluetoothSocket>{
    private static final String TAG = "ClientConnectTask";

    private BluetoothSocket mClientSocket;

    public ClientConnectTask(BluetoothDevice device, UUID uuid){
        BluetoothSocket tmp = null;
        try {
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.v(TAG, "could not get the socket");
            e.printStackTrace();
        }

        mClientSocket = tmp;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... params) {
        Log.v(TAG, "starting client");
        try {
            mClientSocket.connect();
            return mClientSocket;
        } catch (IOException e) {
            Log.v(TAG, "could not connect to server");
            try {
                mClientSocket.close();
            } catch (IOException e1) {
                Log.v(TAG, "could not close socket");
                e1.printStackTrace();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(BluetoothSocket socket) {
        if(socket != null)
            Log.v(TAG, "socket ready for communication");
        else {
            Log.v(TAG, "socket not available for communication");
        }
    }

    @Override
    protected void onCancelled() {
        Log.v(TAG, "canceling client");
        try {
            mClientSocket.close();
        } catch (IOException e1) {
            Log.v(TAG, "could not close socket");
            e1.printStackTrace();
        }
    }
}