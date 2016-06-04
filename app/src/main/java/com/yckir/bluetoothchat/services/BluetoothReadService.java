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

public class BluetoothReadService extends Service {
    private static final String TAG = "ReadService";
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private ReadBinder mBinder;

    //indicates that the user is currently reading
    private boolean mReading;

    //access to reading thread so we can stop it.
    private Thread mReadingThread;

    //handler to be able to communicate with the a client on the main thread;
    Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mBinder = new ReadBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this method will not be used, the bulk of the service involves the binder object

        if(mSocket == null) {
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
        if(mReading)
            mReadingThread.interrupt();
        mSocket = null;
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
        public boolean setSocket(BluetoothSocket socket){
            if(mReading){
                return false;
            }

            InputStream tmpOut;

            try {
                tmpOut = socket.getInputStream();
            }catch (IOException e){
                Log.v(TAG, "could not get input stream from socket");
                e.printStackTrace();
                return false;
            }

            if(mInStream != null){
                try {
                    mInStream.close();
                } catch (IOException e) {
                    Log.v(TAG, "could not close input stream from setSocket");
                }
            }


            mInStream = tmpOut;
            mSocket = socket;

            return true;
        }

        /**
         * remove the socket. This can only be done if not currently reading.
         *
         * @return false if socket not removed because socket was being used to read. true otherwise.
         */
        public boolean removeSocket(){
            if(mReading)
                return false;
            mSocket = null;
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
            if(mReading || mSocket == null|| mHandler == null)
                return false;
            mReading = true;

            mReadingThread = new Thread(){
                @Override
                public void run() {

                    byte[] buffer = new byte[1024];
                    int numBytes;


                    while(!isInterrupted()){

                        try {
                            numBytes = mInStream.read(buffer);
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
            mReadingThread.start();
            return true;

        }

        /**
         * Stops the bluetooth form reading. Can be safely called if not reading or not.
         *
         * @return false if not reading or socket not set. true if was reading and is stopping.
         */
        public boolean stopReading(){
            if(!mReading || mSocket == null)
                return false;

            mReadingThread.interrupt();
            mReading = false;
            return true;
        }

        /**
         * set the handler that will be used to communicate with ui thread.
         *
         * @param handler handler for ui thread
         */
        public void setHandler(ReadServiceHandler handler){
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
