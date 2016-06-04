package com.yckir.bluetoothchat.services;


import android.app.IntentService;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yckir.bluetoothchat.Utility;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that handles write requests using a bluetooth socket. Uses IntentService that handles request
 * one at a time on a separate thread.
 */
public class BluetoothWriteService extends IntentService {

    private static final String TAG = "WriteService";
    private BluetoothSocket mSocket;
    private OutputStream mOutStream;
    private WriteBinder mBinder;

    public BluetoothWriteService() {
        super(TAG);
        mBinder = new WriteBinder();
    }

    /**
     * handles merging the parameters into a message that will be sent.
     * @param displayText the text that will be sent
     */
    private void handleActionSendMessage(String displayText){
        String data = Utility.ID_SEND_DISPLAY_TEXT + displayText;
        byte[] bytes = data.getBytes();
        try {
            mOutStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(TAG, "Could not write message : " + data);
        }
    }

    /**
     * Send text that will be displayed on the connected devices.
     * Sends an intent to the service that requests the message be sent using the bluetooth socket.
     *
     * @param context app context
     * @param message the message to be sent using socket.
     */
    public static void sendDisplayText(Context context, String message){
        Log.v(TAG, "startActionWrite");

        Intent intent = new Intent(context, BluetoothWriteService.class);
        intent.setAction(Utility.ACTION_SEND_DISPLAY_TEXT);
        intent.putExtra(Utility.EXTRA_SEND_DISPLAY_TEXT, message);

        context.startService(intent);
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
            case Utility.ACTION_SEND_DISPLAY_TEXT:
                String message = intent.getStringExtra(Utility.EXTRA_SEND_DISPLAY_TEXT);
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
