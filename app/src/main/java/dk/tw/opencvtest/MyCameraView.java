package dk.tw.opencvtest;


import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;


import java.util.List;

public class MyCameraView extends JavaCameraView implements Camera.PictureCallback{

    Context context;

    public MyCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public MyCameraView(Context context, int cameraId) {
        super(context, cameraId);
        this.context = context;
    }

    public void takePicture() {
        mCamera.setPreviewCallback(null);
        Log.i("MyCameraView", mCamera.getParameters().getPictureSize().height + "x" + mCamera.getParameters().getPictureSize().width);
        mCamera.takePicture(null, null, this);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
        // return mCamera.getParameters().getPictureSize();
    }

    public void setResolution(int h,int w){
//        Camera.Parameters params = mCamera.getParameters();
//        params.setPreviewSize(mFrameWidth, mFrameHeight);
//        mCamera.setParameters(params); // mCamera is a Camera object
        disconnectCamera();
        mMaxHeight = h;
        mMaxWidth = w;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    public List<Camera.Size> getSupportedPictureSizes() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public void setPictureSize(int h, int w) {
        Camera.Parameters params = mCamera.getParameters();
        params.setPictureSize(w, h);
        mCamera.setParameters(params);
    }

    public Camera.Parameters getParameters() {
        return mCamera.getParameters();
    }

    public void setParameters(Camera.Parameters params) {
        mCamera.setParameters(params);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Log.i("MyCameraView", "onPictureTaken");
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        Log.i("MyCameraView", "Starting intent for ProcessPicture");
        Intent intent = new Intent(context, ProcessPictureActivity.class);
        intent.putExtra("pictureData", data);
        context.startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        takePicture();
        return super.onTouchEvent(event);
    }
}
