package com.example.zqh.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ENABLE_BT = 10;


    BluetoothHeadset bluetoothHeadset;
    BluetoothAdapter bluetoothAdapter;
    private Button bluetooth_open_btn;
    private Button bluetooth_setting_btn;
    private TextView mProgressTextView;
    private ProgressBar progressBar;
    private Button mDisConnectBtn;
    private Button mFindDevicesBtn;

    private RecyclerView mRecyclerView;


    private List<BluetoothDeviceData> mDevices;
    private BluetoothDevicesAdapter mAdapter;

    /**
     * 蓝牙连接后的实时通信api
     */
    private BluetoothGatt mBluetoothGatt;
    private int mBluetoothState = BluetoothProfile.STATE_DISCONNECTED;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();


    }

    private void initView() {
        bluetooth_open_btn = findViewById(R.id.bluetooth_open_btn);
        bluetooth_setting_btn = findViewById(R.id.bluetooth_setting_btn);
        mRecyclerView = findViewById(R.id.bluetooth_recyclerview);
        mProgressTextView = findViewById(R.id.progress_tv);
        progressBar = findViewById(R.id.progress);
        mDisConnectBtn = findViewById(R.id.bluetooth_disconnect_btn);
        mFindDevicesBtn = findViewById(R.id.bluetooth_finddevices_btn);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        bluetooth_open_btn.setOnClickListener(this);
        bluetooth_setting_btn.setOnClickListener(this);
        mDisConnectBtn.setOnClickListener(this);
        mFindDevicesBtn.setOnClickListener(this);

    }


    private void initData() {


        //获取已经配对的蓝牙列表
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean hasBluetooth = bluetoothAdapter != null;
        if (hasBluetooth && bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            int size = devices.size();
            Log.i("BLUETOOTHTAG", "bluetooth count : " + size);
            if (size > 0) {
                mDevices = new ArrayList<>();
            }

            for (BluetoothDevice device : devices) {
                String name = device.getName();
                String address = device.getAddress();
                int bluestate = device.getBondState();//蓝牙的状态
                int blueType = 0;//蓝牙的类型
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    blueType = device.getType();
                }
//                mDevices.put(address, name);
                mDevices.add(new BluetoothDeviceData(name, address, bluestate, blueType));
                Log.i("BLUETOOTHTAG", "name : " + name + "\n address : " + address + "\n bluestate： " + bluestate + "\n blueType： " + blueType);
            }
        }

        mAdapter = new BluetoothDevicesAdapter(this, mDevices);
        mRecyclerView.setAdapter(mAdapter);

        //注册广播监听蓝牙连接状态
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    /**
     * 更新蓝牙连接状态
     */
    private void updateConnectState() {
        boolean showProgress = false;
        String progressText = null;
        String deviceName="";
        if(mBluetoothGatt!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                deviceName=mBluetoothGatt.getDevice().getName();
            }
        }

        if (mBluetoothState == BluetoothProfile.STATE_DISCONNECTED) {
            showProgress = false;
            progressText = deviceName+" disconnect";
            release();
        } else if (mBluetoothState == BluetoothProfile.STATE_CONNECTING) {
            showProgress = true;
            progressText = deviceName+" connecting ...";
        } else if (mBluetoothState == BluetoothProfile.STATE_CONNECTED) {
            showProgress = false;
            progressText = deviceName+" connect success";
        }
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
        mProgressTextView.setText(progressText);
    }

    /**
     * 蓝牙扫描设备的提示
     * @param showProgress
     * @param tipStr
     */
    private void updateBluetoothDiscoverTip(boolean showProgress,String tipStr){
        progressBar.setVisibility(showProgress ? View.VISIBLE : View.INVISIBLE);
        mProgressTextView.setText(tipStr);
    }

    /**
     * 蓝牙连接与断开连接监听
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("BLUETOOTHTAG", "onReceive action: "+action);
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                int bluetoothState = device.getBondState();
                Log.i("BLUETOOTHTAG", "connect device deviceName : " + deviceName + "\n deviceHardwareAddress : " + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState);
            }else if(BluetoothDevice.ACTION_FOUND.equals(action)){//发现设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                int bluetoothState = device.getBondState();
                updateBluetoothDiscoverTip(true,"find device:  "+deviceName+"  address: "+deviceHardwareAddress);
                Log.i("BLUETOOTHTAG", "find device deviceName : " + deviceName + "\n deviceHardwareAddress : " + deviceHardwareAddress + "\n bluetoothState： " + bluetoothState);
            }
        }
    };

    /**
     * 打开蓝牙
     */
    private void goBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * 打开系统蓝牙设置界面
     */
    private void openSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    /**
     * 扫描设备
     */
    private void disconverDevices(){
        //当前有连接设备时，不能扫描，因为扫描会占用大量带宽
        if(mBluetoothGatt!=null) return;
        if(bluetoothAdapter.isDiscovering()){//如果在扫描，就关闭扫描
           bluetoothAdapter.cancelDiscovery();
           updateBluetoothDiscoverTip(false,"Cancel Discobery");
           return;
        }
        if (bluetoothAdapter!=null&&bluetoothAdapter.isEnabled()) {
            //第一种方式扫描(广播)
            boolean dicover =  bluetoothAdapter.startDiscovery();
            updateBluetoothDiscoverTip(true,"Start Discobery...");
            Log.i("BLUETOOTHTAG", "discover result: "+dicover);
            //第二种方式,回调
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//                BluetoothLeScanner leScanner =bluetoothAdapter.getBluetoothLeScanner();
//                leScanner.startScan(scanCallback);
//            }
        }
    }



    /**
     * 蓝牙扫描的回调
     */
  private ScanCallback scanCallback =  new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//                        super.onScanResult(callbackType, result);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.i("BLUETOOTHTAG", "discover onScanResult callbackType: "+callbackType+ "device name: "+result.getDevice().getName());
            }

        }

        @Override
        public void onScanFailed(int errorCode) {
//            super.onScanFailed(errorCode);
            Log.i("BLUETOOTHTAG", "discover onScanFailed");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
//            super.onBatchScanResults(results);
            Log.i("BLUETOOTHTAG", "discover onBatchScanResults");
        }
    };
//    private BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
//        public void onServiceConnected(int profile, BluetoothProfile proxy) {
//            if (profile == BluetoothProfile.HEADSET) {
//                bluetoothHeadset = (BluetoothHeadset) proxy;
//            }
//            Log.i("BLUETOOTHTAG","onServiceConnected profile: "+profile);
//        }
//        public void onServiceDisconnected(int profile) {
//            if (profile == BluetoothProfile.HEADSET) {
//                bluetoothHeadset = null;
//            }
//            Log.i("BLUETOOTHTAG","onServiceDisconnected profile: "+profile);
//        }
//    };

    /**
     * 断开蓝牙连接
     */
    private void disConnectBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (mBluetoothGatt == null) return;
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * 连接蓝牙
     *
     * @param address
     */
    private void connectBluetooth(String address) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled() || TextUtils.isEmpty(address))
            return;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGatt = device.connectGatt(this, false, gattCallback);
        }
        mBluetoothState = BluetoothProfile.STATE_CONNECTING;
        updateConnectState();
    }

    /**
     * 释放相关资源
     */
    public void release() {
        if (mBluetoothGatt == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
    }

    //手动触发蓝牙连接的回调
    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("BLUETOOTHTAG", "onConnectionStateChange status: " + status + " newState: " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {//已连接
                mBluetoothState = newState;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//连接失败
                mBluetoothState = newState;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConnectState();

                }
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_ENABLE_BT) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bluetooth_open_btn://打开蓝牙
                goBluetooth();
                break;
            case R.id.bluetooth_setting_btn://打开系统蓝牙界面
                openSetting();
                break;
            case R.id.bluetooth_disconnect_btn://断开蓝牙连接
                disConnectBluetooth();
                break;
            case R.id.bluetooth_finddevices_btn://扫描蓝牙
                disconverDevices();
                break;
        }
    }


    public class BluetoothDevicesAdapter extends RecyclerView.Adapter<BluetoothDevicesAdapter.BluetoothViewHolder> {

        private Context mContext;
        private List<BluetoothDeviceData> devices;
        private LayoutInflater inflater;

        public BluetoothDevicesAdapter(Context context, List<BluetoothDeviceData> devices) {
            mContext = context;
            this.devices = devices;
            inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public BluetoothViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = inflater.inflate(R.layout.bluetooth_item, viewGroup, false);
            return new BluetoothViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BluetoothViewHolder bluetoothViewHolder, int i) {
            BluetoothDeviceData deviceData = devices.get(i);

            bluetoothViewHolder.title.setText(deviceData.getName());
            bluetoothViewHolder.subTitle.setText(deviceData.getAddress());

            bluetoothViewHolder.itemView.setTag(i);
            bluetoothViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer pos = (Integer) v.getTag();
                    String address = devices.get(pos).getAddress();
                    connectBluetooth(address);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (devices != null) {
                return devices.size();
            }
            return 0;
        }

        public class BluetoothViewHolder extends RecyclerView.ViewHolder {

            private TextView title;
            private TextView subTitle;


            public BluetoothViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.bluetooth_item_title);
                subTitle = itemView.findViewById(R.id.bluetooth_item_subtitle);

            }
        }
    }


    public class BluetoothDeviceData {
        private String name;
        private String address;
        private int state;
        private int type;

        public BluetoothDeviceData(String name, String address, int state, int type) {
            this.name = name;
            this.address = address;
            this.state = state;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}
