package hs_mannheim.gestureframework.connection;

import android.database.Observable;
import android.util.Log;

import hs_mannheim.gestureframework.model.IConnection;
import hs_mannheim.gestureframework.model.IPacketReceiver;
import hs_mannheim.gestureframework.model.IPostOffice;
import hs_mannheim.gestureframework.model.Packet;

/**
 * Manages all packet traffic in an interaction context. It can send packages over a connection or
 * receive packages from the connection and distribute them across the InteractionContext.
 */
public class PostOffice extends Observable<IPacketReceiver> implements IPostOffice {

    private IConnection mConnection;

    public PostOffice(IConnection connection) {
        mConnection = connection;
        mConnection.register(this);
    }

    /**
     * Sends a packet through the connection.
     * @param packet to transfer
     */
    public void send(Packet packet) {
        mConnection.transfer(packet);
    }

    /**
     * Registers a packet receiver for certain types of packets.
     * @param receiver to register
     */
    @Override
    public void register(IPacketReceiver receiver) {
        this.registerObserver(receiver);
    }

    /**
     * Unregisters a packet receiver from receiving any packets.
     * @param receiver to unregister
     */
    @Override
    public void unregister(IPacketReceiver receiver) {
        this.unregisterObserver(receiver);
    }

    /**
     * Receives all incoming packets and distributes them to the corresponding receivers.
     * @param packet to be distributes
     */
    @Override
    public void receive(Packet packet) {
        for (IPacketReceiver receiver : this.mObservers) {
            if (receiver.accept(packet.getType())) {
                receiver.receive(packet);
            }
        }
    }

    @Override
    public void onConnectionLost() {
        receive(new Packet("Connection lost"));
    }

    @Override
    public void onConnectionEstablished() {
        receive(new Packet("Connection established"));
    }

    @Override
    public void onDataReceived(Packet packet) {
        Log.d("[PostOffice]", "Received packet: " + packet.toString());
        receive(packet);
    }
}