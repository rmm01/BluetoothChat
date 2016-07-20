package com.yckir.bluetoothchat.services;

import com.yckir.bluetoothchat.services.messages.BT_Message;
import com.yckir.bluetoothchat.services.messages.BT_MessageUtility;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;


public class BT_MessageTest {

    private BT_Message mMessage1;
    private BT_Message mMessage2;
    private BT_Message mMessage3;
    private BT_Message mMessage4;
    private BT_Message mMessage5;

    private final String address1 = "00:00:00:00:00:00";
    private final String address2 = "aa:aa:aa:aa:aa:aa";
    private final String address3 = "--:oo:kk:yy:gg:gg";
    private final String address4 = "ppppppppppppppppp";
    private final String address5 = "44444444444444444";

    private final int type1 = BT_MessageUtility.TYPE_HELLO;
    private final int type2 = BT_MessageUtility.TYPE_HELLO_REPLY;
    private final int type3 = BT_MessageUtility.TYPE_CONNECTION_CLOSED;
    private final int type4 = BT_MessageUtility.TYPE_SERVER_SETUP_FINISHED;
    private final int type5 = BT_MessageUtility.TYPE_APP_MESSAGE;


    @Before
    public void setUp() throws Exception {
        mMessage1 = new BT_Message(type1, address1);
        mMessage2 = new BT_Message(type2, address2);
        mMessage3 = new BT_Message(type3, address3);
        mMessage4 = new BT_Message(type4, address4);
        mMessage5 = new BT_Message(type5, address5);
    }

    @After
    public void tearDown() throws Exception {

    }

    //standard constructor

    @Test
    public void testConstructorString(){
        BT_Message m;
        m = new BT_Message(type1 + address1);

        assertEquals(m.getMessageType(), mMessage1.getMessageType());
        assertEquals(m.getMacAddress(), mMessage1.getMacAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorStringInvalidSmall(){
        String s = "0";
        mMessage1 = new BT_Message(s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorStingInvalidLarge(){
        String s = "02345678901234567890";
        mMessage1 = new BT_Message(s);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructorStingInvalidType(){
        assertTrue( !BT_MessageUtility.isMessageType( 9999 ) );

        String s = "9999" + address1;
        mMessage1 = new BT_Message(s);
    }

    //byte constructor

    @Test
    public void testConstructorBytes(){

        BT_Message m;
        m = new BT_Message((type1 + address1).getBytes());

        assertEquals(m.getMessageType(), mMessage1.getMessageType());
        assertEquals(m.getMacAddress(), mMessage1.getMacAddress());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBytesInvalidSmall(){
        byte[] b = "0".getBytes();
        mMessage1 = new BT_Message(b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBytesInvalidType(){
        assertTrue( !BT_MessageUtility.isMessageType( 9999 ) );

        byte[] b = ("9999" + address1).getBytes();
        mMessage1 = new BT_Message(b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorBytesInvalidLarge(){
        byte[] b = "02345678901234567890".getBytes();
        mMessage1 = new BT_Message(b);
    }


    // string constructor

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2pInvalidType(){
        @BT_MessageUtility.MESSAGE_TYPE int fakeType = 9999;

        assertTrue( !BT_MessageUtility.isMessageType( fakeType ) );

        mMessage1 = new BT_Message(fakeType, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2pInvalidAddressSmall(){
        mMessage1 = new BT_Message(BT_MessageUtility.TYPE_HELLO, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor2pInvalidAddressLarge(){
        mMessage1 = new BT_Message(BT_MessageUtility.TYPE_HELLO, "01234567890123456789");
    }



    @Test
    public void testGetMacAddress(){
        assertEquals(mMessage1.getMessageType(), type1);
        assertEquals(mMessage2.getMessageType(), type2);
        assertEquals(mMessage3.getMessageType(), type3);
        assertEquals(mMessage4.getMessageType(), type4);
        assertEquals(mMessage5.getMessageType(), type5);
    }

    @Test
    public void testGetMessageType(){
        assertEquals(mMessage1.getMacAddress(), address1);
        assertEquals(mMessage2.getMacAddress(), address2);
        assertEquals(mMessage3.getMacAddress(), address3);
        assertEquals(mMessage4.getMacAddress(), address4);
        assertEquals(mMessage5.getMacAddress(), address5);
    }

    @Test
    public void testMakeString1(){
        assertEquals(mMessage1.makeString(), type1 + address1);
        assertEquals(mMessage2.makeString(), type2 + address2);
        assertEquals(mMessage3.makeString(), type3 + address3);
        assertEquals(mMessage4.makeString(), type4 + address4);
        assertEquals(mMessage5.makeString(), type5 + address5);
    }

    @Test
    public void testMakeString2(){
        //checks if class -> string -> class creates same object

        String original, created;

        original = mMessage1.makeString();
        created = new BT_Message( original ).makeString();
        assertEquals( original, created );

        original = mMessage2.makeString();
        created = new BT_Message( original ).makeString();
        assertEquals( original, created );

        original = mMessage3.makeString();
        created = new BT_Message( original ).makeString();
        assertEquals( original, created );

        original = mMessage4.makeString();
        created = new BT_Message( original ).makeString();
        assertEquals( original, created );

        original = mMessage5.makeString();
        created = new BT_Message( original ).makeString();
        assertEquals( original, created );
    }

    @Test
    public void testMakeBytes(){
        //checks if class -> byte -> class creates same object

        byte[] original, created;

        original = mMessage1.makeBytes();
        created = new BT_Message(original).makeBytes();
        assertTrue( Arrays.equals( original, created ) );

        original = mMessage2.makeBytes();
        created = new BT_Message(original).makeBytes();
        assertTrue( Arrays.equals( original, created ) );

        original = mMessage3.makeBytes();
        created = new BT_Message(original).makeBytes();
        assertTrue( Arrays.equals( original, created ) );

        original = mMessage4.makeBytes();
        created = new BT_Message(original).makeBytes();
        assertTrue( Arrays.equals( original, created ) );

        original = mMessage5.makeBytes();
        created = new BT_Message(original).makeBytes();
        assertTrue( Arrays.equals( original, created ) );
    }


}