
package com.textureviewdemo;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import kr.gdg.android.textureview.R;

public class CameraActivity extends Activity {
    public static String TAG = "CameraActivity";
    public Camera camera;
    public int cameraId;
    SurfaceTexture surfaceTexture;
    MediaRecorder mediaRecorder = new MediaRecorder();
    boolean isRecord = false;
    private CameraSurfaceTextureListener mCameraSurfaceTextureListener;
    private OrientationEventListener mOrientationEventListener;
    private TextureView mTextureView;
    private Button record;
    private Button switchBtn;
    boolean backCamera = false;
    final int ALL_PERMISSIONS = 100;

    public boolean isBackCamera() {
        return backCamera;
    }

    public void setBackCamera(boolean backCamera) {
        this.backCamera = backCamera;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ALL_PERMISSIONS:
                Log.i("Camera", "G : " + grantResults[0]);
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                    mCameraSurfaceTextureListener = new CameraSurfaceTextureListener(
                            this);
                    mTextureView = (TextureView) findViewById(R.id.texture_view);
                    mTextureView
                            .setSurfaceTextureListener(mCameraSurfaceTextureListener);
                } else {
                    Toast.makeText(this, "Permission denied.",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    ALL_PERMISSIONS);
        } else {
            mCameraSurfaceTextureListener = new CameraSurfaceTextureListener(
                    this);
            mTextureView = (TextureView) findViewById(R.id.texture_view);
            mTextureView
                    .setSurfaceTextureListener(mCameraSurfaceTextureListener);
            record = findViewById(R.id.record);
            record.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!isRecord) {
                        isRecord = true;
                        record.setText("STOP");
                        CamcorderProfile camcorderProfile = CamcorderProfile.get(getCameraId(),CamcorderProfile.QUALITY_HIGH);
                        mediaRecorder = new MediaRecorder();
                        camera.unlock();
                        mediaRecorder.setCamera(camera);
                        try {
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                        }
                        catch(Exception e){
                            Log.d(TAG,"Camera not having a mic oriented in the same way. Use the default microphone");
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        }
                        try {
                            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                            String suffix = System.currentTimeMillis() + "";
                            File newFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + suffix.substring(suffix.length() - 5, suffix.length())+".mp4");
                            Log.d(TAG, "newFile path = " + newFile.getPath());
                            mediaRecorder.setOutputFile(newFile.getPath());
                            mediaRecorder.setVideoEncodingBitRate(camcorderProfile.videoBitRate);
                            mediaRecorder.setVideoFrameRate(camcorderProfile.videoFrameRate);
                            mediaRecorder.setVideoSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
                            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                            mediaRecorder.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.d(TAG, "EXCEPTION lock camera again");
                            camera.lock();
                        }
                        mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                            @Override
                            public void onError(MediaRecorder mediaRecorder, int what, int extra) {
                                Log.d(TAG, "onError = "+what+" , extra = "+extra);
                            }
                        });
                        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                            @Override
                            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                                Log.d(TAG, "onInfo = "+what+" , extra = "+extra);
                            }
                        });
                        mediaRecorder.start();
                    } else {
                        record.setText("RECORD");
                        Log.d(TAG, "RECORD COMPLETE");
                        isRecord = false;
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        camera.lock();
                    }
                }
            });
            switchBtn = findViewById(R.id.switchBtn);
            switchBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    stopPreview();
                    startPreview();
                    mediaRecorder.setCamera(getCamera());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, ListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.increase_alpha:
                mTextureView.setAlpha(mTextureView.getAlpha() + 0.1f);
                return true;
            case R.id.decrease_alpha:
                mTextureView.setAlpha(mTextureView.getAlpha() - 0.1f);
                return true;
            case R.id.rotate_left:
                mTextureView.setRotation(mTextureView.getRotation() - 5f);
                return true;
            case R.id.rotate_right:
                mTextureView.setRotation(mTextureView.getRotation() + 5f);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mOrientationEventListener == null) {
            mOrientationEventListener = new OrientationEventListener(this,
                    SensorManager.SENSOR_DELAY_NORMAL) {
                private int mOrientation;

                @Override
                public void onOrientationChanged(int orientation) {
                    int lastOrientation = mOrientation;

                    if (orientation >= 315 || orientation < 45) {
                        if (mOrientation != Surface.ROTATION_0) {
                            mOrientation = Surface.ROTATION_0;
                        }
                    } else if (orientation >= 45 && orientation < 135) {
                        if (mOrientation != Surface.ROTATION_90) {
                            mOrientation = Surface.ROTATION_90;
                        }
                    } else if (orientation >= 135 && orientation < 225) {
                        if (mOrientation != Surface.ROTATION_180) {
                            mOrientation = Surface.ROTATION_180;
                        }
                    } else if (mOrientation != Surface.ROTATION_270) {
                        mOrientation = Surface.ROTATION_270;
                    }

                    if (lastOrientation != mOrientation) {
                        Log.d(TAG, "rotation!!! lastOrientation:"
                                + lastOrientation + " mOrientation:"
                                + mOrientation + " orientaion:"
                                + orientation);
                    }
                }
            };
        }

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    CameraInfo cameraInfo;

    private void stopPreview(){
        if (getCamera() != null) {
            getCamera().stopPreview();
            getCamera().release();
            setCamera(null);
        }
    }

    private void startPreview() {
        cameraId = getBackCamera(isBackCamera());
        setCameraId(cameraId);
        camera = Camera.open(cameraId);
        cameraDisplayRotation();
        if (isBackCamera()) {
            setBackCamera(false);
        } else {
            setBackCamera(true);
        }
        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.getParameters().setPreviewSize(720, 1280);
            camera.startPreview();
            for(String focusMode : camera.getParameters().getSupportedFocusModes()){
                if(focusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                    camera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    Log.d(TAG, "SET TO FOCUS_MODE_CONTINUOUS_VIDEO");
                    break;
                }
            }
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public void cameraDisplayRotation() {
        final int rotation = getWindowManager()
                .getDefaultDisplay().getRotation();
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

        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private int getBackCamera(boolean backCam) {
        cameraInfo = new CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (!backCam) {
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    return i;
                }
            } else {
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
    }


    class CameraSurfaceTextureListener implements
            SurfaceTextureListener {
        private Camera mCamera;
        private CameraActivity mActivity;
        private CameraInfo mBackCameraInfo;
        int backCameraId;

        public CameraSurfaceTextureListener(Activity activity) {
            mActivity = (CameraActivity) activity;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            return true;
        }

        @Override
        public void onSurfaceTextureAvailable(
                SurfaceTexture surface,
                int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable!!!");
            backCameraId = getBackCamera(mActivity.isBackCamera());
            if (mActivity.isBackCamera()) {
                mActivity.setBackCamera(false);
            } else {
                mActivity.setBackCamera(true);
            }
            mActivity.setCameraId(backCameraId);
            mCamera = Camera.open(backCameraId);
            mActivity.setCamera(mCamera);
            cameraDisplayRotation();
            try {
                mCamera.setPreviewTexture(surface);
                mCamera.startPreview();
                surfaceTexture = surface;
                for(String focusMode : camera.getParameters().getSupportedFocusModes()){
                    if(focusMode.equalsIgnoreCase(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                        camera.getParameters().setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        Log.d(TAG, "SET TO FOCUS_MODE_CONTINUOUS_VIDEO 222");
                        break;
                    }
                }
            } catch (IOException ioe) {
                // Something bad happened
            }
        }

        public void cameraDisplayRotation() {
            final int rotation = mActivity.getWindowManager()
                    .getDefaultDisplay().getRotation();
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

            int result;
            if (mBackCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (mBackCameraInfo.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (mBackCameraInfo.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(result);
        }

        private int getBackCamera(boolean backCam) {
            CameraInfo cameraInfo = new CameraInfo();
            final int numberOfCameras = Camera.getNumberOfCameras();

            for (int i = 0; i < numberOfCameras; ++i) {
                Camera.getCameraInfo(i, cameraInfo);
                if (!backCam) {
                    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                        mBackCameraInfo = cameraInfo;
                        return i;
                    }
                } else {
                    if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                        mBackCameraInfo = cameraInfo;
                        return i;
                    }
                }
            }
            return -1;
        }

        public boolean isCameraOpen() {
            return mCamera != null;
        }
    }
}
