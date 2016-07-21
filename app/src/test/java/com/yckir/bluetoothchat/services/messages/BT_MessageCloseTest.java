package com.yckir.bluetoothchat.services.messages;


import com.yckir.bluetoothchat.services.ServiceUtility;
import com.yckir.bluetoothchat.services.messages.BT_Message;
import com.yckir.bluetoothchat.services.messages.BT_MessageClose;
import com.yckir.bluetoothchat.services.messages.BT_MessageUtility;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BT_MessageCloseTest {

    private BT_MessageClose mMessage1;
    private BT_MessageClose mMessage2;
    private BT_MessageClose mMessage3;
    private BT_MessageClose mMessage4;
    private BT_MessageClose mMessage5;

    private final String address1 = "00:00:00:00:00:00";
    private final String address2 = "aa:aa:aa:aa:aa:aa";
    private final String address3 = "--:oo:kk:yy:gg:gg";
    private final String address4 = "ppppppppppppppppp";
    private final String address5 = "44444444444444444";

    private final int code1 = ServiceUtility.CLOSE_GET_GOODBYE;
    private final int code2 = ServiceUtility.CLOSE_KICKED_FROM_SERVER;
    private final int code3 = ServiceUtility.CLOSE_READ_CLOSE;
    private final int code4 = ServiceUtility.CLOSE_SAY_GOODBYE;
    private final int code5 = ServiceUtility.CLOSE_SERVER_NOT_RESPONDING;

    @Before
    public void setUp() throws Exception {
        mMessage1 =  new BT_MessageClose(address1, code1);
        mMessage2 =  new BT_MessageClose(address2, code2);
        mMessage3 =  new BT_MessageClose(address3, code3);
        mMessage4 =  new BT_MessageClose(address4, code4);
        mMessage5 =  new BT_MessageClose(address5, code5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidAddressShort(){
        mMessage1 = new BT_MessageClose("0", code1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidAddressLong(){
        mMessage1 = new BT_MessageClose("01234567890123456789", code1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidCode(){
        @ServiceUtility.CLOSE_CODE int fakeCode = -1;
        mMessage1 = new BT_MessageClose( address1, fakeCode );
    }

    @Test
    public void testSuperGet(){
        assertEquals(mMessage1.getMacAddress(), address1);
        assertEquals(mMessage2.getMacAddress(), address2);
        assertEquals(mMessage3.getMacAddress(), address3);
        assertEquals(mMessage4.getMacAddress(), address4);
        assertEquals(mMessage5.getMacAddress(), address5);

        assertEquals(mMessage1.getMessageType(), BT_MessageUtility.TYPE_CONNECTION_CLOSED);
        assertEquals(mMessage2.getMessageType(), BT_MessageUtility.TYPE_CONNECTION_CLOSED);
        assertEquals(mMessage3.getMessageType(), BT_MessageUtility.TYPE_CONNECTION_CLOSED);
        assertEquals(mMessage4.getMessageType(), BT_MessageUtility.TYPE_CONNECTION_CLOSED);
        assertEquals(mMessage5.getMessageType(), BT_MessageUtility.TYPE_CONNECTION_CLOSED);
    }

    @Test
    public void testGetCloseCode(){
        assertEquals(mMessage1.getCloseCode(), code1);
        assertEquals(mMessage2.getCloseCode(), code2);
        assertEquals(mMessage3.getCloseCode(), code3);
        assertEquals(mMessage4.getCloseCode(), code4);
        assertEquals(mMessage5.getCloseCode(), code5);
    }

    @Test
    public void testMakeBytes(){
        //deconstruct object1 into bytes1
        //create new stringA from bytes1
        //compare string value of object1 to stringA

        String original, created;

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address1 + code1;
        created = new String(mMessage1.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address2 + code2;
        created = new String(mMessage2.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address3 + code3;
        created = new String(mMessage3.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address4 + code4;
        created = new String(mMessage4.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address5 + code5;
        created = new String(mMessage5.makeBytes());
        assertEquals(original, created);
    }

    @Test
    public void testReconstruct(){

        //deconstruct object1 into bytes1
        //create new object2 from bytes1
        //compare string value of object1 to object2

        String original, created;
        BT_MessageClose reconstructedMessage;

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address1 + code1;
        reconstructedMessage = BT_MessageClose.reconstruct(mMessage1.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address2 + code2;
        reconstructedMessage = BT_MessageClose.reconstruct(mMessage2.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address3 + code3;
        reconstructedMessage = BT_MessageClose.reconstruct(mMessage3.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address4 + code4;
        reconstructedMessage = BT_MessageClose.reconstruct(mMessage4.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_CONNECTION_CLOSED + address5 + code5;
        reconstructedMessage = BT_MessageClose.reconstruct(mMessage5.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidType(){

        String original = BT_MessageUtility.TYPE_HELLO + address1 + code1;
        byte[] wrongType = original.getBytes();

        mMessage1 = BT_MessageClose.reconstruct(wrongType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidClose(){

        String original = BT_MessageUtility.TYPE_HELLO + address1 + -1;
        byte[] wrongType = original.getBytes();

        mMessage1 = BT_MessageClose.reconstruct(wrongType);
    }
}
