package com.yckir.bluetoothchat;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothFoundAdapter extends RecyclerView.Adapter<BluetoothFoundAdapter.MyViewHolder>{

    private ArrayList<String> mNamesList;
    private ArrayList<String> mAddressesList;
    private BTF_ClickListener mListener = null;

    public BluetoothFoundAdapter(){
        mNamesList = new ArrayList<>(10);
        mAddressesList = new ArrayList<>(10);
    }

    public void setRecyclerItemListener( BTF_ClickListener listener){
        mListener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bluetooth_found, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(mNamesList == null || mNamesList.isEmpty())
            return;
        holder.mNameTextView.setText(mNamesList.get(position));
        holder.mAddressTextView.setText(mAddressesList.get(position));
    }

    @Override
    public int getItemCount() {
        if(mNamesList == null)
            return 0;
        return mNamesList.size();
    }

    public void clearData(){
        mNamesList.clear();
        mAddressesList.clear();
        notifyDataSetChanged();
    }

    public void addItem(String name, String address){
        mNamesList.add(name);
        mAddressesList.add(address);
        notifyDataSetChanged();
    }

    public interface BTF_ClickListener{
        void BTF_ItemClick(String name, String address);

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
                mListener.BTF_ItemClick(mNameTextView.getText().toString(), mAddressTextView.getText().toString());
        }
    }
}
