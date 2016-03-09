package com.cvoltidioten.submarinecontrol;

import android.bluetooth.*;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Set;

/**
 * An implementation of the SubmarineConnector via a Bluetooth interface. Note that for using this
 * implementation, a pairing should be done prior and bluetooth should be previously enabled by the
 * calling activity.
 * Based on the BluetoothChat example on developer.android.com.
 */
class SubmarineBluetoothConnector extends SubmarineConnector {
    private static final String TAG = "SubmarineBluetoothConn";
    private static final UUID MUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // The submarine to connect to.
    private Submarine submarine;
    // The receivers to notify on connection status updates.
    private HashMap<String, SubmarineConnectionNotifyable> connectionStatusReceivers = new HashMap<>();
    // The receivers to notify on message arrivals.
    private HashMap<String, SubmarineMessageNotifyable> messageReceivers = new HashMap<>();
    // The bluetooth adapter to work on.
    private BluetoothAdapter bluetoothAdapter;
    // The concrete bluetooth device to connect to.
    private BluetoothDevice device;
    // The thread that runs during the whole connection and implements read and write.
    private ConnectedThread connectedThread;
    // The that runs during the connection buildup.
    private ConnectThread connectThread;
    // Whether a connection is currently established.
    private boolean isConnected = false;

    /**
     * Constructs a new bluetooth connector for the given submarine.
     * @param submarine The submarine to connect to.
     * @throws HardwareException In case the bluetooth adapter is not present or is not enabled.
     */
    public SubmarineBluetoothConnector(Submarine submarine) throws HardwareException {
        this.submarine = submarine;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Enabling bluetooth should be done in the main activity.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            throw new HardwareException();
        }
    }

    @Override
    public boolean connect() {
        this.disconnect();
        this.bluetoothAdapter.startDiscovery();
        Log.i(TAG, "Listing paired devices now...");
        Set<BluetoothDevice> pairedDevices = this.bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if(pairedDevices.size() > 0) {
            // Loop through paired devices
            for(BluetoothDevice device : pairedDevices) {
                Log.i(TAG, device.getName());
                // Add the name and address to an array adapter to show in a ListView
                if(device.getName() != null && device.getName().equals(submarine.getName())) {
                    // Connect
                    Log.i(TAG, "Found paired submarine " + device.getName() + ".");
                    this.device = device;
                    this.connectThread = new ConnectThread(device);
                    this.connectThread.start();
                    return true;
                }
            }
        }
        // NOTE: Pairing bluetooth device should be done in the main activity.
        return false;
    }

    public boolean send(SubmarineProtos.ControlMessage message) {
        if(this.connectedThread != null) {
            Log.v(TAG, "Sending data:\n" + message.toString());
            this.connectedThread.write(message);
            return true;
        } else {
            Log.e(TAG, "Tried to write data but not connected to any device.");
            return false;
        }
    }

    public boolean disconnect() {
        if(this.connectThread != null) {
            connectThread.cancel(false);
            connectThread.interrupt();
            this.connectThread = null;
        }
        if(this.connectedThread != null) {
            connectedThread.cancel(false);
            connectedThread.interrupt();
            this.connectedThread = null;
            this.device = null;
        }
        return true;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    protected void registerMessageReceiver(String tag, SubmarineMessageNotifyable receiver) {
        Log.i(TAG, "Registered message receiver " + tag);
        this.messageReceivers.put(tag, receiver);
    }

    protected void removeMessageReceiver(String tag) {
        Log.i(TAG, "Removed message receiver " + tag);
        this.messageReceivers.remove(tag);
    }

    protected void registerConnectionStatusReceiver(String tag, SubmarineConnectionNotifyable receiver) {
        Log.i(TAG, "Reigstered status receiver " + tag);
        this.connectionStatusReceivers.put(tag, receiver);
    }

    protected void removeConnectionStatusReceiver(String tag) {
        Log.i(TAG, "Removed connection status receiver " + tag);
        this.connectionStatusReceivers.remove(tag);
    }

    /**
     * Updates the connection status which can be either on- or offline. Notifies any receivers
     * about the connection change and the connection is renewed in case we went offline and the
     * automatic reconnect was set.
     * @param newConnectionStatus The new connection status, false means offline, true means online.
     * @param notify Whether to notify the connection status receivers.
     */
    private void updateConnectionStatus(boolean newConnectionStatus, boolean notify) {
        this.isConnected = newConnectionStatus;
        if(notify) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    for(Map.Entry<String, SubmarineConnectionNotifyable> entry : connectionStatusReceivers.entrySet()) {
                        Log.i(TAG, "Notifying connection status receiver: " + entry.getKey());
                        entry.getValue().receiveConnectionStatus(isConnected);
                    }
                }
            }, 2000);
        }
    }

    /**
     * This thread runs once during connection buildup. If everything goes successfully, it creates
     * a bluetooth socket and directly executes the ConnectedThread where the actual data management
     * happens.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            // Gets a BluetoothSocket to connect with the given bluetooth device.
            try {
                socket = device.createRfcommSocketToServiceRecord(MUUID);
            } catch (IOException e) {
                Log.e(TAG, "Bluetooth socket could not be created", e);
            }
        }

        public void run() {
            // Cancels discovery because it will slow down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connects the device through the socket. This will block.
                socket.connect();
                Log.i(TAG, "Successfully connected to " + device.getName());
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            } catch (IOException connectException) {
                Log.e(TAG, "Exception during connection buildup", connectException);
                try {
                    socket.close();
                    Class<?> deviceClass = this.device.getClass();
                    Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                    Method m = deviceClass.getMethod("createRfcommSocket", paramTypes);
                    Object[] params = new Object[] {Integer.valueOf(1)};
                    socket = (BluetoothSocket) m.invoke(device, params);
                    socket.connect();
                    Log.i(TAG, "Successfully connected to " + device.getName());
                    connectedThread = new ConnectedThread(socket);
                    connectedThread.start();
                } catch (Exception e) {
                    Log.e(TAG, "Exception during fallback connection buildup", e);
                    cancel();
                }
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void cancel(boolean notify) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing during connection buildup failed", e);
            }
            updateConnectionStatus(false, notify);
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all incoming and
     * outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket socket;
        private volatile boolean active = true;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        public void run() {
            // Updates the connection status as this is only called on a working connection.
            updateConnectionStatus(true, true);
            // Keeps listening to the input stream while connected.
            while(this.active) {
                try {
                    Log.i(TAG, "Reading data...");
                    int length = socket.getInputStream().read() & 0xFF;
                    Log.i(TAG, "Got message length " + length);
                    byte byteMessage[] = new byte[length];
                    for(int i = 0; i < length; i++) {
                        byteMessage[i] = (byte)socket.getInputStream().read();
                    }
                    Log.i(TAG, "Got byte message " + Arrays.toString(byteMessage));
                    SubmarineProtos.SubmarineMessage message = SubmarineProtos.SubmarineMessage.parseFrom(byteMessage);
                    // Notifies receivers
                    for(Map.Entry<String, SubmarineMessageNotifyable> entry : messageReceivers.entrySet()) {
                        Log.i(TAG, "Notifying message receiver: " + entry.getKey());
                        entry.getValue().receiveMessage(message);
                    }
                    Log.i(TAG, "Received message:\n" + message.toString());
                } catch (IOException e1) {
                    Log.i(TAG, "Disconnected", e1);
                    cancel();
                    break;
                }
            }
        }

        public void write(SubmarineProtos.ControlMessage message) {
            try {
                // Supporting only messages with size up to 255 bytes.
                socket.getOutputStream().write((byte)message.getSerializedSize());
                message.writeTo(socket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            this.cancel(true);
        }

        public void cancel(boolean notify) {
            this.active = false;
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Regular closing failed", e);
            }
            updateConnectionStatus(false, notify);
        }
    }

    /**
     * Registers a new bluetooth broadcast receiver in case a new device was found. We check if this
     * device is our submarine device and connect to it.
     */
    /**public class BroadcastReceiver {
     public void onReceive(Context context, Intent intent) {
     String action = intent.getAction();
     // When discovery finds a device
     if (BluetoothDevice.ACTION_FOUND.equals(action)) {
     // Get the BluetoothDevice object from the Intent
     BluetoothDevice foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
     if(foundDevice.getName().equals(submarine.getName())) {
     device = foundDevice;
     new ConnectThread(device).start();
     }
     }
     }
     }**/
}
