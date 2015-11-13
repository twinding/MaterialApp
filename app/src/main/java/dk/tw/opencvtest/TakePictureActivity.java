package dk.tw.opencvtest;

import android.content.Context;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.List;

public class TakePictureActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private MyCameraView myCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
//        getSupportActionBar().hide();
        setContentView(R.layout.activity_take_picture);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        myCameraView = (MyCameraView) findViewById(R.id.camera);
        myCameraView.setVisibility(SurfaceView.VISIBLE);
        myCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myCameraView != null) myCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myCameraView != null) myCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("TakePictureActivity", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("TakePictureActivity", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("TakePictureActivity", "OpenCV loaded successfully");
                    myCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
//        Mat mRgba = new Mat(height, width, CvType.CV_8UC4);
//        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
//        mGray = new Mat(height, width, CvType.CV_8UC1);

        List<Camera.Size> mResolutionList = myCameraView.getResolutionList();
//        for (Camera.Size size : mResolutionList) {
//            Log.i("TakePictureActivity", "Resolution list " + size.width + "x" + size.height);
//        }



        int mFrameWidth = (int) mResolutionList.get(0).width;
        int mFrameHeight = (int) mResolutionList.get(0).height;

        Log.i("TakePictureActivity", "Setting resolution to " + mFrameHeight + "x" + mFrameWidth);

        myCameraView.setResolution(mFrameHeight, mFrameWidth);
        myCameraView.setPictureSize(mFrameHeight, mFrameWidth);

        /*Camera.Parameters params = myCameraView.getParameters();
        List<Camera.Size> resList = params.getSupportedPictureSizes();
        int listNum = 1;
        int picWidth = resList.get(listNum).width;
        int picHeight = resList.get(listNum).height;
        params.setPictureSize(picWidth, picHeight);
        myCameraView.setParameters(params);*/

//        Camera.Size largestPictureSize = myCameraView.getSupportedPictureSizes().get(0);
//        for (Camera.Size size : myCameraView.getSupportedPictureSizes()) {
//            if (largestPictureSize.height < size.height) largestPictureSize = size;
//        }
//
//        myCameraView.setPictureSize(largestPictureSize.height, largestPictureSize.width);
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }
}
