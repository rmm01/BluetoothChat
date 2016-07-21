package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;

import com.yckir.bluetoothchat.services.ServiceUtility;

import java.util.Arrays;

/**
 * Creates a message that says why a connection is being closed. ServiceUtility.CLOSE_CODE
 * specifies the different reasons the connection closes. This type of message can come from a
 * remote device when they want to close the connection, or from the service.
 */
public class BT_MessageClose extends BT_Message {

    private @ServiceUtility.CLOSE_CODE int mCloseCode;
    private byte[] mMessage;

    /**
     * Reconstructs a BT_MessageClose object from a byte array. The byte data should derive from a
     * makeBytes() method call.
     *
     * @param byteMessage the bytes to construct a BT_MessageClose object.
     * @return the constructed BT_MessageClose object.
     */
    public static BT_MessageClose reconstruct(byte[] byteMessage){
        //check if the message is correct size
        if(byteMessage.length != BT_MessageUtility.LENGTH_HEADER + ServiceUtility.LENGTH_CLOSE_CODE)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BT_MessageUtility.LENGTH_HEADER + ServiceUtility.LENGTH_CLOSE_CODE);

        BT_Message m = BT_Message.reconstruct(Arrays.copyOfRange(byteMessage, 0, BT_MessageUtility.LENGTH_HEADER));

        //check that the type is TYPE_CONNECTION_CLOSED
        if(m.getMessageType() != BT_MessageUtility.TYPE_CONNECTION_CLOSED)
            throw new IllegalArgumentException(BT_MessageUtility.TYPE_CONNECTION_CLOSED + " must be the " +
                    "message type, found " + m.getMessageType());

        String code =  new String(Arrays.copyOfRange(byteMessage, BT_MessageUtility.LENGTH_HEADER, byteMessage.length));
        @ServiceUtility.CLOSE_CODE int closeCode = Integer.parseInt(code);

        //check if it is a valid close code;
        if( !ServiceUtility.isCloseCode(closeCode) )
            throw new IllegalArgumentException(closeCode + " is not a valid close close");

        return new BT_MessageClose(m.getMacAddress(), closeCode );

    }


    /**
     * Constructs a message object of type BT_MessageUtility.TYPE_CONNECTION_CLOSED.
     *
     * @param macAddress  the mac address of the bluetooth device that is sending the message.
     */
    public BT_MessageClose(@NonNull String macAddress, @ServiceUtility.CLOSE_CODE int closeCode) {
        super(BT_MessageUtility.TYPE_CONNECTION_CLOSED, macAddress);

        if( !ServiceUtility.isCloseCode(closeCode) )
            throw new  IllegalArgumentException(closeCode + " is not a valid close close");

        mMessage = new byte[BT_MessageUtility.LENGTH_HEADER + ServiceUtility.LENGTH_CLOSE_CODE];

        System.arraycopy( super.makeBytes(), 0, mMessage, 0, BT_MessageUtility.LENGTH_HEADER);
        System.arraycopy((closeCode + "").getBytes(), 0, mMessage, BT_MessageUtility.LENGTH_HEADER, ServiceUtility.LENGTH_CLOSE_CODE);
        mCloseCode = closeCode;
    }


    /**
     * @return the close code of the message
     */
    public @ServiceUtility.CLOSE_CODE int getCloseCode(){
        return mCloseCode;
    }

    /**
     * Creates a byte array of the BT_Message plus the close code. Call reconstruct() to recreate
     * the object.
     */
    @Override
    public byte[] makeBytes() {
        return mMessage;
    }
}
