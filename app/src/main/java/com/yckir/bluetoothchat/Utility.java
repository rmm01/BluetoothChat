package com.yckir.bluetoothchat;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yckir.bluetoothchat.services.BluetoothWriteService;

import java.util.UUID;

public class Utility {

    private static final String TAG = "Utility";
    public static final String SDP_NAME = "BluetoothChat";

    //number of bytes int the message identifier
    public static final int LENGTH_OF_SEND_ID = 4;

    //ids of the types of messages being sent
    public static final String ID_SEND_DISPLAY_TEXT = "0000";
    public static final String ID_HELLO             = "0001";
    public static final String ID_HELLO_REPLY       = "0002";

    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }

    /**
     * Send text that will be displayed as a message on the connected devices.
     * Sends an intent to the BluetoothWriteService.
     * <p>
     * The data is sent using BluetoothWriteService with the action
     * BluetoothWriteService.ACTION_SEND_MESSAGE and the data is in the extra field
     * BluetoothWriteService.EXTRA_MESSAGE. The message id is ID_SEND_DISPLAY_TEXT.
     *
     * @param context app context
     * @param displayText the text that will be shown to the user.
     */
    public static void sendDisplayText(Context context, String displayText){
        Log.v(TAG, "startActionWrite");

        String data = ID_SEND_DISPLAY_TEXT + displayText;
        Intent intent = new Intent(context, BluetoothWriteService.class);
        intent.setAction(BluetoothWriteService.ACTION_SEND_MESSAGE);
        intent.putExtra(BluetoothWriteService.EXTRA_MESSAGE, data);

        context.startService(intent);
    }

    /**
     * Sends a hello message to a bluetooth device. This is used to check if the bluetooth client on
     * the the other end is still their.
     *
     * <p>
     * If no response is received in 7 seconds, it can be assumed that the connection has been broken.
     *
     * <p>
     * The data is sent using BluetoothWriteService with the action
     * BluetoothWriteService.ACTION_SEND_MESSAGE and the data is in the extra field
     * BluetoothWriteService.EXTRA_MESSAGE. The message id is ID_HELLO.
     */
    public static void sendHelloMessage(Context context){
        String data = ID_HELLO;
        Intent intent = new Intent(context, BluetoothWriteService.class);
        intent.setAction(BluetoothWriteService.ACTION_SEND_MESSAGE);
        intent.putExtra(BluetoothWriteService.EXTRA_MESSAGE, data);
    }

    /**
     * Sends a reply hello message to a bluetooth device. This is used to tell the device that sent
     * the hello that you are aware of its presence. Should only be used when you get a sendHelloMessage.
     *
     * <p>
     * The data is sent using BluetoothWriteService with the action
     * BluetoothWriteService.ACTION_SEND_MESSAGE and the data is in the extra field
     * BluetoothWriteService.EXTRA_MESSAGE. The message id is ID_HELLO.
     */
    public static void sendReplyHelloMessage(Context context){
        String data = ID_HELLO_REPLY;
        Intent intent = new Intent(context, BluetoothWriteService.class);
        intent.setAction(BluetoothWriteService.ACTION_SEND_MESSAGE);
        intent.putExtra(BluetoothWriteService.EXTRA_MESSAGE, data);
    }
}
