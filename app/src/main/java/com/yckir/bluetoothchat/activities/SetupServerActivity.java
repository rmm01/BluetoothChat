package com.yckir.bluetoothchat.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import com.yckir.bluetoothchat.ServerAcceptTask;
import com.yckir.bluetoothchat.Utility;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver.BlueToothStatusListener;
import com.yckir.bluetoothchat.recyle_adapters.BluetoothSocketAdapter;

/**
 * Sets up the server for the bluetooth chat. The activity will exit if bluetooth is ever off.
 * If the user ever denies a request to become discoverable, the activity will exit. The Server will
 * stop if the activity goes to onStop or no longer becomes discoverable. Has a broadcast receiver
 * for bluetooth state changes and discoverability state changes. Receivers registered and
 * unregistered in onStart and onStop.
 */
public class SetupServerActivity extends AppCompatActivity implements BlueToothStatusListener,
        BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener, ServerAcceptTask.ServerEventListener, BluetoothSocketAdapter.BTF_ClickListener {

    private static final String TAG = "SetupServer";
    private static final int DISCOVERY_DURATION = 180;

    //used to ensure that a request is not sent while one is being sent
    private boolean mRequestingDiscoverable = false;

    private BluetoothAdapter mBluetoothAdapter;

    private TextView mBlueToothName;
    private RecyclerView mConnectedRecyclerView;
    private RecyclerView mUnconnectedRecyclerView;
    private BluetoothSocketAdapter mConnectedAdapter;
    private BluetoothSocketAdapter mUnconnectedAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;

    private ServerAcceptTask mServerTask = null;

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
        mConnectedAdapter = new BluetoothSocketAdapter();
        mConnectedAdapter.setRecyclerItemListener(this);
        mConnectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mConnectedRecyclerView.setAdapter(mConnectedAdapter);

        mUnconnectedRecyclerView = (RecyclerView)findViewById(R.id.unconnected_devices_recycler_view);
        if(mUnconnectedRecyclerView != null)
            mUnconnectedRecyclerView.setHasFixedSize(true);
        mUnconnectedAdapter = new BluetoothSocketAdapter();
        mUnconnectedAdapter.setRecyclerItemListener(this);
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

        //since receivers are unregistered when activity goes into background, need to ensure that
        //the device is in the proper state

        //make sure that bluetooth is enabled
        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothOff();
        }

        //make sure that the server is started if currently discoverable
        if(mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            startServer();
        }

        //make sure that currently discoverable
        makeDiscoverable();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //stop server if going into background
        stopServer();

        //unregister all receivers
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
        // request code and result code should both be equal to the duration of discovery if successful
        if(requestCode == DISCOVERY_DURATION) {
            mRequestingDiscoverable = false;
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "server must be discoverable", Toast.LENGTH_LONG).show();
                finish();
            }else if(resultCode ==  DISCOVERY_DURATION) {
                // if discovery is happening, then start the server
                startServer();
            }else{
                Log.e(TAG, "unknown result code: " + resultCode);
            }
        }else{
            Log.e(TAG, "unknown request code: " + requestCode);
        }
    }

    /**
     * make a request to make device discoverable, onActivityResult will have the result.
     */
    public void makeDiscoverable(){
        //corner case where turning off bluetooth also makes device undiscoverable
        //this will cause activity to finish and also request to make device discoverable
        if(!mBluetoothAdapter.isEnabled())
            return;


        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                && !mRequestingDiscoverable) {

            Log.v(TAG, "makeDiscoverable");

            mRequestingDiscoverable = true;
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERY_DURATION);
            startActivityForResult(discoverableIntent, DISCOVERY_DURATION);
        }
    }

    /**
     * start the server if not already running
     */
    public void startServer() {
        if(mServerTask == null) {
            Log.v(TAG, "startServer");
            mServerTask = new ServerAcceptTask(mBluetoothAdapter, Utility.getBTChatUUID(), Utility.SDP_NAME);
            mServerTask.setListener(this);
            mServerTask.execute();
        }
    }

    /**
     * stop the server if running
     */
    public void stopServer() {
        if(mServerTask != null) {
            Log.v(TAG, "stopServer");
            mServerTask.cancel(true);
            mServerTask.cancelServer();
            mServerTask = null;
        }
    }

    @Override
    public void bluetoothOff() {
        Toast.makeText(this, "bluetooth off, server has shutdown", Toast.LENGTH_LONG).show();
        stopServer();
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
         stopServer();
         makeDiscoverable();
    }

    @Override
    public void foundClient(BluetoothSocket clientSocket) {
        mUnconnectedAdapter.addItem(clientSocket.getRemoteDevice(), clientSocket);
    }

    @Override
    public void BTF_ItemClick(BluetoothDevice device, BluetoothSocket socket) {

    }
}
