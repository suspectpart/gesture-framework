package hs_mannheim.sysplace;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
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
import hs_mannheim.gestureframework.model.Packet;
import hs_mannheim.gestureframework.model.PacketType;
import hs_mannheim.gestureframework.model.Selection;
import hs_mannheim.gestureframework.model.SysplaceContext;

public class MainActivity extends AppCompatActivity implements IViewContext, IPacketReceiver, ILifecycleListener {
    protected final int REQUEST_ENABLE_BT = 100;
    protected final int LOCATION_REQUEST = 1337;

    private String TAG = "[Main Activity]";

    private BluetoothAdapter mBluetoothAdapter;
    private String mOldName;
    private String mCurrentName;
    private IConnection mConn;

    private SysplaceContext mSysplaceContext;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ConfigurationBuilder builder = new ConfigurationBuilder(getApplicationContext(), this);
        builder
                .withBluetooth()
                .specifyGestureComposition(builder.swipe(), builder.bump(), builder.bump(), builder.bump())
                .select(new Selection(new Packet("Empty")))
                .buildAndRegister();

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mSysplaceContext = ((InteractionApplication) getApplicationContext()).getSysplaceContext();
        mSysplaceContext.registerForLifecycleEvents(this);
        mSysplaceContext.registerPacketReceiver(this);
        mConn = mSysplaceContext.getConnection(); // should be forbidden

        // enable bluetooth
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);

        mOldName = mBluetoothAdapter.getName();
        mCurrentName = mOldName + "-sysplace-" + Integer.toString(new Random().nextInt(10000));
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
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
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
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        Log.d(TAG, "Renaming to " + mOldName);
        mBluetoothAdapter.setName(mOldName);
    }

    @Override
    public View getInteractionView() {
        return findViewById(R.id.layout_main);
    }

    @Override
    public Point getDisplaySize() {
        return null;
    }

    public void disconnect(View view) {
        ((InteractionApplication) getApplicationContext()).getSysplaceContext().getConnection().disconnect();
    }

    @Override
    public void receive(Packet packet) {
        Log.d(TAG, "Packet received!");
        if (packet.getMessage().equals("Connection established")) {
            ((TextView) findViewById(R.id.textView)).setText("Connected");
        } else if (packet.getMessage().equals("Connection lost")) {
            ((TextView) findViewById(R.id.textView)).setText("NOT Connected");
        } else {
            Toast.makeText(this, packet.getMessage(), Toast.LENGTH_SHORT).show();
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
        mSysplaceContext.send(new Packet("Ping!"));
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "Connect Happened");
        doBluetoothMagic();
    }

    @Override
    public void onSelect() {
        Log.d(TAG, "Select Happened");
    }

    @Override
    public void onTransfer() {
        Log.d(TAG, "Transfer Happened");
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "Disconnect Happened");
    }
}

