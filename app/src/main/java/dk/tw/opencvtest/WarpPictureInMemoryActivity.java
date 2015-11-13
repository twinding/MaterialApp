package dk.tw.opencvtest;

import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class WarpPictureInMemoryActivity extends AppCompatActivity {

    MatOfPoint2f canonicalMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_warp_picture_in_memory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS ) {
                // OpenCV code can now be called
//                helloworld();
//                matchTemplate();
//                featureMatch();


                /*Point[] canonicalPoints = new Point[4];
                canonicalPoints[0] = new Point(0, 350);
                canonicalPoints[1] = new Point(0, 0);
                canonicalPoints[2] = new Point(350, 0);
                canonicalPoints[3] = new Point(350, 350);*/
                Point[] canonicalPoints = new Point[4];
                canonicalPoints[0] = new Point(0, -390);
                canonicalPoints[1] = new Point(0, 0);
                canonicalPoints[2] = new Point(-390, 0);
                canonicalPoints[3] = new Point(-390, -390);
                canonicalMarker = new MatOfPoint2f();
                canonicalMarker.fromArray(canonicalPoints);
                sjovMetode();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void helloworld() {
        // make a mat and draw something
        Mat m = Mat.zeros(100, 400, CvType.CV_8UC3);
        Imgproc.putText(m, "hi there ;)", new Point(30, 80), Core.FONT_HERSHEY_SCRIPT_SIMPLEX, 2.2, new Scalar(200, 200, 0), 2);

        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bm);

        // find the imageview and draw it!
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(bm);
    }

    public void matchTemplate() { //Does not work properly on rotated images
        String TAG = "OpenCVTest";
        Toast.makeText(this, "Running template matching", Toast.LENGTH_SHORT).show();

        Mat input = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/input4.png");
        Mat template = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/template2.png");
        Log.d(TAG, "input: " + input.rows() + " " + input.cols());
        Log.d(TAG, "template: " + template.rows() + " " + template.cols());

        int result_columns = input.cols() - template.cols() + 1;
        int result_rows = input.rows() - template.rows() + 1;
        Mat result = new Mat(result_rows, result_columns, CvType.CV_32FC1);

        Imgproc.matchTemplate(input, template, result, Imgproc.TM_SQDIFF);
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        Point matchLoc = mmr.minLoc;

        Imgproc.rectangle(input, matchLoc, new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), new Scalar(0, 255, 0));

        /*File output = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "output.jpg");
        if (!output.exists()) {
            try {
                output.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Imgcodecs.imwrite(output.getAbsolutePath(), input);*/


        Bitmap bm = Bitmap.createBitmap(input.cols(), input.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(input, bm);

        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(bm);
    }

    public void featureMatch() {
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        //Input image
        Mat input = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/input3.png", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        input.convertTo(input, CvType.CV_8U);
        MatOfKeyPoint inputKeypoints = new MatOfKeyPoint();
        Mat inputDescriptors = new Mat();

        detector.detect(input, inputKeypoints);
        extractor.compute(input, inputKeypoints, inputDescriptors);

        //Template image
        Mat template = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/template3.png", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        template.convertTo(template, CvType.CV_8U);
        MatOfKeyPoint templateKeypoints = new MatOfKeyPoint();
        Mat templateDescriptors = new Mat();

        detector.detect(template, templateKeypoints);
        extractor.compute(template, templateKeypoints, templateDescriptors);

        //Test
        Log.d("test", input.toString());
        Log.d("test", template.toString());
//        Log.d("test", "Input type: " + input.type() + " ## Input cols: " + input.cols());
//        Log.d("test", "Template type: " + template.type() + " ## Template cols: " + template.cols());


        //Matching
        MatOfDMatch matches = new MatOfDMatch();
        matcher.match(inputDescriptors, templateDescriptors, matches);

        //feature and connection colors
        Scalar RED = new Scalar(255,0,0);
        Scalar GREEN = new Scalar(0,255,0);
        //output image
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        //this will draw all matches, works fine
        Features2d.drawMatches(input, inputKeypoints, template, templateKeypoints, matches,
                outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);

        Bitmap imageMatched = Bitmap.createBitmap(outputImg.cols(), outputImg.rows(), Bitmap.Config.RGB_565);//need to save bitmap
        Utils.matToBitmap(outputImg, imageMatched);
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(imageMatched);


    }

    public void sjovMetode() {
        //Input image

//        Mat inputOriginal = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/marker1scaled.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        Mat inputOriginal = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/marker3scaled.jpg");
        Imgproc.cvtColor(inputOriginal, inputOriginal, Imgproc.COLOR_BGR2RGB); //Convert picture to RGB as it's loaded as BGR

        Mat input = new Mat();
        Imgproc.cvtColor(inputOriginal, input, Imgproc.COLOR_RGB2GRAY); //Convert image to grayscale
        input.convertTo(input, CvType.CV_8U); //Convert image to 8-bit depth

        Mat output = new Mat();
        Imgproc.threshold(input, output, 100/*magic number*/, 255, Imgproc.THRESH_BINARY); //Thresholding on image

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(output.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE); //Find contours on image

        Log.d("Test", "Contours size: "+contours.size());

        for (int i = 0; i < contours.size(); i++) {
            //Filter out markers with an area of # size, number is currently achieved by just testing
            if (Imgproc.contourArea(contours.get(i)) < 5000) {
                contours.remove(i);
                i--;
                continue;
            }

            MatOfPoint2f points = new MatOfPoint2f(contours.get(i).toArray());
            //http://docs.opencv.org/java/org/opencv/imgproc/Imgproc.html#approxPolyDP%28org.opencv.core.MatOfPoint2f,%20org.opencv.core.MatOfPoint2f,%20double,%20boolean%29
            //Approximates polygons, making polygons out of the contours we found
            Imgproc.approxPolyDP(points, points, 10, true);
            contours.set(i, new MatOfPoint(points.toArray()));

            if (contours.get(i).height() != 4) { //Only select polygons with 4 sides
                contours.remove(i);
                i--;
            }
        }

        Log.d("Test", "Contours size after work: " + contours.size());

        //Find the largest contour
        MatOfPoint largestContour = contours.get(0);
        for (int i = 1; i < contours.size(); i++) {
            MatOfPoint candidate = contours.get(i);
            if (Imgproc.contourArea(candidate) > Imgproc.contourArea(largestContour)) {
                largestContour = candidate;
            }
        }
        ArrayList<MatOfPoint> largestContourArrayList = new ArrayList<>();
        largestContourArrayList.add(largestContour);


        Log.d("Test", ""+ Imgproc.arcLength(new MatOfPoint2f(largestContourArrayList.get(0).toArray()), true));
        Log.d("Test", "Largest contour: " + largestContour.toString());

        //Find homography of the largest detected contour
        Mat hg = Calib3d.findHomography(new MatOfPoint2f(largestContour.toArray()), canonicalMarker);

        //Warp the picture according to the homography that was found. This achieves a warped
        //picture as if the picture was taken at a leveled position perpendicular to the plane
        Mat warpedPerspective = new Mat();
        Log.d("Test", "Input image size: " + inputOriginal.size());


        Imgproc.warpPerspective(inputOriginal, warpedPerspective, hg, new Size(inputOriginal.size().width / 2, inputOriginal.size().height / 2));

//        Imgproc.warpPerspective(inputOriginal, warpedPerspective, hg, new Size(700, 700));

//        Imgproc.rectangle(input, matchLoc, new Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), new Scalar(0, 255, 0));

        Imgproc.polylines(inputOriginal, largestContourArrayList, true, new Scalar(0, 255, 0), 10);

//        Bitmap imageMatched = Bitmap.createBitmap(inputOriginal.cols(), inputOriginal.rows(), Bitmap.Config.RGB_565);//need to save bitmap
//        Utils.matToBitmap(inputOriginal, imageMatched);
//        ImageView iv = (ImageView) findViewById(R.id.imageView);
//        iv.setImageBitmap(imageMatched);



        Bitmap imageMatched = Bitmap.createBitmap(warpedPerspective.cols(), warpedPerspective.rows(), Bitmap.Config.RGB_565);//need to save bitmap
        Utils.matToBitmap(warpedPerspective, imageMatched);
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(imageMatched);


    }
}
