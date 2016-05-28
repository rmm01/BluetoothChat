package com.yckir.bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

public class PairingActivity extends AppCompatActivity {

    private TextView mBlueToothName;
    private SwitchCompat mEnableBluetooth;
    private ProgressWheel mDiscoverableWheel;
    private ProgressWheel mDiscoveringWheel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pairing_layout);

        mBlueToothName = (TextView)findViewById(R.id.bluetooth_name);
        mEnableBluetooth = (SwitchCompat)findViewById(R.id.enable_blue_tooth);
        mDiscoverableWheel = (ProgressWheel)findViewById(R.id.enable_discovery);
        mDiscoveringWheel = (ProgressWheel)findViewById(R.id.find_devices);
    }

    public void makeDiscoverable(View view){

    }

    public void findDevices(View view) {
    }
}
