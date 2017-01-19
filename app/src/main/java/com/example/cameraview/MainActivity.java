package com.example.cameraview;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private TextView tv;
    private TextView secondsText;
    private OrientationEventListener orientationEventListener;

    private long timeWhenStopped = 0;
    private boolean stopClicked;
    private Chronometer chronometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // Переводим приложение в полный экран.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceHolderCallback);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        tv = (TextView) findViewById(R.id.textView);

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL){
                @Override
                public void onOrientationChanged(int arg0) {
                    // TODO Auto-generated method stub
                    tv.setText("Orientation: " + String.valueOf(arg0));


                }};

        if (!orientationEventListener.canDetectOrientation()){
            Toast.makeText(this, "Can't DetectOrientation", Toast.LENGTH_SHORT).show();
            finish();
        }

        lockScreenOrientation();
        final TextView tv1= (TextView) findViewById(R.id.textView1);
        tv1.setText(getIntent().getExtras().getString("user"));
        final TextView tv2= (TextView) findViewById(R.id.id);
        tv2.setText(getIntent().getExtras().getString("user1"));
        final TextView tv3= (TextView) findViewById(R.id.location_text);
        tv3.setText(getIntent().getExtras().getString("user2"));

        Button button= (Button) findViewById(R.id.textView2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!stopClicked)  {
                    secondsText = (TextView) findViewById(R.id.chronometer);
                    timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
                    int seconds = (int) timeWhenStopped / 1000;
                    secondsText.setText( Math.abs(seconds) + " seconds");
                    chronometer.stop();
                    stopClicked = true;
                }
                Intent inte=new Intent(MainActivity.this,Main3Activity.class);
                inte.putExtra("user5",tv1.getText().toString());
                inte.putExtra("user6",tv2.getText().toString());
                inte.putExtra("user7",tv3.getText().toString());
                inte.putExtra("user8",secondsText.getText().toString());
                startActivity(inte);
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }
        });
        chronometer = (Chronometer) findViewById(R.id.chronometer);


    }
    public void startButtonClick(View v) {
        chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        chronometer.start();
        stopClicked = false;

    }


    @Override
    protected void onResume() {
        super.onResume();

        initializeCamera();
        if (orientationEventListener.canDetectOrientation())
            orientationEventListener.enable();
    }

    // Обработка выключения камеры
    @Override
    protected void onPause() {
        super.onPause();

        stopCamera();
        orientationEventListener.disable();
    }

    // Обработка отрисовки предпросмотра.
    SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (initializeCamera())
                setOptimalPreview(200, 200);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            refreshCamera(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopCamera();
        }
    };

    private boolean initializeCamera() {
        if (hasCamera(this)) {
            if (camera != null)
                camera.release();

            try {
                camera = Camera.open(); // Может выдать ошибку "Fail to connect to camera service".
            } catch (RuntimeException e) {
                e.printStackTrace();
                Toast.makeText(this, "Невозможно получить доступ к видеокамере."
                        + "\nПопробуйте перезагрузить устройство", Toast.LENGTH_LONG).show();
                finish();
                return false;
            }
        } else {
            Toast.makeText(this, "Извините, на устройстве не обнаружено видеокамеры.",
                    Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private void stopCamera() {
        if (camera != null) {
            // Останавливаем предпросмотр.
            camera.stopPreview();
            // Освобождаем камеру для использования другими приложениями.
            camera.release();
            camera = null;
        }
    }

    private void refreshCamera(int width, int height) {
        if (camera != null) {
            try {
                camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }

            setOptimalPreview(width, height);
        }
    }

    // Установить оптимальное соотношение сторон изображения.
    private void setOptimalPreview(int width, int height) {
        if (surfaceHolder.getSurface() != null && camera != null) {
            Camera.Parameters param = camera.getParameters();
            param.setPreviewSize(width, height);
            // Определим список доступных разрешений экрана.
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            if (sizes != null) {
                int displayOrientation = getScreenOrientation();
                Camera.Size optimalSize = getOptimalPreviewSize(sizes, width, height, displayOrientation);
                if (optimalSize != null) {
                    param.setPreviewSize(optimalSize.width, optimalSize.height);
                    setCameraDisplayOrientation(MainActivity.this, 0, camera);
                    try {
                        camera.setParameters(param); // Может выдать: "RuntimeException: setParameters failed".
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    camera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                camera.startPreview();
            }
        }
    }

    // Имеет ли устройство камеру.
    private boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Определить лучшее соотношение сторон изображения, чтобы объекты на экране имели пропорции,
    // близкие к реальным.
    //
    // Проблема решена в http://stackoverflow.com/questions/17126633/camera-setdisplayorientation-in-portrait-mode-breaks-aspect-ratio.
    // Используется код https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/graphics/CameraPreview.java
    // с изменениями, внесёнными https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height, int displayOrientation) {
        final double ASPECT_TOLERANCE = 0.1;

        if (sizes == null || height == 0)
            return null;
        double targetRatio = (double) width / height;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = height;
        if (displayOrientation == 90 || displayOrientation == 270)
            targetRatio = (double) height / width;

        // Попытаемся найти оптимальное соотношение сторон.
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) <= ASPECT_TOLERANCE)
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
        }
        // Если невозможно найти близкое соотношение сторон, найдём близкие длины сторон.
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    // Обработка поворота изображения при повороте камеры.
    @TargetApi(9)
    private void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        // Определим, насколько повёрнута камера от нормального положения.
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int result = 0;
        // Для передней и задней камеры по-разному считаются повороты.
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            result = (360 - degrees) - info.orientation + 360;
        else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            result = (360 - degrees) + info.orientation;

        //tv.setText(String.valueOf(result) + ", " + String.valueOf(degrees));
        result %= 360;
        camera.setDisplayOrientation(result);
    }

    // Определить ориентацию устройства.
    // http://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164#10383164
    private int getScreenOrientation() {
        if (Build.VERSION.SDK_INT < 9)
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width
                || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    //Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                    //        "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    //Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                    //        "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

    private void lockScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
}