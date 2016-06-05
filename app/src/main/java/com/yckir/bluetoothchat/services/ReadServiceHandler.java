package com.yckir.bluetoothchat.services;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yckir.bluetoothchat.Utility;

public class ReadServiceHandler extends Handler {
    private static final String TAG = "ReadServiceHandler";
    private MessageListener mListener;
    private Context mContext;

    public interface MessageListener{
        void newDisplayText(String message);
    }

    public ReadServiceHandler(Context context){
        mContext = context;
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

        String message_id = (message.substring(0, Utility.LENGTH_OF_SEND_ID));
        message = message.substring(Utility.LENGTH_OF_SEND_ID, size);

        Log.v(TAG, "size = " + size + ", messageId = " + message_id +", message = " + message);

        if(mListener == null){
            Log.e(TAG, "no listener");
            return;
        }
        switch (message_id){
            case Utility.ID_SEND_DISPLAY_TEXT:
                mListener.newDisplayText(message);
                break;
            case Utility.ID_HELLO:
                Utility.sendReplyHelloMessage(mContext);
                break;
            case Utility.ID_HELLO_REPLY:
                //TODO cancel timeout check once timeout has been implemented
                break;
            default:
                Log.v(TAG, " unknown message id");
        }

    }
}