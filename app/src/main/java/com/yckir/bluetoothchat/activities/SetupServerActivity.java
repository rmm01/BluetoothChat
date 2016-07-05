package com.yckir.bluetoothchat.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.ServerAcceptTask;
import com.yckir.bluetoothchat.services.BluetoothServiceHandler;
import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver.BlueToothStatusListener;
import com.yckir.bluetoothchat.recycler.BluetoothServerAdapter;
import com.yckir.bluetoothchat.services.BluetoothService;


import java.lang.ref.WeakReference;

/**
 * Sets up the server for the bluetooth chat. The activity will exit if bluetooth is ever off.
 * If the user ever denies a request to become discoverable, the activity will exit. The Server will
 * stop if the activity goes to onStop or no longer becomes discoverable. Has a broadcast receiver
 * for bluetooth state changes and discoverability state changes. Receivers registered and
 * unregistered in onStart and onStop.
 */
public class SetupServerActivity extends AppCompatActivity implements BlueToothStatusListener,
        BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener, ServerAcceptTask.ServerEventListener, BluetoothServerAdapter.BTF_ClickListener {

    private static final String TAG = "SetupServer";
    private static final int DISCOVERY_DURATION = 180;

    //used to ensure that a request is not sent while one is being sent
    private boolean mRequestingDiscoverable = false;

    private BluetoothAdapter mBluetoothAdapter;

    private ActionMode mActionMode;
    private MyActionModeCallback mActionCallback;
    private Button mStartButton;
    private TextView mBlueToothName;
    private TextView mBlueAddress;
    private TextView mStatusText;
    private RecyclerView mConnectedRecyclerView;
    private RecyclerView mUnconnectedRecyclerView;
    private ProgressWheel mMessageWheel;
    private BluetoothServerAdapter mConnectedAdapter;
    private BluetoothServerAdapter mUnconnectedAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;

    private ServerAcceptTask mServerTask = null;

    private BluetoothService.BluetoothBinder mBinder;

    private MyBluetoothHandler mHandler;

    private boolean mConnected;

    private ServiceConnection mBluetoothConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "BluetoothConnection connected" );
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder ) service;
            mBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "BluetoothConnection disconnected" );
            mConnected = false;
            mBinder = null;
        }
    };

    private static class MyBluetoothHandler extends BluetoothServiceHandler{

        private final WeakReference<SetupServerActivity> mActivity;

        public MyBluetoothHandler(SetupServerActivity activity){
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void serverSetupFinished() {
            //this should never be called here
            Log.w(TAG, "Should not have received a call to serverSetupFinished");
        }

        @Override
        public void connectionClosed(String macAddress, @ServiceUtility.CLOSE_CODE int closeCode) {
            mActivity.get().mUnconnectedAdapter.removeItem(macAddress);
            mActivity.get().mConnectedAdapter.removeItem(macAddress);
            if(mActivity.get().mConnectedAdapter.getItemCount() < 1) {
                mActivity.get().mStartButton.setEnabled(false);
                mActivity.get().mStatusText.setText(R.string.status_no_accepted_clients);
            }

            //close acton mode if it is open for the disconnected device
            if(mActivity.get().mActionCallback != null
                    && mActivity.get().mActionCallback.getAddress().equals(macAddress)){
                mActivity.get().mActionMode.finish();
            }

            Toast.makeText(mActivity.get(), ServiceUtility.getCloseCodeInfo(closeCode) +
                    ": disconnected from " + macAddress, Toast.LENGTH_SHORT).show();

        }

        @Override
        public void appMessage(String message) {

        }
    }

    /**
     * Receives callbacks for the action mode that appears when a recycler item is clicked. The item
     * will remain selected while action mode is visible and be unselected if the device disconnects
     * or the the action mode finishes and is being destroyed. The action will either swap which
     * recycler view it is in, or close the connection with that device.
     */
    private class MyActionModeCallback implements ActionMode.Callback{
        private BluetoothSocket mSocket;
        private View mSelectedView;
        private String mAddress;

        /**
         * Action mode callback for when a recycler item is selected.
         *
         * @param view view that is starting the action mode
         * @param socket socket of the recycler item that was clicked.
         */
        public MyActionModeCallback(View view, BluetoothSocket socket){
            mSelectedView = view;
            mSocket = socket;
            mAddress = socket.getRemoteDevice().getAddress();
        }

        /**
         * @return the mac address of the item that started the action mode.
         */
        public String getAddress(){return mAddress;}

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.setup_server_recycler_item, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mSelectedView.setSelected(true);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            String address = mSocket.getRemoteDevice().getAddress();

            switch (item.getItemId()){
                case R.id.menu_remove:
                    mBinder.removeSocket(address, ServiceUtility.CLOSE_KICKED_FROM_SERVER);
                    mode.finish();
                    return true;
                case R.id.menu_swap:
                    //swap the location of the recycler item
                    if(mUnconnectedAdapter.contains(address) ) {
                        mConnectedAdapter.addItem(mSocket.getRemoteDevice(), mSocket);
                        mUnconnectedAdapter.removeItem(address);
                    }else{
                        mConnectedAdapter.removeItem(address);
                        mUnconnectedAdapter.addItem(mSocket.getRemoteDevice(), mSocket);
                    }

                    //update status of ui message
                    if(mConnectedAdapter.getItemCount() > 0){
                        mStartButton.setEnabled(true);
                        mStatusText.setText(R.string.status_accepted_clients);
                    }else {
                        mStartButton.setEnabled(false);
                        mStatusText.setText(R.string.status_no_accepted_clients);

                    }
                    mode.finish();
                    return true;
                }
                return false;

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectedView.setSelected(false);
            mActionMode = null;
            mActionCallback = null;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStartButton = (Button)findViewById(R.id.start_button);
        mStatusText = (TextView)findViewById(R.id.status_message);
        mBlueToothName = (TextView)findViewById(R.id.user_bluetooth_name);
        mBlueAddress = (TextView)findViewById(R.id.user_bluetooth_address);
        mBlueToothName.setText( mBluetoothAdapter.getName() );
        mBlueAddress.setText( mBluetoothAdapter.getAddress() );

        mMessageWheel = (ProgressWheel)findViewById(R.id.message_progress);
        assert mMessageWheel != null;
        mMessageWheel.stopSpinning();

        mConnectedRecyclerView = (RecyclerView)findViewById(R.id.connected_devices_recycler_view);
        if(mConnectedRecyclerView != null)
            mConnectedRecyclerView.setHasFixedSize(true);
        mConnectedAdapter = new BluetoothServerAdapter();
        mConnectedAdapter.setRecyclerItemListener(this);
        mConnectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mConnectedRecyclerView.setAdapter(mConnectedAdapter);

        mUnconnectedRecyclerView = (RecyclerView)findViewById(R.id.unconnected_devices_recycler_view);
        if(mUnconnectedRecyclerView != null)
            mUnconnectedRecyclerView.setHasFixedSize(true);
        mUnconnectedAdapter = new BluetoothServerAdapter();
        mUnconnectedAdapter.setRecyclerItemListener(this);
        mUnconnectedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mUnconnectedRecyclerView.setAdapter(mUnconnectedAdapter);

        mHandler = new MyBluetoothHandler(this);

        bindService(new Intent(this, BluetoothService.class), mBluetoothConnection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");

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
        Log.v(TAG, "onResume");

        //since receivers are unregistered when activity goes into background, need to ensure that
        //the device is in the proper state

        //make sure that bluetooth is enabled
        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        //make sure that the server is started if currently discoverable
        if(mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            startServer();
        }

        //make sure that currently discoverable
        makeDiscoverable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
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
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
        if(mConnected)
            unbindService(mBluetoothConnection);

        //on onServiceDisconnected may not be called since we are disconnecting gracefully.
        mConnected = false;
        mBinder = null;
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
            mServerTask = new ServerAcceptTask(mBluetoothAdapter, ServiceUtility.getBTChatUUID(), ServiceUtility.SDP_NAME);
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
    public void bluetoothStateChanged(@BluetoothStatusReceiver.BLUETOOTH_STATE int state){
        switch (state) {
            case BluetoothStatusReceiver.BLUETOOTH_OFF:
                Toast.makeText(this, "bluetooth off, server has shutdown", Toast.LENGTH_LONG).show();
                stopServer();
                finish();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_OFF:
                break;
        }
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
        if(!mConnected) {
            Log.e(TAG, "not connected to BluetoothService when a server is found");
            return;
        }

        mStatusText.setText(R.string.status_no_accepted_clients);

        mBinder.addSocket(clientSocket);
    }

    @Override
    public void BTF_ItemClick(View selectedView, BluetoothDevice device, BluetoothSocket socket) {
        if(mActionMode!= null)
            return;

        mActionCallback = new MyActionModeCallback(selectedView, socket);
        mActionMode = startSupportActionMode(mActionCallback);
    }

    public void startChatroom(View view){

        if(mConnectedAdapter.getItemCount() == 0)
            Toast.makeText(this, "no accepted clients",Toast.LENGTH_SHORT).show();

        String address;
        for(BluetoothSocket socket : mUnconnectedAdapter.getSockets()){
            address = socket.getRemoteDevice().getAddress();
            mBinder.removeSocket(address, ServiceUtility.CLOSE_KICKED_FROM_SERVER);
        }
        mBinder.setHandler(null);
        Intent intent = new Intent(this, ChatroomActivity.class);
        intent.putExtra(ChatroomActivity.EXTRA_SERVER, true);
        startActivity(intent);
    }
}
