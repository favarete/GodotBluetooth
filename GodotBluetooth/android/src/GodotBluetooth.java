package org.godotengine.godot;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rodrigo Favarete, Mad Forest Games' Lead Game Developer, on September 8, 2017
 */

public class GodotBluetooth extends Godot.SingletonBase 
{   
    protected Activity activity; 

    private boolean initialized;
    private boolean pairedDevicesListed = false;
    boolean connected = false;
    boolean bluetoothRequired = true;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MESSAGE_READ = 2;
    private int instanceId = 0;

    Object[] pairedDevicesAvailable;

    ConnectedThread cThread;
    Handler localHandler;

    StringBuilder receivedData = new StringBuilder();
    private static String macAdress;
    String remoteBluetoothName;
    String[] externalDevicesDialogAux;
    private static final String TAG = "godotbluetooth";

    BluetoothAdapter localBluetooth;
    BluetoothDevice remoteBluetooth;
    BluetoothSocket socket;
    UUID bluetoothUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* Methods
     * ********************************************************************** */

    /**
     * Initialize the Module
     */

    public void init(final int newInstanceId, final boolean newBluetoothRequired) {
        if (!initialized) {
            activity.runOnUiThread(new Runnable() {
                @Override 
                public void run() {
                    localBluetooth = BluetoothAdapter.getDefaultAdapter();
                    if(localBluetooth == null) {
                        Toast.makeText(activity, "ERROR: Bluetooth Adapter not found!", Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                    else if (!localBluetooth.isEnabled()){
                        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        activity.startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
                    }
                    instanceId = newInstanceId;
                    bluetoothRequired = newBluetoothRequired;
                    initialized = true;

                    localHandler = new Handler(){
                        @Override
                        public void handleMessage(Message msg) {

                            if(msg.what == MESSAGE_READ){
                                String newData = (String) msg.obj;
                                receivedData.append(newData);

                                int endElement = receivedData.indexOf("}");
                                if(endElement > 0) {
                                    String completeData = receivedData.substring(0, endElement);
                                    int dataSize = completeData.length();

                                    if(receivedData.charAt(0) == '{') {
                                        String finalizedData = receivedData.substring(1, dataSize);
                                        GodotLib.calldeferred(instanceId, "_on_data_received", new Object[]{ finalizedData });
                                    }

                                    receivedData.delete(0, receivedData.length());
                                }
                            }
                        }
                    };
                }
            });
        }
    }

    /**
     * Gets a list of all external devices that are already paired with the local device
     */

    public void getPairedDevices(final boolean nativeDialog) {
        if (initialized) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(connected) {
                            try {
                                socket.close();
                                connected = false;
                                pairedDevicesListed = false;
                                Toast.makeText(activity, "Bluetooth Disconnected!", Toast.LENGTH_LONG).show();
                                GodotLib.calldeferred(instanceId, "_on_disconnected", new Object[]{});
                            }
                            catch (IOException e) {
                                Toast.makeText(activity, "ERROR: \n" + e, Toast.LENGTH_LONG).show();
                            }
                    }
                    else{
                        if (nativeDialog){
                            nativeLayoutDialogBox();
                        }
                        else {
                            listPairedDevices();
                        }
                    }
                }
            });
        }

        else {
            Toast.makeText(activity, "ERROR: Module Wasn't Initialized!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Native dialog box to show paired external devices
     */

    private void nativeLayoutDialogBox() {
        String localDeviceName = localBluetooth.getName();
        String localDeviceAddress = localBluetooth.getAddress();

        Set<BluetoothDevice> pairedDevices = localBluetooth.getBondedDevices();

        if(pairedDevices.size() > 0) {
            pairedDevicesAvailable = (Object []) pairedDevices.toArray();

            List<String> externalDeviceInfo = new ArrayList<String>();

            for (BluetoothDevice device : pairedDevices) {
                String externalDeviceName = device.getName();
                String externalDeviceAddress = device.getAddress();

                externalDeviceInfo.add(externalDeviceName + "\n" + externalDeviceAddress);
            }
            externalDevicesDialogAux = new String[externalDeviceInfo.size()];
            externalDevicesDialogAux = externalDeviceInfo.toArray(new String[externalDeviceInfo.size()]);;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Choose a Device To Connect!");
            builder.setItems(externalDevicesDialogAux, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    connect(which);
                }
            });
            pairedDevicesListed = true;
            AlertDialog dialog = builder.create();
            dialog.show();
    }

    /**
     * Organizes and sends to Godot all external paired devices
     */

    private void listPairedDevices() {

        String localDeviceName = localBluetooth.getName();
        String localDeviceAddress = localBluetooth.getAddress();

        Set<BluetoothDevice> pairedDevices = localBluetooth.getBondedDevices();

        if(pairedDevices.size() > 0) {
            pairedDevicesAvailable = (Object []) pairedDevices.toArray();
            int externalDeviceID = 0;

            for (BluetoothDevice device : pairedDevices) {
                String externalDeviceName = device.getName();
                String externalDeviceAddress = device.getAddress();

                GodotLib.calldeferred(instanceId, "_on_single_device_found", new Object[]{ externalDeviceName, externalDeviceAddress, externalDeviceID });
                externalDeviceID += 1;
            }

            pairedDevicesListed = true;
        }
    }

    /**
     * Prepares to connect to another device, identified by the 'newExternalDeviceID'
     */

    public void connect(final int newExternalDeviceID){
        if (initialized && pairedDevicesListed) {
            activity.runOnUiThread(new Runnable() {
                @Override
                    public void run() {
                        if(!connected){
                            BluetoothDevice device = (BluetoothDevice) pairedDevicesAvailable[newExternalDeviceID];

                            macAdress = device.getAddress();
                            remoteBluetoothName = device.getName();
                            
                            createSocket(macAdress);
                        }
                        else{
                            try {
                                socket.close();
                                connected = false;
                                pairedDevicesListed = false;
                                Toast.makeText(activity, "Bluetooth Disconnected!", Toast.LENGTH_LONG).show();
                            }
                            catch (IOException e) {
                                Toast.makeText(activity, "ERROR: \n" + e, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            });
        }
        else {
            Toast.makeText(activity, "ERROR: Module Wasn't Initialized!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates the Socket to communicate with another device and establishes the connection
     */

    private void createSocket (String MAC) {

        remoteBluetooth = localBluetooth.getRemoteDevice(MAC);

        try {
            socket = remoteBluetooth.createRfcommSocketToServiceRecord(bluetoothUUID);
            socket.connect();
            connected = true;
            pairedDevicesListed = true;
            cThread = new ConnectedThread(socket);
            cThread.start();
            GodotLib.calldeferred(instanceId, "_on_connected", new Object[]{ remoteBluetoothName, macAdress });
            Toast.makeText(activity, "Connected With " + remoteBluetoothName, Toast.LENGTH_LONG).show();
            }

        catch (IOException e) {
            pairedDevicesListed = false;
            connected = false;
            GodotLib.calldeferred(instanceId, "_on_connected_error", new Object[]{ });
            Toast.makeText(activity, "ERROR: Cannot connect to " + MAC, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Calls the method that converts the desired data from String to Bytes and sends it to the connected device
     */

    public void sendData(final String dataToSend){

        if (initialized) {
            activity.runOnUiThread(new Runnable() {
                @Override
                    public void run() {
                        if(connected) {
                            cThread.sendData(dataToSend);
                        }
                        else {
                            Toast.makeText(activity, "Bluetooth not connected!", Toast.LENGTH_LONG).show();
                        }
                    }
            });
        }
    }

    /**
     * Calls the method that sends data as bytes to the connected device
     */

    public void sendDataBytes(final byte[] dataBytesToSend){

        if (initialized) {
            activity.runOnUiThread(new Runnable() {
                @Override
                    public void run() {
                        if(connected) {
                            cThread.sendDataBytes(dataBytesToSend);
                        }
                        else {
                            Toast.makeText(activity, "Bluetooth not connected!", Toast.LENGTH_LONG).show();
                        }
                    }
            });
        }
    }

    /**
     * Class responsible for communication between connected devices
     */

    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket newSocket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = newSocket.getInputStream();
                tmpOut = newSocket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String externalData = new String(buffer, 0, bytes);
                    localHandler.obtainMessage(MESSAGE_READ, bytes, -1, externalData).sendToTarget();
                } 

                catch (IOException e) {
                    break;
                }
            }
        }

        public void sendData(String dataToSend) {

            byte[] dataBuffer = dataToSend.getBytes();

            try {
                mmOutStream.write(dataBuffer);
            } 
            catch (IOException e) { }
        }

        public void sendDataBytes(byte[] bytes) {

            try {
                mmOutStream.write(bytes);
            } 
            catch (IOException e) { }
        }
    }

    /**
     * Internal callbacks
     */

    @Override
    protected void onMainActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                
                if(resultCode == Activity.RESULT_OK) {
                    Toast.makeText(activity, "Bluetooth Activated!", Toast.LENGTH_LONG).show();
                }
                else {
                    if(bluetoothRequired){
                        Toast.makeText(activity, "Bluetooth wasn't activated, application closed!", Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                    else{
                        Toast.makeText(activity, "Bluetooth wasn't activated!", Toast.LENGTH_LONG).show();
                    }
                }

                break;
                
            default:
                Toast.makeText(activity, "ERROR: Unknown situation!", Toast.LENGTH_LONG).show();
        }
    }

    /* Definitions
     * ********************************************************************** */

    /**
     * Initilization of the Singleton
     */

    static public Godot.SingletonBase initialize(Activity p_activity)
    {
        return new GodotBluetooth(p_activity);
    }

    /**
     * Constructor
     */

    public GodotBluetooth (Activity activity) 
    {
        registerClass("GodotBluetooth", new String[]
        {
            "init", 
            "getPairedDevices",
            "connect",
            "sendData",
            "sendDataBytes",
        });

        this.activity = activity;
    }
}
