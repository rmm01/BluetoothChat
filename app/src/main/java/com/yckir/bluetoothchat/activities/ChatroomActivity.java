package com.yckir.bluetoothchat.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.Utility;
import com.yckir.bluetoothchat.services.BluetoothService;

import java.lang.ref.WeakReference;

public class ChatroomActivity extends AppCompatActivity {

    public static final String TAG = "ChatroomActivity";
    public static final String EXTRA_SERVER = "EXTRA_SERVER";

    private TextView mTextView;
    private EditText mEditText;

    private BluetoothService.BluetoothBinder mBinder;
    private MyReadHandler mHandler;

    private boolean mConnected;
    private boolean mIsServer;

    private static class MyReadHandler extends Handler {

        private final WeakReference<ChatroomActivity> mActivity;
        private final boolean mServer;

        public MyReadHandler(ChatroomActivity activity, boolean isServer){
            mActivity = new WeakReference<>(activity);
            mServer = isServer;
        }

        @Override
        public void handleMessage(Message msg) {

            int size = msg.arg1;
            byte[] byte_message = (byte[]) msg.obj;

            String rawMessage = new String(byte_message);

            if(msg.what == 1){
                Toast.makeText(mActivity.get(), "disconnected from " + rawMessage, Toast.LENGTH_SHORT).show();
                return;
            }

            String message_id = (rawMessage.substring(0, Utility.LENGTH_OF_SEND_ID));
            String message = rawMessage.substring(Utility.LENGTH_OF_SEND_ID, size);

            Log.v(TAG, "size = " + size + ", messageId = " + message_id +", message = " + message);

            switch (message_id){
                case Utility.ID_SEND_DISPLAY_TEXT:
                    mActivity.get().showMessage(message);
                    if(mServer)
                        mActivity.get().mBinder.writeMessage(Utility.makeDisplayTextMessage(message));
                    break;
                case Utility.ID_HELLO:
                    mActivity.get().mBinder.writeMessage(Utility.makeReplyHelloMessage());
                    break;
                case Utility.ID_HELLO_REPLY:
                    //TODO cancel timeout check once timeout has been implemented
                    break;
                default:
                    Log.v(TAG, " unknown message id " + message_id + ", with message " + message);
                    break;
            }
        }
    }

    ServiceConnection mBluetoothConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "BluetoothConnection connected" );
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder) service;

            mBinder.setHandler(mHandler);

            if(mIsServer)
                mBinder.writeMessage(Utility.makeConnectionReadyMessage());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "BluetoothConnection disconnected" );
            mConnected = false;
            mBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mIsServer = getIntent().getBooleanExtra(EXTRA_SERVER, false);

        mTextView = (TextView) findViewById(R.id.conversation);
        mEditText = (EditText) findViewById(R.id.send_text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "sending message", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    String text = mEditText.getText().toString();
                    if(mConnected)
                        mBinder.writeMessage(Utility.makeDisplayTextMessage(text));
                    if(mIsServer)
                        mTextView.append("\n-----YOU\n" + text + "\n-----\n");
                }
            });
        }

        mHandler = new MyReadHandler(this, mIsServer);
        bindService(new Intent(this, BluetoothService.class), mBluetoothConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnected) {
            unbindService(mBluetoothConnection);
        }

        //on onServiceDisconnected may not be called since we are disconnecting gracefully.
        mConnected = false;
        mBinder = null;
    }

    /**
     * Shows a message to the user.
     *
     * @param message the message that will be displayed.
     */
    public void showMessage(String message) {
        mTextView.append("\n-----THEM\n" + message + "\n-----\n");
    }
}
