package com.yckir.bluetoothchat.activities;

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

import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.services.BluetoothReadService;
import com.yckir.bluetoothchat.services.BluetoothWriteService;
import com.yckir.bluetoothchat.services.ReadServiceHandler;

public class ChatroomActivity extends AppCompatActivity implements ReadServiceHandler.MessageListener {

    private static final String TAG = "ChatroomActivity";

    private TextView mTextView;
    private EditText mEditText;
    private boolean mWriteConnected;
    private boolean mReadConnected;
    private BluetoothReadService.ReadBinder mReadBinder;
    private BluetoothWriteService.WriteBinder mWriteBinder;
    private ReadServiceHandler mHandler;


    ServiceConnection mWriteConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "WriteConnection connected" );
            mWriteConnected = true;
            mWriteBinder = (BluetoothWriteService.WriteBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "WriteConnection disconnected" );
            mWriteConnected = false;
            mWriteBinder = null;
        }
    };

    ServiceConnection mReadConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "ReadConnection connected" );
            mReadConnected = true;
            mReadBinder = (BluetoothReadService.ReadBinder) service;
            mHandler = mReadBinder.getHandler();
            mReadBinder.startReading();
            mHandler.setListener(ChatroomActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mReadConnected = false;
            mReadBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                    BluetoothWriteService.sendDisplayText(ChatroomActivity.this, text);
                    mTextView.append("\n-----YOU\n" + text + "\n-----\n");

                }
            });
        }

        bindService(new Intent(this, BluetoothWriteService.class), mWriteConnection, BIND_AUTO_CREATE);
        bindService(new Intent(this, BluetoothReadService.class), mReadConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mReadConnected)
            unbindService(mReadConnection);
        if(mWriteConnected)
            unbindService(mWriteConnection);

        //on onServiceDisconnected may not be called since we are disconnecting gracefully.
        mReadConnected = false;
        mWriteConnected = false;
        mReadBinder = null;
        mWriteBinder = null;
    }

    @Override
    public void newDisplayText(String message) {
        mTextView.append("\n-----THEM\n" + message + "\n-----\n");
    }
}
