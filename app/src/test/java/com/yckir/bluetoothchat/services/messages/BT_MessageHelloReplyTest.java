package com.yckir.bluetoothchat.services.messages;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class BT_MessageHelloReplyTest {

    private BT_MessageHelloReply mMessage1;
    private BT_MessageHelloReply mMessage2;
    private BT_MessageHelloReply mMessage3;
    private BT_MessageHelloReply mMessage4;
    private BT_MessageHelloReply mMessage5;

    private final String address1 = "00:00:00:00:00:00";
    private final String address2 = "aa:aa:aa:aa:aa:aa";
    private final String address3 = "--:oo:kk:yy:gg:gg";
    private final String address4 = "ppppppppppppppppp";
    private final String address5 = "44444444444444444";

    @Before
    public void setUp() throws Exception {
        mMessage1 =  new BT_MessageHelloReply(address1);
        mMessage2 =  new BT_MessageHelloReply(address2);
        mMessage3 =  new BT_MessageHelloReply(address3);
        mMessage4 =  new BT_MessageHelloReply(address4);
        mMessage5 =  new BT_MessageHelloReply(address5);
    }

    @Test
    public void testSuperGet(){
        assertEquals(mMessage1.getMacAddress(), address1);
        assertEquals(mMessage2.getMacAddress(), address2);
        assertEquals(mMessage3.getMacAddress(), address3);
        assertEquals(mMessage4.getMacAddress(), address4);
        assertEquals(mMessage5.getMacAddress(), address5);

        assertEquals(mMessage1.getMessageType(), BT_MessageUtility.TYPE_HELLO_REPLY);
        assertEquals(mMessage2.getMessageType(), BT_MessageUtility.TYPE_HELLO_REPLY);
        assertEquals(mMessage3.getMessageType(), BT_MessageUtility.TYPE_HELLO_REPLY);
        assertEquals(mMessage4.getMessageType(), BT_MessageUtility.TYPE_HELLO_REPLY);
        assertEquals(mMessage5.getMessageType(), BT_MessageUtility.TYPE_HELLO_REPLY);
    }


    @Test
    public void testReconstruct(){

        //deconstruct object1 into bytes1
        //create new object2 from bytes1
        //compare string value of object1 to object2

        String original, created;
        BT_MessageHelloReply reconstructedMessage;

        original = BT_MessageUtility.TYPE_HELLO_REPLY + address1;
        reconstructedMessage = BT_MessageHelloReply.reconstruct(mMessage1.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_HELLO_REPLY + address2;
        reconstructedMessage = BT_MessageHelloReply.reconstruct(mMessage2.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_HELLO_REPLY + address3;
        reconstructedMessage = BT_MessageHelloReply.reconstruct(mMessage3.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_HELLO_REPLY + address4;
        reconstructedMessage = BT_MessageHelloReply.reconstruct(mMessage4.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_HELLO_REPLY + address5;
        reconstructedMessage = BT_MessageHelloReply.reconstruct(mMessage5.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidType(){

        String original = BT_MessageUtility.TYPE_HELLO + address1;
        byte[] wrongType = original.getBytes();

        mMessage1 = BT_MessageHelloReply.reconstruct(wrongType);
    }


}
