package com.yckir.bluetoothchat.recycler;

import android.bluetooth.BluetoothSocket;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.yckir.bluetoothchat.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Recycler adapter that displays information about a BluetoothSocket. The Sockets are grouped into
 * two types, ACCEPTED and UNACCEPTED.
 */
public class ServerRecyclerAdapter extends RecyclerView.Adapter<ServerRecyclerAdapter.MyViewHolder>{
    private static final String TAG = "ServerRecyclerAdapter";


    /**
     * The type of of a bluetooth socket. It can either be ACCEPTED or UNACCEPTED. ALL usually
     * indicates to a method that the type does not matter. See corresponding method for more details
     * on ALL.
     */
    @IntDef({ACCEPTED, UNACCEPTED, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ITEM_TYPE {}
    public static final int ACCEPTED        = 0;
    public static final int UNACCEPTED      = 1;
    public static final int ALL             = 2;

    private ArrayList<BluetoothSocket> mAccepted;
    private ArrayList<BluetoothSocket> mUnaccepted;
    private ServerItemClickListener mListener;

    private String mSelectedItemAddress;


    /**
     * Constructor that creates a recycler adapter.
     *
     * @param listener callback listener that will be notified when a recycler item is selected
     */
    public ServerRecyclerAdapter(@NonNull ServerItemClickListener listener){
        mListener = listener;
        mAccepted = new ArrayList<>(10);
        mUnaccepted = new ArrayList<>(10);
        mSelectedItemAddress = null;
    }


    @Override
    public ServerRecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_item_setup_server, parent, false);
        return new MyViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ServerRecyclerAdapter.MyViewHolder holder, int position) {
        //if the last empty item
        if(position == mAccepted.size() + mUnaccepted.size()) {
            holder.setVisible(false);
            return;
        }
        holder.setVisible(true);

        boolean connected = position < mAccepted.size();
        if(connected){
            holder.mNameTextView.setText(mAccepted.get(position).getRemoteDevice().getName());
            holder.mAddressTextView.setText(mAccepted.get(position).getRemoteDevice().getAddress());
            holder.mStatusImageView.setImageResource(R.drawable.ic_person_black_24dp);
        }else{
            position -= mAccepted.size();
            holder.mNameTextView.setText(mUnaccepted.get(position).getRemoteDevice().getName());
            holder.mAddressTextView.setText(mUnaccepted.get(position).getRemoteDevice().getAddress());
            holder.mStatusImageView.setImageResource(R.drawable.ic_person_outline_black_24dp);
        }

        if(mSelectedItemAddress == null || !mSelectedItemAddress.equals(holder.mAddressTextView.getText()))
            holder.mExtrasImageView.setSelected(false);
        else
            holder.mExtrasImageView.setSelected(true);

    }

    /**
     * Gets the number of items in the recycler view. By default their is always an empty item as
     * the last item. this is done so that a floatingActionButton will not obscure the last element.
     *
     * @return the number of items in the recycler view, always at least 1.
     */
    @Override
    public int getItemCount() {
        return mAccepted.size() + mUnaccepted.size() + 1;
    }


    /**
     * get the number of bluetooth sockets of a given type
     *
     * @param itemType they type of bluetooth socket
     * @return the number of bluetooth sockets of a given type, -1 if invalid type given.
     */
    public int getNumSockets(@ITEM_TYPE int itemType){
        switch (itemType) {
            case ACCEPTED:
                return mAccepted.size();
            case UNACCEPTED:
                return mUnaccepted.size();
            case ALL:
                return mAccepted.size() + mUnaccepted.size();
            default:
                return -1;
        }
    }


    /**
     * Get the bluetooth sockets of a given type
     *
     * @param itemType they type of bluetooth socket
     * @return the bluetooth sockets of a given type.
     */
    public ArrayList<BluetoothSocket> getSockets(@ITEM_TYPE int itemType){
        switch (itemType) {
            case ACCEPTED:
                return mAccepted;
            case UNACCEPTED:
                return mUnaccepted;
            case ALL:
                //test what this does to other lists
                ArrayList<BluetoothSocket> list = new ArrayList<>(mAccepted);
                list.addAll(mUnaccepted);
                return list;
            default:
                return new ArrayList<>();
        }
    }


    /**
     * set an item that is selected and will be highlighted from the other elements. The
     * mExtrasImageView will be highlighted. notifyDataSetChanged will be called.
     * @param address the address of the selected view
     */
    public void setSelectedItemAddress(@NonNull String address){
        if(!contains(ALL, address)){
            Log.w(TAG, address + ": address does not exist for setSelectedItemAddress ");
            return;
        }
        mSelectedItemAddress = address;
        //cannot get holder to change the property so i am invalidating.
        notifyDataSetChanged();
    }


    /**
     * @return the address of the currently selected item.
     */
    public @Nullable String getSelectedItemAddress(){
        return mSelectedItemAddress;
    }


    /**
     * no item will be selected if this is called. notifyDataSetChanged will be called.
     */
    public void removeSelectedItem(){
        mSelectedItemAddress = null;
        //cannot get holder to change the property so i am invalidating.
        notifyDataSetChanged();
    }


    /**
     * Add a bluetooth socket of a given type. Type ALL will do nothing for this method.
     *
     * @param itemType they type of bluetooth socket
     * @param socket the bluetooth socket to add
     */
    public void addItem(@ITEM_TYPE int itemType, @NonNull BluetoothSocket socket){
        switch (itemType) {
            case ACCEPTED:
                if( !contains(ACCEPTED, socket.getRemoteDevice().getAddress()) ) {
                    mAccepted.add(socket);
                    notifyDataSetChanged();
                }
                break;
            case UNACCEPTED:
                if( !contains(UNACCEPTED, socket.getRemoteDevice().getAddress()) ) {
                    mUnaccepted.add(socket);
                    notifyDataSetChanged();
                }
                break;
            case ALL:
                break;
        }
    }


    /**
     * Add a list of bluetooth socket of a given type. Type ALL will do nothing for this method.
     *
     * @param itemType they type of bluetooth socket
     * @param sockets the bluetooth socket to add
     */
    public void addItems(@ITEM_TYPE int itemType, @NonNull ArrayList<BluetoothSocket> sockets){
        for(BluetoothSocket socket: sockets){
            addItem(itemType, socket);
        }
    }


    /**
     * changes the item type of a bluetooth socket. If the given itemType is All, then the item is
     * changed from Accepted to Unaccepted or vice versa.
     *
     * @param itemType the item type to change to.
     * @param socket the bluetooth socket that will change type.
     */
    public void changeItemType(@ITEM_TYPE int itemType, @NonNull BluetoothSocket socket){
        switch (itemType) {
            case ALL:
            case ACCEPTED:
                if(mUnaccepted.contains(socket)){
                    mUnaccepted.remove(socket);
                    mAccepted.add(socket);
                    notifyDataSetChanged();
                    break;
                }
                if(itemType == ACCEPTED)
                    break;
            case UNACCEPTED:
                if(mAccepted.contains(socket)){
                    mAccepted.remove(socket);
                    mUnaccepted.add(socket);
                    notifyDataSetChanged();
                    break;
                }
                if(itemType == UNACCEPTED)
                    break;
        }
    }


    /**
     * remove all items.
     */
    public void clearData(){
        mAccepted.clear();
        mUnaccepted.clear();
        notifyDataSetChanged();
    }


    /**
     * Checks if a bluetooth socket exists for a given type.
     *
     * @param itemType the type of bluetooth socket.
     * @param address the address of the bluetooth socket.
     * @return true if the socket exists for the given itemType. If All is the itemType, the result
     * if true if the socket exists as either ACCEPTED or UNACCEPTED.
     */
    public boolean contains(@ITEM_TYPE int itemType, String address){
        switch (itemType) {
            case ALL:
            case ACCEPTED:
                for(BluetoothSocket socket: mAccepted) {
                    if(socket.getRemoteDevice().getAddress().equals(address))
                        return true;
                }
                if(itemType == ACCEPTED)
                    break;

            case UNACCEPTED:
                for(BluetoothSocket socket: mUnaccepted) {
                    if(socket.getRemoteDevice().getAddress().equals(address))
                        return true;
                }
                if(itemType == UNACCEPTED)
                    break;
        }
        return false;
    }


    /**
     * remove the bluetooth socket if it exists.
     *
     * @param socket the socket to remove
     */
    public void removeItem(BluetoothSocket socket) {
        if(mAccepted.remove(socket) || mUnaccepted.remove(socket))
            notifyDataSetChanged();
    }


    /**
     * Remove the bluetooth socket with the given mac address if it exists.
     *
     * @param address the mac address of the bluetooth socket.
     */
    public void removeItem(String address){
        int i = 0;
        boolean changed = false;
        for(BluetoothSocket socket: mAccepted){
            if(socket.getRemoteDevice().getAddress().equals(address)) {
                mAccepted.remove(i);
                changed = true;
                break;
            }
            i++;
        }

        for(BluetoothSocket socket: mUnaccepted){
            if(socket.getRemoteDevice().getAddress().equals(address)) {
                mUnaccepted.remove(i);
                changed = true;
                break;
            }
            i++;
        }

        if(changed)
            notifyDataSetChanged();
    }


    /**
     * callback interface that gets called when an item is selected
     */
    public interface ServerItemClickListener{

        /**
         * Called when a recycler item is clicked
         *
         * @param socket the socket for the clicked view
         */
        void itemClick(BluetoothSocket socket);
    }


    /**
     * view holder for the recycler item. Calls ServerItemClickListener itemCLick when it is selected.
     */
    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mNameTextView;
        public TextView mAddressTextView;
        public ImageView mStatusImageView;
        public ImageView mExtrasImageView;

        /**
         * Constructs a view holder.
         *
         * @param view view of the view holder.
         */
        public MyViewHolder(View view) {
            super(view);
            mNameTextView = (TextView) view.findViewById(R.id.bluetooth_name);
            mAddressTextView = (TextView) view.findViewById(R.id.bluetooth_address);
            mStatusImageView = (ImageView) view.findViewById(R.id.status_icon);
            mExtrasImageView = (ImageView) view.findViewById(R.id.recycler_item_extras);

            mExtrasImageView.setOnClickListener(this);
        }


        /**
         * set the visibility of the recycler items.
         * @param visible true if the items should be VISIBLE, false if they should be INVISIBLE.
         */
        public void setVisible(boolean visible) {
            if (visible) {
                mNameTextView.setVisibility(View.VISIBLE);
                mAddressTextView.setVisibility(View.VISIBLE);
                mStatusImageView.setVisibility(View.VISIBLE);
                mExtrasImageView.setVisibility(View.VISIBLE);
            } else {
                mNameTextView.setVisibility(View.INVISIBLE);
                mAddressTextView.setVisibility(View.INVISIBLE);
                mStatusImageView.setVisibility(View.INVISIBLE);
                mExtrasImageView.setVisibility(View.INVISIBLE);
                mExtrasImageView.setSelected(false);
            }
        }

        @Override
        public void onClick(View v) {
            if(mListener != null){
                boolean connected = getAdapterPosition() < mAccepted.size();
                if(connected){
                    mListener.itemClick(mAccepted.get(getAdapterPosition()));
                }else{
                    mListener.itemClick(mUnaccepted.get(getAdapterPosition() - mAccepted.size()));
                }
            }
        }
    }
}
