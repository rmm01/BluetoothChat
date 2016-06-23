package com.yckir.bluetoothchat.services;

import java.util.UUID;

public class ServiceUtility {

    private static final String TAG = "ServiceUtility";
    public static final String SDP_NAME = "BluetoothChat";

    //number of bytes int the message identifier
    public static final int ID_LENGTH = 4;

    //max number of bluetooth devices that a server can have communicate with
    public static final int MAX_NUM_BLUETOOTH_DEVICES = 4;

    //ids between 1000 and 1099 are reserved for service to service communication, handled in service an the
    //activity never sees.
    public static final String ID_HELLO              = "1000";
    public static final String ID_HELLO_REPLY        = "1001";

    //ids between 1200 and 1299 are used for messages from service to application and vice versa
    //used mainly to tell user of important events such as client leaving
    public static final String ID_CONNECTION_CLOSED        = "1100";
    public static final String ID_SERVER_NOT_RESPONDING    = "1101";
    public static final String ID_IO_EXCEPTION             = "1102";
    public static final String ID_APP_MESSAGE              = "1103";

    /**
     * @return the UUID for bluetooth communication
     */
    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }

    /**
     * Make a hello message to a bluetooth device. This is used to check if the bluetooth client on
     * the the other end is still their.
     *
     * <p>
     * If no response is received in 7 seconds, it can be assumed that the connection has been broken.
     */
    public static String makeHelloMessage(){
        return ID_HELLO;

    }

    /**
     * Make a reply hello message to a bluetooth device. This is used to tell the device that sent
     * the hello that you are aware of its presence. Should only be used when you get a
     * sendHelloMessage.
     */
    public static String makeReplyHelloMessage(){
        return ID_HELLO_REPLY;
    }

    /**
     * Make a message that should be parsed by the activity, rather than the service.
     *
     * @param message the message that should be sent
     * @return a ID_APP_MESSAGE header attached to the beginning of the message.
     */
    public static String makeAppMessage(String message){
        return ID_APP_MESSAGE + message;
    }

    /**
     * Make a message that says a connection has been closed.
     *
     * @param macAddress mac address of closed connection
     * @return header and mac address
     */
    public static String makeConnectionClosedMessage(String macAddress){
        return ID_CONNECTION_CLOSED + macAddress;
    }
}
