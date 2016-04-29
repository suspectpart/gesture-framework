package hs_mannheim.gestureframework.connection.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.database.Observable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.UUID;

import hs_mannheim.gestureframework.model.IConnection;
import hs_mannheim.gestureframework.model.IConnectionListener;
import hs_mannheim.gestureframework.model.Packet;

public class BluetoothChannel extends Observable<IConnectionListener> implements IConnection {

    public final static UUID MY_UUID = UUID.fromString("0566981a-1c02-11e5-9a21-1697f925ec7b");
    private final int MSG_DATA_RECEIVED = 0x0A;
    private final int MSG_CONNECTION_ESTABLISHED = 0x0B;
    private final int MSG_CONNECTION_LOST = 0x0C;

    private final String TAG = "[BluetoothChannel]";

    private final Handler mHandler;
    private BluetoothDevice mConnectedDevice;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean mIsConnected = false;
    private ConnectedThread mConnectionThread;

    public BluetoothChannel(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
        mHandler = createListenerHandler();
    }

    private Handler createListenerHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_DATA_RECEIVED:
                        for (IConnectionListener listener : mObservers) {
                            listener.onDataReceived((Packet) message.obj);
                        }
                        break;
                    case MSG_CONNECTION_ESTABLISHED:
                        for (IConnectionListener listener : mObservers) {
                            listener.onConnectionEstablished();
                        }
                        break;
                    case MSG_CONNECTION_LOST:
                        for (IConnectionListener listener : mObservers) {
                            listener.onConnectionLost();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void disconnect() {
        if (!isConnected()) {
            return;
        }

        mIsConnected = false;

        Log.d(TAG, "disconnecting...");

        // this will close the socket, NOT the P2P connection
        mConnectionThread.cancel();
        mConnectionThread = null;

        mHandler.obtainMessage(MSG_CONNECTION_LOST).sendToTarget();
    }

    public String getConnectedDevice() {
        return this.isConnected() ? this.mConnectedDevice.getAddress() : "";
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public void transfer(final Packet message) {
        if (mIsConnected) {
            mConnectionThread.write(message);
        }
    }

    @Override
    public void register(IConnectionListener listener) {
        registerObserver(listener);
    }

    @Override
    public void unregister(IConnectionListener listener) {
        unregisterObserver(listener);
    }

    public void connect(ConnectionInfo connectionInfo) {
        if (isConnected()) return;

        mConnectedDevice = mBluetoothAdapter.getRemoteDevice(connectionInfo.getMacAddress());

        Log.d(TAG, String.format("Device to connect to: %s", mConnectedDevice.getName()));

        if (connectionInfo.isServer()) {
            Log.d(TAG, "Connecting as server.");
            new AcceptThread(this, mBluetoothAdapter).start();
        } else {
            Log.d(TAG, "Connecting as client.");
            new ConnectThread(mConnectedDevice, this, mBluetoothAdapter).start();
        }
    }

    // TODO: This interface is not needed anymore
    public void connect(String address) {
        // not supported
    }

    public void receive(Packet data) {
        Log.d(TAG, "Data received: " + data);
        mHandler.obtainMessage(MSG_DATA_RECEIVED, data).sendToTarget();
    }

    public void connected(ConnectedThread connectionThread) {
        mIsConnected = true;
        this.mConnectionThread = connectionThread;
        mHandler.obtainMessage(MSG_CONNECTION_ESTABLISHED).sendToTarget();
    }
}
