package kong.qingwei.opencv320;

import android.Manifest;
import android.app.Activity;    //
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.kongqw.permissionslibrary.PermissionsManager;

public class MainActivity extends BaseActivity {

/************************ 蓝牙相关变量以下 ***********************************/

    public static Activity activity;
    public static Context context;
    private static Toast mToast;

    private static final String TAG = "MainActivity";
    public static final boolean D = BuildConfig.DEBUG; // This is automatically set when building

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_DISCONNECTED = 4;
    public static final int MESSAGE_RETRY = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothHandler mBluetoothHandler = null;
    // Member object for the chat services
    public static BluetoothChatService mChatService = null;

    private BluetoothDevice btDevice; // The BluetoothDevice object 蓝牙设备
    private boolean btSecure; // If it's a new device we will pair with the device
    public static boolean stopRetrying;

//    public static int currentTabSelected;

    public static String accValue = "";
    public static String gyroValue = "";
    public static String kalmanValue = "";
    public static boolean newIMUValues;

    public static String Qangle = "";
    public static String Qbias = "";
    public static String Rmeasure = "";
    public static boolean newKalmanValues;

    public static String pValue = "";
    public static String iValue = "";
    public static String dValue = "";
    public static String targetAngleValue = "";
    public static boolean newPIDValues;

    public static boolean backToSpot;
    public static int maxAngle = 8; // Eight is the default value
    public static int maxTurning = 20; // Twenty is the default value

    public static String appVersion;
    public static String firmwareVersion;
    public static String eepromVersion;
    public static String mcu;
    public static boolean newInfo;

    public static String batteryLevel;
    public static double runtime;
    public static boolean newStatus;

    public static boolean pairingWithDevice;

//    public static boolean buttonState;
//    public static boolean joystickReleased;

    public final static String getPIDValues = "GP;";
    public final static String getSettings = "GS;";
    public final static String getInfo = "GI;";
    public final static String getKalman = "GK;";

//    public final static String setPValue = "SP,";
//    public final static String setIValue = "SI,";
//    public final static String setDValue = "SD,";
//    public final static String setKalman = "SK,";
//    public final static String setTargetAngle = "ST,";
//    public final static String setMaxAngle = "SA,";
//    public final static String setMaxTurning = "SU,";
//    public final static String setBackToSpot = "SB,";

    public final static String imuBegin = "IB;";
    public final static String imuStop = "IS;";

    public final static String statusBegin = "RB;";
    public final static String statusStop = "RS;";

    public final static String sendStop = "CS;";
    public final static String sendIMUValues = "CM,";
    public final static String sendJoystickValues = "CJ,";
    public final static String sendPairWithWii = "CPW;";
    public final static String sendPairWithPS4 = "CPP;";

    // 追踪命令
    public final static String sendTrackedObject = "ST,";
    // 同步命令
    public final static String sendMatSize = "SM,";

    public final static String restoreDefaultValues = "CR;";

    public final static String responsePIDValues = "P";
    public final static String responseKalmanValues = "K";
    public final static String responseSettings = "S";
    public final static String responseInfo = "I";
    public final static String responseIMU = "V";
    public final static String responseStatus = "R";
    public final static String responsePairConfirmation = "PC";

    public final static int responsePIDValuesLength = 5;
    public final static int responseKalmanValuesLength = 4;
    public final static int responseSettingsLength = 4;
    public final static int responseInfoLength = 4;
    public final static int responseIMULength = 4;
    public final static int responseStatusLength = 3;
    public final static int responsePairConfirmationLength = 1;

/******************************** 蓝牙相关以上 ***********************************/

    private PermissionsManager mPermissionsManager;

    // 要校验的权限
    private final String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    // 识别请求码
    private final int REQUEST_CODE_DETECTION = 0;
    // 追踪请求码
    private final int REQUEST_CODE_TRACK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /* 创建界面 */

//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        super.onCreate(savedInstanceState);
        activity = this;
        context = getApplicationContext();

        if (!getResources().getBoolean(R.bool.isTablet))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Set portrait mode only - for small screens like phones
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER); // Full screen rotation
            else
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR); // Full screen rotation
            new Handler().postDelayed(new Runnable()
            { // Hack to hide keyboard when the layout it rotated
                @Override
                public void run()
                {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); // Hide the keyboard - this is needed when the device is rotated
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getApplicationWindowToken(), 0);
                }
            }, 1000);
        }

        setContentView(R.layout.activity_main);


        /* 获取本地蓝牙适配器 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        else
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            showToast("Bluetooth is not available", Toast.LENGTH_LONG);
            finish();
            return;
        }

        try {
            PackageManager mPackageManager = getPackageManager();
            if (mPackageManager != null)
                MainActivity.appVersion = mPackageManager.getPackageInfo(getPackageName(), 0).versionName; // Read the app version name
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // 动态权限校验
        mPermissionsManager = new PermissionsManager(this) {

            @Override
            public void authorized(int requestCode) {
                // 权限通过
                switch (requestCode) {
                    case REQUEST_CODE_DETECTION:
                        startActivity(new Intent(MainActivity.this, ObjectDetectingActivity.class));
                        break;
                    case REQUEST_CODE_TRACK:
                        startActivity(new Intent(MainActivity.this, ObjectTrackingActivity.class));
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void noAuthorization(int requestCode, String[] lacksPermissions) {
                // 缺少必要权限
                showPermissionDialog();
            }

            @Override
            public void ignore(int requestCode) {
                // Android 6.0 以下系统不校验
                authorized(requestCode);
            }
        };
    }

    /**
     * 复查权限
     *
     * @param requestCode  requestCode
     * @param permissions  permissions
     * @param grantResults grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 用户做出选择以后复查权限，判断是否通过了权限申请
        mPermissionsManager.recheckPermissions(requestCode, permissions, grantResults);
    }

    /**
     * 目标检测
     *
     * @param view view
     */
    public void onDetecting(View view) {
        // 检查权限
        mPermissionsManager.checkPermissions(REQUEST_CODE_DETECTION, PERMISSIONS);
    }

    /**
     * 目标追踪
     *
     * @param view view
     */
    public void onTracking(View view) {
        // 检查权限
        mPermissionsManager.checkPermissions(REQUEST_CODE_TRACK, PERMISSIONS);
    }

/*********************************** APP运行相关 *********************************/
    @Override
    public void onStart()
    {
        super.onStart();    // 指向父类的引用
        if (D)
            Log.d(TAG, "++ ON START ++");
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            if (D)
                Log.d(TAG, "Request enable BT");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);    // 开启蓝牙服务
        } else{
            if (D)
                Log.d(TAG, "Set up BT");
            setupBTService(); // Otherwise, setup the chat session
        }


        /*
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this); // Create SharedPreferences instance
        String filterCoefficient = preferences.getString("filterCoefficient", null); // Read the stored value for filter coefficient
        if (filterCoefficient != null) {
            mSensorFusion.filter_coefficient = Float.parseFloat(filterCoefficient);
            mSensorFusion.tempFilter_coefficient = mSensorFusion.filter_coefficient;
        }
        // Read the previous back to spot value
        backToSpot = preferences.getBoolean("backToSpot", true); // Back to spot is true by default
        // Read the previous max angle
        maxAngle = preferences.getInt("maxAngle", 8); // Eight is the default value
        // Read the previous max turning value
        maxTurning = preferences.getInt("maxTurning", 20); // Twenty is the default value
        */
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D)
            Log.d(TAG, "-- ON STOP --");
        /*
        // unregister sensor listeners to prevent the activity from draining the
        // device's battery.
        // 关闭传感器监听，保护设备电源

        mSensorFusion.unregisterListeners();

        // Store the value for FILTER_COEFFICIENT and max angle at shutdown
        Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
        edit.putString("filterCoefficient", Float.toString(mSensorFusion.filter_coefficient));
        edit.putBoolean("backToSpot", backToSpot);
        edit.putInt("maxAngle", maxAngle);
        edit.putInt("maxTurning", maxTurning);
        edit.commit();
        */
    }

    @Override
    public void onBackPressed()
    {
        if (mChatService != null) {
            new Handler().postDelayed(new Runnable()
            {
                public void run()
                {
                    mChatService.stop(); // Stop the Bluetooth chat services if the user exits the app
                }
            }, 1000); // Wait 1 second before closing the connection, this is needed as onPause() will send stop messages before closing
        }
        finish(); // Exits the app
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (D)
            Log.d(TAG, "--- ON DESTROY ---");
//        mSensorFusion.unregisterListeners();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (D)
            Log.d(TAG, "- ON PAUSE -");
        // Unregister sensor listeners to prevent the activity from draining the device's battery.
//        mSensorFusion.unregisterListeners();
        if (mChatService != null) { // Send stop command and stop sending graph data command
            if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
                mChatService.write(sendStop + imuStop + statusStop);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (D)
            Log.d(TAG, "+ ON RESUME +");
        // Restore the sensor listeners when user resumes the application.
//        mSensorFusion.initListeners();
    }

/****************************** 菜单相关 *********************************/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {   // 蓝牙连接成功，修改菜单图标为连接状态
        if (D)
            Log.d(TAG, "onPrepareOptionsMenu");
        MenuItem menuItem = menu.findItem(R.id.menu_connect); // Find item
        if (mChatService != null && mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
            menuItem.setIcon(R.drawable.device_access_bluetooth_connected);
        else
            menuItem.setIcon(R.drawable.device_access_bluetooth);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if (D)
            Log.d(TAG, "onCreateOptionsMenu");
        //getSupportMenuInflater().inflate(R.menu.menu, menu); // Inflate the menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.menu_settings:
                // Open up the settings dialog
//                SettingsDialogFragment dialogFragment = new SettingsDialogFragment();
                /* to be fix
                dialogFragment.show(getSupportFragmentManager(), null);
                 */
                // 设置界面
//                dialogFragment.show(getSupportFragmentManager(), null);

                return true;
            case android.R.id.home:
//                Log.d(TAG, "onOptionsItemSelected: Here is for home.");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://pengzhihui.xyz/"));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

/******************************** 主要蓝牙相关以下 ***************************************/

    // 初始化蓝牙通信服务
    private void setupBTService()
    {
        if (mChatService != null) {
            if (D)
                Log.d(TAG, "mChatService is not null.");
            return;
        }


        if (D)
            Log.d(TAG, "setupBTService()");
        if (mBluetoothHandler == null)
            mBluetoothHandler = new BluetoothHandler(this);
        mChatService = new BluetoothChatService(mBluetoothHandler, mBluetoothAdapter); // Initialize the BluetoothChatService to perform Bluetooth connections
    }

    // 消息提示器
    public static void showToast(String message, int duration)
    {
        if (duration != Toast.LENGTH_SHORT && duration != Toast.LENGTH_LONG)
            throw new IllegalArgumentException();
        if (mToast != null)
            mToast.cancel(); // Close the toast if it's already open
        mToast = Toast.makeText(context, message, duration);
        mToast.show();
    }


    // The Handler class that gets information back from the BluetoothChatService
    // 主活动信息处理器，处理蓝牙返回数据
    static class BluetoothHandler extends Handler
    {
        private final MainActivity mMainActivity;
        private String mConnectedDeviceName; // Name of the connected device

        BluetoothHandler(MainActivity mMainActivity)
        {
            this.mMainActivity = mMainActivity;
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    mMainActivity.supportInvalidateOptionsMenu();
                    if (D)
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            MainActivity.showToast(mMainActivity.getString(R.string.connected_to) + " " + mConnectedDeviceName, Toast.LENGTH_SHORT);
                            if (mChatService == null)
                                return;
                            Handler mHandler = new Handler();
                            mHandler.postDelayed(new Runnable()
                            {
                                public void run()
                                {
                                    mChatService.write(getPIDValues + getSettings + getInfo + getKalman);
                                }
                            }, 1000); // Wait 1 second before sending the message

                            /* 不考虑GraphFragment和ViewPagerAdapter
                            if (GraphFragment.mToggleButton != null) {
                                if (GraphFragment.mToggleButton.isChecked() && checkTab(ViewPagerAdapter.GRAPH_FRAGMENT)) {
                                    mHandler.postDelayed(new Runnable()
                                    {
                                        public void run()
                                        {
                                            mChatService.write(imuBegin); // Request data
                                        }
                                    }, 1000); // Wait 1 second before sending the message
                                } else {
                                    mHandler.postDelayed(new Runnable()
                                    {
                                        public void run()
                                        {
                                            mChatService.write(imuStop); // Stop sending data
                                        }
                                    }, 1000); // Wait 1 second before sending the message
                                }
                            }
                            if (checkTab(ViewPagerAdapter.INFO_FRAGMENT)) {
                                mHandler.postDelayed(new Runnable()
                                {
                                    public void run()
                                    {
                                        mChatService.write(statusBegin); // Request data
                                    }
                                }, 1000); // Wait 1 second before sending the message
                            }*/

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                    }
//                    PIDFragment.updateButton();
                    break;
                case MESSAGE_READ:
                    if (newPIDValues) {
                        newPIDValues = false;
//                        PIDFragment.updateView();
                    }
                    if (newInfo || newStatus) {
                        newInfo = false;
                        newStatus = false;
//                        InfoFragment.updateView();
                    }
                    if (newIMUValues) {
                        newIMUValues = false;
//                        GraphFragment.updateIMUValues();
                    }
                    if (newKalmanValues) {
                        newKalmanValues = false;
//                        GraphFragment.updateKalmanValues();
                    }
                    if (pairingWithDevice) {
                        pairingWithDevice = false;
                        MainActivity.showToast("Now enable discovery of your device", Toast.LENGTH_LONG);
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // Save the connected device's name
                    if (msg.getData() != null)
                        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_DISCONNECTED:
                    mMainActivity.supportInvalidateOptionsMenu();
//                    PIDFragment.updateButton();
                    if (msg.getData() != null)
                        MainActivity.showToast(msg.getData().getString(TOAST), Toast.LENGTH_SHORT);
                    break;
                case MESSAGE_RETRY:
                    if (D)
                        Log.d(TAG, "MESSAGE_RETRY");
                    mMainActivity.connectDevice(null, true);
                    break;
            }
        }
    }

    // 监听蓝牙服务事项
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect to
                if (resultCode == Activity.RESULT_OK)
                    connectDevice(data, false);
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK)
                    setupBTService(); // Bluetooth is now enabled, so set up a chat session
                else {
                    // User did not enable Bluetooth or an error occured
                    if (D)
                        Log.d(TAG, "BT not enabled");
                    showToast(getString(R.string.bt_not_enabled_leaving), Toast.LENGTH_SHORT);
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean retry)
    {
        if (retry) {
            if (btDevice != null && !stopRetrying) {
                mChatService.start(); // This will stop all the running threads
                mChatService.connect(btDevice, btSecure); // Attempt to connect to the device
            }
        } else { // It's a new connection
            stopRetrying = false;
            mChatService.newConnection = true;
            mChatService.start(); // This will stop all the running threads
            if (data.getExtras() == null)
                return;

            // 获取设备蓝牙地址
            String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS); // Get the device Bluetooth address
            btSecure = data.getExtras().getBoolean(DeviceListActivity.EXTRA_NEW_DEVICE); // If it's a new device we will pair with the device
            btDevice = mBluetoothAdapter.getRemoteDevice(address); // Get the BluetoothDevice object

            mChatService.nRetries = 0; // Reset retry counter
            mChatService.connect(btDevice, btSecure); // Attempt to connect to the device
            showToast(getString(R.string.connecting), Toast.LENGTH_SHORT);
        }
    }
}
