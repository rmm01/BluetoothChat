package com.yckir.bluetoothchat.services;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public class ServiceUtility {

    private static final String TAG = "ServiceUtility";
    public static final String SDP_NAME = "BluetoothChat";

    //number of bytes int the message identifier
    public static final int LENGTH_ID = 4;

    //max number of bluetooth devices that a server can have communicate with
    public static final int MAX_NUM_BLUETOOTH_DEVICES = 4;

    @IntDef({CLOSE_SERVER_NOT_RESPONDING, CLOSE_READ_CLOSE, CLOSE_WRITE_CLOSE,
            CLOSE_SERVICE_DESTROYED, CLOSE_KICKED_FROM_SERVER, CLOSE_SAY_GOODBYE, CLOSE_GET_GOODBYE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CLOSE_CODE {}
    public static final int CLOSE_SERVER_NOT_RESPONDING    = 101;
    public static final int CLOSE_READ_CLOSE               = 102;
    public static final int CLOSE_WRITE_CLOSE              = 103;
    public static final int CLOSE_SERVICE_DESTROYED        = 104;
    public static final int CLOSE_KICKED_FROM_SERVER       = 105;
    public static final int CLOSE_SAY_GOODBYE              = 106;
    public static final int CLOSE_GET_GOODBYE              = 107;

    //ids between 1000 and 1099 are reserved for service to service communication, handled in service an the
    //activity never sees.
    public static final String ID_HELLO                    = "1000";
    public static final String ID_HELLO_REPLY              = "1001";

    //ids between 1100 and 1199 are reserved for service to activity messages
    //used mainly to tell user of important events such as client leaving
    public static final String ID_CONNECTION_CLOSED        = "1100";
    public static final String ID_SERVER_SETUP_FINISHED    = "1101";
    public static final String ID_APP_MESSAGE              = "1102";

    /**
     * @param closeCode id for why connection is being closed
     * @return message detailing why connection is being closed
     */
    public static String getCloseCodeInfo(@CLOSE_CODE int closeCode){
        switch (closeCode){
            case ServiceUtility.CLOSE_SERVER_NOT_RESPONDING:
                return "ID_SERVER_NOT_RESPONDING";
            case ServiceUtility.CLOSE_READ_CLOSE:
                return "ID_READ_CLOSE";
            case ServiceUtility.CLOSE_WRITE_CLOSE:
                return "ID_WRITE_CLOSE";
            case ServiceUtility.CLOSE_SERVICE_DESTROYED:
                return "ID_SERVICE_DESTROYED";
            case ServiceUtility.CLOSE_KICKED_FROM_SERVER:
                return "ID_KICKED_FROM_SERVER";
            case ServiceUtility.CLOSE_SAY_GOODBYE:
                return "CLOSE_SAY_GOODBYE";
            case ServiceUtility.CLOSE_GET_GOODBYE:
                return "CLOSE_GET_GOODBYE";
            default:
                return "UNKNOWN ERROR CODE " + closeCode;
        }
    }

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
     * Makes a message that tells the clients that the Server has finished setting up and is  in
     * the chatroom. PairingActivity. Upon receiving this message the client will then start the
     * ChatroomActivity. This message should only be used by the server.
     *
     * @return  message that says that the server has finished setting up.
     */
    public static String makeServerSetupFinishedMessage(){
        return ID_SERVER_SETUP_FINISHED;
    }

    /**
     * Make a message that says a connection has been closed.
     *
     * @param closeCode details on why the connection is being closed.
     * @return message to be sent to remote bluetooth device
     */
    public static String makeCloseMessage(@CLOSE_CODE int closeCode){
        return ID_CONNECTION_CLOSED + Integer.toString(closeCode);
    }
}
