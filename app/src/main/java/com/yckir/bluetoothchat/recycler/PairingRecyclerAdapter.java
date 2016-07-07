package com.yckir.bluetoothchat.recycler;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yckir.bluetoothchat.R;

import java.util.ArrayList;

public class PairingRecyclerAdapter extends RecyclerView.Adapter<PairingRecyclerAdapter.MyViewHolder> {

    private PairingItemClickListener mListener;
    private ArrayList<BluetoothDevice> mDevices;
    private Context mContext;
    public PairingRecyclerAdapter(Context context){
        mContext = context;
        mDevices = new ArrayList<>(10);
    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item_pairing, parent, false);

        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if(position == mDevices.size()){
            holder.mConnectionImageView.setVisibility(View.INVISIBLE);
        }else {
            holder.mConnectionImageView.setVisibility(View.VISIBLE);
            holder.mNameTextView.setText(mDevices.get(position).getName());
            holder.mAddressTextView.setText(mDevices.get(position).getAddress());
        }
    }


    /**
     * Gets the number of items in the recycler view. By default their is always an empty item as
     * the last item. this is done so that a floatingActionButton will not obscure the last element.
     *
     * @return the number of items in the recycler view, always at least 1.
     */
    @Override
    public int getItemCount() {
        return mDevices.size() + 1;
    }

    public void addItem(BluetoothDevice device){
        if(device == null)
            throw new IllegalArgumentException("addItem parameter cannot be null");
        if( !contains(device.getAddress()) ) {
            mDevices.add(device);
            notifyDataSetChanged();
        }
    }

    public void addItems(ArrayList<BluetoothDevice> devices){
        if(devices == null)
            throw new IllegalArgumentException("parameter is null");
        for(BluetoothDevice device: devices){
            addItem(device);
        }
    }

    public boolean contains(String address){
        for (BluetoothDevice i: mDevices) {
            if(i.getAddress().equals(address))
                return true;
        }
        return false;
    }

    public void clearData(){
        mDevices.clear();
        notifyDataSetChanged();
    }

    public void setPairingItemClickListener(PairingItemClickListener listener){
        mListener = listener;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView mNameTextView;
        public TextView mAddressTextView;
        public ImageView mConnectionImageView;

        public MyViewHolder(View itemView) {
            super(itemView);
            mNameTextView = (TextView) itemView.findViewById(R.id.bluetooth_name);
            mAddressTextView = (TextView) itemView.findViewById(R.id.mac_address);
            mConnectionImageView = (ImageView) itemView.findViewById(R.id.connection_icon);

            Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/Roboto-Regular.ttf");
            mNameTextView.setTypeface(typeface);
            mAddressTextView.setTypeface(typeface);
            mConnectionImageView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mListener != null){
                mListener.itemClick(v,mDevices.get(getAdapterPosition()));
            }
        }
    }

    public interface PairingItemClickListener{
        void itemClick(View clickedView, BluetoothDevice device);
    }
}
