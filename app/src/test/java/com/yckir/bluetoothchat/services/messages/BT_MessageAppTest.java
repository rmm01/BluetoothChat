package com.yckir.bluetoothchat.services.messages;

import com.yckir.bluetoothchat.services.messages.BT_MessageApp;
import com.yckir.bluetoothchat.services.messages.BT_MessageUtility;

import org.junit.Before;
import org.junit.Test;


import static org.junit.Assert.*;

public class BT_MessageAppTest {

    private BT_MessageApp mMessage1;
    private BT_MessageApp mMessage2;
    private BT_MessageApp mMessage3;
    private BT_MessageApp mMessage4;
    private BT_MessageApp mMessage5;

    private final String address1 = "00:00:00:00:00:00";
    private final String address2 = "aa:aa:aa:aa:aa:aa";
    private final String address3 = "--:oo:kk:yy:gg:gg";
    private final String address4 = "ppppppppppppppppp";
    private final String address5 = "44444444444444444";

    private final String data1 = "hello";
    private final String data2 = "1234";
    private final String data3 = "test123";
    private final String data4 = "k";
    private final String data5 = "";

    @Before
    public void setUp() throws Exception {
        mMessage1 = new BT_MessageApp(address1, data1.getBytes());
        mMessage2 = new BT_MessageApp(address2, data2.getBytes());
        mMessage3 = new BT_MessageApp(address3, data3.getBytes());
        mMessage4 = new BT_MessageApp(address4, data4.getBytes());
        mMessage5 = new BT_MessageApp(address5, data5.getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidAddressShort(){
        mMessage1 = new BT_MessageApp("1", data1.getBytes());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidAddressLong(){
        mMessage1 = new BT_MessageApp("01234567890123456789", data1.getBytes());
    }

    @Test
    public void testConstructorNullData(){
        mMessage1 = new BT_MessageApp(address1, null);
    }

    @Test
    public void testSuperGet(){
        assertEquals(mMessage1.getMacAddress(), address1);
        assertEquals(mMessage2.getMacAddress(), address2);
        assertEquals(mMessage3.getMacAddress(), address3);
        assertEquals(mMessage4.getMacAddress(), address4);
        assertEquals(mMessage5.getMacAddress(), address5);

        assertEquals(mMessage1.getMessageType(), BT_MessageUtility.TYPE_APP_MESSAGE);
        assertEquals(mMessage2.getMessageType(), BT_MessageUtility.TYPE_APP_MESSAGE);
        assertEquals(mMessage3.getMessageType(), BT_MessageUtility.TYPE_APP_MESSAGE);
        assertEquals(mMessage4.getMessageType(), BT_MessageUtility.TYPE_APP_MESSAGE);
        assertEquals(mMessage5.getMessageType(), BT_MessageUtility.TYPE_APP_MESSAGE);
    }

    @Test
    public void testGetData(){
        assertEquals(data1, new String(mMessage1.getData()) );
        assertEquals(data2, new String(mMessage2.getData()) );
        assertEquals(data3, new String(mMessage3.getData()) );
        assertEquals(data4, new String(mMessage4.getData()) );
        assertEquals(data5, new String(mMessage5.getData()) );
    }

    @Test
    public void testGetEmptyData(){
        BT_MessageApp m = new BT_MessageApp(address1, null);
        byte[] b = m.getData();
        assertEquals("", new String(b));
    }

    @Test
    public void testMakeBytes(){

        //deconstruct object1 into bytes1
        //create new stringA from bytes1
        //compare string value of object1 to stringA

        String original, created;

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address1 + data1;
        created = new String(mMessage1.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address2 + data2;
        created = new String(mMessage2.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address3 + data3;
        created = new String(mMessage3.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address4 + data4;
        created = new String(mMessage4.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address5 + data5;
        created = new String(mMessage5.makeBytes());
        assertEquals(original, created);
    }

    @Test
    public void testReconstruct(){

        //deconstruct object1 into bytes1
        //create new object2 from bytes1
        //compare string value of object1 to object2

        String original, created;
        BT_MessageApp reconstructedMessage;

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address1 + data1;
        reconstructedMessage = BT_MessageApp.reconstruct(mMessage1.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address2 + data2;
        reconstructedMessage = BT_MessageApp.reconstruct(mMessage2.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address3 + data3;
        reconstructedMessage = BT_MessageApp.reconstruct(mMessage3.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address4 + data4;
        reconstructedMessage = BT_MessageApp.reconstruct(mMessage4.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);

        original = BT_MessageUtility.TYPE_APP_MESSAGE + address5 + data5;
        reconstructedMessage = BT_MessageApp.reconstruct(mMessage5.makeBytes());
        created = new String(reconstructedMessage.makeBytes());
        assertEquals(original, created);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReconstructInvalidType(){

        String original = BT_MessageUtility.TYPE_HELLO + address1 + data1;
        byte[] wrongType = original.getBytes();

        mMessage1 = BT_MessageApp.reconstruct(wrongType);
    }

}
