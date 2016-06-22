package com.yckir.bluetoothchat.services;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.yckir.bluetoothchat.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;


public class BluetoothService extends Service {
    //check connections every increment of this many milliseconds
    public static final int TIMEOUT_DURATION = 6000;
    //after TIMEOUT_LENGTH * MAX_CONNECTION_ATTEMPTS milliseconds without reply, close the connection
    public static final int MAX_CONNECTION_ATTEMPTS = 3;

    private static final String TAG = "BluetoothService";
    private HashMap<String, BluetoothConnectionInfo> mClients;
    private BluetoothBinder mBinder;
    private Handler mClientHandler;
    private TimeoutHandler mTimeoutHandler;

    /**
     * Handler used to check every TIMEOUT_DURATION for a connection timeout. Sends a hello message
     * and increments connectionAttempts for each BluetoothConnectionInfo. If connectionAttempts is
     * greater greater than or equal to MAX_CONNECTION_ATTEMPTS, then the connection is closed. When
     * a Hello reply message is received, its connectionAttempts field is set to zero.
     */
    private static class TimeoutHandler extends Handler{
        public int mTimeoutWhat = 1993;
        WeakReference<BluetoothService> mService;

        public TimeoutHandler(BluetoothService service){
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what != mTimeoutWhat)
                return;
            for(BluetoothConnectionInfo info : mService.get().mClients.values()){
                if(info.connectionAttempts >= MAX_CONNECTION_ATTEMPTS){
                    Log.v(TAG, "timeout has occurred: " + info.device.getAddress());
                    mService.get().mBinder.removeSocket(info.device.getAddress());
                }else{
                    info.connectionAttempts++;
                    if(info.connectionAttempts >1)
                        Log.v(TAG, "connection attempt " + info.connectionAttempts + ": " + info.device.getAddress());
                    mService.get().mBinder.writeMessage(Utility.makeHelloMessage(), info.device.getAddress());
                }
            }
            Message m = obtainMessage(mTimeoutWhat);
            sendMessageDelayed(m,TIMEOUT_DURATION);
        }
    }

    /**
     * Enable reading and writing if it hasn't already for the bluetooth device with the
     * specified info.
     *
     * @param info info and data about the bluetooth connection

     */
    private void enableRW(@NonNull BluetoothConnectionInfo info){
        Log.v(TAG, "enableRW for " + info.device.getAddress());

        if(info.readThread == null || !info.readThread.isAlive()){
            info.readThread = new ReadThread(info.device.getAddress(), 1024, info.inputStream);
            info.readThread.start();
        }else{
            Log.v(TAG, "already reading");
        }

        if(info.writeThread == null || !info.writeThread.isAlive()){
            info.writeThread = new WriteThread(info.device.getAddress(), info.outputStream, info.blockingQueue);
            info.writeThread.start();
        }else{
            Log.v(TAG, "already writing");
        }
    }

    /**
     * Disable reading and writing if it hasn't already for the bluetooth device with the
     * specified info.
     *
     * @param info info and data about the bluetooth connection
     */
    private void disableRW(BluetoothConnectionInfo info){
        Log.v(TAG, "disableRW for " + info.device.getAddress());

        if(info.writeThread == null || !info.writeThread.isAlive()){
            Log.v(TAG,"not currently writing");
        }else{
            Log.v(TAG,"disable writing");
            info.blockingQueue.offer(WriteThread.SHUTDOWN_KEY);
        }

        if(info.readThread == null || !info.readThread.isAlive()){
            Log.v(TAG,"not currently reading");
        }else{
            Log.v(TAG,"disable reading");
            info.readThread.interrupt();
        }
        info.readThread = null;
        info.writeThread = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        mClients = new HashMap<>(Utility.MAX_NUM_BLUETOOTH_DEVICES);
        mBinder = new BluetoothBinder();
        mTimeoutHandler = new TimeoutHandler(this);
        mTimeoutHandler.sendMessageDelayed(mTimeoutHandler.obtainMessage(mTimeoutHandler.mTimeoutWhat),TIMEOUT_DURATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onDestroy() {
        mBinder.removeSockets();
    }

    /**
     * Thread that reads input from an inputStream. The data that is read will be sent to attached
     * handler. In the Message, the number of bytes will be sent as arg1 and the bytes will be sent
     * in obj parameter.
     */
    private class ReadThread extends Thread {

        private InputStream mInputStream;
        private final int mBufferSize;
        private final String mAddress;

        /**
         * Creates a new thread that will read input from a stream.
         *
         * @param address mac address for the remote bluetooth device
         * @param bufferSize max size of input buffer
         * @param inputStream input stream of the bluetooth connection
         */
        public ReadThread(String address, int bufferSize, InputStream inputStream){
            mInputStream = inputStream;
            mBufferSize = bufferSize;
            mAddress = address;
        }

        /**
         * Takes action on a message that was read based on the id.
         * Default will send the message to client handler. Hello messages are handled here.
         *
         * @param numBytes size of message
         * @param buffer the message that was read
         */
        private void parseMessage(int numBytes, byte[] buffer){
            String message = new String(buffer);
            String message_id = (message.substring(0, Utility.LENGTH_OF_SEND_ID));

            switch (message_id){

                //service messages
                case Utility.ID_HELLO:
                    mBinder.writeMessage(Utility.makeReplyHelloMessage());
                    break;

                case Utility.ID_HELLO_REPLY:
                    BluetoothConnectionInfo info = mClients.get(mAddress);
                    if(info != null)
                        info.connectionAttempts = 0;
                    break;

                //app messages
                default:
                    Message m = mClientHandler.obtainMessage(0, numBytes, -1, buffer);
                    mClientHandler.sendMessage(m);
                    break;
            }
        }

        @Override
        public void run() {
            Log.v(TAG, "START READING: " + mAddress);

            byte[] buffer = new byte[mBufferSize];
            int numBytes;

            while( !isInterrupted() ){
                try {
                    numBytes = mInputStream.read(buffer);

                    Log.v(TAG, "READ INPUT: " + mAddress);
                    if(mClientHandler == null) {
                        Log.e(TAG, "NO HANDLER, LOSING MESSAGE: " + new String(buffer));
                        continue;
                    }
                    parseMessage(numBytes,buffer);
                } catch (IOException e) {
                    Log.v(TAG, "READ EXCEPTION: " + mAddress);
                    e.printStackTrace();
                    break;
                }
            }
            Log.v(TAG,"CLOSING READING: " + mAddress);
            mBinder.removeSocket(mAddress);
        }
    }

    /**
     * Thread that writes to other bluetooth devices.
     */
    private class WriteThread extends Thread{
        private final String mAddress;
        private OutputStream mOutputStream;
        private ArrayBlockingQueue<String> mQueue;
        private static final String SHUTDOWN_KEY = "poliknbybdkfnammchyjmdnyukkdujmnyiyrtffsedrae";

        /**
         * Creates a thread that writes input using a stream. Communicate to this thread by giving it
         * messages to send using the blocking queue that is given as  a parameter.
         *
         * @param address mac address of the thread used for debugging
         * @param outputStream output stream of a bluetooth socket
         * @param queue a queue that will be given messages to be sent using output stream..
         */
        public WriteThread(String address, OutputStream outputStream, ArrayBlockingQueue<String> queue){
            mAddress = address;
            mOutputStream = outputStream;
            mQueue = queue;
        }

        /**
         * Send a message to the attached outputStream
         *
         * @param message message to be sent
         */
        private void sendMessage(String message){
            byte[] bytes = message.getBytes();

            try {
                mOutputStream.write(bytes);
            } catch (IOException e) {
                Log.v(TAG, "WRITE EXCEPTION, MESSAGE : " + message + ", ADDRESS " + mAddress);
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Log.v(TAG, "START WRITING: " + mAddress);
            while ( !isInterrupted() ){
                try{
                    String data = mQueue.take();
                    if(data.equals( SHUTDOWN_KEY )) {
                        Log.v(TAG, "SHUTDOWN KEY RECEIVED: " + mAddress);
                        break;
                    }
                    sendMessage(data);
                }catch (InterruptedException e){
                    Log.v(TAG, "WRITE QUEUE EXCEPTION  " + mAddress);
                    e.printStackTrace();
                    break;
                }
            }

            Log.v(TAG,"CLOSING WRITING: " + mAddress);
            mBinder.removeSocket(mAddress);
        }
    }

    /**
     * Stores data associated with a bluetooth connection
     */
    private class BluetoothConnectionInfo {
        volatile boolean deleting;
        int connectionAttempts;
        OutputStream outputStream;
        InputStream inputStream;
        //threads will be null until they need to run
        Thread writeThread;
        Thread readThread;
        BluetoothSocket socket;
        BluetoothDevice device;
        ArrayBlockingQueue<String> blockingQueue;
    }

    /**
     * Class that allows for communication with the service. Has four primary steps to setup.
     * <p>
     * 1. Give the service bluetooth sockets.
     * <p>
     * 2. Give the service the handler from the ui thread.
     * <p>
     * 3. Enable the socket for reading and writing.
     * <p>
     * Sockets can be disabled, re-enabled, and removed. Removed sockets are also closed.
     * Sockets are automatically disabled and closed when the services onDestroy is called.
     */
    public class BluetoothBinder extends Binder{

        /**
         * Add a bluetooth socket that has been connected with a remote bluetooth device. A socket
         * can be connected using BluetoothServerSocket.accept() for a server or
         * createRfcommSocketToServiceRecord().connect() for a client.
         *
         * @param socket bluetooth socket that will be added
         * @return false if output streams could not be created from socekts
         */
        public boolean addSocket(@NonNull BluetoothSocket socket){
            Log.v(TAG, "adding socket with address " +socket.getRemoteDevice().getAddress());
            BluetoothConnectionInfo info = new BluetoothConnectionInfo();

            OutputStream tmpOut;
            InputStream tmpIn;

            //get outputStream, return if cannot
            try {
                tmpOut = socket.getOutputStream();

            } catch (IOException e) {
                Log.e(TAG, "could not get an outputStream from socket " + socket.getRemoteDevice().getAddress());
                e.printStackTrace();
                return false;
            }

            //get inputStream, close outputStream and return if cannot
            try {
                tmpIn = socket.getInputStream();

            } catch (IOException e) {
                Log.e(TAG, "could not get an inputStream from socket " + socket.getRemoteDevice().getAddress());
                e.printStackTrace();
                try {
                    Log.e(TAG, "closing outputStream because could not get inputStream from socket "
                            + socket.getRemoteDevice().getAddress());
                    tmpOut.close();
                } catch (IOException e1) {
                    Log.e(TAG, "closing outputStream because could not get inputStream from socket "
                            + socket.getRemoteDevice().getAddress() + ": has FAILED");
                    e1.printStackTrace();
                }
                return false;
            }
            info.connectionAttempts = 0;
            info.deleting = false;
            info.inputStream = tmpIn;
            info.outputStream = tmpOut;
            info.socket = socket;
            info.device = socket.getRemoteDevice();
            //TODO determine ideal queue size for requests
            info.blockingQueue = new ArrayBlockingQueue<>(10, true);

            mClients.put(info.device.getAddress(), info);
            enableRW(info);
            return true;
        }


        /**
         * Removes and closes all sockets that have been added. The reading and writing will
         *  also stop for that socket if it hasn't already.
         */
        public void removeSockets(){
            for(BluetoothConnectionInfo tmpInfo: mClients.values()){
                removeSocket(tmpInfo.device.getAddress());
            }
        }


        /**
         * Removes and closes the socket with the given mac address if it exists. The reading and
         * writing will also stop for that socket.
         *
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         */
        public void removeSocket(String macAddress){
            BluetoothConnectionInfo tmpInfo = mClients.get(macAddress);

            //if cant find connection or already deleting, return
            if(tmpInfo == null || tmpInfo.deleting)
                return;
            tmpInfo.deleting = true;

            Log.v(TAG, "removeSocket for " + macAddress);

            disableRW(tmpInfo);

            try {
                Log.v(TAG, "closing the input stream: " + macAddress);
                tmpInfo.inputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close the input stream: " + macAddress);
            }

            try {
                Log.v(TAG, "closing the output stream: " + macAddress);
                tmpInfo.outputStream.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close the output stream: " + macAddress);
            }

            try {
                Log.v(TAG, "closing the input socket: " + macAddress);
                tmpInfo.socket.close();
            } catch (IOException e) {
                Log.w(TAG, "could not close socket: " + macAddress);
            }

            mClients.remove(tmpInfo.device.getAddress());
            Log.v(TAG, "removed from clients: " + tmpInfo.device.getAddress());

            //Send message to handler
            if(mClientHandler != null){
                Message m = mClientHandler.obtainMessage(1, macAddress.length(), -1, macAddress.getBytes());
                mClientHandler.sendMessage(m);
            }

        }


        /**
         * Write a message to all bluetooth sockets that are enabled. The mac address ia available
         * using BluetoothSocket.getRemoteDevice().getAddress().
         *
         * @param message message to be sent
         */
        public void writeMessage(String message){
            for(BluetoothConnectionInfo tmpInfo: mClients.values()){
                writeMessage(message, tmpInfo.device.getAddress());
            }
        }


        /**
         * write a message to the specified bluetooth device with the specified mac address.The mac
         * address ia available using BluetoothSocket.getRemoteDevice().getAddress().
         *
         * @param message message to be sent
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         * @return false if mac address doesn't exist for an added socket.
         */
        public boolean writeMessage(String message, String macAddress){
            Log.v(TAG, "writeMessage to " + macAddress);

            if( !mClients.containsKey(macAddress) )
                return false;

            BluetoothConnectionInfo tmpInfo = mClients.get(macAddress);

            if(tmpInfo == null || tmpInfo.socket == null)
                return false;

            if(!tmpInfo.blockingQueue.offer(message))
                Log.v(TAG, "blocking queue is full, cannot put message " + message);
            return true;
        }


        /**
         * Get the bluetooth device with the given mac address.
         *
         * @param macAddress mac address of the bluetooth device that will have the message sent to
         * @return the bluetooth device with the given macAddress, null if it does not exist.
         */
        public BluetoothDevice getDevice(String macAddress){
            if( !mClients.containsKey(macAddress) )
                return null;
            return (mClients.get(macAddress)).device;
        }


        /**
         * Handler that will receive messages when something is read. Byte message will be in obj
         * parameter and arg1 will contain size of message.
         *
         * @param handler handler to receive messages.
         */
        public void setHandler(Handler handler){
            mClientHandler = handler;
        }


        /**
         * Get an instance of the handler that has been set.
         * @return handler that has been set
         */
        public @Nullable Handler getHandler(){
            return mClientHandler;
        }
    }
}
