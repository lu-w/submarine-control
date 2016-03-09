package com.cvoltidioten.submarinecontrol;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the physical submarine. It wraps the connection to the submarine. If the submarine is
 * not connected, any requests will be discarded. The user needs to try again when the submarine
 * becomes available again. If you want to be informed about changes in either the submarine's
 * status or any messages we receive, you should implement the corresponding interface and register
 * yourself as a message resp. status receiver.
 */
class Submarine implements SubmarineConnector.SubmarineMessageNotifyable, SubmarineConnector.SubmarineConnectionNotifyable {
    private final static String TAG = "Submarine";
    private final static String DEFAULT_NAME = "USS Sea Tiger";

    private String name = "";
    private SubmarineProtos.Status.StatusType status = SubmarineProtos.Status.StatusType.AVAILABLE;
    private int batteryPercentage = 100;
    private List<Dive> previousDives;
    private SubmarineConnector connection;
    private boolean automaticReconnect = false;

    /**
     * Creates a new submarine with the default name.
     * @throws SubmarineConnector.HardwareException In case the connection could not be established.
     */
    protected Submarine() throws SubmarineConnector.HardwareException {
        this(DEFAULT_NAME);
    }

    /**
     * Creates a new submarine with the given name.
     * @param name The name of the submarine.
     * @throws SubmarineConnector.HardwareException In case the connection could not be established.
     */
    protected Submarine(String name) throws SubmarineConnector.HardwareException {
        this.name = name;
        this.previousDives = new ArrayList<>();
        this.connection = new SubmarineBluetoothConnector(this);
        this.connection.registerConnectionStatusReceiver(TAG, this);
        this.connection.registerMessageReceiver(TAG, this);
    }

    protected String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected SubmarineProtos.Status.StatusType getStatus() {
        return status;
    }

    protected void setStatus(SubmarineProtos.Status.StatusType status) {
        this.status = status;
    }

    protected int getBatteryPercentage() {
        return this.batteryPercentage;
    }

    protected List<Dive> getDives() {
        return this.previousDives;
    }

    /**
     * Connects to the submarine. Returns immediately.
     * @param automaticReconnect If the connection attempt should be automatically retried if it
     *                           fails.
     * @return True if the attempt to connect could be established.
     */
    protected boolean connect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
        return this.connection.connect();
    }

    /**
     * Connects to the submarine. Returns immediately.
     * @return True if the attempt to connect could be established.
     */
    protected boolean connect() {
        this.automaticReconnect = false;
        return this.connection.connect();
    }

    /**
     * Disconnects from the submarine.
     */
    protected void disconnect() {
        this.connection.disconnect();
    }

    /**
     * Returns true if the submarine is currently connected.
     * @return True if the submarine is connected.
     */
    protected boolean isConnected() {
        return this.connection.isConnected();
    }

    /**
     * Registers a new message receiver which will be informed about any incoming message via the
     * implemented callback method.
     * @param tag The unique name of the receiver.
     * @param receiver The receiving object.
     */
    protected void registerMessageReceiver(String tag, SubmarineConnector.SubmarineMessageNotifyable receiver) {
        this.connection.registerMessageReceiver(tag, receiver);
    }

    /**
     * Removes a message receiver which will not be anymore informed about any incoming messages.
     * @param tag The name of the receiver to remove.
     */
    protected void removeMessageReceiver(String tag) {
        this.connection.removeMessageReceiver(tag);
    }

    /**
     * Registers a new status receiver which will be informed about any incoming message via the
     * implemented callback method.
     * @param tag The unique name of the receiver.
     * @param receiver The receiving object.
     */
    protected void registerConnectionStatusReceiver(String tag, SubmarineConnector.SubmarineConnectionNotifyable receiver) {
        this.connection.registerConnectionStatusReceiver(tag, receiver);
    }

    /**
     * Removes a status receiver which will not be anymore informed about any incoming messages.
     * implemented callback method.
     * @param tag The unique name of the receiver.
     */
    protected void removeConnectionStatusReceiver(String tag) {
        this.connection.removeConnectionStatusReceiver(tag);
    }

    /**
     * Issues a dive request to the submarine.
     * @param dive The dive to execute.
     * @return True of the connection allows to send the message, false otherwise.
     */
    protected boolean dive(Dive dive) {
        if(connection != null) {
            dive.setData(null);
            this.previousDives.add(dive);
            SubmarineProtos.ControlMessage diveMessage = SubmarineProtos.ControlMessage.newBuilder()
                    .setType(SubmarineProtos.ControlMessage.MessageType.DIVE)
                    .setDive(
                            SubmarineProtos.Dive.newBuilder()
                                    .setOffsetS(dive.getOffsetS())
                                    .setDepthM(dive.getDepthM())
                    )
                    .build();
            this.connection.send(diveMessage);
            this.status = SubmarineProtos.Status.StatusType.DIVE_SCHEDULED;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sends a cancel message to the submarine such that any scheduled dive will not be executed.
     * @return True of the connection allows to send the message, false otherwise.
     */
    protected boolean cancelDive() {
        if(this.connection != null) {
            this.status = SubmarineProtos.Status.StatusType.AVAILABLE;
            this.connection.send(
                    SubmarineProtos.ControlMessage.newBuilder()
                            .setType(SubmarineProtos.ControlMessage.MessageType.CANCEL_DIVE)
                            .build()
            );
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the submarine content with the given information.
     * @param message The message gotten from the submarine.
     */
    public void receiveMessage(SubmarineProtos.SubmarineMessage message) {
        switch(message.getType()) {
            case STATUS:
                updateStatus(message);
                break;
            case DATA:
                updateData(message.getDataList());
                break;
        }
    }

    /**
     * Sends a data update request to the submarine.
     */
    protected boolean updateData() {
        if(this.connection != null) {
            this.connection.send(
                    SubmarineProtos.ControlMessage.newBuilder()
                            .setType(SubmarineProtos.ControlMessage.MessageType.DATA_REQUEST)
                            .build()
            );
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the submarine with the given status update.
     * @param update The update to apply.
     */
    protected void updateStatus(SubmarineProtos.SubmarineMessage update) {
        if(update.getType() == SubmarineProtos.SubmarineMessage.MessageType.STATUS) {
            switch(update.getStatus().getType()) {
                case AVAILABLE:
                    Log.i(TAG, "New status: AVAILABLE");
                    break;
                case DIVE_SCHEDULED:
                    Log.i(TAG, "New status: DIVE_SCHEDULED");
                    break;
                case DIVING:
                    Log.i(TAG, "New status: DIVING");
                    break;
            }
            this.status = update.getStatus().getType();
        }
    }

    /**
     * Sends a status update request to the submarine.
     */
    protected void updateStatus() {
        SubmarineProtos.ControlMessage message = SubmarineProtos.ControlMessage.newBuilder()
                .setType(SubmarineProtos.ControlMessage.MessageType.STATUS_REQUEST)
                .build();
        this.connection.send(message);
    }

    /**
     * Sets the given data as the data from the last dive.
     * @param data The data to set.
     */
    private void updateData(List<SubmarineProtos.Datum> data) {
        // Adds data to last dive if no data is there.
        if(this.previousDives.size() > 0) {
            Dive lastDive = this.previousDives.get(this.previousDives.size() - 1);
            lastDive.setData(data);
        } else {
            // Edge case, creates new dive if nothing's there.
            this.previousDives.add(new Dive(10, 0, data));
        }
    }

    /**
     * Called when the connection status changes. If an automatic reconnect was set, we try another
     * connection.
     * @param status The new status of the connection.
     */
    public void receiveConnectionStatus(boolean status) {
        String statusString = "";
        if(status) statusString = "online"; else statusString = "offline";
        Log.i(TAG, "Submarine is now " + statusString + ".");
        if(!status && this.automaticReconnect) {
            connect(true);
        }
    }
}
