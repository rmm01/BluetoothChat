package com.yckir.bluetoothchat.services;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class BluetoothReadService extends Service {
    private static final String TAG = "ReadService";

    private ArrayList<InputStream> mInputStreams;
    private ArrayList<BluetoothSocket> mSockets;
    private ArrayList<Thread> mReadingThreads;

    private ReadBinder mBinder;

    //indicates that the user is currently reading
    private boolean mReading;

    //handler to be able to communicate with the a client on the main thread;
    Handler mHandler;


    private Thread getReadThread(final InputStream inputStream){
        return new Thread(){
            @Override
            public void run() {

                byte[] buffer = new byte[1024];
                int numBytes;


                while(!isInterrupted()){

                    try {
                        numBytes = inputStream.read(buffer);
                        Message m = mHandler.obtainMessage(0, numBytes, -1, buffer);
                        mHandler.sendMessage(m);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                Log.v(TAG,"finishing thread");
                mReading = false;

            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mBinder = new ReadBinder();
        mSockets = new ArrayList<>(4);
        mInputStreams = new ArrayList<>(4);
        mReadingThreads = new ArrayList<>(4);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this method will not be used, the bulk of the service involves the binder object

        if(mSockets.size() == 0) {
            Log.v(TAG, "onStartCommand not enabled");
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
        if(mReading){
            for(int i = 0; i < mReadingThreads.size(); i++){
                mReadingThreads.get(i).interrupt();
            }
        }

        mSockets = null;
        mInputStreams = null;
        mReadingThreads = null;
        mHandler = null;
        super.onDestroy();
    }

    /**
     * Class that allows for communication with the service. Has three primary uses.
     * <p>
     * 1. Give the service the bluetooth sockets.
     * <p>
     * 2. Start and stop the socket from reading input,.
     * <p>
     * 3. Setting the handler that will be used to notify ui thread of messages.
     */
    public class ReadBinder extends Binder {
        /**
         * sets the socket. This can only be done if the bluetooth socket is not currently reading.
         *
         * @return false if socket was being used to read or input stream could not be retrieved
         * from the socket, true otherwise
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

            mInputStreams.add(tmpIn);
            mSockets.add(socket);
            return true;
        }

        /**
         * @return true if the bluetooth socket is currently reading.
         */
        public boolean isReading(){
            return mReading;
        }

        /**
         * Have the socket start reading for input if it is not already.
         *
         * @return false if already reading or socket is null, true otherwise.
         */
        public boolean startReading(){
            if(mReading || mSockets.size() == 0 || mHandler == null)
                return false;
            mReading = true;

            Thread tempThread;
            for(int i = 0; i < mInputStreams.size(); i++){
                tempThread = getReadThread( mInputStreams.get(i) );
                mReadingThreads.add( tempThread );
                tempThread.start();
            }

            return true;
        }

        /**
         * Stops the bluetooth form reading. Can be safely called if not reading or not.
         *
         * @return false if not reading or socket not set. true if was reading and is stopping.
         */
        public boolean stopReading(){
            if(!mReading || mSockets.size() == 0)
                return false;

            for(int i = 0; i < mReadingThreads.size(); i++){
                mReadingThreads.get(i).interrupt();
            }
            mReading = false;
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
}
