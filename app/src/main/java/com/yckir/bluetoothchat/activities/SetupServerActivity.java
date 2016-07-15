package com.yckir.bluetoothchat.activities;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;
import com.yckir.bluetoothchat.R;
import com.yckir.bluetoothchat.ServerAcceptTask;
import com.yckir.bluetoothchat.recycler.RecyclerDivider;
import com.yckir.bluetoothchat.recycler.RecyclerDividerDecoration;
import com.yckir.bluetoothchat.recycler.ServerRecyclerAdapter;
import com.yckir.bluetoothchat.services.BluetoothServiceHandler;
import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.receivers.BluetoothDiscoverStateReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver;
import com.yckir.bluetoothchat.receivers.BluetoothStatusReceiver.BlueToothStatusListener;
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
        BluetoothDiscoverStateReceiver.BlueToothDiscoverStateListener,
        ServerAcceptTask.ServerEventListener, ServerRecyclerAdapter.ServerItemClickListener {

    private static final String TAG = "SetupServer";
    private static final int DISCOVERY_DURATION = 180;

    //used to ensure that a request is not sent while one is being sent
    private boolean mRequestingDiscoverable = false;

    private BluetoothAdapter mBluetoothAdapter;

    private ActionMode mActionMode;
    private MyActionModeCallback mActionCallback;

    private TextView mBlueToothName;
    private TextView mBlueAddress;
    private TextView mStatusText;
    private ProgressWheel mMessageWheel;
    private FloatingActionButton mStartFab;
    private RecyclerView mRecyclerView;
    private ServerRecyclerAdapter mRecyclerAdapter;

    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverStateReceiver mBTDStateReceiver = null;

    private ServerAcceptTask mServerTask = null;
    private BluetoothService.BluetoothBinder mBinder;
    private ServiceConnection mBluetoothConnection;
    private MyBluetoothHandler mHandler;
    private boolean mConnected;

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

        mStartFab = (FloatingActionButton)findViewById(R.id.fab);
        mStartFab.hide();
        mStartFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRecyclerAdapter.getNumSockets(ServerRecyclerAdapter.ACCEPTED) == 0) {
                    Toast.makeText(SetupServerActivity.this, "no accepted clients", Toast.LENGTH_SHORT).show();
                    return;
                }

                for(BluetoothSocket socket : mRecyclerAdapter.getSockets(ServerRecyclerAdapter.UNACCEPTED))
                    mBinder.removeSocket(socket.getRemoteDevice().getAddress(), ServiceUtility.CLOSE_KICKED_FROM_SERVER);

                mBinder.setHandler(null);
                Intent intent = new Intent(SetupServerActivity.this, ChatroomActivity.class);
                intent.putExtra(ChatroomActivity.EXTRA_SERVER, true);
                startActivity(intent);
            }
        });
        mStatusText = (TextView)findViewById(R.id.status_message);
        mBlueToothName = (TextView)findViewById(R.id.user_bluetooth_name);
        mBlueToothName.setText( mBluetoothAdapter.getName() );
        mBlueAddress = (TextView)findViewById(R.id.user_bluetooth_address);
        mBlueAddress.setText( mBluetoothAdapter.getAddress() );

        mMessageWheel = (ProgressWheel)findViewById(R.id.message_progress);
        assert mMessageWheel != null;
        mMessageWheel.stopSpinning();

        mRecyclerView = (RecyclerView)findViewById(R.id.connected_devices_recycler_view);
        mRecyclerAdapter = new ServerRecyclerAdapter(this, this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);

        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        mRecyclerView.addItemDecoration(new RecyclerDivider(px));
        mRecyclerView.addItemDecoration(new RecyclerDividerDecoration(this));

        mHandler = new MyBluetoothHandler(this);
        mBluetoothConnection = new MyBluetoothConnection();
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
        if(!mConnected) {
            Log.e(TAG, "not connected to BluetoothService when a server is found");
            return;
        }
        mRecyclerAdapter.addItem(ServerRecyclerAdapter.UNACCEPTED, clientSocket);
        mStatusText.setText(R.string.status_no_accepted_clients);
        mBinder.addSocket(clientSocket);
    }

    @Override
    public void itemClick(BluetoothSocket socket) {
        //Since recycler adapter is using this callback, it can be assumed that the socket is in the
        //adapter and thus, is either ACCEPTED or UNACCEPTED

        String address = socket.getRemoteDevice().getAddress();
        boolean connected = mRecyclerAdapter.contains(ServerRecyclerAdapter.ACCEPTED, address);
        int type = connected ? ServerRecyclerAdapter.ACCEPTED : ServerRecyclerAdapter.UNACCEPTED;

        if(mActionCallback == null)
            mActionCallback = new MyActionModeCallback(type, socket);
        else
            mActionCallback.setData(type, socket);


        if(mActionMode == null)
            mActionMode = startSupportActionMode(mActionCallback);
        else
            mActionMode.invalidate();
    }


    private class MyBluetoothConnection implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "BluetoothConnection connected" );
            mConnected = true;
            mBinder = (BluetoothService.BluetoothBinder ) service;
            mBinder.setHandler(mHandler);
            mBinder.setDeviceInfo(mBluetoothAdapter.getName(), mBluetoothAdapter.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "BluetoothConnection disconnected" );
            mConnected = false;
            mBinder = null;
        }
    }


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
            mActivity.get().mRecyclerAdapter.removeItem(macAddress);
            if(mActivity.get().mRecyclerAdapter.getNumSockets(ServerRecyclerAdapter.ACCEPTED) < 1) {
                mActivity.get().mStartFab.hide();
                mActivity.get().mStatusText.setText(R.string.status_no_accepted_clients);
            }

            //close acton mode if it is open for the disconnected device
            if(mActivity.get().mActionMode != null
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
        private String mAddress;
        private @ServerRecyclerAdapter.ITEM_TYPE int mItemType;

        /**
         * Action mode callback for when a recycler item is selected.
         *
         * @param itemType the item type of the selected socket
         * @param socket socket of the recycler item that was clicked.
         */
        public MyActionModeCallback(@ServerRecyclerAdapter.ITEM_TYPE int itemType, BluetoothSocket socket){
            mSocket = socket;
            mAddress = socket.getRemoteDevice().getAddress();
            mItemType = itemType;
        }

        /**
         * @return the mac address of the item that started the action mode.
         */
        public String getAddress(){return mAddress;}


        /**
         * sets the fields for the callback.
         *
         * @param itemType the item type of the selected socket
         * @param socket socket of the recycler item that was clicked.
         */
        public void setData(@ServerRecyclerAdapter.ITEM_TYPE int itemType, BluetoothSocket socket){
            mSocket = socket;
            mAddress = socket.getRemoteDevice().getAddress();
            mItemType = itemType;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode: " + mAddress);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.setup_server_recycler_item, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onPrepareActionMode: " + mAddress);
            mRecyclerAdapter.setSelectedItemAddress(mAddress);

            mActionMode.setTitle(mSocket.getRemoteDevice().getName());

            MenuItem item = menu.findItem(R.id.menu_swap);

            if(item == null)
                return true;

            if(mItemType == ServerRecyclerAdapter.ACCEPTED )
                item.setIcon(R.drawable.ic_person_remove_white_24dp);
            else
                item.setIcon(R.drawable.ic_person_add_white_24dp);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.v(TAG, "onActionItemClicked: " + mAddress);
            String address = mSocket.getRemoteDevice().getAddress();

            switch (item.getItemId()){
                case R.id.menu_remove:
                    mBinder.removeSocket(address, ServiceUtility.CLOSE_KICKED_FROM_SERVER);
                    mode.finish();
                    return true;
                case R.id.menu_swap:
                    //swap the location of the recycler item
                    mRecyclerAdapter.changeItemType(ServerRecyclerAdapter.ALL, mSocket);

                    //update status of ui message
                    if(mRecyclerAdapter.getNumSockets(ServerRecyclerAdapter.ACCEPTED) > 0){
                        mStartFab.show();
                        mStatusText.setText(R.string.status_accepted_clients);
                    }else {
                        mStartFab.hide();
                        mStatusText.setText(R.string.status_no_accepted_clients);

                    }
                    mode.finish();
                    return true;
            }
            return false;

        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.v(TAG, "onDestroyActionMode: " + mAddress);
            mRecyclerAdapter.removeSelectedItem();
            mActionMode = null;
        }
    }
}
