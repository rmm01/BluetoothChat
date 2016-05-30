package com.yckir.bluetoothchat.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;

public class MainActivity extends AppCompatActivity implements BluetoothStatusReceiver.BlueToothStatusListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener, BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBlueToothAdapter;
    private TextView mTextView;
    private BluetoothStatusReceiver mBTStatusReceiver;
    private BluetoothDiscoverReceiver mBTDiscoverReceiver;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver;


    public void setBlueToothStatus(){
        if(mBlueToothAdapter.isEnabled()){
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
        if (mBlueToothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setBlueToothStatus();

        mBTStatusReceiver = new BluetoothStatusReceiver();
        mBTStatusReceiver.setListener(this);

        mBTDiscoverReceiver = new BluetoothDiscoverReceiver();
        mBTDiscoverReceiver.setListener(this);

        mBTDStateReceiver = new BluetoothDiscoverStateReceiver();
        mBTDStateReceiver.setListener(this);

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
    protected void onStart() {
        super.onStart();

        // Register for broadcasts on BluetoothAdapter state change

        registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

        registerReceiver(mBTDStateReceiver, BluetoothDiscoverStateReceiver.getIntentFilter());

        IntentFilter[] filters = BluetoothDiscoverReceiver.getIntentFilters();
        for(IntentFilter filter: filters ){
            registerReceiver(mBTDiscoverReceiver, filter);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mBTStatusReceiver);
        unregisterReceiver(mBTDStateReceiver);
        unregisterReceiver(mBTDiscoverReceiver);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

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
        if(id == R.id.action_discover){
            mBlueToothAdapter.startDiscovery();
            //call cancelDiscovery() to stop it prematurely
        }
        if(id == R.id.action_enable_discovery){
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 20);
            startActivity(discoverableIntent);
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        //if bluetooth is disabled then disable the buttons that require bluetooth
        if(!mBlueToothAdapter.isEnabled()){
            menu.getItem(0).setEnabled(false);
            menu.getItem(1).setEnabled(false);
            menu.getItem(2).setEnabled(false);
        }else{
            menu.getItem(0).setEnabled(true);
            menu.getItem(1).setEnabled(true);
            menu.getItem(2).setEnabled(true);
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

    @Override
    public void deviceDiscovered(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice) {
        Log.v("DISCOVER", bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress());
    }

    @Override
    public void discoveryStarted() {
        Log.v("DISCOVER", "Discover started");
    }

    @Override
    public void discoveryFinished() {
        Log.v("DISCOVER", "Discover finished");
    }

    @Override
    public void discoverable() {
        Log.v("Discoverable", "is discoverable");
    }

    @Override
    public void undiscoverable() {
        Log.v("Discoverable", "is not discoverable");
    }
}
