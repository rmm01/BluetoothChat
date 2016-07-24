package com.yckir.bluetoothchat.services;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public class ServiceUtility {

    private static final String TAG = "ServiceUtility";
    public static final String SDP_NAME = "BluetoothChat";
    public static final int LENGTH_CLOSE_CODE = 3;

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


    /**
     * checks if the parameter is a valid close code. Necessary because
     * an int parsed from a byte array can not be safely cast to
     * CLOSE_CODE.
     *
     * @param code the code to be checked.
     * @return true if is a valid close code, false otherwise.
     */
    public static boolean isCloseCode(int code){
        switch (code) {
            case CLOSE_GET_GOODBYE:
            case CLOSE_KICKED_FROM_SERVER:
            case CLOSE_READ_CLOSE:
            case CLOSE_SAY_GOODBYE:
            case CLOSE_SERVER_NOT_RESPONDING:
            case CLOSE_SERVICE_DESTROYED:
            case CLOSE_WRITE_CLOSE:
                return true;
            default:
                return false;
        }
    }

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
}
