package com.example.alex.spp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private Camera camera;
    private MediaRecorder mediaRecorder;
    private ImageButton recordImageButton;
    private SurfaceHolder holder;
    private boolean isRecording;
    private Thread timeThread;
    private TextView stopWatchText;
    private int degrees;
    private StopWatch sw;
    private int CAMERA_ID = 1;
    private boolean FULL_SCREEN = true;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private SharedPreferences sp;
    private int recordTimer;
    private boolean isExternalStorage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);
        recordImageButton = findViewById(R.id.imageButtonRecord);
        stopWatchText = findViewById(R.id.textViewStopWatch);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        sw = new StopWatch();
        isRecording = false;
        degrees = 0;
        holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                camera.stopPreview();
                setPreviewSize(FULL_SCREEN);

                setCameraDisplayOrientation(CAMERA_ID);
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

       timeThread = new Thread(){
           @Override
           public void run(){
               try{
                   while(true){
                       Thread.sleep(1000);
                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               long date = System.currentTimeMillis();
                               SimpleDateFormat sdf;
                               if(degrees == 0 || degrees == 180){
                                   sdf = new SimpleDateFormat("HH:mm:ss");
                               }
                               else{
                                   sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                               }
                               String dateString = sdf.format(date);
                               getSupportActionBar().setTitle(dateString);
                               if(isRecording){
                                   recordTimer++;
                                   stopWatchText.setText("REC: " + sw.toString());
                               }
                           }
                       });
                   }
               }
               catch(InterruptedException e){
               }
           }
       };
       timeThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent intent;

        switch (item.getItemId())
        {
            case R.id.gallery:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.settings:
                intent = new Intent(this, PrefActivity.class);
                startActivity(intent);
                return true;
            case R.id.exit:
                finish();
                System.exit(0);
                return  true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCameraHardware(this);

        FULL_SCREEN = sp.getBoolean("screenKey", true);

        String storage = sp.getString("storageKey", null);
        isExternalStorage = storage.equals("gallery") ? false : true;

        String camString = sp.getString("cameraKey", null);
        if(camString != null){
            if (Camera.getNumberOfCameras() > 1 && Integer.parseInt(camString) == 1) {
                CAMERA_ID = 1;
            }else{
                CAMERA_ID = 0;
            }
        }
        camera = Camera.open(CAMERA_ID);
        camera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        if (camera != null)
            camera.release();
        camera = null;
        isRecording = false;
        sw.stop();
        stopWatchText.setVisibility(View.INVISIBLE);
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        timeThread.interrupt();
    }

    public void onClickPicture(View view) {
        if(isRecording){
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                releaseMediaRecorder();
            }
            sw.stop();
            stopWatchText.setVisibility(View.INVISIBLE);
            camera.lock();
            isRecording = false;
            recordImageButton.setImageResource(R.drawable.record);
            Toast toast = Toast.makeText(this, "PAUSED", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        final File photoFile = getOutputMediaFile(1);
        Toast toast = Toast.makeText(this, photoFile.toString(), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    FileOutputStream fos = new FileOutputStream(photoFile);
                    fos.write(data);
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        camera.startPreview();
    }

    public void onClickRecord(View view){
        if(isRecording){
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                releaseMediaRecorder();
            }
            sw.stop();
            stopWatchText.setVisibility(View.INVISIBLE);
            isRecording = false;
            recordImageButton.setImageResource(R.drawable.record);
            Toast toast = Toast.makeText(this, "PAUSED", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        else{
            if (prepareVideoRecorder()) {
                mediaRecorder.start();
                isRecording = true;
                sw.start();
                stopWatchText.setVisibility(View.VISIBLE);
                recordImageButton.setImageResource(R.drawable.pause);
            } else {
                releaseMediaRecorder();
            }
        }
    }

    private boolean prepareVideoRecorder() {

        File videoFile = getOutputMediaFile(2);
        Toast toast = Toast.makeText(this, videoFile.toString(), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        camera.unlock();

        mediaRecorder = new MediaRecorder();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            finish();
            System.exit(0);
            return false;
        }
    }

    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.


        File mediaFile;

        if (type == MEDIA_TYPE_IMAGE){
            if(isExternalStorage){
                File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "DVROne/Photo");
                if (! mediaStorageDir.exists()){
                    if (! mediaStorageDir.mkdirs()){
                        Log.d("DVROne", "failed to create directory");
                        return null;
                    }
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
            }
            else{
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                mediaFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "IMG_"+ timeStamp + ".jpg");
            }
        } else if(type == MEDIA_TYPE_VIDEO) {
            if(isExternalStorage){
                File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "DVROne/Video");
                if (! mediaStorageDir.exists()){
                    if (! mediaStorageDir.mkdirs()){
                        Log.d("DVROne", "failed to create directory");
                        return null;
                    }
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
            }
            else{
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                mediaFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + File.separator + "VID_"+ timeStamp + ".mp4");
            }
        } else {
            return null;
        }

        // initiate media scan and put the new things into the path array to
        // make the scanner aware of the location and the files you want to see
        MediaScannerConnection.scanFile(this, new String[] {mediaFile.toString()}, null, null);

        return mediaFile;
    }

    void setPreviewSize(boolean fullScreen) {

        // get screen size
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // get camera preview size
        Camera.Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // screen's RectF
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // preview's RectF
        if (widthIsMax) {
            // horizontal preview
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // vertical preview
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // make ready matrix to convert
        if (!fullScreen) {
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // convert
        matrix.mapRect(rectPreview);

        // set surface size
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        //check screen rotation
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        degrees = 0;
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

        int result = 0;

        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // back camera
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // front camera
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }
}
