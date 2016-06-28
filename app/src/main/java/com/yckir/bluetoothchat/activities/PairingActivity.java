package com.yckir.bluetoothchat.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.yckir.bluetoothchat.ChatroomUtility;
import com.yckir.bluetoothchat.ClientConnectTask;
import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.services.BluetoothServiceHandler;
import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.recyle_adapters.BluetoothPairingAdapter;
import com.yckir.bluetoothchat.services.BluetoothService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class PairingActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        BluetoothStatusReceiver.BlueToothStatusListener, BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener, BluetoothPairingAdapter.BTF_ClickListener, ClientConnectTask.ClientEventListener {

    private static final String TAG = "PairingActivity";

    private TextView mBlueToothName;
    private TextView mBlueToothAddress;
    private TextView mDiscoverable;
    private TextView mFindDevices;
    private TextView mStatusText;
    private Button mCancelConnectionButton;
    private SwitchCompat mBluetoothSwitch;
    private ProgressWheel mDiscoverableWheel;
    private ProgressWheel mFindDevicesWheel;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;
    private BluetoothDiscoverReceiver mBTDReceiver = null;

    private RecyclerView mPairedRecyclerView;
    private BluetoothPairingAdapter mPairedAdapter;
    private RecyclerView mFoundRecyclerView;
    private BluetoothPairingAdapter mFoundAdapter;

    private ClientConnectTask mClientTask;

    private BluetoothService.BluetoothBinder mBinder;

    private boolean mConnected;

    private MyBluetoothHandler mHandler;

    private static class MyBluetoothHandler extends BluetoothServiceHandler{

        private final WeakReference<PairingActivity> mActivity;

        public MyBluetoothHandler(PairingActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            mActivity.get().mBinder.setHandler(null);
            mActivity.get().startActivity(new Intent(mActivity.get(), ChatroomActivity.class));
        }

        @Override
        public void connectionClosed(String macAddress, @ServiceUtility.CLOSE_CODE int closeCode) {
            Toast.makeText(mActivity.get(), ServiceUtility.getCloseCodeInfo(closeCode) +
                    ": disconnected from " + macAddress, Toast.LENGTH_SHORT).show();
            mActivity.get().mStatusText.setText(R.string.status_not_finding);
            mActivity.get().enableBluetoothFields(true);
        }

        @Override
        public void appMessage(String message) {
            String message_id = (message.substring(0, ServiceUtility.ID_LENGTH));

            String messageData = null;
            if(message.length() > ChatroomUtility.ID_LENGTH)
                messageData = message.substring(ServiceUtility.ID_LENGTH, message.length());

            switch (message_id) {
                default:
                    Log.v(TAG, " unknown app message id " + message_id + ", with message " + messageData);
                    break;
            }
        }
    }

    private ServiceConnection mBluetoothConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "bluetoothConnection connected" );
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder ) service;
            mBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "bluetoothConnection disconnected" );
            mConnected = false;
            mBinder = null;
        }
    };

    private ArrayList<BluetoothDevice> getPairs(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> devices = new ArrayList<>(pairedDevices.size());
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                devices.add( device );
            }
        }
        return devices;
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
            mPairedAdapter.updateItems(getPairs());
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
        setContentView(R.layout.activity_pairing);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mBlueToothName = (TextView)findViewById(R.id.client_bluetooth_name);
        mBlueToothAddress = (TextView)findViewById(R.id.client_bluetooth_address);
        mDiscoverable = (TextView)findViewById(R.id.enable_discovery_label);
        mFindDevices = (TextView)findViewById(R.id.find_devices_prompt);
        mStatusText = (TextView)findViewById(R.id.status_message);
        mCancelConnectionButton = (Button) findViewById(R.id.cancel_button);
        mBluetoothSwitch = (SwitchCompat)findViewById(R.id.enable_blue_tooth);
        mDiscoverableWheel = (ProgressWheel)findViewById(R.id.enable_discovery);
        mFindDevicesWheel = (ProgressWheel)findViewById(R.id.find_devices);

        mPairedRecyclerView = (RecyclerView) findViewById(R.id.paired_devices_recycler_view);

        if(mPairedRecyclerView != null)
            mPairedRecyclerView.setHasFixedSize(true);
        mPairedAdapter = new BluetoothPairingAdapter();
        mPairedAdapter.updateItems(getPairs());
        mPairedAdapter.setRecyclerItemListener(this);
        mPairedRecyclerView.setAdapter(mPairedAdapter);
        mPairedRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFoundRecyclerView = (RecyclerView)findViewById(R.id.found_devices_recycler_view);
        if(mFoundRecyclerView != null)
            mFoundRecyclerView.setHasFixedSize(true);
        mFoundAdapter = new BluetoothPairingAdapter();
        mFoundAdapter.setRecyclerItemListener(this);
        mFoundRecyclerView.setAdapter(mFoundAdapter);
        mFoundRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set default state
        mBlueToothName.setText( mBluetoothAdapter.getName() );
        mBlueToothAddress.setText( mBluetoothAdapter.getAddress() );
        mDiscoverableWheel.stopSpinning();
        mFindDevicesWheel.stopSpinning();
        mBluetoothSwitch.setChecked(mBluetoothAdapter.isEnabled());
        enableBluetoothFields(mBluetoothAdapter.isEnabled());

        mBluetoothSwitch.setOnCheckedChangeListener(this);

        mHandler = new MyBluetoothHandler(this);

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent ,mBluetoothConnection ,BIND_AUTO_CREATE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnected)
            unbindService(mBluetoothConnection);

        //on onServiceDisconnected may not be called since we are disconnecting gracefully.
        mConnected = false;
        mBinder = null;
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
        mPairedAdapter.updateItems(getPairs());
        mBluetoothAdapter.startDiscovery();
    }

    public void cancelConnection(View v){
        mStatusText.setText(R.string.status_connect_canceled);
        mCancelConnectionButton.setVisibility(View.INVISIBLE);
        enableBluetoothFields(true);
        mBinder.removeSockets(ServiceUtility.CLOSE_SAY_GOODBYE);
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
            mFoundAdapter.addItem(bluetoothDevice);
    }

    @Override
    public void discoveryStarted() {
        mFindDevicesWheel.spin();
        mStatusText.setText(R.string.status_finding);
    }

    @Override
    public void discoveryFinished() {
        mFindDevicesWheel.stopSpinning();
        if(mPairedAdapter.getItemCount() + mFoundAdapter.getItemCount() > 0)
            mStatusText.setText(R.string.status_found);
        else
            mStatusText.setText(R.string.status_not_found);
    }

    @Override
    public void BTF_ItemClick(BluetoothDevice device) {
        Toast.makeText(this, "Attempting to connect with " +device.getName() + ", " + device.getAddress(), Toast.LENGTH_SHORT).show();
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mStatusText.setText(R.string.status_connecting);
        mClientTask = new ClientConnectTask(device, ServiceUtility.getBTChatUUID());
        mClientTask.setListener(this);
        mClientTask.execute();
    }

    @Override
    public void serverSearchFinished(boolean found, BluetoothSocket socket) {
        if(found){
            if(mConnected) {
                Toast.makeText(PairingActivity.this, "connected to server", Toast.LENGTH_SHORT).show();
                mStatusText.setText(R.string.status_connected);
                mBinder.addSocket(socket);
                mCancelConnectionButton.setVisibility(View.VISIBLE);
                enableBluetoothFields(false);
                return;
            }
            Log.e(TAG, "not connected to read or write service when a server is found");
        }
        Toast.makeText(PairingActivity.this, "Could not connect to server, try again", Toast.LENGTH_SHORT).show();
        mStatusText.setText(R.string.status_connect_failed);

    }
}
