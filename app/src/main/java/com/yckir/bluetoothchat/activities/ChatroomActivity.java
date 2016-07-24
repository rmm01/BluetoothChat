package com.yckir.bluetoothchat.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yckir.bluetoothchat.ChatroomUtility;

import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.services.BluetoothServiceHandler;
import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.services.BluetoothService;

import java.lang.ref.WeakReference;

/**
 * It is the job of the server to send a makeServerSetupFinishedMessage to clients so that they
 * can start the chatroom activity also. EXTRA_SERVER intent extra field tells activity if it is a
 * server or client.
 */
public class ChatroomActivity extends AppCompatActivity {

    public static final String TAG = "ChatroomActivity";
    public static final String EXTRA_SERVER = "EXTRA_SERVER";

    private TextView mTextView;
    private EditText mEditText;

    private BluetoothService.BluetoothBinder mBinder;
    private MyBluetoothHandler mBT_Handler;

    private String mName;
    private String mAddress;
    private boolean mConnected;
    private boolean mIsServer;

    private static class MyBluetoothHandler extends BluetoothServiceHandler {

        private final WeakReference<ChatroomActivity> mActivity;

        public MyBluetoothHandler(ChatroomActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            //this should never be called here
            Log.w(TAG, "Should not have received a call to serverSetupFinished");
        }

        @Override
        public void connectionClosed(String macAddress, @ServiceUtility.CLOSE_CODE int closeCode) {
            Toast.makeText(mActivity.get(), ServiceUtility.getCloseCodeInfo(closeCode) +
                    ": disconnected from " + macAddress, Toast.LENGTH_SHORT).show();
            //TODO if server, tell all clients that somebody disconnected.
        }

        @Override
        public void appMessage(String address, byte[] data) {
            String msg = new String(data);
            if(data.length < ChatroomUtility.ID_LENGTH){
                Log.w(TAG, "unreadable message " + msg);
                return;
            }

            String messageId = (msg.substring(0, ChatroomUtility.ID_LENGTH));
            String messageData = "";
            if( msg .length() > ChatroomUtility.ID_LENGTH )
                messageData = msg.substring(ChatroomUtility.ID_LENGTH, msg.length());

            switch (messageId){
                case ChatroomUtility.ID_SEND_DISPLAY_TEXT:
                    mActivity.get().showMessage(messageData);
                    if(mActivity.get().mIsServer) {
                        String appMessage = ChatroomUtility.makeDisplayTextMessage(messageData);
                        mActivity.get().mBinder.writeMessage(appMessage.getBytes());
                    }
                    break;
                default:
                    Log.v(TAG, " unknown chatroom message id " + messageId + ", with message " + messageData);
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
            mBinder.setHandler(mBT_Handler);

            if(mIsServer) {
                mBinder.serverReady();
            }
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

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mName = adapter.getName();
        mAddress = adapter.getAddress();

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
                    String message = "\n-----"+mName+"\n" + text + "\n-----\n";
                    if(mConnected) {
                        String chatMessage = ChatroomUtility.makeDisplayTextMessage(message);
                        mBinder.writeMessage(chatMessage.getBytes());
                    }
                    if(mIsServer)
                        showMessage(message);
                }
            });
        }

        mBT_Handler = new MyBluetoothHandler(this);
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
        mTextView.append(message);
    }
}
