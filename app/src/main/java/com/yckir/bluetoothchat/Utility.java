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

    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }

    /**
     * Send text that will be displayed as a message on the connected devices.
     * Sends an intent to the BluetoothWriteService.
     *
     * @param context app context
     * @param displayText the text to be sent using BluetoothWriteService.
     */
    public static void sendDisplayText(Context context, String displayText){
        Log.v(TAG, "startActionWrite");

        String data = Utility.ID_SEND_DISPLAY_TEXT + displayText;
        Intent intent = new Intent(context, BluetoothWriteService.class);
        intent.setAction(BluetoothWriteService.ACTION_SEND_MESSAGE);
        intent.putExtra(BluetoothWriteService.EXTRA_MESSAGE, data);

        context.startService(intent);
    }
}
