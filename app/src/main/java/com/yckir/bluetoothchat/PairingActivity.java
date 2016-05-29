package com.yckir.bluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;
import java.util.Set;

public class PairingActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        BluetoothStatusReceiver.BlueToothStatusListener, BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener {

    private TextView mBlueToothName;
    private TextView mDiscoverable;
    private TextView mFindDevices;
    private SwitchCompat mBluetoothSwitch;
    private ProgressWheel mDiscoverableWheel;
    private ProgressWheel mFindDevicesWheel;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;
    private BluetoothDiscoverReceiver mBTDReceiver = null;

    private RecyclerView mPairedRecyclerView;
    private BluetoothPairAdapter mPairedAdapter;
    private RecyclerView mFoundRecyclerView;
    private BluetoothFoundAdapter mFoundAdapter;

    private void updatePairs(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<String> namesList = new ArrayList<>(pairedDevices.size());
        ArrayList<String> addressesList = new ArrayList<>(pairedDevices.size());
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                namesList.add( device.getName() );
                addressesList.add( device.getAddress() );
            }
        }

        mPairedAdapter.updateItems(namesList, addressesList);
    }

    /**
     * Enables and disables the state of the fields depending on the parameter.
     * This function checks if the state of any of the fields matches the parameter.
     * This allows this function to return quickly when the state and parameter are the same.
     *
     * @param enabled true if fields should be enabled, false if they should be disabled.
     */
    private void enableBluetoothFields(boolean enabled){

        //if one of the fields is enabled and the parameter is the same, do nothing since state is already correct
        if(enabled == mDiscoverable.isEnabled())
            return;

        mDiscoverable.setEnabled(enabled);
        mFindDevices.setEnabled(enabled);

        if(enabled) {
            updatePairs();
            mPairedRecyclerView.setVisibility(View.VISIBLE);
            mFoundRecyclerView.setVisibility(View.VISIBLE);
        }else {
            mPairedRecyclerView.setVisibility(View.INVISIBLE);
            mFoundRecyclerView.setVisibility(View.INVISIBLE);
            mFoundAdapter.clearData();
            mDiscoverableWheel.stopSpinning();
            mFindDevicesWheel.stopSpinning();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pairing_layout);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBlueToothName = (TextView)findViewById(R.id.bluetooth_name);
        mDiscoverable = (TextView)findViewById(R.id.enable_discovery_label);
        mFindDevices = (TextView)findViewById(R.id.find_devices_prompt);
        mBluetoothSwitch = (SwitchCompat)findViewById(R.id.enable_blue_tooth);
        mDiscoverableWheel = (ProgressWheel)findViewById(R.id.enable_discovery);
        mFindDevicesWheel = (ProgressWheel)findViewById(R.id.find_devices);


        ArrayList<String> namesList = new ArrayList<>(10);
        ArrayList<String> addressesList = new ArrayList<>(10);
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                namesList.add( device.getName() );
                addressesList.add( device.getAddress() );
            }
        }

        mPairedRecyclerView = (RecyclerView) findViewById(R.id.paired_devices_recycler_view);

        if(mPairedRecyclerView != null)
            mPairedRecyclerView.setHasFixedSize(true);
        mPairedAdapter = new BluetoothPairAdapter(namesList, addressesList);
        mPairedRecyclerView.setAdapter(mPairedAdapter);
        mPairedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFoundRecyclerView = (RecyclerView)findViewById(R.id.found_devices_recycler_view);
        if(mFoundRecyclerView != null)
            mFoundRecyclerView.setHasFixedSize(true);
        mFoundAdapter = new BluetoothFoundAdapter();
        mFoundRecyclerView.setAdapter(mFoundAdapter);
        mFoundRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set default state
        mBlueToothName.setText( mBluetoothAdapter.getName() );
        mDiscoverableWheel.stopSpinning();
        mFindDevicesWheel.stopSpinning();
        mBluetoothSwitch.setChecked(mBluetoothAdapter.isEnabled());
        enableBluetoothFields(mBluetoothAdapter.isEnabled());

        mBluetoothSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mBluetoothSwitch.setChecked(mBluetoothAdapter.isEnabled());

        //set the spinner progress bar to the correct state if the activity is coming from foreground
        if(mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            mDiscoverableWheel.spin();
        else
            mDiscoverableWheel.stopSpinning();

        //set the spinner progress bar to the correct state if the activity is coming from foreground
        if(mBluetoothAdapter.isDiscovering())
            mFindDevicesWheel.spin();
        else
            mFindDevicesWheel.stopSpinning();

        if(mBTStatusReceiver == null) {
            mBTStatusReceiver = new BluetoothStatusReceiver();
            mBTStatusReceiver.setListener(this);
            registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

            mBTDStateReceiver = new BluetoothDiscoverStateReceiver();
            mBTDStateReceiver.setListener(this);
            registerReceiver(mBTDStateReceiver, BluetoothDiscoverStateReceiver.getIntentFilter());

            mBTDReceiver = new BluetoothDiscoverReceiver();
            mBTDReceiver.setListener(this);

            IntentFilter[] filters = BluetoothDiscoverReceiver.getIntentFilters();
            for(IntentFilter filter: filters ){
                registerReceiver(mBTDReceiver, filter);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mBTStatusReceiver != null) {
            unregisterReceiver(mBTStatusReceiver);
            mBTStatusReceiver = null;
            unregisterReceiver(mBTDStateReceiver);
            mBTDStateReceiver = null;
            unregisterReceiver(mBTDReceiver);
            mBTDReceiver = null;
        }
    }

    public void makeDiscoverable(View view){
        Log.v("PAIR_ACTIVITY", "makeDiscoverable");
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivity(discoverableIntent);
        }
    }

    public void findDevices(View view) {
        Log.v("PAIR_ACTIVITY", "findDevices");
        if(mBluetoothAdapter.isDiscovering())
            return;
        mFoundAdapter.clearData();
        updatePairs();
        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.v("PAIR_ACTIVITY", "bluetoothSwitch");

        if(isChecked){
            if(mBluetoothAdapter.isEnabled()){
                //bluetooth is already on, make sure fields are enabled
                enableBluetoothFields(true);
            }else{
                mBluetoothAdapter.enable();
                //don't enable fields since bluetooth takes time to turn on
            }
        }else{
            if(mBluetoothAdapter.isEnabled()){
                mBluetoothAdapter.disable();
                enableBluetoothFields(false);
            }else{
                //bluetooth is already disabled, make sure fields are off
                enableBluetoothFields(false);
            }
        }
    }

    @Override
    public void bluetoothOff() {
        Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
        mBluetoothSwitch.setChecked(false);
        mBluetoothSwitch.setEnabled(true);
        enableBluetoothFields(false);
    }

    @Override
    public void bluetoothOn() {
        Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
        mBluetoothSwitch.setChecked(true);
        mBluetoothSwitch.setEnabled(true);
        enableBluetoothFields(true);
    }

    @Override
    public void bluetoothTurningOff() {
        //Toast.makeText(this, "Bluetooth is turning off", Toast.LENGTH_SHORT).show();
        mBluetoothSwitch.setChecked(false);
        mBluetoothSwitch.setEnabled(true);
        enableBluetoothFields(false);
    }

    @Override
    public void bluetoothTurningOn() {
        Toast.makeText(this, "Bluetooth is turning on", Toast.LENGTH_SHORT).show();
        mBluetoothSwitch.setChecked(true);
        mBluetoothSwitch.setEnabled(false);
        enableBluetoothFields(false);
    }

    @Override
    public void discoverable() {
        mDiscoverableWheel.spin();
    }

    @Override
    public void undiscoverable() {
        mDiscoverableWheel.stopSpinning();
    }

    @Override
    public void deviceDiscovered(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice) {
       Toast.makeText(this, bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress(), Toast.LENGTH_SHORT).show();
        if(!mPairedAdapter.contains(bluetoothDevice.getAddress()))
            mFoundAdapter.addItem(bluetoothDevice.getName(), bluetoothDevice.getAddress());
    }

    @Override
    public void discoveryStarted() {
        mFindDevicesWheel.spin();
    }

    @Override
    public void discoveryFinished() {
        mFindDevicesWheel.stopSpinning();
    }
}
