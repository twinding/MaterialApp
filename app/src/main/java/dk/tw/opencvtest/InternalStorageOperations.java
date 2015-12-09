package dk.tw.opencvtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Environment;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class InternalStorageOperations {
    public static boolean saveExternal = false;

    public static void save(Context context, String filename, String contents, Mat gray, Mat bilateral, Mat canny, Mat morphology, Mat filledContours, Mat imageWithGuidingSizes, Mat warpedPerspective) {
        if (saveExternal) {
            saveExternal(gray, "gray.png", context);
            saveExternal(bilateral, "bilateral.png", context);
            saveExternal(canny, "canny.png", context);
            saveExternal(bilateral, "bilateral.png", context);
            saveExternal(morphology, "morphology.png", context);
            saveExternal(filledContours, "filledContours.png", context);
            saveExternal(imageWithGuidingSizes, "imageWithGuidingSizes.png", context);
            saveExternal(warpedPerspective, "warpedPerspective.png", context);
        }

        HashMap<String, SerializableMat> matHashMap = new HashMap<>();
        matHashMap.put("gray", new SerializableMat(gray));
        matHashMap.put("bilateral", new SerializableMat(bilateral));
        matHashMap.put("canny", new SerializableMat(canny));
        matHashMap.put("morphology", new SerializableMat(morphology));
        matHashMap.put("filledContours", new SerializableMat(filledContours));
        matHashMap.put("imageWithGuidingSizes", new SerializableMat(imageWithGuidingSizes));
        matHashMap.put("warpedPerspective", new SerializableMat(warpedPerspective));

        try {
            //Make directories
            File directory = new File(context.getFilesDir() + "/opencvdata");
            directory.mkdirs(); //Make the /opencvdata directory
            directory = new File(context.getFilesDir() + "/svg");
            directory.mkdirs(); //Make the /svg directory

            //Write the matHashMap to a file in the /opencvdata directory
            File file = new File(context.getFilesDir(), "opencvdata/" + filename);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(matHashMap);
            fos.close();
            oos.close();

            //Write the SVG to the /svg directory
            file = new File(context.getFilesDir(), "svg/" + filename + ".svg");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(contents);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveSVG(Context context, String filename, String contents) {
        try {
            File directory = new File(context.getFilesDir() + "/cutReadySVGs");
            directory.mkdirs();

            File file = new File(context.getFilesDir(), "cutReadySVGs/" + filename + ".svg");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(contents);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static OpenCVDataContainer load(Context context, String filename) throws IOException, ClassNotFoundException {
        /*File svgDir = new File(context.getFilesDir(), "svg");
        File opencvdataDir = new File(context.getFilesDir(), "opencvdata");
        Log.i("IntStoOperations", "svg: " + Arrays.toString(svgDir.list()));
        Log.i("IntStoOperations", "opencvdata: " + Arrays.toString(opencvdataDir.list()));*/
        File opencvdata = new File(context.getFilesDir(), "opencvdata/" + filename);
        FileInputStream fis = new FileInputStream(opencvdata);
        ObjectInputStream ois = new ObjectInputStream(fis);
        return convertToOpenCVDataContainer((HashMap<String, SerializableMat>) ois.readObject());
    }

    private static byte[] getBytes(Mat mat) {
        byte[] bytes = new byte[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, bytes);
        return bytes;
    }

    private static OpenCVDataContainer convertToOpenCVDataContainer(HashMap<String, SerializableMat> map) {
        Mat gray, bilateral, canny, morphology, filledContours, imageWithGuidingSizes, warpedPerspective;

        SerializableMat sMat = map.get("gray");
        gray = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        gray.put(0, 0, sMat.getBytes());

        sMat = map.get("bilateral");
        bilateral = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        bilateral.put(0, 0, sMat.getBytes());

        sMat = map.get("canny");
        canny = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        canny.put(0, 0, sMat.getBytes());

        sMat = map.get("morphology");
        morphology = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        morphology.put(0, 0, sMat.getBytes());

        sMat = map.get("filledContours");
        filledContours = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        filledContours.put(0, 0, sMat.getBytes());

        sMat = map.get("imageWithGuidingSizes");
        imageWithGuidingSizes = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        imageWithGuidingSizes.put(0, 0, sMat.getBytes());

        sMat = map.get("warpedPerspective");
        warpedPerspective = new Mat(sMat.getRows(), sMat.getCols(), sMat.getType());
        warpedPerspective.put(0, 0, sMat.getBytes());

        return new OpenCVDataContainer(gray, bilateral, canny, morphology, filledContours, imageWithGuidingSizes, warpedPerspective);
    }

    private static class SerializableMat implements Serializable {

        private byte[] bytes;
        private int cols, rows, type;

        public SerializableMat(Mat mat) {
            bytes = InternalStorageOperations.getBytes(mat);
            cols = mat.cols();
            rows = mat.rows();
            type = mat.type();
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getCols() {
            return cols;
        }

        public int getRows() {
            return rows;
        }

        public int getType() {
            return type;
        }
    }

    public static void saveExternal(Mat mat, String filename, Context context) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        File externalDirectory = new File(Environment.getExternalStorageDirectory() + "/OpenCvOut");
        if (!externalDirectory.exists()) externalDirectory.mkdirs();

        File destination = new File(externalDirectory, filename);
        try {
            FileOutputStream out = new FileOutputStream(destination);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaScannerConnection.scanFile(context, new String[]{destination.getAbsolutePath()}, null, null);
    }
}
