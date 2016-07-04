package com.yckir.bluetoothchat.recycler;


import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.yckir.bluetoothchat.R;

import java.util.ArrayList;

public class BluetoothPairingAdapter extends RecyclerView.Adapter<BluetoothPairingAdapter.MyViewHolder>{

    ArrayList<BluetoothDevice> mDevices;
    private BTF_ClickListener mListener = null;

    public BluetoothPairingAdapter(){
        mDevices = new ArrayList<>(10);
    }

    public void setRecyclerItemListener( BTF_ClickListener listener){
        mListener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(mDevices.isEmpty())
            return;
        holder.mNameTextView.setText(mDevices.get(position).getName());
        holder.mAddressTextView.setText(mDevices.get(position).getAddress());
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public void clearData(){
        mDevices.clear();
        notifyDataSetChanged();
    }

    public void addItem(BluetoothDevice device){
        if(device == null)
            throw new IllegalArgumentException("parameter cannot be null");
        mDevices.add(device);
        notifyDataSetChanged();
    }

    public void updateItems(ArrayList<BluetoothDevice> devices){
        if(devices == null)
            throw new IllegalArgumentException("parameter is null");

        mDevices = devices;
        notifyDataSetChanged();
    }

    public boolean contains(String address){
        for (BluetoothDevice i: mDevices) {
            if(i.getAddress().equals(address))
                return true;
        }
        return false;
    }

    public interface BTF_ClickListener{
        void BTF_ItemClick(BluetoothDevice device);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mNameTextView;
        public TextView mAddressTextView;

        public MyViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.vh_name);
            mAddressTextView = (TextView) view.findViewById(R.id.vh_address);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mListener != null)
                mListener.BTF_ItemClick(mDevices.get(getAdapterPosition()));
        }
    }
}
