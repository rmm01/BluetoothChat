package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BT_MessageUtility {

    @IntDef(flag = true, value = {TYPE_HELLO, TYPE_HELLO_REPLY, TYPE_CONNECTION_CLOSED, TYPE_SERVER_SETUP_FINISHED, TYPE_APP_MESSAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MESSAGE_TYPE {}


    //ids between 1000 and 1099 are reserved for service to service communication, handled in service an the
    //activity never sees.
    public static final int TYPE_HELLO = 1000;
    public static final int TYPE_HELLO_REPLY = 1001;

    //ids between 1100 and 1199 are reserved for service to activity messages
    //used mainly to tell user of important events such as client leaving
    public static final int TYPE_CONNECTION_CLOSED = 1100;
    public static final int TYPE_SERVER_SETUP_FINISHED = 1101;
    public static final int TYPE_APP_MESSAGE = 1102;

    //number of bytes int the message identifier
    public static final int LENGTH_ID = 4;
    public static final int LENGTH_ADDRESS = 17;
    public static final int LENGTH_HEADER = LENGTH_ID +LENGTH_ADDRESS;

    public static boolean isMessageType(int type){
        switch (type) {
            case TYPE_APP_MESSAGE:
            case TYPE_CONNECTION_CLOSED:
            case TYPE_HELLO_REPLY:
            case TYPE_HELLO:
            case TYPE_SERVER_SETUP_FINISHED:
                return true;
            default:
                return false;
        }
    }
}
