package com.yckir.bluetoothchat.services;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yckir.bluetoothchat.Utility;

public class ReadServiceHandler extends Handler {
    private static final String TAG = "ReadServiceHandler";
    private MessageListener mListener;

    public interface MessageListener{
        void newDisplayText(String message);
    }


    public void setListener(MessageListener listener){
        Log.v(TAG, "setting listener");
        mListener = listener;
    }

    @Override
    public void handleMessage(Message msg) {

        int size = msg.arg1;
        byte[] byte_message = (byte[]) msg.obj;

        String message = new String(byte_message);

        String message_id = (message.substring(0,4));
        message = message.substring(4,size);

        Log.v(TAG, "size = " + size + ", messageId = " + message_id +", message = " + message);

        if(mListener == null){
            Log.e(TAG, "no listener");
            return;
        }
        switch (message_id){
            case Utility.ID_SEND_DISPLAY_TEXT:
                mListener.newDisplayText(message);
                break;
            default:
                Log.v(TAG, " unknown message id");
        }

    }
}