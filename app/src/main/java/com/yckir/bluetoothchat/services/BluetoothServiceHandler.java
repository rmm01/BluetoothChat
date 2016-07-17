package com.yckir.bluetoothchat.services;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Handler that listens to messages from BluetoothService. The abstract methods will be called
 * from the default handleMessage method. HandleMessage should not be extended, implement the
 * abstract methods to handle the messages from the service.
 */
public abstract class BluetoothServiceHandler extends Handler {
    public static final String TAG = "BluetoothServiceHandler";
    public static final String EXTRA_MAC_ADDRESS = "BluetoothServiceHandler:EXTRA_MAC_ADDRESS";

    /**
     * Called when a connection has closed.
     *
     * @param macAddress mac address of the closed connection.
     */
    public abstract void connectionClosed(String macAddress, @ServiceUtility.CLOSE_CODE int closeCode);

    /**
     * message that was sent form a remote bluetooth device and should be parsed by the activity.
     *
     * @param message message from remote bluetooth device.
     */
    public abstract void appMessage(String message);

    /**
     * Tells client that the server has finished setting up and it can
     *  start the activity that will be using bluetooth communication.
     */
    public abstract void serverSetupFinished();

    @Override
    public final void handleMessage(Message msg) {
        int size = msg.arg1;
        String serviceMessage = (String)msg.obj;

        String messageId = (serviceMessage.substring(0, ServiceUtility.LENGTH_ID));
        String messageData = null;
        if(ServiceUtility.LENGTH_ID != size)
            messageData = serviceMessage.substring(ServiceUtility.LENGTH_ID, size);

        switch (messageId){
            case ServiceUtility.ID_APP_MESSAGE:
                appMessage(messageData);
                break;
            case ServiceUtility.ID_SERVER_SETUP_FINISHED:
                serverSetupFinished();
                break;
            case ServiceUtility.ID_CONNECTION_CLOSED:
                @ServiceUtility.CLOSE_CODE int closeCode =  Integer.parseInt( messageData);
                connectionClosed(msg.getData().getString(EXTRA_MAC_ADDRESS), closeCode);
                break;
            default:
                Log.v(TAG, " unknown service message id " + messageId + ", with message " + messageData);
                break;
        }
    }
}
