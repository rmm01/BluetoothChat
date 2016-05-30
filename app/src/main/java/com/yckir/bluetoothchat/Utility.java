package com.yckir.bluetoothchat;

import java.util.UUID;

public class Utility {


    public static final String SDP_NAME = "BluetoothChat";
    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }


}
