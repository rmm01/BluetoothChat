package com.yckir.bluetoothchat.services;


import android.app.IntentService;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that handles write requests using a bluetooth socket. Uses IntentService that handles request
 * one at a time on a separate thread.
 */
public class BluetoothWriteService extends IntentService {

    public static final String ACTION_SEND_MESSAGE = "ACTION_SEND_MESSAGE";
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    private static final String TAG = "WriteService";
    private BluetoothSocket mSocket;
    private OutputStream mOutStream;
    private WriteBinder mBinder;

    public BluetoothWriteService() {
        super(TAG);
        mBinder = new WriteBinder();
    }

    /**
     * handles sending the message to the bluetooth clients.
     *
     * @param message the text that will be sent
     */
    private void handleActionSendMessage(String message){
        byte[] bytes = message.getBytes();
        try {
            mOutStream.write(bytes);
        } catch (IOException e) {
            Log.v(TAG, "Could not write message : " + message);
            e.printStackTrace();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(mSocket == null){
            Log.v(TAG, "onHandleIntent no socket");
            return;
        }
        Log.v(TAG, "onHandleIntent");

        if(intent == null){
            Log.v(TAG, "null intent");
            return;
        }

        String action = intent.getAction();

        if(action == null){
            Log.v(TAG, "null action");
            return;
        }


        switch (action){
            case ACTION_SEND_MESSAGE:
                String message = intent.getStringExtra(EXTRA_MESSAGE);
                handleActionSendMessage(message);
                break;
            default:
                Log.v(TAG,"unknown action " + action);
                break;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return false;
    }


    /**
     * Binder that is used to give the service a reference to the bluetooth socket to write to.
     */
    public class WriteBinder extends Binder {
        /**
         * @param socket the bluetooth socket used to communicate with another device
         * @return true false if an OutputStream could not be retrieved from the socket.
         */
        public boolean setSocket(@NonNull BluetoothSocket socket){

            Log.v(TAG, "socket set");

            OutputStream tmpOut;

            try {
                tmpOut = socket.getOutputStream();
            }catch (IOException e){
                Log.e(TAG, "Could not get output stream form socket");
                return false;
            }

            if(mOutStream != null){
                try {
                    mOutStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.w(TAG, "could not close the socket before switching");
                }
            }

            mSocket = socket;
            mOutStream = tmpOut;
            return true;
        }
    }
}
