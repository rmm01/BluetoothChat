package com.yckir.bluetoothchat;


import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothPairAdapter extends RecyclerView.Adapter<BluetoothPairAdapter.PairViewHolder>{

    ArrayList<BluetoothDevice> mDevices;

    public BluetoothPairAdapter(ArrayList<BluetoothDevice> devices){
        if(devices == null)
            throw new IllegalArgumentException("parameter cannot be null");

        mDevices = devices;
    }


    @Override
    public PairViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bluetooth_pair, parent, false);

        return new PairViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PairViewHolder holder, int position) {
        if(mDevices.isEmpty())
            return;
        holder.mNameTextView.setText(mDevices.get(position).getName());
        holder.mAddressTextView.setText(mDevices.get(position).getAddress());
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void updateItems(ArrayList<BluetoothDevice> devices){
        if(devices == null)
            throw new IllegalArgumentException("parameter is null");

        mDevices = devices;
        notifyDataSetChanged();
    }

    public void addItem(BluetoothDevice device){
        if(device == null)
            throw new IllegalArgumentException("parameter is null");

        mDevices.add(device);
        notifyDataSetChanged();
    }

    public boolean contains(String address){
        for (BluetoothDevice i: mDevices) {
            if(i.getAddress().equals(address))
                return true;
        }
        return false;
    }

    public class PairViewHolder extends RecyclerView.ViewHolder {
        public TextView mNameTextView;
        public TextView mAddressTextView;
        public PairViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.vh_name);
            mAddressTextView = (TextView)view.findViewById(R.id.vh_address);
        }
    }
}