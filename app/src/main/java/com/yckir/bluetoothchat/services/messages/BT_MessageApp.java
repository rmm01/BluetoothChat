package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * This type of message is to be used by the main application that will be using the bluetooth
 * communication. They will send byte data to the service. The data will be wrapped in this class
 * and sent to the remote bluetooth device in byte form. The remote device will reconstruct the
 * BT_Message from the byte data. The remote client then gets the data with getData() method.
 */
public class BT_MessageApp extends BT_Message{

    private byte[] mMessage;

    /**
     * Reconstructs a BT_MessageApp object from a byte array. The byte data should derive from a
     * makeBytes() method call.
     *
     * @param byteMessage the bytes to construct a BT_MessageApp object.
     * @return the constructed BT_MessageApp object.
     */
    public static BT_MessageApp reconstruct(@NonNull byte[] byteMessage){
        if(byteMessage.length < BT_MessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " must be longer than the header size " + BT_MessageUtility.LENGTH_HEADER);

        BT_Message m = BT_Message.reconstruct(Arrays.copyOfRange(byteMessage, 0, BT_MessageUtility.LENGTH_HEADER));

        if(m.getMessageType() != BT_MessageUtility.TYPE_APP_MESSAGE)
            throw new IllegalArgumentException(BT_MessageUtility.TYPE_APP_MESSAGE + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BT_MessageApp(m.getMacAddress(), Arrays.copyOfRange(byteMessage, BT_MessageUtility.LENGTH_HEADER, byteMessage.length));
    }


    /**
     * Constructs a message object with type BT_MessageUtility.TYPE_APP_MESSAGE.
     *
     * @param macAddress the mac address of the bluetooth device that is sending the message.
     * @param data the data that will be sent to remote bluetooth device
     */
    public BT_MessageApp(@NonNull String macAddress,@Nullable byte[] data) {
        super(BT_MessageUtility.TYPE_APP_MESSAGE, macAddress);
        if(data == null)
            data = "".getBytes();

        mMessage = new byte[BT_MessageUtility.LENGTH_HEADER + data.length];

        System.arraycopy( super.makeBytes(), 0, mMessage, 0, BT_MessageUtility.LENGTH_HEADER);
        System.arraycopy(data, 0, mMessage, BT_MessageUtility.LENGTH_HEADER, data.length);

    }


    /**
     * @return the data that was specified in constructor.
     */
    public byte[] getData(){
        return Arrays.copyOfRange(mMessage, BT_MessageUtility.LENGTH_HEADER, mMessage.length);
    }


    /**
     * Creates a byte array of the BT_Message plus the data specified from the constructor. Call
     * reconstruct() to recreate the object.
     */
    @Override
    public byte[] makeBytes() {
        return mMessage;
    }
}
