package com.yckir.bluetoothchat;

import java.util.UUID;

public class Utility {
    //number of bytes int the message
    public static final int LENGTH_OF_SEND_ID = 4;

    //ids of the types of messages being sent
    public static final String ID_SEND_DISPLAY_TEXT = "0000";
    public static final String ID_SEND_CLOSE        = "0001";

    //send actions for intents
    public static final String ACTION_SEND_DISPLAY_TEXT = "ACTION_SEND_DISPLAY_TEXT";

    //extra fields for send intents
    public static final String EXTRA_SEND_DISPLAY_TEXT  = "EXTRA_SEND_TEXT";
    public static final String EXTRA_SEND_ID  = "EXTRA_SEND_ID";

    public static final String SDP_NAME = "BluetoothChat";
    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }
}
