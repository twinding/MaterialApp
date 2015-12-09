package dk.tw.opencvtest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadFromInternalStorageActivity extends AppCompatActivity {

    private Mat gray, bilateral, canny, morphology, filledContours, imageWithGuidingSizes, eroded, warpedPerspective, imageWithModelFitting;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_from_internal_storage);

        filename = getIntent().getExtras().getString("fileToLoad");
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
                init(filename);
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    public void init(String fileToLoad) {
        try {
            eroded = new Mat();
            OpenCVDataContainer data = InternalStorageOperations.load(this, fileToLoad);
            gray = data.getGray();
            bilateral = data.getBilateral();
            canny = data.getCanny();
            morphology = data.getMorphology();
            filledContours = data.getFilledContours();
            imageWithGuidingSizes = data.getImageWithGuidingSizes();
            warpedPerspective = data.getWarpedPerspective();

            setImage(imageWithGuidingSizes);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
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
        menu.addSubMenu("Guiding sizes");
        menu.addSubMenu("Model fitting");
        menu.addSubMenu("Canny");
        menu.addSubMenu("Gray");
        menu.addSubMenu("Morphology");
        menu.addSubMenu("Filled contours");
        menu.addSubMenu("Bilateral filter");
        menu.addSubMenu("Eroded");
        menu.addSubMenu("Test geometry");
        menu.addSubMenu("Add geometry");


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            case "Warped perspective":
                Toast.makeText(this, "warped perspective", Toast.LENGTH_SHORT).show();
                setImage(warpedPerspective);
                break;
            case "Guiding sizes":
                Toast.makeText(this, "guiding sizes", Toast.LENGTH_SHORT).show();
                setImage(imageWithGuidingSizes);
                break;
            case "Model fitting":
                Toast.makeText(this, "model fitting", Toast.LENGTH_SHORT).show();
                if (imageWithModelFitting == null) {
                    Toast.makeText(this, "No model selected.", Toast.LENGTH_SHORT).show();
                    break;
                }
                setImage(imageWithModelFitting);
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
            case "Filled contours":
                Toast.makeText(this, "filledContours", Toast.LENGTH_SHORT).show();
                setImage(filledContours);
                break;
            case "Bilateral filter":
                Toast.makeText(this, "bilateralFilter", Toast.LENGTH_SHORT).show();
                setImage(bilateral);
                break;
            case "Eroded":
                if (eroded.height() <= 0 || eroded.width() <= 0) {
                    Toast.makeText(this, "Not available till a geometry has been tested", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "eroded", Toast.LENGTH_SHORT).show();
                    setImage(eroded);
                }
                break;
            case "Test geometry":
                pickGeometryToTestDialog().show();
                break;
            case "Add geometry":
                Intent intent = new Intent(this, DrawingActivity.class);
                intent.putExtra("filename", filename);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void testGeometryButton(View view) {
        pickGeometryToTestDialog().show();
    }

    public void addGeometryButton(View view) {
        Intent intent = new Intent(this, DrawingActivity.class);
        intent.putExtra("filename", filename);
        startActivity(intent);
    }

    public void menuButton(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.warpedPerspective:
                        setImage(warpedPerspective);
                        break;
                    case R.id.guidingSizes:
                        setImage(imageWithGuidingSizes);
                        break;
                    case R.id.modelFitting:
                        if (imageWithModelFitting == null) {
                            Toast.makeText(LoadFromInternalStorageActivity.this, "No model has been selected.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        setImage(imageWithModelFitting);
                        break;
                }
                return true;
            }
        });
        popupMenu.show();
    }

    /**
     * Creates a dialog that shows all files in the SVG asset folder and allows the user
     * to pick one
     * @return A Dialog that shows the list of SVGs and allows the user to pick one
     */
    private Dialog pickGeometryToTestDialog() {
        //StringContainer is used because it's not possible to change a String from an inner anonymous class
        final StringContainer selectedItem = new StringContainer("");
        //Get builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Title text for the dialog
        builder.setTitle("Pick a geometry to test");

        try {
            //Get list of SVGs
            final CharSequence[] assetFileList = this.getAssets().list("SVG");
            //Initially set selected item to item 0
            selectedItem.setString(assetFileList[0].toString());
            //Single choice list with the SVGS, updates the StringContainer when something is selected
            builder.setSingleChoiceItems(assetFileList, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedItem.setString(assetFileList[which].toString()); //Update selected item
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set a confirmation button on the dialog
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(LoadFromInternalStorageActivity.this, selectedItem.getString(), Toast.LENGTH_SHORT).show();
                testGeometry(selectedItem.getString());
            }
        });

        return builder.create();
    }

    /**
     * Tests whether a given geometry fits somewhere inside the scanned material
     * @param filename Filename of the file inside Assets folder to test
     */
    public void testGeometry(String filename) { //TODO Perhaps add some uncertainty factor, such as adding 2cm to width/height of the geometry
        try {
            SVG geometry = SVG.getFromAsset(this.getAssets(), "SVG/" + filename); //Retrieve SVG
            RectF viewBox = geometry.getDocumentViewBox(); //Get the viewBox of the SVG
            float scaleUp = 1.28205f; //Our magic scale number, since we determine 1px = 0.78mm
            //Scale the geometry which should be made with 1px = 1mm for ease of use
            float geometryWidth = (viewBox.right - viewBox.left) * scaleUp;
            float geometryHeight = (viewBox.bottom - viewBox.top) * scaleUp;

            //Structuring element constructed from the width and height of the SVG
            //Could be made more advanced in following the path of the SVG more closely for better fitting
            Mat structuringElement2 = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(geometryWidth, geometryHeight));
            Imgproc.erode(filledContours, eroded, structuringElement2); //Erosion using the structuring element
            if (Core.countNonZero(eroded) < 1) { //Check if anything is left after eroding, if there is, the SVG fits
                Toast.makeText(this, "Your model does not fit.", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(this, "Your model fits!", Toast.LENGTH_SHORT).show();

            //Constructing an image to show the user where the origin/centerpoint of their model can be placed to fit the model
            //Copy perspective transformed image
            imageWithModelFitting = warpedPerspective.clone();
            //Finding a contour to draw from the erosion image
            List<MatOfPoint> erosionContours = new ArrayList<>();
            Imgproc.findContours(eroded, erosionContours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            //Copy the perspective transformed image
            Mat erosionDrawing = imageWithModelFitting.clone();
            //Draw the filled contour on the second copy of the perspective transformed image
            Imgproc.drawContours(erosionDrawing, erosionContours, -1, new Scalar(0, 0, 255), -1);
            //Blend the two images, which creates a transparent look of the drawn contour
            Core.addWeighted(erosionDrawing, 0.3, imageWithModelFitting, 1 - 0.3, 0.0, imageWithModelFitting);
            //Show the image with the model fitting
            setImage(imageWithModelFitting);
        } catch (SVGParseException | IOException e) {
            e.printStackTrace();
        }
    }
}
