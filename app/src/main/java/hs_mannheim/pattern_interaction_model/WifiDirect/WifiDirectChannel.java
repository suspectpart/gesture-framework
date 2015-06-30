package hs_mannheim.pattern_interaction_model.wifidirect;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import hs_mannheim.pattern_interaction_model.model.ConnectionListener;
import hs_mannheim.pattern_interaction_model.model.IConnection;

public class WifiDirectChannel extends BroadcastReceiver implements IConnection,
        WifiP2pManager.ConnectionInfoListener {
    private static final int MSG_DATA_RECEIVED = 0xAA;
    private static final int MSG_CONNECTION_ESTABLISHED = 0xBB;
    private static final int MSG_CONNECTION_LOST = 0xCC;

    private final String TAG = "[WifiP2P Channel]";
    private final IntentFilter mIntentFilter;
    private final Handler _handler;

    private WifiP2pInfo connectedDeviceInfo = null;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private boolean isConnected;
    private ConnectionListener mListener;
    private ConnectedThread mConnectionThread;

    public WifiDirectChannel(WifiP2pManager manager, WifiP2pManager.Channel channel, Context context) {
        this.mManager = manager;
        this.mChannel = channel;

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Server.ACTION_DATA_RECEIVED);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        context.registerReceiver(this, mIntentFilter);

        this._handler = createListenerHandler();
    }

    private Handler createListenerHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch(message.what) {
                    case MSG_DATA_RECEIVED:
                        mListener.onDataReceived((String) message.obj);
                        break;
                    case MSG_CONNECTION_ESTABLISHED:
                        mListener.onConnectionEstablished();
                        break;
                    case MSG_CONNECTION_LOST:
                        mListener.onConnectionLost();
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void transfer(String data) {
        Log.d(TAG, "Sending " + data + " to " + connectedDeviceInfo.groupOwnerAddress);

        if (this.isConnected()) {
            this.mConnectionThread.write(data.getBytes());
        }
    }

    @Override
    public void register(ConnectionListener listener) {
        this.mListener = listener;
    }

    @Override
    public void connect(String address) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.groupOwnerIntent = 15;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Wifi P2P Connection established.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to establish Wifi P2P Connection.");
            }
        });
    }

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {
            onConnectionChanged(intent, context);
        }
    }

    private void onConnectionChanged(Intent intent, Context context) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

        if (networkInfo.isConnected()) {

            mManager.requestConnectionInfo(mChannel, this);
        } else {

            this.disconnected();
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        this.connectedDeviceInfo = info;

        if (info.groupFormed && info.isGroupOwner) { // group owner starts the server
            Log.d(TAG, "SERVER");
            new Server(8090, this).execute();
        } else if (info.groupFormed) { // and the other party connects to it
            Log.d(TAG, "CLIENT");
            new Client(info.groupOwnerAddress, 8090, this).execute();
        }
    }


    public void disconnected() {
        this.isConnected = false;
        this.mConnectionThread = null;
        _handler.obtainMessage(MSG_CONNECTION_LOST).sendToTarget();
    }

    public void receive(String data) {
        _handler.obtainMessage(MSG_DATA_RECEIVED, data).sendToTarget();
        Log.d(TAG, "Data received: " + data);
    }

    public void connected(ConnectedThread connectionThread) {
        isConnected = true;
        this.mConnectionThread = connectionThread;
        _handler.obtainMessage(MSG_CONNECTION_ESTABLISHED).sendToTarget();
        Log.d(TAG, "Connected to other device");
    }
}