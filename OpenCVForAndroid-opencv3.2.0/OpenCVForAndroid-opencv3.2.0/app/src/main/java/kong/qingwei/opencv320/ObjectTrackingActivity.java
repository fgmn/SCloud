package kong.qingwei.opencv320;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.kongqw.ObjectTrackingView;
import com.kongqw.listener.OnCalcBackProjectListener;
import com.kongqw.listener.OnObjectTrackingListener;
import com.kongqw.listener.OnOpenCVLoadListener;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.concurrent.TimeUnit;

public class ObjectTrackingActivity extends BaseActivity {

    private static final String TAG = "RobotTrackingActivity";
    // 在OpenCVLib320中
    private ObjectTrackingView objectTrackingView;
    private ImageView imageView;
    private Bitmap bitmap;

    private Runnable mRunnable;
    private Handler mHandler = new Handler();

    private boolean isSync = false;
    private int matCols = 1280;
    private int matRows = 720;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_object_tracking);

        imageView = (ImageView) findViewById(R.id.image_view);

        objectTrackingView = (ObjectTrackingView) findViewById(R.id.tracking_view);

        // 启动OpenCV监视器
        objectTrackingView.setOnOpenCVLoadListener(new OnOpenCVLoadListener() {
            @Override
            public void onOpenCVLoadSuccess() {
                Toast.makeText(getApplicationContext(), "OpenCV 加载成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onOpenCVLoadFail() {
                Toast.makeText(getApplicationContext(), "OpenCV 加载失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNotInstallOpenCVManager() {
                showInstallDialog();
            }   // 提示未下载OpenCVManager
        });

        // 显示反投影图(调试用)
        objectTrackingView.setOnCalcBackProjectListener(new OnCalcBackProjectListener() {
            @Override
            public void onCalcBackProject(final Mat backProject) {
                Log.i(TAG, "onCalcBackProject: " + backProject);
                ObjectTrackingActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (null == bitmap) {
                            bitmap = Bitmap.createBitmap(backProject.width(), backProject.height(), Bitmap.Config.ARGB_8888);
                        }
                        Utils.matToBitmap(backProject, bitmap);
                        // 显示位图
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });

        // 目标检测回调
        objectTrackingView.setOnObjectTrackingListener(new OnObjectTrackingListener() {
            @Override
            public void onObjectLocation(Point center) {
                Log.i(TAG, "onObjectLocation: 目标位置 [" + center.x + ", " + center.y + "]");

                /* 发送蓝牙指令
                mRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {

                        mHandler.postDelayed(this, 150); // Send data every 150ms
                        if (MainActivity.mChatService == null) {
                            Log.d("unaughty", "mChatService is null.");
                            return;
                        }

                        if (MainActivity.mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                                // 通过蓝牙发送命令
                                String message = MainActivity.sendTrackedObject + center.x + ',' + center.y + ";";
                                MainActivity.mChatService.write(message);
                                Log.d("unaughty", "run: " + message);
                        }
                    }
                };
                */

                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (MainActivity.mChatService == null) {
                    Log.d("unaughty", "mChatService is null.");
                    return;
                }

                if (MainActivity.mChatService.getState() == BluetoothChatService.STATE_CONNECTED) {
                    // 通过蓝牙发送命令
                    String message;
                    if (!isSync) {
                        message = MainActivity.sendMatSize + matCols + ',' + matRows + ";";
                        isSync = true;
                    }
                    message = MainActivity.sendTrackedObject + center.x + ',' + center.y + ";";
                    MainActivity.mChatService.write(message);
                    Log.d("unaughty", "run: " + message);
                }
            }

            @Override
            public void onObjectLost() {
                ObjectTrackingActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "目标丢失", Toast.LENGTH_SHORT).show();
                        imageView.setImageBitmap(null);
                    }
                });
            }
        });

        objectTrackingView.loadOpenCV(getApplicationContext());
    }

    /**
     * 切换摄像头
     *
     * @param view view
     */
    public void swapCamera(View view) {
        objectTrackingView.swapCamera();
    }
}
