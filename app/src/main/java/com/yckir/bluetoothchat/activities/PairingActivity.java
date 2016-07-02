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
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class PairingActivity extends AppCompatActivity implements BluetoothStatusReceiver.BlueToothStatusListener,
        BluetoothDiscoverReceiver.BlueToothDiscoverListener, ClientConnectTask.ClientEventListener,
        PairingRecyclerAdapter.PairingItemClickListener {

    private static final String TAG = "PairingActivity";

    @IntDef({DEFAULT, FINDING_EMPTY, FINDING_NOT_EMPTY, FOUND_DEVICES, CONNECTING, CONNECTED})
    @Retention(RetentionPolicy.SOURCE)
    /**
     * The states that the activity can be in. The state can be an indication to what is
     * happening in the activity.
     */
    public @interface PAIRING_STATE {}

    /**
     * The starting state of the activity. The user can click the fab to transition into the
     * FINDING_EMPTY state.
     */
    public static final int DEFAULT             = 0;
    /**
     * The activity is searching for bluetooth devices. No bluetooth devices have been found so far.
     * If none are found, goes back to DEFAULT state. If at least one is found, the activity goes
     * into the FINDING_EMPTY state.
     */
    public static final int FINDING_EMPTY       = 1;
    /**
     * The activity is searching for bluetooth devices. At least one bluetooth device has been
     * found so far. The user can click on a device to start a connection and go to connecting state.
     */
    public static final int FINDING_NOT_EMPTY   = 2;
    /**
     * The activity has finished searching for bluetooth devices. At least one bluetooth device
     * has been found. The user can click the fab to transition to FINDING_NOT_EMPTY or
     * FINDING_EMPTY state. The user can click on a device to start a connection and go to
     * connecting state.
     */
    public static final int FOUND_DEVICES       = 3;
    /**
     * The user is connecting to a bluetooth device. If connection fails, the activity goes to
     * FoundDevices state. If connection is successful, then goes into connected state.
     */
    public static final int CONNECTING          = 4;
    /**
     * the connection has been made to a remote bluetooth device. The user is waiting for host to
     * finish setting up. host will tell client that it can move on to content activity. If the
     * connection is closed for any reason, goes to FOUND_DEVICES state.
     */
    public static final int CONNECTED           = 5;

    private TextView mMessageText;
    private TextView mActionText1;
    private TextView mActionText2;
    private Button mCancelConnectionButton;
    private FloatingActionButton mFab;
    private ProgressWheel mMessageWheel;
    private ProgressWheel mConnectionWheel;
    private RecyclerView mRecyclerView;
    private PairingRecyclerAdapter mAdapter;
    private ViewGroup mConnectionViewGroup;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothStatusReceiver mBTStatusReceiver = null;
    private BluetoothDiscoverReceiver mBTDReceiver = null;

    private ClientConnectTask mClientTask;
    private MyBluetoothHandler mHandler;
    private BluetoothService.BluetoothBinder mBinder;
    private boolean mServiceConnected;
    public @PAIRING_STATE int mState = DEFAULT;

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
            mActivity.get().changeState(FOUND_DEVICES);
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
            mServiceConnected = true;
            mBinder = (BluetoothService.BluetoothBinder ) service;
            mBinder.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "bluetoothConnection disconnected" );
            mServiceConnected = false;
            mBinder = null;
        }
    };

    /**
     * reset the state to the initial state.
     */
    private void resetState(){
        mState = DEFAULT;
        mMessageText.setText("");
        mActionText1.setText(R.string.pairing_action_click_fab);
        mActionText2.setText("");
    }

    /**
     * Changes the current state of the activity, illegal exception thrown if the current state
     * cannot transition to new state.See PAIRING_STATE variables for more details
     *
     * @param state the state to change to
     */
    private void changeState(@PAIRING_STATE int state) {
        if (mState == state){
            Log.v(TAG, "going to same state " + state);
            return;
        }
        switch (state) {
            case DEFAULT:
                if(mState != FINDING_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.setText(R.string.pairing_message_search_failed);
                mActionText1.setText(R.string.pairing_action_click_fab);
                mActionText2.setText("");
                break;
            case FINDING_EMPTY:
                if(mState != DEFAULT && mState != FOUND_DEVICES)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.setText(R.string.pairing_message_searching);
                mActionText1.setText("");
                mActionText2.setText("");
                break;
            case FINDING_NOT_EMPTY:
                if(mState != FINDING_EMPTY && mState != DEFAULT && mState != FOUND_DEVICES)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.setText(R.string.pairing_message_searching);
                mActionText1.setText(R.string.pairing_action_click_btd);
                mActionText2.setText("");
                break;
            case FOUND_DEVICES:
                if(mState != CONNECTING && mState != CONNECTED && mState!= FINDING_NOT_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                if(mState == FINDING_NOT_EMPTY)
                    mMessageText.setText(R.string.pairing_message_search_finished);
                else if(mState == CONNECTING)
                    mMessageText.setText(R.string.pairing_message_connect_failed);
                else
                    mMessageText.setText(R.string.pairing_message_connection_closed);
                mActionText1.setText(R.string.pairing_action_click_btd);
                mActionText2.setText(R.string.pairing_action_click_fab);
                break;
            case CONNECTING:
                if(mState != FOUND_DEVICES && mState != FINDING_NOT_EMPTY)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.setText(R.string.pairing_message_connecting);
                mActionText1.setText("");
                mActionText2.setText("");
                break;
            case CONNECTED:
                if(mState != CONNECTING)
                    throw new IllegalArgumentException("cannot go from state" + mState + " to state " + state);
                mMessageText.setText(R.string.pairing_message_connected);
                mActionText1.setText(R.string.pairing_action_waiting);
                mActionText2.setText("");
                break;
            default:
                throw new IllegalArgumentException("invalid state " + state + " sent to changeState");
        }

        Log.v(TAG, "going from state " + mState + " to state " + state);
        mState = state;
    }

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
     * @param showConnection true if connection view should be visible, false if recycler view should
     *                       be visible.
     */
    private void transitionConnectionVisibility(boolean showConnection){
        if(showConnection){
            mConnectionViewGroup.setVisibility(View.VISIBLE);
            mRecyclerView.setVisibility(View.INVISIBLE);
            mFab.hide();
        }else{
            mConnectionViewGroup.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            if(mBluetoothAdapter.isDiscovering())
                mFab.hide();
            else
                mFab.show();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if( !mBluetoothAdapter.isEnabled() ) {
            bluetoothStateChanged(BluetoothStatusReceiver.BLUETOOTH_OFF);
        }

        TextView t = ((TextView)findViewById(R.id.client_bluetooth_name));
        if (t != null)
            t.setText( mBluetoothAdapter.getName() );

        t = (TextView)findViewById(R.id.client_bluetooth_address);
        if (t != null)
            t.setText( mBluetoothAdapter.getAddress() );

        mFab = (FloatingActionButton)findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFab.hide();
                if(mBluetoothAdapter.isDiscovering())
                    return;
                mAdapter.clearData();
                mBluetoothAdapter.startDiscovery();

            }
        });

        mMessageText = (TextView)findViewById(R.id.status_message);
        mActionText1 = (TextView)findViewById(R.id.status_action1);
        mActionText2 = (TextView)findViewById(R.id.status_action2);

        mCancelConnectionButton = (Button) findViewById(R.id.cancel_button);
        mMessageWheel = (ProgressWheel)findViewById(R.id.message_progress);
        mConnectionWheel = (ProgressWheel)findViewById(R.id.connected_progress);
        mConnectionViewGroup = (ViewGroup) findViewById(R.id.pairing_connection);

        mRecyclerView = (RecyclerView) findViewById(R.id.found_devices_recycler_view);
        if(mRecyclerView != null)
            mRecyclerView.setHasFixedSize(true);
        mAdapter = new PairingRecyclerAdapter();
        mAdapter.setPairingItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set default state
        mMessageWheel.stopSpinning();
        mConnectionWheel.stopSpinning();
        mHandler = new MyBluetoothHandler(this);
        resetState();

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent ,mBluetoothConnection ,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //set the spinner progress bar to the correct state if the activity is coming from foreground
        if(mBluetoothAdapter.isDiscovering())
            mMessageWheel.spin();
        else
            mMessageWheel.stopSpinning();

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
        if(mServiceConnected)
            unbindService(mBluetoothConnection);

        //on onServiceDisconnected may not be called since we are disconnecting gracefully.
        mServiceConnected = false;
        mBinder = null;
    }

    public void cancelConnection(View v){
        //transition connection and changing state will be done in handler
        mBinder.removeSockets(ServiceUtility.CLOSE_SAY_GOODBYE);
    }

    @Override
    public void bluetoothStateChanged(@BluetoothStatusReceiver.BLUETOOTH_STATE int state){
        switch (state) {
            case BluetoothStatusReceiver.BLUETOOTH_ON:
                Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_SHORT).show();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_OFF:
                Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                finish();
                break;
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_ON:
            case BluetoothStatusReceiver.BLUETOOTH_TURNING_OFF:
                finish();
                break;
        }
    }

    @Override
    public void discoveryStarted() {
        mMessageWheel.spin();
        mAdapter.addItems(getPairs());
        if(mAdapter.getItemCount() > 0)
            changeState(FINDING_NOT_EMPTY);
        else
            changeState(FINDING_EMPTY);
    }

    @Override
    public void discoveredDevice(BluetoothClass bluetoothClass, BluetoothDevice bluetoothDevice) {
        mAdapter.addItem(bluetoothDevice);
        changeState(FINDING_NOT_EMPTY);
    }

    @Override
    public void discoveryFinished() {
        mMessageWheel.stopSpinning();
        //not sure of possibility if connecting is much faster that closing discovery
        if(mState == CONNECTING || mState == CONNECTED)
            return;
        if(mAdapter.getItemCount() > 0)
            changeState(FOUND_DEVICES);
        else
            changeState(DEFAULT);
        mFab.show();
    }

    @Override
    public void itemClick(View clickedView, BluetoothDevice device) {
        changeState(CONNECTING);
        ((TextView)mConnectionViewGroup.findViewById(R.id.connected_bluetooth_name)).setText(device.getName());
        ((TextView)mConnectionViewGroup.findViewById(R.id.connected_mac_address)).setText(device.getAddress());
        mCancelConnectionButton.setEnabled(false);
        mConnectionWheel.spin();
        transitionConnectionVisibility(true);

        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mClientTask = new ClientConnectTask(device, ServiceUtility.getBTChatUUID());
        mClientTask.setListener(this);
        mClientTask.execute();
    }

    @Override
    public void serverSearchFinished(boolean found, BluetoothSocket socket) {
        mConnectionWheel.stopSpinning();
        if(found){
            if(mServiceConnected) {
                Toast.makeText(PairingActivity.this, "connected to server", Toast.LENGTH_SHORT).show();
                changeState(CONNECTED);
                mBinder.addSocket(socket);
                mCancelConnectionButton.setEnabled(true);
                return;
            }
            Log.e(TAG, "not connected to read or write service when a server is found");
        }
        Toast.makeText(PairingActivity.this, "Could not connect to server, try again", Toast.LENGTH_SHORT).show();
        changeState(FOUND_DEVICES);
        transitionConnectionVisibility(false);
    }
}
