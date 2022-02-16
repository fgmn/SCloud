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

    public static String appVersion;


    public static boolean pairingWithDevice;

    public final static String sendStop = "CS;";

    // 追踪命令
    public final static String sendTrackedObject = "ST,";
    // 同步命令
    public final static String sendMatSize = "SM,";


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
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D)
            Log.d(TAG, "-- ON STOP --");
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
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (D)
            Log.d(TAG, "- ON PAUSE -");

        if (mChatService != null) { // Send stop command and stop sending graph data command
            if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
                mChatService.write(sendStop);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (D)
            Log.d(TAG, "+ ON RESUME +");
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
                return true;
            case android.R.id.home:
//                Log.d(TAG, "onOptionsItemSelected: Here is for home.");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fgmn"));
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
    // 主信息处理器，处理BluetoothChatService返回数据
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
                    // 解析第一个参数（蓝牙状态）
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            MainActivity.showToast(mMainActivity.getString(R.string.connected_to) + " " + mConnectedDeviceName, Toast.LENGTH_SHORT);
                            if (mChatService == null)
                                return;
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                    }
                    break;
                case MESSAGE_READ:
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
