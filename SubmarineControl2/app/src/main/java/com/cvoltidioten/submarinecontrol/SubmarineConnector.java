package com.cvoltidioten.submarinecontrol;

abstract class SubmarineConnector {
    /**
     * Connects to the submarine if the connection can be established. Returns true if the
     * connection process could be started successfully and false otherwise. This method returns
     * immediately and does not wait for the connection buildup to finish.
     * @return True if the connection buildup was successfully started, false otherwise.
     */
    abstract protected boolean connect();

    /**
     * Disconnects from the submarine. Returns immediately and does not wait for the disconnection
     * to finish. If the connection is currently building up, we will stop trying to establish this
     * connection.
     * @return True if the disconnect process was successfully initiated, false otherwise.
     */
    abstract protected boolean disconnect();

    /**
     * Returns true iff the connection is active and working.
     * @return True iff the connection is active and working.
     */
    abstract protected boolean isConnected();

    /**
     * Sends the given bytes to the submarine. Returns immediately and does not wait for the
     * sending process to finish.
     * @param message The data to send.
     * @return True if the sending process was initiated successfully, false otherwise.
     */
    abstract protected boolean send(SubmarineProtos.ControlMessage message);

    /**
     * Registers a new receiver which is notified on arrival of any message of the submarine.
     * @param receiver The receiver to register.
     */
    abstract protected void registerMessageReceiver(String tag, SubmarineMessageNotifyable receiver);

    /**
     * Removes an existing receiver.
     * @param tag The receiver to remove.
     */
    abstract protected void removeMessageReceiver(String tag);

    /**
     * Registers a new connection status receiver which is notified on any connection status change.
     * @param receiver The receiver to register.
     */
    abstract protected void registerConnectionStatusReceiver(String tag, SubmarineConnectionNotifyable receiver);

    /**
     * Removes an existing receiver.
     * @param tag The receiver to remove.
     */
    abstract protected void removeConnectionStatusReceiver(String tag);

    /**
     * Should be implemented by any member that wants to receive updates on new submarine messages.
     */
    interface SubmarineMessageNotifyable {
        void receiveMessage(SubmarineProtos.SubmarineMessage message);
    }

    /**
     * Should be implemented by any member that wants to receive updates on the submarine connection
     * status.
     */
    interface SubmarineConnectionNotifyable {
        void receiveConnectionStatus(boolean status);
    }

    /**
     * Is thrown in case the necessary hardware for the connection is not available.
     */
    protected class HardwareException extends Exception {
    }
}
