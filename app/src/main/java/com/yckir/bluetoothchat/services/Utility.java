package com.yckir.bluetoothchat.services;

import java.util.UUID;

public class Utility {

    private static final String TAG = "Utility";
    public static final String SDP_NAME = "BluetoothChat";

    //number of bytes int the message identifier
    public static final int LENGTH_OF_SEND_ID = 4;

    //max number of bluetooth devices that a server can have communicate with
    public static final int MAX_NUM_BLUETOOTH_DEVICES = 4;

    //ids between 1000 and 1099 are reserved for service connection, handled in service
    public static final String ID_HELLO              = "1000";
    public static final String ID_HELLO_REPLY        = "1001";

    //ids between 1100 and 1199 are reserved for the pairing and server setup activities
    public static final String ID_CONNECTION_READY   = "1100";
    public static final String ID_CONNECTION_DECLINE = "1101";
    public static final String ID_CLIENT_INFO        = "1102";

    //ids between 1200 and 1299 are used for messages from service to application and vice versa
    //used mainly to tell user of important events such as client leaving
    public static final String ID_CONNECTION_CLOSED        = "1200";
    public static final String ID_SERVER_NOT_RESPONDING    = "1201";
    public static final String ID_IO_EXCEPTION             = "1202";
    public static final String ID_APP_MESSAGE              = "1202";

    //ids over 300 are for the application to remote application messages
    public static final String ID_SEND_DISPLAY_TEXT  = "1300";

    /**
     * @return the UUID for bluetooth communication
     */
    public static UUID getBTChatUUID(){
        byte[] b = "ajsvcrgcdfg".getBytes();
        return UUID.nameUUIDFromBytes(b);
    }

    /**
     * creates a message that telling client that communication can begin. This is an indicator to
     * start the activity that allows clients to communicate with each other. Should only be sent by
     * server.
     * @return the message to be sent to remote bluetooth device.
     */
    public static String makeConnectionReadyMessage(){
        return ID_CONNECTION_READY;
    }

    /**
     * Creates a message telling the client that the client has been denied connection with the
     * server. Should only be sent by server.
     *
     * @return the message to be sent to remote bluetooth device.
     */
    public static String makeConnectionDeclinedMessage(){
        return ID_CONNECTION_DECLINE;
    }

    /**
     * Creates a message that will be visible to all devices. A client can use this message to have
     * the server broadcast the message to all clients, including itself. A server can use this to have
     * the message sent to all clients.
     *
     * @param displayText the text that will be shown to the user.
     */
    public static String makeDisplayTextMessage(String displayText){
        return ID_SEND_DISPLAY_TEXT + displayText;

    }

    /**
     * Sends a hello message to a bluetooth device. This is used to check if the bluetooth client on
     * the the other end is still their.
     *
     * <p>
     * If no response is received in 7 seconds, it can be assumed that the connection has been broken.
     */
    public static String makeHelloMessage(){
        return ID_HELLO;

    }

    /**
     * Sends a reply hello message to a bluetooth device. This is used to tell the device that sent
     * the hello that you are aware of its presence. Should only be used when you get a
     * sendHelloMessage.
     */
    public static String makeReplyHelloMessage(){
        return ID_HELLO_REPLY;
    }
}
