package hs_mannheim.sysplace;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

import hs_mannheim.gestureframework.ConfigurationBuilder;
import hs_mannheim.gestureframework.connection.IConnection;
import hs_mannheim.gestureframework.messaging.IPacketReceiver;
import hs_mannheim.gestureframework.messaging.Packet;
import hs_mannheim.gestureframework.model.ILifecycleListener;
import hs_mannheim.gestureframework.model.ISysplaceContext;
import hs_mannheim.gestureframework.model.IViewContext;
import hs_mannheim.gestureframework.model.InteractionApplication;
import hs_mannheim.gestureframework.model.MultipleTouchView;
import hs_mannheim.gestureframework.model.Selection;
import hs_mannheim.gestureframework.model.SysplaceContext;

public class MainActivity extends AppCompatActivity implements IViewContext, IPacketReceiver, ILifecycleListener {
    private String TAG = "[Main Activity]";

    private BluetoothAdapter mBluetoothAdapter;
    private String mOldName;
    private String mCurrentName;

    private MultipleTouchView mInteractionView;
    private TextView mTextView;
    private Button mPingButton;
    private Button mPhotoButton;
    private Button mDisconnectButton;
    private EditText mTextfield;
    private SysplaceContext mSysplaceContext;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        bootstrapBluetooth();

        // TODO: don't let the client do this, should be somewhere in the framework
        mInteractionView = new MultipleTouchView(findViewById(R.id.layout_main));

        mTextView = ((TextView) findViewById(R.id.textView));
        mPingButton = ((Button) findViewById(R.id.btn_ping));
        mPhotoButton = ((Button) findViewById(R.id.btn_send_photo));
        mDisconnectButton = ((Button) findViewById(R.id.btn_disconnect));
        mTextfield = ((EditText) findViewById(R.id.et_tosend));

        ConfigurationBuilder builder = new ConfigurationBuilder(getApplicationContext(), this);
        builder
                .withBluetooth()
                .toConnect(builder.swipeLeftRight())
                .toSelect(builder.doubleTap())
                .toTransfer(builder.swipeUpDown())
                .toDisconnect(builder.bump())
                .select(Selection.Empty)
                .registerForLifecycleEvents(this)
                .registerPacketReceiver(this)
                .buildAndRegister();

        mSysplaceContext = ((InteractionApplication) getApplicationContext()).getSysplaceContext();
        mTextfield.addTextChangedListener(new SysplaceTextWatcher(mSysplaceContext));
    }

    // TODO: move elsewhere
    private void bootstrapBluetooth() {
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mOldName = mBluetoothAdapter.getName();
        mCurrentName = mOldName + "-sysplace-" + Integer.toString(new Random().nextInt(10000));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: move elsewhere
        Log.d(TAG, "Renaming to " + mCurrentName);
        mBluetoothAdapter.setName(mCurrentName);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: move elsewhere
        Log.d(TAG, "Renaming to " + mOldName);
        mBluetoothAdapter.setName(mOldName);
    }

    public void ping(View view) {
        ((InteractionApplication) getApplicationContext()).getSysplaceContext().send(new Packet("Ping!"));
    }

    public void switchToConnectedActivity(View view) {
        startActivity(new Intent(this, ConnectedActivity.class));
    }

    public void disconnect(View view) {
        mSysplaceContext.getConnection().disconnect();
    }

    // IViewContext Stuff

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

    // IPacketReceiver Stuff

    @Override
    public boolean accept(Packet.PacketType type) {
        return true;
    }

    @Override
    public void receive(Packet packet) {
        switch (packet.getType()) {
            case ConnectionEstablished:
                mTextView.setText(R.string.connected_info);
                mTextView.setTextColor(Color.GREEN);
                mPingButton.setEnabled(true);
                mPhotoButton.setEnabled(true);
                mDisconnectButton.setEnabled(true);
                break;
            case ConnectionLost:
                mTextView.setText(R.string.not_connected_info);
                mTextView.setTextColor(getResources().getColor(android.R.color.holo_red_light, null));
                mPingButton.setEnabled(false);
                mPhotoButton.setEnabled(false);
                mDisconnectButton.setEnabled(false);
                break;
            case Image:
                // because the other View displays images
                break;
            default:
                Toast.makeText(this, packet.getMessage(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // ILifecycleListener Stuff

    @Override
    public void onConnect() {
        Toast.makeText(this, "CONNECT", Toast.LENGTH_SHORT).show();

        // TODO: move elsewhere
        startService(new Intent(this, BluetoothPairingService.class));
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

