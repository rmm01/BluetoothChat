package com.yckir.bluetoothchat.activities;


import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver.BlueToothStatusListener;
import com.yckir.bluetoothchat.recyle_adapters.BluetoothFoundAdapter;

public class SetupServerActivity extends AppCompatActivity implements BlueToothStatusListener, BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener {

    private static final int DISCOVERY_DURATION = 180;

    //used to ensure that a request is not sent while one is being sent
    private boolean mRequestingDiscoverable = false;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView mBlueToothName;
    private RecyclerView mConnectedRecyclerView;
    private RecyclerView mUnconnectedRecyclerView;
    private BluetoothFoundAdapter mConnectedAdapter;
    private BluetoothFoundAdapter mUnconnectedAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothOff();
        }

        setContentView(R.layout.activity_setup_server);

        mBlueToothName = (TextView)findViewById(R.id.bluetooth_name);
        mBlueToothName.setText( mBluetoothAdapter.getName() );

        mConnectedRecyclerView = (RecyclerView)findViewById(R.id.connected_devices_recycler_view);
        if(mConnectedRecyclerView != null)
            mConnectedRecyclerView.setHasFixedSize(true);
        mConnectedAdapter = new BluetoothFoundAdapter();
        mConnectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mConnectedRecyclerView.setAdapter(mConnectedAdapter);

        mUnconnectedRecyclerView = (RecyclerView)findViewById(R.id.unconnected_devices_recycler_view);
        if(mUnconnectedRecyclerView != null)
            mUnconnectedRecyclerView.setHasFixedSize(true);
        mUnconnectedAdapter = new BluetoothFoundAdapter();
        mUnconnectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUnconnectedRecyclerView.setAdapter(mUnconnectedAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(mBTStatusReceiver == null) {
            mBTStatusReceiver = new BluetoothStatusReceiver();
            mBTStatusReceiver.setListener(this);
            registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

            mBTDStateReceiver = new BluetoothDiscoverStateReceiver();
            mBTDStateReceiver.setListener(this);
            registerReceiver(mBTDStateReceiver, BluetoothDiscoverStateReceiver.getIntentFilter());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //recheck if bluetooth is off because we unregister listeners after onStop,
        //state could have changed while activity was in background and had no listeners
        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothOff();
        }
        makeDiscoverable();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(mBTStatusReceiver != null) {
            unregisterReceiver(mBTStatusReceiver);
            mBTStatusReceiver = null;
            unregisterReceiver(mBTDStateReceiver);
            mBTDStateReceiver = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == DISCOVERY_DURATION) {
            mRequestingDiscoverable = false;
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "server must be discoverable", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public void makeDiscoverable(){
        //corner case where turning off bluetooth also makes device undiscoverable
        //this will cause activity to finish and also request to make device discoverable
        if(!mBluetoothAdapter.isEnabled())
            return;

        Log.v("PAIR_ACTIVITY", "makeDiscoverable");
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                && !mRequestingDiscoverable) {

            mRequestingDiscoverable = true;
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERY_DURATION);
            startActivityForResult(discoverableIntent, DISCOVERY_DURATION);
        }
    }

    @Override
    public void bluetoothOff() {
        Toast.makeText(this, "bluetooth off, server has shutdown", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void bluetoothOn() {
    }

    @Override
    public void bluetoothTurningOff() {
    }

    @Override
    public void bluetoothTurningOn() {
    }

    @Override
    public void discoverable() {

    }

    @Override
    public void undiscoverable() {
         makeDiscoverable();
    }
}
