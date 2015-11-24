package dk.tw.opencvtest;

import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FindContoursActivity extends AppCompatActivity {

    private final String TAG = "FindContoursActivity";
    private Mat canny, blur, morphology, gray, filledContours, warpedPerspective, bilateral, eroded, h, s, v;
    int markerSize = 250;
    Bitmap inputPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_contours);

        if (getIntent().getExtras() != null) {
            Log.i(TAG, getIntent().getExtras().getString("pictureURI"));

            Uri uri = Uri.parse(getIntent().getExtras().getString("pictureURI"));

            try {
                inputPicture = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                Log.i(TAG, "Exception on creating BMP");
                e.printStackTrace();
            }
        }
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
                analyzePicture();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void analyzePicture() {
        warpedPerspective = new Mat();
        Utils.bitmapToMat(inputPicture, warpedPerspective);

        Mat hsv = new Mat();
        Imgproc.cvtColor(warpedPerspective, hsv, Imgproc.COLOR_RGB2HSV);
        List<Mat> channels = new ArrayList<>();
        Core.split(hsv, channels);
        h = channels.get(0);
        s = channels.get(1);
        v = channels.get(2);

        //Finding contours on warped picture
        //Each step has its own mat so we can display them
        //Some of these actions/procedures are destructive as well, which is why some are cloned
        canny = new Mat();
        blur = new Mat();
        morphology = new Mat();
        gray = new Mat();
        filledContours = new Mat();
        eroded = new Mat();

        Imgproc.cvtColor(warpedPerspective, gray, Imgproc.COLOR_RGB2GRAY); //Convert image to grayscale
//        Imgproc.blur(warpedPerspective.clone(), canny, new Size(5, 5)); //Blur image
        Imgproc.GaussianBlur(gray, blur, new Size(3, 3), 0); //Gaussian blur image

        bilateral = new Mat(gray.rows(), gray.cols(), CvType.CV_8UC1);
        int kernelLength = 23;
        Imgproc.bilateralFilter(gray.clone(), bilateral, kernelLength, kernelLength * 2, kernelLength / 2);

        Scalar imageMean = Core.mean(blur); //Mean of image
        Log.i("Image mean", "Mean of image: " + imageMean.toString());

        //Not used
        MatOfDouble mean = new MatOfDouble(), stddev = new MatOfDouble();
        Core.meanStdDev(blur, mean, stddev);
        double mu, sigma;
        mu = mean.get(0,0)[0];
        sigma = stddev.get(0,0)[0];
        Log.i("Image mean", "Mu: " + mu + ", sigma: " + sigma);

        //Canny edge detector threshold calculation
        //http://stackoverflow.com/questions/24672414/adaptive-parameter-for-canny-edge
        //With bilateral filter blur
        double cannyThresh = Imgproc.threshold(bilateral.clone(), new Mat(), 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgproc.Canny(bilateral.clone(), canny, cannyThresh * 0.25 /*mu - sigma*//*imageMean.val[0]*//*50*/, cannyThresh /*mu + sigma*//*imageMean.val[0] * 3 *//*150*/, 3, true); //http://www.kerrywong.com/2009/05/07/canny-edge-detection-auto-thresholding/

        //With gaussian blur
        /*double cannyThresh = Imgproc.threshold(blur.clone(), new Mat(), 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgproc.Canny(blur.clone(), canny, cannyThresh * 0.33 *//*mu - sigma*//**//*imageMean.val[0]*//**//*50*//*, cannyThresh *//*mu + sigma*//**//*imageMean.val[0] * 3 *//**//*150*//*, 3, true); //http://www.kerrywong.com/2009/05/07/canny-edge-detection-auto-thresholding/*/

        //http://dsp.stackexchange.com/questions/2564/opencv-c-connect-nearby-contours-based-on-distance-between-them/2618#2618
        //http://stackoverflow.com/questions/19123165/join-close-enough-contours-in-opencv
        //Computation heavy calculation
        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(4,4));
        Imgproc.morphologyEx(canny, morphology, Imgproc.MORPH_CLOSE, structuringElement, new Point(-1, -1), 2);

//        thinned = zhangSuenThinning(morphology);
//        thinned.convertTo(thinned, CvType.CV_8UC1);

        ArrayList<MatOfPoint> contours2 = new ArrayList<>();
        Mat hierarchy = new Mat();
//        Imgproc.findContours(canny.clone(), contours2, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE); //Find contours on image
        Imgproc.findContours(morphology.clone(), contours2, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE); //Find contours on image


//        for (int i = 0; i < contours2.size(); i++) {
//            Log.i("isBelow1Depth", "Hierarchy: " + hierarchy.get(0,i)[0] + ", "+ hierarchy.get(0, i)[1] + ", "+ hierarchy.get(0,i)[2] + ", "+ hierarchy.get(0, i)[3]);
//        }

        /*ArrayList<Integer> contoursBelow1depthIndices = new ArrayList<>();
        for (int i = 0; i < contours2.size(); i++) {
            boolean isBelow = isBelow1depth(i, 0, hierarchy);
            Log.i("isBelow1Depth", "Contour " + i + " below 1 depth:" + isBelow);
            if (isBelow) contoursBelow1depthIndices.add(i);
        }
        Collections.sort(contoursBelow1depthIndices, Collections.reverseOrder());
        Log.i("isBelow1Depth", "Sorted contours below 1 depth array " + contoursBelow1depthIndices.toString());
        for (Integer i : contoursBelow1depthIndices) {
            contours2.remove(i.intValue());
        }*/

        //Different approach, finding contours with CCOMP, trying removing all children, could be tried with parents as well
        ArrayList<Integer> contoursChildren = new ArrayList<>();
        for (int i = 0; i < contours2.size(); i++) {
            if (hierarchy.get(0, i)[2] == -1) continue;
            contoursChildren.add((int) hierarchy.get(0, i)[2]);
        }
        Collections.sort(contoursChildren, Collections.reverseOrder());
        Log.i("isBelow1Depth", "Sorted contours below 1 depth array " + contoursChildren.toString());
        for (Integer i : contoursChildren) {
            contours2.remove(i.intValue());
        }

        //Remove small contours, such as smudges being picked up
        Log.i("ProcessPictureActivity", "Size of found contours in warped picture: " + contours2.size());
        for (int i = 0; i < contours2.size(); i++) {
            Log.i("Warped contours", "Contour area for contour " + i + ": " + Imgproc.contourArea(contours2.get(i)));
            if (Imgproc.contourArea(contours2.get(i)) <= 10) {
                contours2.remove(i);
                i--;
            }
        }

        //Removing closely situated contours. Very computation heavy
        ArrayList<MatOfPoint> toBeRemoved = new ArrayList<>();
        for (MatOfPoint mat : contours2) {
            if (toBeRemoved.contains(mat)) continue;
            Point[] points = mat.toArray();
            for (Point point : points) {
                for (MatOfPoint contour : contours2) {
                    if (mat.equals(contour) || toBeRemoved.contains(contour)) continue;
                    MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                    double distance = Imgproc.pointPolygonTest(contour2f, point, true);
                    if (Math.abs(distance) < 1) {
                        MatOfPoint removalItem = Imgproc.contourArea(contour) > Imgproc.contourArea(mat) ? mat : contour;
//                        Log.i("Distance", "Distance between " + point.toString() + " and contour " + contour.toString() + " is " + distance);
                        if (!toBeRemoved.contains(removalItem)) toBeRemoved.add(removalItem);
                    }
                }
            }
        }

        contours2.removeAll(toBeRemoved);
        Log.i("ProcessPictureActivity", "Size of found contours in warped picture: " + contours2.size());

        //Find bounding boxes, draw them plus text showing their size converted to mm
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        float scale = 195f / markerSize; //Scale to convert to mm. One of the operands has to be a float, otherwise the result is just 1
        for (int i = 0; i < contours2.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contours2.get(i).toArray());
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            MatOfPoint points = new MatOfPoint(approxCurve.toArray());

            Rect rect = Imgproc.boundingRect(points);

            //Draw bounding boxes
            Imgproc.rectangle(warpedPerspective, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);
            //Calculate size of bounding box and draw it
            float rectWidth = roundFloat(rect.width * scale, 0);
            float rectHeight = roundFloat(rect.height * scale, 0);
            Log.i("NUMBERS", "scale: " + scale + ". Calculated rect size: " + rectWidth + "x" + rectHeight);
            Imgproc.putText(warpedPerspective, rectWidth + "x" + rectHeight, new Point(rect.x + rect.width / 3, rect.y + rect.height), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0), 3);
        }

        //Doing polygons on scrap material doesn't seem to work well
        /*for (int i = 0; i < contours2.size(); i++) {
            MatOfPoint2f points = new MatOfPoint2f(contours2.get(i).toArray());
            //http://docs.opencv.org/java/org/opencv/imgproc/Imgproc.html#approxPolyDP%28org.opencv.core.MatOfPoint2f,%20org.opencv.core.MatOfPoint2f,%20double,%20boolean%29
            //Approximates polygons, making polygons out of the contours we found
            Imgproc.approxPolyDP(points, points, 10, true);
            contours2.set(i, new MatOfPoint(points.toArray()));
        }*/

        //Writing the contours to SVG format
        double maxX = 0.0, maxY = 0.0;
        StringBuilder svgFile = new StringBuilder();
        svgFile.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" width=\"100%\" height=\"100%\" viewBox=\"#viewBox\">\r\n");
        for (MatOfPoint mat : contours2) {
            svgFile.append("<polygon fill=\"none\" stroke=\"#000000\" stroke-width=\"1\" points=\"");

            for (Point point : mat.toArray()) {
                if (point.x > maxX) maxX = point.x;
                if (point.y > maxY) maxY = point.y;
                svgFile.append(point.x);
                svgFile.append(",");
                svgFile.append(point.y);
                svgFile.append(" ");
            }
            svgFile.append("\" />\r\n");
        }
        svgFile.append("</svg>");
        Log.i("svgFile", svgFile.toString());

        String svgFileString = svgFile.toString();
        svgFileString = svgFileString.replace("#viewBox", "0 0 " + maxX + " " + maxY);
        Log.i("svgFileString", svgFileString);


        //Writing SVG to file
        File file = new File(Environment.getExternalStorageDirectory().toString()+ "/SVG/test.svg"); //TODO naming
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(svgFileString);
            fileWriter.close();
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Imgproc.drawContours(warpedPerspective, contours2, -1, new Scalar(0, 255, 0), 1/*, Imgproc.LINE_4, hierarchy, 0, new Point(0,0)*/);

        setImage(warpedPerspective);

        //Trying to fill the outermost contour so we could apply erosion to it, need to somehow
        //fill the outmost but not children
        //The above happens fine, as long as the outermost contour is actually closed
        filledContours = morphology.clone();
        Core.bitwise_not(filledContours, filledContours);
        Imgproc.drawContours(filledContours, contours2, -1, new Scalar(0, 255, 0), -1);
        Core.bitwise_not(filledContours, filledContours);
//        Mat structuringElement2 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(256, 256));
        Mat structuringElement2 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1024, 1024));
        Imgproc.erode(filledContours, eroded, structuringElement2);
        if (Core.countNonZero(eroded) < 1) {
            Toast.makeText(this, "Your model does not fit.", Toast.LENGTH_SHORT).show();
        } else Toast.makeText(this, "Your model fits!", Toast.LENGTH_SHORT).show();
    }

    public float roundFloat(float number, int decimals) {
        BigDecimal bd = new BigDecimal(Float.toString(number));
        bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public void setImage(Mat image) {
        Bitmap imageMatched = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);//need to save bitmap
        Utils.matToBitmap(image, imageMatched);
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(imageMatched);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.addSubMenu("Warped perspective");
        menu.addSubMenu("Canny");
        menu.addSubMenu("Gray");
        menu.addSubMenu("Morphology");
        menu.addSubMenu("Blur");
        menu.addSubMenu("Filled contours");
        menu.addSubMenu("Bilateral filter");
        menu.addSubMenu("Eroded");
        menu.addSubMenu("Hue");
        menu.addSubMenu("Saturation");
        menu.addSubMenu("Values");



        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            case "Warped perspective":
                Toast.makeText(this, "warped perspective", Toast.LENGTH_SHORT).show();
                setImage(warpedPerspective);
                break;
            case "Canny":
                Toast.makeText(this, "canny", Toast.LENGTH_SHORT).show();
                setImage(canny);
                break;
            case "Gray":
                Toast.makeText(this, "gray", Toast.LENGTH_SHORT).show();
                setImage(gray);
                break;
            case "Morphology":
                Toast.makeText(this, "morphology", Toast.LENGTH_SHORT).show();
                setImage(morphology);
                break;
            case "Blur":
                Toast.makeText(this, "blur", Toast.LENGTH_SHORT).show();
                setImage(blur);
                break;
            case "Filled contours":
                Toast.makeText(this, "filledContours", Toast.LENGTH_SHORT).show();
                setImage(filledContours);
                break;
            case "Eroded":
                Toast.makeText(this, "eroded", Toast.LENGTH_SHORT).show();
                setImage(eroded);
                break;
            case "Bilateral filter":
                Toast.makeText(this, "bilateralFilter", Toast.LENGTH_SHORT).show();
                setImage(bilateral);
                break;
            case "Hue":
                Toast.makeText(this, "hue", Toast.LENGTH_SHORT).show();
                setImage(h);
                break;
            case "Saturation":
                Toast.makeText(this, "saturation", Toast.LENGTH_SHORT).show();
                setImage(s);
                break;
            case "Values":
                Toast.makeText(this, "values", Toast.LENGTH_SHORT).show();
                setImage(v);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
