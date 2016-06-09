package com.yckir.bluetoothchat.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yckir.bluetoothchat.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

public class BluetoothReadService extends Service {
    private static final String TAG = "ReadService";

    private HashMap<String, ReadServiceInfo> mClients;
    private ReadBinder mBinder;

    //handler to be able to communicate with the a client on the main thread;
    private Handler mHandler;

    /**
     * gets a new thread that will read from a given inputStream. Sends messages to the handler
     * when it reads a message.
     *
     * @param inputStream input stream that will be reading input.
     * @return thread that will read input when started
     */
    private Thread getReadThread(final InputStream inputStream){
        return new Thread(){
            @Override
            public void run() {

                Log.v(TAG, "starting reading thread");
                byte[] buffer = new byte[1024];
                int numBytes;

                while(!isInterrupted()){

                    try {
                        numBytes = inputStream.read(buffer);
                        Log.v(TAG, "read something");
                        Message m = mHandler.obtainMessage(0, numBytes, -1, buffer);
                        mHandler.sendMessage(m);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.v(TAG,"finishing thread");
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mBinder = new ReadBinder();
        mClients = new HashMap<>(Utility.MAX_NUM_BLUETOOTH_DEVICES);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this method will not be used, the bulk of the service involves the binder object

        if(mClients.isEmpty()) {
            Log.v(TAG, "onStartCommand has no bluetooth clients");
            return START_NOT_STICKY;
        }

        if(intent == null){
            Log.v(TAG, "onStartCommand null intent");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        Log.v(TAG, "onStartCommand with unknown action: " + action);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //unbinding is not important, since the service can be bound by multiple components
        //we could enforce here that the thread stops.
        Log.v(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        ReadServiceInfo info;
        Set<String> keySet = mClients.keySet();
        for(String key : keySet){
            info = mClients.get(key);
            if(info.thread.isAlive()) {
                Log.v(TAG, "Interrupting thread with key: " + key);
                info.thread.interrupt();
            }

            try {
                info.socket.close();
            } catch (IOException e) {
                Log.v(TAG, "could not close socket with key " + key);
                e.printStackTrace();
            }
        }

        mHandler = null;
        super.onDestroy();
    }

    /**
     * Class that allows for communication with the service. Has three primary uses.
     * <p>
     * 1. Give the service the bluetooth sockets.
     * <p>
     * 2. Start and stop the socket from reading input.
     * <p>
     * 3. Setting the handler that will be used to notify ui thread of messages.
     */
    public class ReadBinder extends Binder {

        /**
         * add a bluetooth socket that has been connected with another bluetooth device. Use
         * socket.getRemoteDevice().getAddress() to get the mac address so that you can start and
         * stop reading.
         *
         * @return false if an input stream could not be retrieved from the socket, true otherwise
         */
        public boolean addSocket(BluetoothSocket socket){
            Log.v(TAG, "adding socket");
            InputStream tmpIn;

            try {
                tmpIn = socket.getInputStream();
            }catch (IOException e){
                Log.e(TAG, "Could not get input stream form socket");
                return false;
            }

            ReadServiceInfo info = new ReadServiceInfo();
            info.socket = socket;
            info.device = socket.getRemoteDevice();
            info.inputStream = tmpIn;

            mClients.put(info.device.getAddress(), info);

            return true;
        }

        /**
         * Starts reading using an already added bluetooth socket with the given macAddress.
         * The macAddress can be found using socket.getRemoteDevice().getAddress().
         *
         * @param macAddress the mac address of an already added bluetooth socket.
         * @return false if no handler has been added, or if no socket has the given macAddress,
         *                  true otherwise.
         */
        public boolean startReading(String macAddress){
            Log.v(TAG, "startReading " + macAddress);

            if(mClients.isEmpty() || mHandler == null || !mClients.containsKey(macAddress))
                return false;

            ReadServiceInfo info = mClients.get(macAddress);

            if(info.thread == null || !info.thread.isAlive()) {
                Log.v(TAG, "starting thread with mac address: " + macAddress);
                info.thread = getReadThread(info.inputStream);
                info.thread.start();
            }//else it is already reading

            return true;
        }

        /**
         * Have all sockets start reading if they are not already.
         *
         * @return false if no bluetooth sockets added or no handler set, true otherwise.
         */
        public boolean startReading(){
            Log.v(TAG, "startReading all");

            if(mClients.isEmpty() || mHandler == null)
                return false;

            ReadServiceInfo info;
            Set<String> keySet = mClients.keySet();

            for(String key : keySet){
                info = mClients.get(key);
                if(info.thread == null || !info.thread.isAlive()) {
                    Log.v(TAG, "starting thread with key: " + key);
                    info.thread = getReadThread(info.inputStream);
                    info.thread.start();
                }//else it is already reading
            }
            return true;
        }

        /**
         * Stops reading from an already added bluetooth socket with the given macAddress.
         * The macAddress can be found using socket.getRemoteDevice().getAddress().
         *
         * @param macAddress the mac address of an already added bluetooth socket.
         * @return false if no handler has been added, or if no socket has the given macAddress,
         *                  true otherwise.
         */
        public boolean stopReading(String macAddress){

            if(mClients.isEmpty() || mHandler == null || !mClients.containsKey(macAddress))
                return false;

            ReadServiceInfo info = mClients.get(macAddress);

            if(info.thread == null)
                return true;

            if(info.thread.isAlive()) {
                Log.v(TAG, "stopping thread with key: " + macAddress);
                info.thread.interrupt();
            }

            info.thread = null;
            return true;
        }

        /**
         * Stops all bluetooth devices from reading.
         *
         * @return false if no bluetooth clients or handler set, true otherwise.
         */
        public boolean stopReading(){
            if(mClients.isEmpty() || mHandler == null)
                return false;

            ReadServiceInfo info;
            Set<String> keySet = mClients.keySet();

            for(String key : keySet){
                info = mClients.get(key);
                if(info.thread == null)
                    continue;
                if(info.thread.isAlive()) {
                    Log.v(TAG, "Interrupting thread with key: " + key);
                    info.thread.interrupt();
                }
                info.thread = null;
            }
            return true;
        }

        /**
         * set the handler that will be used to communicate with ui thread.
         *
         * @param handler handler for ui thread
         */
        public void setHandler(Handler handler){
            mHandler = handler;
        }

        /**
         * @return get the handler that was given from setHandler, null otherwise
         */
        public Handler getHandler(){
            return mHandler;
        }

    }

    private static class ReadServiceInfo {
        //should be non null only if it is currently running,
        //set equal to null once it stops.
        public Thread thread;
        public BluetoothSocket socket;
        public BluetoothDevice device;
        public InputStream inputStream;
    }
}
