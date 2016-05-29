package com.yckir.bluetoothchat;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothPairAdapter extends RecyclerView.Adapter<BluetoothPairAdapter.PairViewHolder>{

    private ArrayList<String> mNamesList;
    private ArrayList<String> mAddressesList;

    public BluetoothPairAdapter(ArrayList<String> names, ArrayList<String> addresses){
        mNamesList = names;
        mAddressesList = addresses;
    }


    @Override
    public PairViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bluetooth_pair, parent, false);

        return new PairViewHolder(v);
    }

    @Override
    public void onBindViewHolder(PairViewHolder holder, int position) {
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

    public void updateItems(ArrayList<String> names, ArrayList<String> addresses){

        if(mNamesList.size() != names.size()) {
            mNamesList = names;
            mAddressesList = addresses;
            notifyDataSetChanged();
            return;
        }

        if(!mAddressesList.containsAll(addresses)){
            mNamesList = names;
            mAddressesList = addresses;
            notifyDataSetChanged();
        }
    }

    public void addItem(String name, String address){
        mNamesList.add(name);
        mAddressesList.add(address);
        notifyItemInserted(mNamesList.size() - 1);
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