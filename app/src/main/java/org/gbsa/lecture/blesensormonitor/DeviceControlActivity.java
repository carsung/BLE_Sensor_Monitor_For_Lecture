package org.gbsa.lecture.blesensormonitor;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends AppCompatActivity {
    private static final String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServiceList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    MqttAndroidClient mqttAndroidClient;
    private final static String HIVEMQ_SERVER_URI = "tcp://broker.hivemq.com";
    private final static String HIVEMQ_CLIENT_ID = "GBSA_TEST_01";
    private final static String HIVEMQ_TOPIC = "gbsa/5g";
    private final static String SERVER_URI = "tcp://iot.amtel.co.kr";
    private final static String CLIENT_ID = "GBSA_TEST_01";
    private final static String TOPIC = "v1/devices/me/telemetry";

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                clearUi();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private final ExpandableListView.OnChildClickListener serviceListClickListener =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    // todo 5
                    // characteristic을 구하여

                    // todo 6
                    // BluetoothLeService.readCharacteristic()으로 전달하고
                    // characteristic data가 변경되면 처리될 수 있도록
                    // mBluetoothLeService.setCharacteristicNotification()을 호출한다.

                    return true;
                }
            };

    private void clearUi() {
        mGattServiceList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServiceList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServiceList.setOnChildClickListener(serviceListClickListener);
        mConnectionState = findViewById(R.id.connection_state);
        mDataField = findViewById(R.id.data_value);
        Button send = findViewById(R.id.btn_send);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "{\"batt_state\":\"" + mDataField.getText() + "\"}";
                MqttMessage message = new MqttMessage();
                message.setPayload(data.getBytes());

                // todo 9
                // mqtt publish
            }
        });

        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Todo 2
        // BluetoothLeService binding
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        // todo 8
        // MqttAndroidClient.connect()를 이용하여 MQTT broker에 연결한다.
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    private void updateConnectionState(final int resId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }


    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gatCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String , String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharData);
            }
            mGattCharacteristics.add(charas);
            gatCharacteristicData.add(gattCharacteristicGroupData);

            SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                    this,
                    gattServiceData,
                    android.R.layout.simple_expandable_list_item_2,
                    new String[] {LIST_NAME, LIST_UUID},
                    new int[] { android.R.id.text1, android.R.id.text2},
                    gatCharacteristicData,
                    android.R.layout.simple_expandable_list_item_2,
                    new String[] {LIST_NAME, LIST_UUID},
                    new int[] {android.R.id.text1, android.R.id.text2}
            );
            mGattServiceList.setAdapter(gattServiceAdapter);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
