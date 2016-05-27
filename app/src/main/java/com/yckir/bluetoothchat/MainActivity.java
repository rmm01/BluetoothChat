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

public class MainActivity extends AppCompatActivity implements BluetoothStatusReceiver.BlueToothStatusListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBlueToothAdapter;
    private boolean mBlueToothSupported;
    private TextView mTextView;
    private BluetoothStatusReceiver mBTStatusReceiver;


    public void setBlueToothStatus(){
        if(!mBlueToothSupported){
            mTextView.setText(R.string.no_bluetooth);
        }else if(mBlueToothAdapter.isEnabled()){
            bluetoothOn();
        }else{
            bluetoothOff();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.message);

        mBlueToothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBlueToothSupported = mBlueToothAdapter != null;

        setBlueToothStatus();

        if(!mBlueToothSupported)
            return;

        mBTStatusReceiver = new BluetoothStatusReceiver();
        mBTStatusReceiver.setListener(this);

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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mBlueToothSupported)
            return;

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBTStatusReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(!mBlueToothSupported)
            return;

        unregisterReceiver(mBTStatusReceiver);
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

        if(!mBlueToothSupported)
            return super.onPrepareOptionsMenu(menu);

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

    @Override
    public void bluetoothOff() {
        mTextView.setText(R.string.bluetooth_disabled_message);
    }

    @Override
    public void bluetoothOn() {
        mTextView.setText(R.string.bluetooth_enabled_message);
    }

    @Override
    public void bluetoothTurningOff() {
        mTextView.setText(R.string.bluetooth_turning_off_message);
    }

    @Override
    public void bluetoothTurningOn() {
        mTextView.setText(R.string.bluetooth_turning_on_message);
    }
}
