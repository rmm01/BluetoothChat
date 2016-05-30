package com.yckir.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ServerAcceptTask extends AsyncTask<Void, BluetoothSocket, Void>{
    private static final String TAG = "ServerAcceptTask";

    private BluetoothServerSocket mServerSocket;
    private BluetoothAdapter mBluetoothAdapter;

    public ServerAcceptTask(BluetoothAdapter adapter,UUID uuid, String sdpName){
        mBluetoothAdapter = adapter;
        BluetoothServerSocket tmp = null;


        try {
            tmp = adapter.listenUsingRfcommWithServiceRecord(sdpName, uuid);
        } catch (IOException e) {
            Log.v(TAG, "could not get the server socket");
            e.printStackTrace();
        }

        mServerSocket = tmp;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.v(TAG, "starting server");
        BluetoothSocket clientSocket = null;

        while(!isCancelled()){
            Log.v(TAG, "attempt accept");
            try {
                clientSocket = mServerSocket.accept();
            } catch (IOException e) {
                Log.v(TAG, "accept was aborted or timed out");
            }
            if(clientSocket != null){
                Log.v(TAG, "got a client");
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    Log.v(TAG, "could not close client");
                    e.printStackTrace();
                }
                publishProgress(clientSocket);
                break;
            }

        }

        Log.v(TAG,"I have been canceled");

        return null;
    }

    @Override
    protected void onProgressUpdate(BluetoothSocket... values) {
        Log.v(TAG, "socket ready for communication");
    }

    public void cancelServer() {
        Log.v(TAG, "cancel server");

        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.v(TAG, "could not close socket in cancelServer");
            e.printStackTrace();
        }
    }

    @Override
    protected void onCancelled() {
        Log.v(TAG, "onCancelled");
    }
}
