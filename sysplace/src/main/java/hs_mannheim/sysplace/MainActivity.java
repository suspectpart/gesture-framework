package hs_mannheim.sysplace;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import hs_mannheim.gestureframework.ConfigurationBuilder;
import hs_mannheim.gestureframework.InteractionApplication;
import hs_mannheim.gestureframework.connection.bluetooth.ConnectionInfo;
import hs_mannheim.gestureframework.model.IConnection;
import hs_mannheim.gestureframework.model.ILifecycleListener;
import hs_mannheim.gestureframework.model.IPacketReceiver;
import hs_mannheim.gestureframework.model.IViewContext;
import hs_mannheim.gestureframework.model.MultipleTouchView;
import hs_mannheim.gestureframework.model.Packet;
import hs_mannheim.gestureframework.model.PacketType;
import hs_mannheim.gestureframework.model.Selection;

public class MainActivity extends AppCompatActivity implements IViewContext, IPacketReceiver, ILifecycleListener {
    protected final int REQUEST_ENABLE_BT = 100;

    private String TAG = "[Main Activity]";

    private BluetoothAdapter mBluetoothAdapter;
    private String mOldName;
    private String mCurrentName;
    private IConnection mConn;

    private MultipleTouchView mInteractionView;
    private TextView mTextView;
    private Button mPingButton;
    private Button mPhotoButton;
    private Button mDisconnectButton;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        bootstrapBluetooth();

        // TODO: handle this somewhere else!
        mInteractionView = new MultipleTouchView(findViewById(R.id.layout_main));

        mTextView = ((TextView) findViewById(R.id.textView));
        mPingButton = ((Button) findViewById(R.id.btn_ping));
        mPhotoButton = ((Button) findViewById(R.id.btn_send_photo));
        mDisconnectButton = ((Button) findViewById(R.id.btn_disconnect));

        ConfigurationBuilder builder = new ConfigurationBuilder(getApplicationContext(), this);
        builder
                .withBluetooth()
                .toConnect(builder.swipeLeftRight())
                .toSelect(builder.doubleTap())
                .toTransfer(builder.stitch())
                .toDisconnect(builder.bump())
                .select(new Selection(new Packet("Photo Received")))
                .registerForLifecycleEvents(this)
                .registerPacketReceiver(this)
                .buildAndRegister();

        // TODO: should be forbidden
        mConn = ((InteractionApplication) getApplicationContext()).getSysplaceContext().getConnection();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "Found " + device.getAddress());

                // TODO: if the server is faster, the client can not connect. Fix this.

                if (device.getName() != null && device.getName().contains("-sysplace-")) {
                    mConn.connect(ConnectionInfo.from(mCurrentName, device.getName(), device.getAddress()));
                    Toast.makeText(MainActivity.this, "Found " + device.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private void bootstrapBluetooth() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mOldName = mBluetoothAdapter.getName();
        mCurrentName = mOldName + "-sysplace-" + Integer.toString(new Random().nextInt(10000));
    }

    private void doBluetoothMagic() {
        setDiscoverable();
        mBluetoothAdapter.startDiscovery();
    }

    private void setDiscoverable() {
        if (!(mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
            startActivity(discoverableIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        Log.d(TAG, "Renaming to " + mCurrentName);
        mBluetoothAdapter.setName(mCurrentName);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "Renaming to " + mOldName);
        mBluetoothAdapter.setName(mOldName);
    }

    @Override
    public MultipleTouchView getMultipleTouchView() {
        return mInteractionView;
    }

    @Override
    public Point getDisplaySize() {
        //TODO: this only works properly in portrait mode. We have to subtract everything that
        // does not belong to the App (such as the StatusBar)
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return new Point(metrics.widthPixels, metrics.heightPixels);
    }

    public void disconnect(View view) {
        ((InteractionApplication) getApplicationContext()).getSysplaceContext().getConnection().disconnect();
    }

    @Override
    public void receive(Packet packet) {
        Log.d(TAG, "Packet received: " + packet.getMessage());
        switch (packet.getMessage()) {
            case "Connection established":
                mTextView.setText(R.string.connected_info);
                mTextView.setTextColor(Color.GREEN);
                mPingButton.setEnabled(true);
                mPhotoButton.setEnabled(true);
                mDisconnectButton.setEnabled(true);
                break;
            case "Connection lost":
                mTextView.setText(R.string.not_connected_info);
                mTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
                mPingButton.setEnabled(false);
                mPhotoButton.setEnabled(false);
                mDisconnectButton.setEnabled(false);
                break;
            default:
                Toast.makeText(this, packet.getMessage(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean accept(PacketType type) {
        return true;
    }

    public void switchToConnectedActivity(View view) {
        Intent intent = new Intent(this, ConnectedActivity.class);
        startActivity(intent);
    }

    public void ping(View view) {
        ((InteractionApplication) getApplicationContext()).getSysplaceContext().send(new Packet("Ping!"));
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connect Happened");
        doBluetoothMagic();
    }

    @Override
    public void onSelect() {
        Toast.makeText(this, "SELECT", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTransfer() {
        Toast.makeText(this, "TRANSFER", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(this, "DISCONNECT", Toast.LENGTH_SHORT).show();
    }
}

