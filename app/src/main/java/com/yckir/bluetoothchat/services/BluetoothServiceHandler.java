package com.yckir.bluetoothchat.services;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yckir.bluetoothchat.services.messages.BT_Message;
import com.yckir.bluetoothchat.services.messages.BT_MessageApp;
import com.yckir.bluetoothchat.services.messages.BT_MessageClose;
import com.yckir.bluetoothchat.services.messages.BT_MessageHello;
import com.yckir.bluetoothchat.services.messages.BT_MessageHelloReply;
import com.yckir.bluetoothchat.services.messages.BT_MessageUtility;

/**
 * Handler that listens to messages from BluetoothService. The abstract methods will be called
 * from the default handleMessage method. HandleMessage should not be extended, implement the
 * abstract methods to handle the messages from the service.
 */
public abstract class BluetoothServiceHandler extends Handler {
    public static final String TAG = "BluetoothServiceHandler";

    /**
     * Called when a connection has closed.
     *
     * @param macAddress mac address of the closed connection.
     */
    public abstract void connectionClosed(String macAddress, @ServiceUtility.CLOSE_CODE int closeCode);

    /**
     * message that was sent form a remote bluetooth device and should be parsed by the activity.
     *
     * @param macAddress address of the remote bluetooth device that sent the message
     * @param bytes data that was sent by remote bluetooth device
     */
    public abstract void appMessage(String macAddress, byte[] bytes);

    /**
     * Tells client that the server has finished setting up and it can
     *  start the activity that will be using bluetooth communication.
     */
    public abstract void serverSetupFinished();

    public final void handleMessage(Message msg){
        //The service filters out illegal BT_Message objects so
        //it error checking is not required.
        int type = BT_Message.getType( ((BT_Message) msg.obj).makeBytes() );

        switch (type) {
            case BT_MessageUtility.TYPE_APP_MESSAGE :
                BT_MessageApp m = (BT_MessageApp)msg.obj;
                appMessage(m.getMacAddress(), (m.getData()));
                break;

            case BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED:
                serverSetupFinished();
                break;

            case BT_MessageUtility.TYPE_CONNECTION_CLOSED:
                BT_MessageClose m1 = (BT_MessageClose)msg.obj ;
                connectionClosed(m1.getMacAddress(), m1.getCloseCode());
                break;

            case BT_MessageUtility.TYPE_HELLO:
                //hello messages should have been handled by service and should not have been passed
                //to handler
                BT_MessageHello m2 = (BT_MessageHello)msg.obj ;
                Log.w(TAG, "should not have received message " + new String(m2.makeBytes()));
                break;

            case BT_MessageUtility.TYPE_HELLO_REPLY:
                //hello messages should have been handled by service and should not have been passed
                //to handler
                BT_MessageHelloReply m3 = (BT_MessageHelloReply) msg.obj ;
                Log.w(TAG, "should not have received message " + new String(m3.makeBytes()));
                break;

            default:
                Log.w(TAG, "unknown type " + type);

        }
    }
}
