package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;

/**
 * A bluetooth message object that is used to store and convert data that will be sent
 * to through bluetooth sockets. This object will always be at the start of a message. The type is
 * used to determine the structure of the remaining message and the address specifies who has
 * sent the message. Address is important because the server will forward a clients message to the
 * remaining clients.
 */
public class BT_Message {

    @BT_MessageUtility.MESSAGE_TYPE
    private final int mMessageType;
    private final String mMacAddress;


    /**
     * Checks if the length of the mac address is equal to BT_MessageUtility.LENGTH_ADDRESS.
     *
     * @param address the address to be checked
     */
    private static void checkAddressLength(String address){
        if(address.length() != BT_MessageUtility.LENGTH_ADDRESS)
            throw new IllegalArgumentException(address +" is not length " + BT_MessageUtility.LENGTH_ADDRESS);
    }


    /**
     * Checks of the message type is valid.
     *
     * @param messageType the type to be checked
     */
    private static void checkMessageType(@BT_MessageUtility.MESSAGE_TYPE int messageType){
        if(!BT_MessageUtility.isMessageType(messageType))
            throw new IllegalArgumentException(messageType + " is not a valid MESSAGE_TYPE");
    }


    /**
     * Reconstructs a bluetooth message object that was converted to byte format using the result from
     * makeBytes().
     *
     * @param byteMessage the result of a makeBytes() call.
     */
    public static BT_Message reconstruct(byte[] byteMessage){
        if(byteMessage.length != BT_MessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " must be length " + BT_MessageUtility.LENGTH_HEADER);

        String stringMessage = (new String(byteMessage, 0, byteMessage.length));

        @BT_MessageUtility.MESSAGE_TYPE int messageType =  Integer.parseInt(
                stringMessage.substring(0, BT_MessageUtility.LENGTH_ID));

        checkMessageType(messageType);
        String address = stringMessage.substring( BT_MessageUtility.LENGTH_ID, BT_MessageUtility.LENGTH_HEADER );
        return new BT_Message(messageType, address);
    }


    /**
     * Constructs a message object from a type and address
     *
     * @param messageType the type of message being created
     * @param macAddress the mac address of the bluetooth device that is sending the message.
     */
    public BT_Message(@BT_MessageUtility.MESSAGE_TYPE int messageType, @NonNull String macAddress){
        checkMessageType(messageType);
        checkAddressLength(macAddress);

        mMessageType = messageType;
        mMacAddress = macAddress;
    }


    /**
     * @return the mac address of the bluetooth device that is sending the message.
     */
    public String getMacAddress(){ return mMacAddress; }


    /**
     * @return the bluetooth message type of this object.
     */
    public @BT_MessageUtility.MESSAGE_TYPE int getMessageType(){return mMessageType;}


    /**
     * Converts the current message object into byte form. The object can be reconstructed using
     * this string by calling reconstruct(byte[]).
     *
     * @return the current message object into byte form.
     */
    public byte[] makeBytes(){
        return ( mMessageType + mMacAddress).getBytes();
    }
}
