package com.yckir.bluetoothchat.services.messages;

import android.support.annotation.NonNull;

/**
 * This message is used to tell clients that the server has finished the setup phase. The clients
 * that receives this are the clients that the server has accepted. The clients should start the main
 * content activity that utilizes the bluetooth communication when this message is received.
 */
public class BT_MessageSetupFinished extends BT_Message{

    /**
     * Reconstructs a BT_MessageSetupFinished object from a byte array. The byte data should derive from a
     * makeBytes() method call.
     *
     * @param byteMessage the bytes to construct a BT_MessageSetupFinished object.
     * @return the constructed BT_MessageSetupFinished object.
     */
    public static BT_MessageSetupFinished reconstruct(byte[] byteMessage){

        //check if the message is correct size
        if(byteMessage.length != BT_MessageUtility.LENGTH_HEADER)
            throw new IllegalArgumentException(byteMessage + " is invalid param, must be length "
                    + BT_MessageUtility.LENGTH_HEADER);

        BT_Message m = BT_Message.reconstruct(byteMessage);

        //check its correct type
        if(m.getMessageType() != BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED)
            throw new IllegalArgumentException(BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED + " must be the " +
                    "message type, found " + m.getMessageType());

        return new BT_MessageSetupFinished(m.getMacAddress());
    }

    /**
     * Constructs a message object with type BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED.
     *
     * @param macAddress the mac address of the bluetooth device that is sending the message.
     */
    public BT_MessageSetupFinished(@NonNull String macAddress) {
        super(BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED, macAddress);
    }
}
