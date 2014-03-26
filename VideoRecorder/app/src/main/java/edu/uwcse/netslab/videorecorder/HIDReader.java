package edu.uwcse.netslab.videorecorder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by syhan on 2014. 1. 19..
 */
public class HIDReader {
    public enum PacketType {
        None, Configure((byte) 'C'), Heartbeat((byte) 'H'), Reading_SinglePrecision((byte) 'r'), Name((byte) 'N');
        public byte type;
        private PacketType()
        {
            this.type = (byte) 0;
        }

        private PacketType(byte type)
        {
            this.type = type;
        }
    }
    List<Thread> threads = new ArrayList<Thread>();
    BluetoothDevice mDevice;
    ConnectThread mConnect = null;
    BufferedOutputStream ostream;
    public boolean start(String startdate)
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.w("VideoRecorder::HIDReader", "BluetoothAdapter is null");
            return false;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.i("VideoRecorder::HIDReader", device.getName() + "\n" + device.getAddress());
                if(device.getName().startsWith("Thermal")) {
                    mDevice = device;
                }
            }
        }
        else
        {
            Log.w("VideoRecorder::HIDReader", "No Paired Device, first pair with thermal sensor");
            return false;
        }
        try{
            ostream = new BufferedOutputStream(new FileOutputStream(startdate));

        }
        catch(Exception e)
        {
            Log.w("VideoRecorder::HIDReader", "File Creation Failed");
            e.printStackTrace();
            return false;
        }
        mConnect = new ConnectThread(mDevice);
        mConnect.start();
        threads.add(mConnect);
        return true;
    }


    public void stop()
    {
        if(mConnect == null) return;

        mConnect.cancel();
        mConnect = null;
        for(Thread t:threads)
        {
            try {
                t.join();
            }
            catch(Exception e)
            {

            }

        }
        threads.clear();
        try{
            ostream.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        ostream = null;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final byte Frame = 0x7C;
        private final byte Escape = 0x7D;
        private final byte Tilde = 0x7E;              // '~'. Special character used by KC-22 to enter command mode
        private final byte EscapeFlag = (byte) 0x80;         // Flag ORed with escaped character
        private final byte EscapeMask = (byte) 0xFC;         // These are the bits used to determine if escaping of a character is needed
        private final byte EscapeNeeded = (byte) 0x7C;       // If a character ANDed with the MASK equals this value, it needs escaping


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public int Encode(byte[] plainPacket, byte[] encodedPacket, int length)
        {
            byte checksum = 0;
            int pos = 0;
            encodedPacket[pos++] = Frame;
            for (int i = 0; i < length; i++) {
                if ((plainPacket[i] & EscapeMask) == EscapeNeeded) {
                    encodedPacket[pos++] = Escape;
                    encodedPacket[pos++] = (byte)(plainPacket[i] | EscapeFlag);
                } else {
                    encodedPacket[pos++] = plainPacket[i];
                }
                checksum += plainPacket[i];
            }

            // Send check byte (2's complement of sum of rest of packet)
            encodedPacket[pos++] = (byte)((~checksum & 0xff) + 1);

            // End of frame
            encodedPacket[pos++] = Frame;

            return pos;
        }

        private void Configure(int bSleep, int iInfraredRefreshRate, int iAmbientRefreshRate, int iI2cRate, int bUseAdcHighReference) {
            byte[] message = new byte[21];
            message[0] = (byte) PacketType.Configure.type;
            message[1] = (byte)bSleep; message[2] = (byte)(bSleep >> 8); message[3] = (byte)(bSleep >> 16); message[4] = (byte)(bSleep >> 24);
            message[5] = (byte)iInfraredRefreshRate; message[6] = (byte)(iInfraredRefreshRate >> 8); message[7] = (byte)(iInfraredRefreshRate >> 16); message[8] = (byte)(iInfraredRefreshRate >> 24);
            message[9] = (byte)iAmbientRefreshRate; message[10] = (byte)(iAmbientRefreshRate >> 8); message[11] = (byte)(iAmbientRefreshRate >> 16); message[12] = (byte)(iAmbientRefreshRate >> 24);
            message[13] = (byte)iI2cRate; message[14] = (byte)(iI2cRate >> 8); message[15] = (byte)(iI2cRate >> 16); message[16] = (byte)(iI2cRate >> 24);
            message[17] = (byte)bUseAdcHighReference; message[18] = (byte)(bUseAdcHighReference >> 8); message[19] = (byte)(bUseAdcHighReference >> 16); message[20] = (byte)(bUseAdcHighReference >> 24);
            Send(message, 21);
                /*byte[] response = SendAndWait(message, 21, PacketType.Configure);
                if (null == response)
                    return false;

                bSleep = BitConverter.ToInt32(response, 1);
                iInfraredRefreshRate = BitConverter.ToInt32(response, 5);
                iAmbientRefreshRate = BitConverter.ToInt32(response, 9);
                iI2cRate = BitConverter.ToInt32(response, 13);
                bUseAdcHighReference = BitConverter.ToInt32(response, 17);

                return true;*/
        }

        public void Send(byte[] packet, int length)
        {
            byte[] buffer = new byte[2*length+4];
            int txLength = Encode(packet, buffer, length);
            try {
                mmOutStream.write(buffer, 0, txLength);
            } catch (IOException e) {
                Log.w("BTTest", "IOexception on Send");
                e.printStackTrace();
            }
        }

        private float [] getFloats(byte [] bytes, int offset, int length)
        {
            float[] toret = new float[length/4];
            for(int i=0;i<length/4;i++)
            {
                float f = ByteBuffer.wrap(bytes, offset + i * 4, 4).order(ByteOrder.nativeOrder()).getFloat();
                toret[i] = f;
            }
            return toret;
        }

        boolean writeBinary = false;

        public void Received(byte[] packet, int length)
        {
            if(packet[0] == 'r')
            {
                long curtime = System.currentTimeMillis();
                float [] readings = getFloats(packet, 1, length-1);

                if(writeBinary) {
                    ByteBuffer b = ByteBuffer.allocate(12);
                    b.putLong(curtime);
                    b.putInt(length - 1);
                    byte [] buffer = b.array();
                    ByteBuffer b2 = ByteBuffer.allocate(256);
                    for(float reading:readings)
                    {
                        b2.putFloat(reading);
                    }
                    try{
                        ostream.write(buffer);
                        ostream.write(b2.array());
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append(curtime);
                    sb.append(' ');
                    sb.append(length-1);
                    for(float reading:readings)
                    {
                        sb.append(' ');
                        sb.append(reading);
                    }
                    sb.append('\n');
                    try {
                        byte [] bytes = sb.toString().getBytes();
                        ostream.write(bytes, 0, bytes.length);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void decode(byte[] buffer, int length)
        {
            //Log.i("BTTest", "before decode from BT: " + length + " " + (char) buffer[1]);
            boolean rxEscaped = false;
            boolean rxError = true;
            int rxLength = 0;
            byte rxChecksum = 0;
            byte [] packet = new byte[1+64*8+1];
            for(int i=0;i<length;i++)
            {
                byte c = buffer[i];
                if (rxEscaped && (0 == (c & EscapeFlag)))
                    rxError = true;
                switch(c) {
                    case Escape:
                        rxEscaped = true;
                        break;
                    case Frame:
                        int tmpLength = rxError ? -1 : rxLength;
                        rxError = false;
                        rxEscaped = false;
                        rxLength = 0;
                        if( 0 < tmpLength ) {
                            if(0 == rxChecksum) {
                                Received(packet, tmpLength -1);
                            }
                            else
                            {
                                Log.d("BTTest", "invalid checksum");
                            }
                        }


                        rxChecksum = 0;
                        break;
                    default:
                        if(rxEscaped) {
                            c &= (byte)(~EscapeFlag & 0xff);
                            rxEscaped = false;
                        }
                        if(rxLength >= (1+64*8+1)) {
                            Log.d("BTTest", "over length");
                            rxError = true;
                        }
                        if(!rxError) {
                            packet[rxLength++] = c;
                            rxChecksum += c;
                        }
                        break;
                }
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            boolean sent = false;
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    decode(buffer, bytes);
                    //mmInStream.r
                    // Send the obtained bytes to the UI activity
                    /*mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();*/
                    if(!sent)
                    {
                        Configure(-1,-1,-1,-1,-1);
                        //Send(new byte[]{ (byte) 'R' }, 1);
                        sent = true;
                    }
                    else
                    {
                        Send(new byte[]{(byte) 'H'}, 1);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//UUID.nameUUIDFromBytes(new String("1").getBytes());

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            //mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.i("BTTest", "connection starts");
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.i("BTTest", "connection Failed");
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }
            Log.i("BTTest", "connection Succeeded");
            //this.cancel();


            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        private void manageConnectedSocket(BluetoothSocket socket)
        {
            ConnectedThread t = new ConnectedThread(socket);
            t.start();
            threads.add(t);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
