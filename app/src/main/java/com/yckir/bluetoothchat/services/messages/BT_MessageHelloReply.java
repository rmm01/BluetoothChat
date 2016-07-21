package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;

/**
 * This of message is sent in response to a BT_MessageHello. No extra data is required since
 * receiving this is enough to acknowledge the connection is valid.
 */
public class BT_MessageHelloReply extends BT_Message {

    /**
     * Reconstructs a BT_MessageHelloReply object from a byte array. The byte data should derive from a
     * makeBytes() method call.
     *
     * @param byteMessage the bytes to construct a BT_MessageHelloReply object.
     * @return the constructed BT_MessageHelloReply object.
     */
    public static BT_MessageHelloReply reconstruct(byte[] byteMessage){

        //check if the message is correct size
        if(byteMessage.length != BT_MessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BT_MessageUtility.LENGTH_HEADER);

        BT_Message m = BT_Message.reconstruct(byteMessage);

        //check its correct type
        if(m.getMessageType() != BT_MessageUtility.TYPE_HELLO_REPLY)
            throw new IllegalArgumentException(BT_MessageUtility.TYPE_HELLO_REPLY + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BT_MessageHelloReply(m.getMacAddress());
    }

    /**
     * Constructs a message object with type BT_MessageUtility.TYPE_HELLO_REPLY.
     *
     * @param macAddress the mac address of the bluetooth device that is sending the message.
     */
    public BT_MessageHelloReply(@NonNull String macAddress) {
        super(BT_MessageUtility.TYPE_HELLO_REPLY, macAddress);
    }

}
