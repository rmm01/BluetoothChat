package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;


/**
 * A hello message is sent constantly to ensure that the recipient is aware and responding to us.
 * This type of message requires no extra data associated with it.
 */
public class BT_MessageHello extends BT_Message {

    /**
     * Reconstructs a BT_MessageHello object from a byte array. The byte data should derive from a
     * makeBytes() method call.
     *
     * @param byteMessage the bytes to construct a BT_MessageHello object.
     * @return the constructed BT_MessageHello object.
     */
    public static BT_MessageHello reconstruct(byte[] byteMessage){

        //check if the message is correct size
        if(byteMessage.length != BT_MessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BT_MessageUtility.LENGTH_HEADER);

        BT_Message m = BT_Message.reconstruct(byteMessage);

        //check its correct type
        if(m.getMessageType() != BT_MessageUtility.TYPE_HELLO)
            throw new IllegalArgumentException(BT_MessageUtility.TYPE_HELLO + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BT_MessageHello(m.getMacAddress());
    }


    /**
     * Constructs a message object with type BT_MessageUtility.TYPE_HELLO.
     *
     * @param macAddress the mac address of the bluetooth device that is sending the message.
     */
    public BT_MessageHello(@NonNull String macAddress) {
        super(BT_MessageUtility.TYPE_HELLO, macAddress);
    }



}
