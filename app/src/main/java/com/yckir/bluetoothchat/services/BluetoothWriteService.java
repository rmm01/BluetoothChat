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
import java.util.ArrayList;

/**
 * Class that handles write requests using a bluetooth socket. Uses IntentService that handles request
 * one at a time on a separate thread provided by using IntentService.
 */
public class BluetoothWriteService extends IntentService {
    //TODO constantly check the other end of the bluetooth socket if no communication has happened in a set amount of time
    //TODO implement a way to cancel the connection if no hello reply is received after 7 seconds

    public static final String ACTION_SEND_MESSAGE = "ACTION_SEND_MESSAGE";
    public static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

    private static final String TAG = "WriteService";
    private WriteBinder mBinder;

    private ArrayList<BluetoothSocket> mSockets;
    private ArrayList<OutputStream> mOutputStreams;

    public BluetoothWriteService() {
        super(TAG);
        mBinder = new WriteBinder();
        mOutputStreams = new ArrayList<>(4);
        mSockets = new ArrayList<>(4);
    }

    /**
     * handles sending the message to the all bluetooth clients.
     *
     * @param message the message that will be sent
     */
    private void handleActionSendMessage(String message){
        byte[] bytes = message.getBytes();

        OutputStream tmpOut;
        for(int i = 0; i < mOutputStreams.size(); i++){
            tmpOut = mOutputStreams.get(i);
            try {
                tmpOut.write(bytes);
            } catch (IOException e) {
                Log.v(TAG, "Could not write message : " + message + ", to socket number " + i);
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(mSockets.size() == 0){
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
         * Adds a bluetooth socket to use for communication.
         *
         * @param socket the bluetooth socket used to communicate with another device
         * @return true false if an OutputStream could not be retrieved from the socket.
         */
        public boolean addSocket(@NonNull BluetoothSocket socket){
            Log.v(TAG, "adding socket");

            OutputStream tmpOut;

            try {
                tmpOut = socket.getOutputStream();
            }catch (IOException e){
                Log.e(TAG, "Could not get output stream form socket");
                return false;
            }

            mOutputStreams.add(tmpOut);
            mSockets.add(socket);
            return true;
        }
    }
}
