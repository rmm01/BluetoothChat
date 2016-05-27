package com.yckir.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        mTextView.setText(R.string.bluetooth_disabled_message);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mTextView.setText(R.string.bluetooth_turning_off_message);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        mTextView.setText(R.string.bluetooth_enabled_message);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        mTextView.setText(R.string.bluetooth_turning_on_message);
                        break;
                }
            }
        }
    };

    private BluetoothAdapter mBlueToothAdapter;
    private boolean mBlueToothSupported;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.message);

        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBlueToothSupported = mBlueToothAdapter != null;

        if(!mBlueToothSupported){
            mTextView.setText(R.string.no_bluetooth);
            return;
        }

        if(mBlueToothAdapter.isEnabled()){
            mTextView.setText(R.string.bluetooth_enabled_message);
        }else{
            mTextView.setText(R.string.bluetooth_disabled_message);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!mBlueToothSupported)
            return false;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_create) {
            if(!mBlueToothAdapter.isEnabled())
                return true;
            //bluetooth is enabled, launch activity to create a chat room
        }
        if (id == R.id.action_join) {
            if(!mBlueToothAdapter.isEnabled())
                return true;
            //bluetooth is enabled, launch activity to join a chat room
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //if bluetooth is disabled then disable the buttons that require bluetooth
        if(!mBlueToothAdapter.isEnabled()){
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(false);
        }else{
            menu.getItem(0).setEnabled(true);
            menu.getItem(1).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }
}
