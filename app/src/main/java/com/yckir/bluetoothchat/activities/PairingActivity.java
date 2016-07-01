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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.yckir.bluetoothchat.ChatroomUtility;
import com.yckir.bluetoothchat.ClientConnectTask;
import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.recyle_adapters.PairingRecyclerAdapter;
import com.yckir.bluetoothchat.services.BluetoothServiceHandler;
import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.services.BluetoothService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class PairingActivity extends AppCompatActivity implements BluetoothStatusReceiver.BlueToothStatusListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener, ClientConnectTask.ClientEventListener,
        PairingRecyclerAdapter.PairingItemClickListener {

    private static final String TAG = "PairingActivity";

    private ViewGroup mConnectionViewGroup;
    private TextView mStatusText;
    private Button mCancelConnectionButton;
    private ProgressWheel mFindDevicesWheel;
    private RecyclerView mRecyclerView;
    private PairingRecyclerAdapter mAdapter;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverReceiver mBTDReceiver = null;

    private ClientConnectTask mClientTask;
    private MyBluetoothHandler mHandler;
    private BluetoothService.BluetoothBinder mBinder;
    private boolean mConnected;

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
            mActivity.get().transitionConnectionVisibility(false);
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

    /**
     * @return gets a list of paired bluetooth devices
     */
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
     * Used to switch between an recycler view and the connection view.
     *
     * @param enabled true if connection view should be visible, false otherwise
     */
    private void transitionConnectionVisibility(boolean enabled){
        if(enabled){
            mConnectionViewGroup.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
        }else{
            mConnectionViewGroup.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
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

        TextView t = ((TextView)findViewById(R.id.client_bluetooth_name));
        if (t != null)
            t.setText( mBluetoothAdapter.getName() );

        t = (TextView)findViewById(R.id.client_bluetooth_address);
        if (t != null)
            t.setText( mBluetoothAdapter.getAddress() );

        mStatusText = (TextView)findViewById(R.id.status_message);
        mCancelConnectionButton = (Button) findViewById(R.id.cancel_button);
        mFindDevicesWheel = (ProgressWheel)findViewById(R.id.find_devices);
        mConnectionViewGroup = (ViewGroup) findViewById(R.id.pairing_connection);

        mRecyclerView = (RecyclerView) findViewById(R.id.found_devices_recycler_view);
        if(mRecyclerView != null)
            mRecyclerView.setHasFixedSize(true);
        mAdapter = new PairingRecyclerAdapter();
        mAdapter.setPairingItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set default state
        mFindDevicesWheel.stopSpinning();
        mHandler = new MyBluetoothHandler(this);

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent ,mBluetoothConnection ,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //set the spinner progress bar to the correct state if the activity is coming from foreground
        if(mBluetoothAdapter.isDiscovering())
            mFindDevicesWheel.spin();
        else
            mFindDevicesWheel.stopSpinning();

        if(mBTStatusReceiver == null) {
            mBTStatusReceiver = new BluetoothStatusReceiver();
            mBTStatusReceiver.setListener(this);
            registerReceiver(mBTStatusReceiver, BluetoothStatusReceiver.getIntentFilter());

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

    public void findDevices(View view) {
        Log.v("PAIR_ACTIVITY", "findDevices");
        if(mBluetoothAdapter.isDiscovering())
            return;
        mAdapter.clearData();
        mAdapter.addItems(getPairs());
        mBluetoothAdapter.startDiscovery();
    }

    public void cancelConnection(View v){
        mStatusText.setText(R.string.status_connect_canceled);
        transitionConnectionVisibility(false);
        mBinder.removeSockets(ServiceUtility.CLOSE_SAY_GOODBYE);
    }

    @Override
    public void bluetoothOff() {
        Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void bluetoothOn() {
        Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void bluetoothTurningOff() {
        finish();
    }

    @Override
    public void bluetoothTurningOn() {
        finish();
    }

    @Override
    public void deviceDiscovered(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice) {
       Toast.makeText(this, bluetoothDevice.getName() + ", " + bluetoothDevice.getAddress(), Toast.LENGTH_SHORT).show();
        mAdapter.addItem(bluetoothDevice);
    }

    @Override
    public void discoveryStarted() {
        mFindDevicesWheel.spin();
        mStatusText.setText(R.string.status_finding);
    }

    @Override
    public void discoveryFinished() {
        mFindDevicesWheel.stopSpinning();
        if(mAdapter.getItemCount() > 0)
            mStatusText.setText(R.string.status_found);
        else
            mStatusText.setText(R.string.status_not_found);
    }

    @Override
    public void itemClick(View clickedView, BluetoothDevice device) {
        Toast.makeText(this, "Attempting to connect with " +device.getName() + ", " + device.getAddress(), Toast.LENGTH_SHORT).show();
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mStatusText.setText(R.string.status_connecting);
        mClientTask = new ClientConnectTask(device, ServiceUtility.getBTChatUUID());
        mClientTask.setListener(this);
        mClientTask.execute();
        ((TextView)mConnectionViewGroup.findViewById(R.id.connected_bluetooth_name)).setText(device.getName());
        ((TextView)mConnectionViewGroup.findViewById(R.id.connected_mac_address)).setText(device.getAddress());
        mCancelConnectionButton.setEnabled(false);
        transitionConnectionVisibility(true);
    }

    @Override
    public void serverSearchFinished(boolean found, BluetoothSocket socket) {
        if(found){
            if(mConnected) {
                Toast.makeText(PairingActivity.this, "connected to server", Toast.LENGTH_SHORT).show();
                mStatusText.setText(R.string.status_connected);
                mBinder.addSocket(socket);
                mCancelConnectionButton.setEnabled(true);
                return;
            }
            Log.e(TAG, "not connected to read or write service when a server is found");
        }
        Toast.makeText(PairingActivity.this, "Could not connect to server, try again", Toast.LENGTH_SHORT).show();
        mStatusText.setText(R.string.status_connect_failed);
        transitionConnectionVisibility(false);
    }
}
