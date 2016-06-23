package com.yckir.bluetoothchat;

public class PairingSetupUtility {
    private static final String TAG = "PairingSetupUtility";

    //number of bytes int the message identifier
    public static final int ID_LENGTH = 4;

    public static final String ID_CONNECTION_READY   = "1100";
    public static final String ID_CONNECTION_DECLINE = "1101";

    /**
     * creates a message that telling client that communication can begin. This is an indicator to
     * start the activity that allows clients to communicate with each other. Should only be sent by
     * server.
     * @return the message to be sent to remote bluetooth device.
     */
    public static String makeConnectionReadyMessage(){
        return ID_CONNECTION_READY;
    }

    /**
     * Creates a message telling the client that the client has been denied connection with the
     * server. Should only be sent by server.
     *
     * @return the message to be sent to remote bluetooth device.
     */
    public static String makeConnectionDeclinedMessage(){
        return ID_CONNECTION_DECLINE;
    }
}
