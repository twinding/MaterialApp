package dk.tw.opencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.soundcloud.android.crop.Crop;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProcessPictureActivity extends AppCompatActivity {

    private static final String TAG = "ProcessPictureActivity";
    MatOfPoint2f canonicalMarker;
    byte[] pictureData;
    private int markerSize = 250;

    Mat warpedPerspective, canny, blur, morphology, gray, thinned;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_picture);


        if (getIntent().getExtras() != null) {
           pictureData = (byte[]) getIntent().getExtras().get("pictureData");
        } else pictureData = (byte[]) savedInstanceState.get("pictureData");
        /*Bitmap bmp = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
        Log.i(TAG, "Picture dimensions: " + bmp.getHeight() + "x" + bmp.getWidth());
        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(bmp);*/
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bmp = imageView.getDrawingCache();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] pictureData = stream.toByteArray();

        outState.putByteArray("pictureData", pictureData);

        super.onSaveInstanceState(outState, outPersistentState);
    }*/

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
                Point[] canonicalPoints = new Point[4];
                canonicalPoints[0] = new Point(0, markerSize);
                canonicalPoints[1] = new Point(0, 0);
                canonicalPoints[2] = new Point(markerSize, 0);
                canonicalPoints[3] = new Point(markerSize, markerSize);
                canonicalMarker = new MatOfPoint2f();
                canonicalMarker.fromArray(canonicalPoints);
                pictureWarping();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void pictureWarping() {
        //Input image

//        Mat inputOriginal = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/marker1scaled.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
//        Mat inputOriginal = Imgcodecs.imread(Environment.getExternalStorageDirectory().getAbsolutePath() + "/marker3scaled.jpg");
        Mat inputOriginal = Imgcodecs.imdecode(new MatOfByte(pictureData), Imgcodecs.IMREAD_UNCHANGED);

        if (InternalStorageOperations.saveExternal) InternalStorageOperations.saveExternal(inputOriginal, "beforeProcessing.png", this);

        Imgproc.cvtColor(inputOriginal, inputOriginal, Imgproc.COLOR_BGR2RGB); //Convert picture to RGB as it's loaded as BGR

        Mat input = new Mat();
        Imgproc.cvtColor(inputOriginal, input, Imgproc.COLOR_RGB2GRAY); //Convert image to grayscale
        input.convertTo(input, CvType.CV_8U); //Convert image to 8-bit depth

        Mat output = new Mat();
        Imgproc.threshold(input, output, 100/*magic number*/, 255, Imgproc.THRESH_BINARY); //Thresholding on image

        if (InternalStorageOperations.saveExternal) InternalStorageOperations.saveExternal(output, "threshold.png", this);

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
        if (contours.size() <= 0) { //If the marker is not visible in the picture
            Toast.makeText(this, "Invalid picture, please try again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

        //Marker recognition?
        Mat unwarpedMarker = new Mat(50,50, CvType.CV_8U);
        Imgproc.warpPerspective(input, unwarpedMarker, hg, new Size(markerSize,markerSize));
        Imgproc.threshold(unwarpedMarker, unwarpedMarker, 100, 255, Imgproc.THRESH_BINARY);
        float cellSize = markerSize/6;
        boolean[][] markerCells = new boolean[6][6];
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 6; col++) {
                int cellX = (int) (col*cellSize);
                int cellY = (int) (row*cellSize);
                Mat cell = unwarpedMarker.submat(cellX, cellX + (int) cellSize, cellY, cellY + (int) cellSize);
                markerCells[row][col] = (Core.countNonZero(cell) > (cellSize * cellSize)/1.5);
            }
        }

        String markerOutput = "######";
        for (int row = 0; row < 6; row++) {
            markerOutput += "\n";
            for (int col = 0; col < 6; col++) {
                if (markerCells[row][col]) markerOutput += "O";
                else markerOutput += "X";
            }
        }
        Log.i("MARKER", markerOutput);

        boolean[][] rotate180Signature = {
                {true,false},
                {false,false},
        },
                rotate90ccwSignature = {
                        {false,false},
                        {true,false},
                },
                rotate90cwSignature = {
                        {false,true},
                        {false,false},
                };

        //Grabbing the "signature" of the marker, the midmost four squares, to reduce error
        boolean[][] markerSignature = new boolean[2][2];
        for (int row = 0; row < 2; row++) {
            System.arraycopy(markerCells[row + 2], 2, markerSignature[row], 0, 2);
        }
        Log.i("MARKER", "Discovered marker signature: " + Arrays.deepToString(markerSignature));

        Point[] canonicalPoints = new Point[4];
        String markerRotation = "No rotation needed";

        if (Arrays.deepEquals(markerSignature, rotate180Signature)) {
            //rotate 180
//            Core.flip(inputOriginal, inputOriginal, -1);
            canonicalPoints[0] = new Point(markerSize, 0);
            canonicalPoints[1] = new Point(markerSize, markerSize);
            canonicalPoints[2] = new Point(0, markerSize);
            canonicalPoints[3] = new Point(0, 0);
            canonicalMarker = new MatOfPoint2f();
            canonicalMarker.fromArray(canonicalPoints);
            markerRotation = "Picture should be rotated 180 degrees";
        } else if (Arrays.deepEquals(markerSignature, rotate90ccwSignature)) {
            //rotate 90 counter-clockwise
//            Core.flip(inputOriginal.t(), inputOriginal, 0);
            canonicalPoints[0] = new Point(0, 0);
            canonicalPoints[1] = new Point(markerSize, 0);
            canonicalPoints[2] = new Point(markerSize, markerSize);
            canonicalPoints[3] = new Point(0, markerSize);
            canonicalMarker = new MatOfPoint2f();
            canonicalMarker.fromArray(canonicalPoints);
            markerRotation = "Picture should be rotated 90 degrees counter-clockwise";
        } else if (Arrays.deepEquals(markerSignature, rotate90cwSignature)) {
            //rotate 90 clockwise
//            Core.flip(inputOriginal.t(), inputOriginal, 1);
            canonicalPoints[0] = new Point(markerSize, markerSize);
            canonicalPoints[1] = new Point(0, markerSize);
            canonicalPoints[2] = new Point(0, 0);
            canonicalPoints[3] = new Point(markerSize, 0);

            canonicalMarker = new MatOfPoint2f();
            canonicalMarker.fromArray(canonicalPoints);
            markerRotation = "Picture should be rotated 90 degrees clockwise";
        }
        Log.i("MARKER", markerRotation);
        Log.i("MARKER", "Canonical points: " + Arrays.toString(canonicalPoints));
        Log.i("MARKER", "Canonical marker: " + Arrays.toString(canonicalMarker.toArray()));
        hg = Calib3d.findHomography(new MatOfPoint2f(largestContour.toArray()), canonicalMarker);


        /**
         *Calculating an offset matrix
         *http://stackoverflow.com/questions/6087241/opencv-warpperspective
         */
//        Mat matrix = new Mat(3, 3, CvType.CV_32FC1);
//        float[] data = {1, 0, -195/*X offset*/, 0, 1, -195/*Y offset*/, 0, 0, 1};
//        matrix.put(0,0, data);
//        matrix.convertTo(matrix, CvType.CV_64FC1);
//
//        Mat offsetMatrix = new Mat(3, 3, CvType.CV_64FC1);
//        //Matrix multiplication
//        Core.gemm(hg, matrix, 1, Mat.zeros(3, 3, CvType.CV_64FC1), 0, offsetMatrix, 0);

        Log.i("Homography", "Homography " + hg.dump());
//        Log.i("Homography", "Offset matrix " + offsetMatrix.dump());

        //Trying to find size of resulting picture
        //http://stackoverflow.com/questions/22220253/cvwarpperspective-only-shows-part-of-warped-image
        Mat P = new Mat(3, 4, CvType.CV_32FC1);
        float[] data2 = {0, 1920, 1920, 0,
                        0, 0, 1080, 1080,
                        1, 1, 1, 1};
        P.put(0,0, data2);
        P.convertTo(P, CvType.CV_64FC1);

        Mat Pmark = new Mat(3, 4, CvType.CV_64FC1);
        Core.gemm(hg, P, 1, Mat.zeros(3, 3, CvType.CV_64FC1), 0, Pmark, 0);

        Log.i("Homography", "Pmark " + Pmark.dump());

        for (int row = 0; row < Pmark.rows()-1; row++) {
            for (int col = 0; col < Pmark.cols(); col++) {
                Pmark.put(row, col, Pmark.get(row, col)[0] / Pmark.get(Pmark.rows()-1, col)[0]);
            }
        }
        Log.i("Homography", "Pmark with stuff done" + Pmark.dump());

        double minX = Double.MAX_VALUE, maxX = 0, minY = Double.MAX_VALUE, maxY = 0, tmp;
        for (int col = 0; col < Pmark.cols(); col++) {
            tmp = Pmark.get(0, col)[0];
            if (tmp < minX) minX = tmp;
            if (tmp > maxX) maxX = tmp;
        }

        for (int col = 0; col < Pmark.cols(); col++) {
            tmp = Pmark.get(1, col)[0];
            if (tmp < minY) minY = tmp;
            if (tmp > maxY) maxY = tmp;
        }
        Log.i("Homography", "MinX: " + minX + ", MaxX: " + maxX + ", MinY: " + minY + ", MaxY: " + maxY);

        //Supposedly should show the whole picture, but it makes the warp wrong
//        if (minX < 0) {
//            double[] val = hg.get(0,2);
//            hg.put(0, 2, val[0]-minX);
//        }
//        if (minY < 0) {
//            double[] val = hg.get(1,2);
//            hg.put(0, 2, val[0]-minY);
//        }

        //Warp the picture according to the homography that was found. This achieves a warped
        //picture as if the picture was taken at a leveled position perpendicular to the plane
        warpedPerspective = new Mat();
        Log.d("Test", "Input image size: " + inputOriginal.size());
//        Imgproc.warpPerspective(inputOriginal, warpedPerspective, hg, new Size(inputOriginal.size().width, inputOriginal.size().height));
        Imgproc.warpPerspective(inputOriginal, warpedPerspective, hg, new Size(maxX - minX, maxY - minY));

        //TODO Crop at this point? Should we draw a rectangle to show how well the warp succeeded?

        setImage(warpedPerspective);

        if (InternalStorageOperations.saveExternal) InternalStorageOperations.saveExternal(warpedPerspective, "warpedNoText.png", this);






//        //remove marker part of image
//        Rect cropRect = new Rect(markerSize,markerSize,warpedPerspective.width()-markerSize,warpedPerspective.height()-markerSize);
//        warpedPerspective = new Mat(warpedPerspective, cropRect);
//
//        //What did this do? Probably draw on original picture
//        Imgproc.polylines(inputOriginal, largestContourArrayList, true, new Scalar(0, 255, 0), 10);
//
//        //Finding contours on warped picture
//        canny = new Mat();
        blur = new Mat();
//        morphology = new Mat();
        gray = new Mat();
//        thinned = new Mat();
        Imgproc.cvtColor(warpedPerspective, gray, Imgproc.COLOR_RGB2GRAY); //Convert image to grayscale
////        Imgproc.blur(warpedPerspective.clone(), canny, new Size(5, 5)); //Blur image
        Imgproc.GaussianBlur(gray, blur, new Size(3, 3), 0); //Gaussian blur image
//
        Scalar imageMean = Core.mean(blur); //Mean of image
        Log.i("Image mean", "Mean of image: " + imageMean.toString());
//
//        //Not used
//        MatOfDouble mean = new MatOfDouble(), stddev = new MatOfDouble();
//        Core.meanStdDev(blur, mean, stddev);
//        double mu, sigma;
//        mu = mean.get(0,0)[0];
//        sigma = stddev.get(0,0)[0];
//        Log.i("Image mean", "Mu: " + mu + ", sigma: " + sigma);
//
//        //Canny edge detector
//        Imgproc.Canny(blur.clone(), canny, /*mu - sigma*/imageMean.val[0]/*50*/, /*mu + sigma*/imageMean.val[0] * 3 /*150*/, 3, true); //http://www.kerrywong.com/2009/05/07/canny-edge-detection-auto-thresholding/
//
//        //http://dsp.stackexchange.com/questions/2564/opencv-c-connect-nearby-contours-based-on-distance-between-them/2618#2618
//        //http://stackoverflow.com/questions/19123165/join-close-enough-contours-in-opencv
//        //Computation heavy calculation
//        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(4,4));
//        Imgproc.morphologyEx(canny, morphology, Imgproc.MORPH_CLOSE, structuringElement, new Point(-1, -1), 2);
//
////        thinned = zhangSuenThinning(morphology);
////        thinned.convertTo(thinned, CvType.CV_8UC1);
//
//        ArrayList<MatOfPoint> contours2 = new ArrayList<>();
//        Mat hierarchy = new Mat();
////        Imgproc.findContours(canny.clone(), contours2, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE); //Find contours on image
//        Imgproc.findContours(morphology.clone(), contours2, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE); //Find contours on image
//
//
////        for (int i = 0; i < contours2.size(); i++) {
////            Log.i("isBelow1Depth", "Hierarchy: " + hierarchy.get(0,i)[0] + ", "+ hierarchy.get(0, i)[1] + ", "+ hierarchy.get(0,i)[2] + ", "+ hierarchy.get(0, i)[3]);
////        }
//
//        /*ArrayList<Integer> contoursBelow1depthIndices = new ArrayList<>();
//        for (int i = 0; i < contours2.size(); i++) {
//            boolean isBelow = isBelow1depth(i, 0, hierarchy);
//            Log.i("isBelow1Depth", "Contour " + i + " below 1 depth:" + isBelow);
//            if (isBelow) contoursBelow1depthIndices.add(i);
//        }
//        Collections.sort(contoursBelow1depthIndices, Collections.reverseOrder());
//        Log.i("isBelow1Depth", "Sorted contours below 1 depth array " + contoursBelow1depthIndices.toString());
//        for (Integer i : contoursBelow1depthIndices) {
//            contours2.remove(i.intValue());
//        }*/
//
//        //Different approach, finding contours with CCOMP, trying removing all children, could be tried with parents as well
//        ArrayList<Integer> contoursChildren = new ArrayList<>();
//        for (int i = 0; i < contours2.size(); i++) {
//            if (hierarchy.get(0, i)[2] == -1) continue;
//            contoursChildren.add((int) hierarchy.get(0, i)[2]);
//        }
//        Collections.sort(contoursChildren, Collections.reverseOrder());
//        Log.i("isBelow1Depth", "Sorted contours below 1 depth array " + contoursChildren.toString());
//        for (Integer i : contoursChildren) {
//            contours2.remove(i.intValue());
//        }
//
//        Log.i("ProcessPictureActivity", "Size of found contours in warped picture: " + contours2.size());
//        for (int i = 0; i < contours2.size(); i++) {
//            Log.i("Warped contours", "Contour area for contour " + i + ": " + Imgproc.contourArea(contours2.get(i)));
//            if (Imgproc.contourArea(contours2.get(i)) <= 10) {
//                contours2.remove(i);
//                i--;
//            }
//        }
//
//        //Removing closely situated contours. Very computation heavy
//        ArrayList<MatOfPoint> toBeRemoved = new ArrayList<>();
//        for (MatOfPoint mat : contours2) {
//            if (toBeRemoved.contains(mat)) continue;
//            Point[] points = mat.toArray();
//            for (Point point : points) {
//                for (MatOfPoint contour : contours2) {
//                    if (mat.equals(contour) || toBeRemoved.contains(contour)) continue;
//                    MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
//                    double distance = Imgproc.pointPolygonTest(contour2f, point, true);
//                    if (Math.abs(distance) < 1) {
//                        MatOfPoint removalItem = Imgproc.contourArea(contour) > Imgproc.contourArea(mat) ? mat : contour;
////                        Log.i("Distance", "Distance between " + point.toString() + " and contour " + contour.toString() + " is " + distance);
//                        if (!toBeRemoved.contains(removalItem)) toBeRemoved.add(removalItem);
//                    }
//                }
//            }
//        }
//
//        contours2.removeAll(toBeRemoved);
//        Log.i("ProcessPictureActivity", "Size of found contours in warped picture: " + contours2.size());
//
//        //Find bounding boxes, draw them plus text showing their size converted to mm
//        MatOfPoint2f approxCurve = new MatOfPoint2f();
//        float scale = 195f / markerSize; //Scale to convert to mm. One of the operands has to be a float, otherwise the result is just 1
//        for (int i = 0; i < contours2.size(); i++) {
//            MatOfPoint2f contour2f = new MatOfPoint2f(contours2.get(i).toArray());
//            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
//            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//
//            MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//
//            Rect rect = Imgproc.boundingRect(points);
//
//            //Draw bounding boxes
//            Imgproc.rectangle(warpedPerspective, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);
//            //Calculate size of bounding box and draw it
//            float rectWidth = roundFloat(rect.width * scale, 0);
//            float rectHeight = roundFloat(rect.height * scale, 0);
//            Log.i("NUMBERS", "scale: " + scale + ". Calculated rect size: " + rectWidth + "x" + rectHeight);
//            Imgproc.putText(warpedPerspective, rectWidth + "x" + rectHeight, new Point(rect.x + rect.width / 3, rect.y + rect.height), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0), 3);
//        }
//
//        //Doing polygons on scrap material doesn't seem to work well
//        /*for (int i = 0; i < contours2.size(); i++) {
//            MatOfPoint2f points = new MatOfPoint2f(contours2.get(i).toArray());
//            //http://docs.opencv.org/java/org/opencv/imgproc/Imgproc.html#approxPolyDP%28org.opencv.core.MatOfPoint2f,%20org.opencv.core.MatOfPoint2f,%20double,%20boolean%29
//            //Approximates polygons, making polygons out of the contours we found
//            Imgproc.approxPolyDP(points, points, 10, true);
//            contours2.set(i, new MatOfPoint(points.toArray()));
//        }*/
//
//        //Writing the contours to SVG format
//        StringBuilder svgFile = new StringBuilder();
//        svgFile.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">\r\n");
//        for (MatOfPoint mat : contours2) {
//            svgFile.append("<polygon fill=\"none\" stroke=\"#000000\" stroke-width=\"1\" points=\"");
//
//            for (Point point : mat.toArray()) {
//                svgFile.append(point.x);
//                svgFile.append(",");
//                svgFile.append(point.y);
//                svgFile.append(" ");
//            }
//            svgFile.append("\" />\r\n");
//        }
//        svgFile.append("</svg>");
//        Log.i("svgFile", svgFile.toString());
//
//        //Writing SVG to file
//        File file = new File(Environment.getExternalStorageDirectory().toString()+"/test.svg"); //TODO naming
//        try {
//            FileWriter fileWriter = new FileWriter(file);
//            fileWriter.write(svgFile.toString());
//            fileWriter.close();
//            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        Imgproc.drawContours(warpedPerspective, contours2, -1, new Scalar(0, 255, 0), 1/*, Imgproc.LINE_4, hierarchy, 0, new Point(0,0)*/);

//        setImage(warpedPerspective);



        //Crop stuff, for cropping the warped picture
        /*Bitmap imageMatched = Bitmap.createBitmap(warpedPerspective.cols(), warpedPerspective.rows(), Bitmap.Config.RGB_565);//need to save bitmap
        Utils.matToBitmap(warpedPerspective, imageMatched);

        File cropInput, cropOutput;
        FileOutputStream out = null;
        try {
            cropInput = File.createTempFile("cropInput", "bmp");
            out = new FileOutputStream(cropInput);
            imageMatched.compress(Bitmap.CompressFormat.PNG, 100, out);

            cropOutput = File.createTempFile("cropOutput", "bmp");

            Crop.of(Uri.fromFile(cropInput), Uri.fromFile(cropOutput)).withAspect(0,0).start(this);

            ImageView iv = (ImageView) findViewById(R.id.imageView);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(cropOutput.getAbsolutePath(), options);
            iv.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/


    }

    //When done button is pressed
    public void startCropping(View view) {
        try {
            Bitmap imageMatched = Bitmap.createBitmap(warpedPerspective.cols(), warpedPerspective.rows(), Bitmap.Config.RGB_565);//need to save bitmap
            Utils.matToBitmap(warpedPerspective, imageMatched);

            File cropInput = File.createTempFile("cropInput", "png");
            FileOutputStream fos = new FileOutputStream(cropInput);
            imageMatched.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Uri uri = Uri.fromFile(cropInput);

            Intent intent = new Intent(this, CropPictureActivity.class);
            intent.putExtra("pictureURI", uri.toString());
            startActivity(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void goBack(View view) {
        finish();
    }

    public float roundFloat(float number, int decimals) {
        BigDecimal bd = new BigDecimal(Float.toString(number));
        bd = bd.setScale(decimals, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    public Mat calcHistogram(Mat image) {
        List<Mat> images = new ArrayList<>();
        images.add(image);

        MatOfInt channels = new MatOfInt(0);
        Mat mask = new Mat();
        Mat hist = new Mat();
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat ranges = new MatOfFloat(0.0f, 256.0f);

        Imgproc.calcHist(images, channels, mask, hist, histSize, ranges);

        String result = "\n";
        for (int row = 0; row < hist.rows(); row++) {
            result += "\n";
            for (int col = 0; col < hist.cols(); col++) {
                result += hist.get(row, col);
            }
        }
        Log.i("Histogram", result);

        double med = -1.0;
        int bin = 0;
        for (int i = 0; i < 256 && med < 0.0; i++) {

        }

        /*// Create space for histogram image
        Mat histImage = Mat.zeros(100, (int)histSize.get(0, 0)[0], CvType.CV_8UC1);
        // Normalize histogram
        Core.normalize(hist, hist, 1, histImage.rows() , Core.NORM_MINMAX, -1, new Mat());
        // Draw lines for histogram points
        for(int i = 0; i < (int)histSize.get(0, 0)[0]; i++) {
            Imgproc.line(
                histImage,
                new org.opencv.core.Point(i, histImage.rows()),
                new org.opencv.core.Point(i, histImage.rows() - Math.round(hist.get(i,0)[0])),
                new Scalar( 255, 255, 255),
                1, 8, 0 );
        }*/
        return hist;
    }

    public boolean isBelow1depth(int index, int depth, Mat hierarchy) {
        if (depth > 1) return true;
        if (hierarchy.get(0, index)[3] == -1 && depth <= 1) return false;
        depth++;
        index = (int) hierarchy.get(0, index)[3];
        isBelow1depth(index, depth, hierarchy);
        return isBelow1depth(index, depth, hierarchy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.addSubMenu("Warped perspective");
        menu.addSubMenu("Canny");
        menu.addSubMenu("Gray");
        menu.addSubMenu("Morphology");
        menu.addSubMenu("Blur");
        /*menu.addSubMenu("Thinned");*/


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
            /*case "Thinned":
                Toast.makeText(this, "thinned", Toast.LENGTH_SHORT).show();
                setImage(thinned);
                break;*/
        }

        return super.onOptionsItemSelected(item);
    }

    public void setImage(Mat image) {
        Bitmap imageMatched = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.RGB_565);//need to save bitmap
        Utils.matToBitmap(image, imageMatched);
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        iv.setImageBitmap(imageMatched);
    }

    public Mat zhangSuenThinning(Mat input) {
        boolean bDone = false;
        int rows = input.rows();
        int cols = input.cols();
        Mat output = new Mat();

        input.convertTo(input, CvType.CV_32FC1);
        input.copyTo(output);
        output.convertTo(output, CvType.CV_32FC1);

        //pad source
        Mat p_enlarged_src = new Mat(rows + 2, cols + 2, CvType.CV_32FC1);
        for(int i = 0; i < (rows+2); i++) {
            p_enlarged_src.get(i, 0)[0] = 0.0f;
            p_enlarged_src.get(i, cols + 1)[0] = 0.0f;
        }
        for(int j = 0; j < (cols+2); j++) {
            p_enlarged_src.get(0, j)[0] = 0.0f;
            p_enlarged_src.get(rows + 1, j)[0] = 0.0f;
        }
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                if (input.get(i, j)[0] >= 20.0f) {
                    p_enlarged_src.get(i + 1, j + 1)[0] = 1.0f;
                }
                else p_enlarged_src.get(i + 1, j + 1)[0] = 0.0f;
            }
        }

        //Start thinning
        Mat p_thinMat1 = Mat.zeros(rows + 2, cols + 2, CvType.CV_32FC1);
        Mat p_thinMat2 = Mat.zeros(rows + 2, cols + 2, CvType.CV_32FC1);
        Mat p_cmp = Mat.zeros(rows + 2, cols + 2, CvType.CV_8UC1);

        while (!bDone) {
            /// sub-iteration 1
            ThinSubiteration1(p_enlarged_src, p_thinMat1);
            /// sub-iteration 2
            ThinSubiteration2(p_thinMat1, p_thinMat2);
            /// compare
            Core.compare(p_enlarged_src, p_thinMat2, p_cmp, Core.CMP_EQ);
            /// check
            int num_non_zero = Core.countNonZero(p_cmp);
            if(num_non_zero == (rows + 2) * (cols + 2)) {
                bDone = true;
            }
            /// copy
            p_thinMat2.copyTo(p_enlarged_src);
        }
        // copy result
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                output.get(i, j)[0] = p_enlarged_src.get(i + 1, j + 1)[0];
            }
        }
        return output;
    }

    public void ThinSubiteration1(Mat pSrc, Mat pDst) {
        int rows = pSrc.rows();
        int cols = pSrc.cols();
        pSrc.copyTo(pDst);
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                if(pSrc.get(i, j)[0] == 1.0f) {
                    /// get 8 neighbors
                    /// calculate C(p)
                    int neighbor0 = (int) pSrc.get( i-1, j-1)[0];
                    int neighbor1 = (int) pSrc.get( i-1, j)[0];
                    int neighbor2 = (int) pSrc.get( i-1, j+1)[0];
                    int neighbor3 = (int) pSrc.get( i, j+1)[0];
                    int neighbor4 = (int) pSrc.get( i+1, j+1)[0];
                    int neighbor5 = (int) pSrc.get( i+1, j)[0];
                    int neighbor6 = (int) pSrc.get( i+1, j-1)[0];
                    int neighbor7 = (int) pSrc.get( i, j-1)[0];
                    int C = (~neighbor1 & ( neighbor2 | neighbor3)) +
                    (~neighbor3 & ( neighbor4 | neighbor5)) +
                    (~neighbor5 & ( neighbor6 | neighbor7)) +
                    (~neighbor7 & ( neighbor0 | neighbor1));
                    if(C == 1) {
                        /// calculate N
                        int N1 = (neighbor0 | neighbor1) +
                        (neighbor2 | neighbor3) +
                        (neighbor4 | neighbor5) +
                        (neighbor6 | neighbor7);
                        int N2 = (neighbor1 | neighbor2) +
                        (neighbor3 | neighbor4) +
                        (neighbor5 | neighbor6) +
                        (neighbor7 | neighbor0);
                        int N = Math.min(N1, N2);
                        if ((N == 2) || (N == 3)) {
                            /// calculate criteria 3
                            int c3 = ( neighbor1 | neighbor2 | ~neighbor4) & neighbor3;
                            if(c3 == 0) {
                                pDst.get(i, j)[0] = 0.0f;
                            }
                        }
                    }
                }
            }
        }
    }

    void ThinSubiteration2(Mat pSrc, Mat pDst) {
        int rows = pSrc.rows();
        int cols = pSrc.cols();
        pSrc.copyTo(pDst);
        for(int i = 0; i < rows; i++) {
            for(int j = 0; j < cols; j++) {
                if (pSrc.get( i, j)[0] == 1.0f) {
                    /// get 8 neighbors
                    /// calculate C(p)
                    int neighbor0 = (int) pSrc.get( i-1, j-1)[0];
                    int neighbor1 = (int) pSrc.get( i-1, j)[0];
                    int neighbor2 = (int) pSrc.get( i-1, j+1)[0];
                    int neighbor3 = (int) pSrc.get( i, j+1)[0];
                    int neighbor4 = (int) pSrc.get( i+1, j+1)[0];
                    int neighbor5 = (int) pSrc.get( i+1, j)[0];
                    int neighbor6 = (int) pSrc.get( i+1, j-1)[0];
                    int neighbor7 = (int) pSrc.get( i, j-1)[0];
                    int C = (~neighbor1 & ( neighbor2 | neighbor3)) +
                    (~neighbor3 & ( neighbor4 | neighbor5)) +
                    (~neighbor5 & ( neighbor6 | neighbor7)) +
                    (~neighbor7 & ( neighbor0 | neighbor1));
                    if(C == 1) {
                        /// calculate N
                        int N1 = (neighbor0 | neighbor1) +
                        (neighbor2 | neighbor3) +
                        (neighbor4 | neighbor5) +
                        (neighbor6 | neighbor7);
                        int N2 = (neighbor1 | neighbor2) +
                        (neighbor3 | neighbor4) +
                        (neighbor5 | neighbor6) +
                        (neighbor7 | neighbor0);
                        int N = Math.min(N1,N2);
                        if((N == 2) || (N == 3)) {
                            int E = (neighbor5 | neighbor6 | ~neighbor0) & neighbor7;
                            if(E == 0) {
                                pDst.get(i, j)[0] = 0.0f;
                            }
                        }
                    }
                }
            }
        }
    }
}
